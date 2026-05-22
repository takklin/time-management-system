<template>
  <div class="ai-chat-drawer" v-if="isOpen">
    <!-- 抽屉头部 -->
    <div class="drawer-header">
      <div class="title-bar">
        <span class="title">🤖 AI 智能助手</span>
        <el-select v-model="selectedModel" size="small" @change="onModelChange" style="width: 120px">
          <el-option label="ChatGPT3.5" value="gpt-3.5" />
          <el-option label="DeepSeek" value="deepseek" />
        </el-select>
        <el-switch v-model="includeContext" active-text="附加待办上下文" inactive-text="不附加" size="small" style="margin-left:8px" />
        <el-button link size="small" @click="clearChatHistory" style="margin-left:8px">清空记录</el-button>
      </div>
      <button @click="isOpen = false" class="close-btn">✕</button>
    </div>

    <!-- 对话消息区 -->
    <div class="messages-container" ref="messagesContainer">
      <div v-for="msg in messages" :key="msg.id" :class="['message', msg.role]">
        <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
        <div class="content">
          <!-- 文本消息 -->
          <div v-if="msg.type === 'text'" class="text">
            <div v-if="msg.role === 'assistant'" v-html="renderMarkdown(msg.content)"></div>
            <div v-else>{{ msg.content }}</div>
          </div>
          
          <!-- 任务建议 -->
          <div v-else-if="msg.type === 'task'" class="task-suggestion">
            <h4>📝 建议的任务</h4>
            <el-form :model="msg.taskData" label-width="80px" size="small">
              <el-form-item label="标题">
                <el-input v-model="msg.taskData.title" />
              </el-form-item>
              <el-form-item label="起始时间">
                <el-date-picker 
                  v-model="msg.taskData.startTime"
                  type="datetime"
                  placeholder="选择日期时间"
                />
              </el-form-item>
              <el-form-item label="预估时长(分)">
                <el-input v-model.number="msg.taskData.estimatedMinutes" type="number" />
              </el-form-item>
              <el-button type="primary" size="small" @click="confirmTask(msg.taskData)" style="width: 100%">
                ✓ 添加到任务列表
              </el-button>
            </el-form>
          </div>

          <!-- 日程建议 -->
          <div v-else-if="msg.type === 'schedule'" class="schedule-suggestion">
            <h4>📅 建议的日程</h4>
            <el-form :model="msg.scheduleData" label-width="80px" size="small">
              <el-form-item label="标题">
                <el-input v-model="msg.scheduleData.title" />
              </el-form-item>
              <el-form-item label="开始时间">
                <el-date-picker v-model="msg.scheduleData.startTime" type="datetime" placeholder="选择开始时间" />
              </el-form-item>
              <el-form-item label="结束时间">
                <el-date-picker v-model="msg.scheduleData.endTime" type="datetime" placeholder="选择结束时间" />
              </el-form-item>
              <el-form-item label="提醒时间(分钟)">
                <el-input-number v-model.number="msg.scheduleData.reminderTime" :min="0" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="msg.scheduleData.description" type="textarea" :rows="3" />
              </el-form-item>
              <el-button type="primary" size="small" @click="confirmSchedule(msg.scheduleData)" style="width: 100%">
                ✓ 打开日程创建并填充
              </el-button>
            </el-form>
          </div>
          
          <!-- 加载动画 -->
          <div v-else-if="msg.type === 'loading'" class="loading">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷按钮 -->
    <div class="shortcuts">
      <button @click="quickAction('create')" class="shortcut-btn">✨ 创建任务</button>
      <button @click="quickAction('summary')" class="shortcut-btn">📊 今日总结</button>
      <button @click="quickAction('help')" class="shortcut-btn">❓ 帮助</button>
    </div>

    <!-- 输入框 -->
    <div class="input-area">
      <el-input 
        v-model="userInput"
        @keyup.enter="sendMessage"
        placeholder="输入你的需求或问题..."
        :disabled="loading"
        clearable
      />
      <el-button 
        @click="sendMessage" 
        type="primary"
        :loading="loading"
        size="small"
      >
        发送
      </el-button>
    </div>
  </div>

  <!-- 浮动按钮 -->
  <button v-else class="chat-fab" @click="isOpen = true" title="打开 AI 助手">
    💬
  </button>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { useRouter } from 'vue-router'
import { useTodoStore } from '@/stores/todo'
import { useTaskStore } from '@/store/task'
import { useUserStore } from '@/store/user'
import { useScheduleStore } from '@/store/schedule'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as userAiApi from '@/api/user/ai'

type MessageType = 'text' | 'loading' | 'task' | 'schedule'

interface Message {
  id: string
  role: 'user' | 'assistant'
  type: MessageType
  content?: string
  taskData?: any
  scheduleData?: any
}


const isOpen = ref(false)
const messages = ref<Message[]>([])
const userInput = ref('')
const loading = ref(false)
const selectedModel = ref('gpt-3.5')
const messagesContainer = ref<HTMLElement>()
let messageIdCounter = 0

const includeContext = ref(true)
// 请求去重与处理跟踪（防止重复弹窗）
const pendingRequests = new Map<string, string>() // requestId -> loadingMsgId
const processedRequestIds = new Set<string>()
// 本地建议去重（防止本地预解析与后端返回的重复弹窗）
const recentLocalSuggestionHashes = new Set<string>()

const hashScheduleSuggestion = (s: any) => {
  try {
    const t = s || {}
    const st = t.startTime ? (new Date(t.startTime)).toISOString() : ''
    const et = t.endTime ? (new Date(t.endTime)).toISOString() : ''
    return `${(t.title||'').trim()}|${st}|${et}`
  } catch (e) { return '' }
}

const hashTaskSuggestion = (s: any) => {
  try {
    const t = s || {}
    const st = t.startTime ? (new Date(t.startTime)).toISOString() : (t.deadline ? (new Date(t.deadline)).toISOString() : '')
    return `${(t.title||'').trim()}|${st}`
  } catch (e) { return '' }
}
const todoStore = useTodoStore()
const taskStore = useTaskStore()
const scheduleStore = useScheduleStore()
const userStore = useUserStore()
const router = useRouter()

// 简单工具：确保可安全将可能为单对象或数组的值转换为数组
const toArray = (v: any) => {
  if (!v) return []
  return Array.isArray(v) ? v : [v]
}

// 中文数字映射（parseChineseNumber 使用）
const cnNumMap: Record<string, number> = { '零':0,'一':1,'二':2,'两':2,'三':3,'四':4,'五':5,'六':6,'七':7,'八':8,'九':9,'十':10 }

// 关键词白名单：仅当用户消息包含这些关键词时，才自动触发创建任务/日程的弹窗
const CREATE_TASK_KEYWORDS: string[] = [
  '创建任务','添加任务','帮我创建','新建任务','创建一个任务','添加一个任务','帮我加个任务','帮我建个任务','生成任务','创建锻炼任务','安排任务','帮我安排任务'
]
const CREATE_SCHEDULE_KEYWORDS: string[] = [
  '创建日程','添加日程','帮我安排','新建日程','创建一个日程','安排日程','安排一个日程','帮我安排日程'
]

const containsAnyKeyword = (text: string | undefined | null, keywords: string[]) => {
  if (!text) return false
  try {
    const s = String(text)
    for (const k of keywords) {
      if (s.indexOf(k) !== -1) return true
    }
  } catch (e) { /* ignore */ }
  return false
}

const isCreateTaskIntent = (text: string | undefined | null) => containsAnyKeyword(text, CREATE_TASK_KEYWORDS)
const isCreateScheduleIntent = (text: string | undefined | null) => containsAnyKeyword(text, CREATE_SCHEDULE_KEYWORDS)

// 会话ID 基础 key（会在 key 后拼接 userId），确保每个用户独立
const STORAGE_SESSION_ID_BASE = 'user_ai_session_id'
const sessionId = ref('')
const getSessionStorageKey = () => {
  const uid = String(userStore.user?.id ?? 'guest')
  return `${STORAGE_SESSION_ID_BASE}_${uid}`
}
const initSessionId = () => {
  const key = getSessionStorageKey()
  let saved = localStorage.getItem(key)
  if (!saved) {
    // 为了避免不同用户共享同一会话 id（导致聊天记录互相可见），
    // 对已登录用户始终生成新的 sessionId；仅在未登录（guest）情况下才复用旧的全局 sessionId 以保留访客历史。
    const globalOld = localStorage.getItem('user_ai_session_id')
    if (userStore.user && userStore.user.id != null) {
      // 已登录用户：为该用户创建独立的 sessionId，避免复用全局 id
      saved = generateSessionId()
    } else {
      // 未登录用户：若存在旧的全局 sessionId，则复用以保留访客会话体验
      if (globalOld) saved = globalOld
      else saved = generateSessionId()
    }
    localStorage.setItem(key, saved)
  }
  sessionId.value = saved
}
const generateSessionId = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c == 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

// 欢迎消息与本地持久化恢复
const loadChatHistory = async () => {
  initSessionId()

  // 恢复用户之前选择的模型，如果没有则默认为 'gpt-3.5'
  const savedModel = localStorage.getItem('ai_user_selected_model')
  if (savedModel) selectedModel.value = savedModel

  // 恢复会话消息（按 sessionId）
  try {
    const key = 'ai_chat_history_' + sessionId.value
    const raw = localStorage.getItem(key)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) {
        messages.value = parsed.map((m: any) => ({
          id: m.id ?? String(messageIdCounter++),
          role: m.role ?? 'assistant',
          type: m.type ?? 'text',
          content: m.content ?? (m.type === 'text' ? '' : ''),
          taskData: m.taskData ?? null,
          scheduleData: m.scheduleData ?? null
        }))
        // 保证 messageIdCounter 大于已存在 id
        const numericIds = messages.value.map(m => Number(m.id)).filter(n => !isNaN(n))
        if (numericIds.length) messageIdCounter = Math.max(...numericIds) + 1
      }
    } else {
      // 默认欢迎消息
      messages.value = [{
        id: String(messageIdCounter++),
        role: 'assistant',
        type: 'text',
        content: '👋 你好！我是你的 AI 助手。我可以帮你：\n• 💡 用自然语言创建任务\n• 📊 生成今日总结\n• 🔍 查询任务信息\n\n试试输入"帮我创建一个任务"吧！'
      }]
    }

    // 如果之前有 pending 的 AI 创建建议（后端触发时存入 sessionStorage），尝试在打开助手时导航并分发事件
    const pendingTask = sessionStorage.getItem('ai_pending_create_task')
    if (pendingTask) {
      try {
        const payload = JSON.parse(pendingTask)
        await router.push('/dashboard/tasks')
        setTimeout(() => {
          window.dispatchEvent(new CustomEvent('ai-create-task', { detail: payload }))
          sessionStorage.removeItem('ai_pending_create_task')
        }, 220)
      } catch (e) { console.warn('[AI助手] 恢复 pending create task 失败', e) }
    }

    const pendingSchedule = sessionStorage.getItem('ai_pending_create_schedule')
    if (pendingSchedule) {
      try {
        const payload = JSON.parse(pendingSchedule)
        await router.push('/dashboard/schedules')
        setTimeout(() => {
          window.dispatchEvent(new CustomEvent('ai-create-schedule', { detail: payload }))
          sessionStorage.removeItem('ai_pending_create_schedule')
        }, 220)
      } catch (e) { console.warn('[AI助手] 恢复 pending create schedule 失败', e) }
    }
  } catch (e) {
    console.warn('[AI助手] 恢复历史失败', e)
  }
}

// 处理模型选择变更：同步 UI 与 localStorage
const onModelChange = (newModel: string) => {
  selectedModel.value = newModel
  try { localStorage.setItem('ai_user_selected_model', newModel) } catch (e) { /* ignore */ }
  console.log('[AI助手] 用户选择了模型:', newModel)
}
const parseChineseNumber = (s: string) => {
  if (!s) return NaN
  if (/^\d+$/.test(s)) return Number(s)
  // simple handling for up to 二十
  let total = 0
  if (s.includes('十')) {
    const parts = s.split('十')
    const tens = parts[0] === '' ? 1 : (cnNumMap[parts[0]] || 0)
    const ones = parts[1] ? (cnNumMap[parts[1]] || 0) : 0
    total = tens * 10 + ones
  } else {
    total = cnNumMap[s] ?? NaN
  }
  return total
}

const buildSuggestionFromParsed = (parsed: any) => {
  const estMin = parsed?.estimatedMinutes ?? (parsed?.estimatedTime ? Math.round(parsed.estimatedTime * 60) : null)
  const estimatedHours = estMin ? Math.round((estMin/60)*100)/100 : (parsed?.estimatedTime || null)
  return {
    title: parsed?.title || '',
    startTime: parsed?.startTime || parsed?.deadline || null,
    deadline: parsed?.deadline || null,
    estimatedMinutes: estMin || 0,
    estimatedTime: estimatedHours || 0,
    // 优先使用 parsed.note（后端/新字段），兼容老字段 parsed.notes
    description: parsed?.description || parsed?.note || parsed?.notes || '',
    categoryName: parsed?.categoryName || null,
  }
}

const parseScheduleFromText = (text: string) => {
  if (!text) return null
  const t = text
  // YYYY-MM-DD to YYYY-MM-DD
  let m = t.match(/(\d{4}-\d{2}-\d{2}).{0,10}?(\d{4}-\d{2}-\d{2})/)
  if (m) {
    const start = dayjs(m[1]).hour(0).minute(0).second(0)
    const end = dayjs(m[2]).hour(0).minute(0).second(0)
    const title = t.replace(m[0], '').replace(/我打算|我要|计划|安排|出差|旅游|旅行|从|到/g, '').trim() || '日程'
    return { title, startTime: start.toISOString(), endTime: end.toISOString(), reminderTime: 15, description: '' }
  }

  // e.g. 5月28号到30号 或 28号到30号
  m = t.match(/(\d{1,2})月?(\d{1,2})号?.{0,10}?(?:到|至|\-|~)\s*(?:(\d{1,2})月?)?(\d{1,2})号?/) || t.match(/(?:从)?(\d{1,2})号?.{0,10}?(?:到|至|\-|~)\s*(\d{1,2})号?/)
  if (m) {
    try {
      const now = dayjs()
      let startDay = Number(m[1])
      let startMonth = now.month() + 1
      let endDay = m[4] ? Number(m[4]) : (m[2] ? Number(m[2]) : Number(m[2] || m[1]))
      let endMonth = m[3] ? Number(m[3]) : startMonth
      if (!m[3] && m[2] && m[1] && (Number(m[2]) !== undefined)) {
        // handled by second regex
      }
      const start = dayjs().month(startMonth - 1).date(startDay).hour(0).minute(0).second(0)
      let end = dayjs().month(endMonth - 1).date(endDay).hour(0).minute(0).second(0)
      if (end.isBefore(start)) end = end.add(1, 'month')
      const title = t.replace(/\d{1,2}月?\d{0,2}号?|从|到|至|出差|计划|我要|我打算|安排/g, '').trim() || '日程'
      return { title, startTime: start.toISOString(), endTime: end.toISOString(), reminderTime: 15, description: '' }
    } catch (e) {
      return null
    }
  }

  return null
}

const confirmSchedule = async (_schData: any) => {
  try {
    const payload = {
      title: _schData.title || '日程',
      startTime: _schData.startTime ? (new Date(_schData.startTime)).toISOString() : null,
      endTime: _schData.endTime ? (new Date(_schData.endTime)).toISOString() : null,
      reminderTime: _schData.reminderTime ?? 15,
      description: _schData.description ?? '',
    }
    sessionStorage.setItem('ai_pending_create_schedule', JSON.stringify(payload))
    await router.push('/dashboard/schedules')
    setTimeout(() => {
      window.dispatchEvent(new CustomEvent('ai-create-schedule', { detail: payload }))
      sessionStorage.removeItem('ai_pending_create_schedule')
    }, 220)
    ElMessage.success('已在日程页面打开创建表单并填入建议内容')
  } catch (error) {
    ElMessage.error('填入日程表单失败')
  }
}

const localParseTask = (text: string) => {
  if (!text) return null
  let t = text
  const res: any = { title: null, startTime: null, deadline: null, estimatedMinutes: null }

  // duration minutes (数字 or Chinese)
  const durMatch = t.match(/(\d+)\s*分钟/) || t.match(/([零一二三四五六七八九十]+)分钟/)
  if (durMatch) {
    const num = durMatch[1]
    res.estimatedMinutes = /^\d+$/.test(num) ? Number(num) : parseChineseNumber(num)
    t = t.replace(durMatch[0], '')
  }

  // hours
  const hrMatch = t.match(/(\d+)\s*小时/) || t.match(/([零一二三四五六七八九十]+)小时/)
  if (hrMatch) {
    const num = hrMatch[1]
    const hours = /^\d+$/.test(num) ? Number(num) : parseChineseNumber(num)
    res.estimatedMinutes = (hours || 0) * 60
    t = t.replace(hrMatch[0], '')
  }

  // time like 明天下午5点 or 明天下午五点
  // time parsing helper (localParseTask) - no separate temp var needed here
  const whenMatch = t.match(/(今天|明天|后天)?\s*(凌晨|早上|上午|中午|下午|晚上)?\s*([0-9]{1,2}|[零一二三四五六七八九十]{1,3})(?:[:点：](\d{1,2}|[零一二三四五六七八九十]{1,3}))?/) 
  if (whenMatch) {
    const dayWord = whenMatch[1]
    const mod = whenMatch[2]
    const hourStr = whenMatch[3]
    const minStr = whenMatch[4]
    let base = dayjs()
    if (dayWord === '明天') base = base.add(1,'day')
    else if (dayWord === '后天') base = base.add(2,'day')
    let hour = /^\d+$/.test(hourStr) ? Number(hourStr) : parseChineseNumber(hourStr)
    let minute = minStr ? (/^\d+$/.test(minStr) ? Number(minStr) : parseChineseNumber(minStr)) : 0
    if (mod === '下午' || mod === '晚上') { if (hour < 12) hour += 12 }
    const dt = base.hour(hour).minute(minute).second(0)
    res.startTime = dt.toISOString()
    res.deadline = dt.toISOString()
    t = t.replace(whenMatch[0], '')
  }

  // title: remove common verbs
  let title = t.replace(/帮我|创建|一个|任务|请帮我|，|,|。/g,'').trim()
  if (title.length === 0) title = '新任务'
  res.title = title
  return res
}

const sendMessage = async () => {
  if (!userInput.value.trim()) return

  const userMsg = userInput.value
  userInput.value = ''

  // 添加用户消息
  messages.value.push({
    id: String(messageIdCounter++),
    role: 'user',
    type: 'text',
    content: userMsg
  })

  // 先尝试解析是否为“创建日程/范围”意图（如“28号出差到30号”）
  const scheduleSuggestion = parseScheduleFromText(userMsg)
  if (scheduleSuggestion) {
    // 本地解析到日程建议，但不再以卡片形式展示以避免重复弹窗。
    // 后端返回 create_schedule 时会负责触发表单弹窗。
    console.debug('[AI助手] 本地解析到日程建议（已禁用卡片显示）:', scheduleSuggestion)
    // 可保留后续去重逻辑（用于防止同一建议短期内重复触发）
    const schHash = hashScheduleSuggestion(scheduleSuggestion)
    if (!recentLocalSuggestionHashes.has(schHash)) {
      recentLocalSuggestionHashes.add(schHash)
      setTimeout(() => recentLocalSuggestionHashes.delete(schHash), 2 * 60 * 1000)
    }
  }

  // 使用本地解析作为快速建议（已移除对后端 parse-task 接口的调用以避免重复/错误弹窗）
  let parsedSuggestion: any = null
  try {
    const local = localParseTask(userMsg)
    if (local && (local.title || local.startTime || local.deadline || local.estimatedMinutes)) {
      parsedSuggestion = buildSuggestionFromParsed(local)
    }
  } catch (err) { /* ignore */ }

  // 如果有建议，则先将建议消息展示，并准备跳转/填表
  if (parsedSuggestion) {
    // 本地解析到任务建议，但不再以卡片形式展示以避免重复弹窗。
    console.debug('[AI助手] 本地解析到任务建议（已禁用卡片显示）', parsedSuggestion)
    // 记录去重哈希以避免后续重复建议
    const taskHashLocal = hashTaskSuggestion(parsedSuggestion)
    if (!recentLocalSuggestionHashes.has(taskHashLocal)) {
      recentLocalSuggestionHashes.add(taskHashLocal)
      setTimeout(() => recentLocalSuggestionHashes.delete(taskHashLocal), 2 * 60 * 1000)
    }
    await scrollToBottom()
  }

  // 继续正常发送对话请求到后端（不依赖于 parse 结果）
  // 添加加载指示器
  const requestId = String(Date.now()) + '-' + Math.random().toString(36).slice(2,8)
  const loadingMsgId = String(messageIdCounter++)
  messages.value.push({ id: loadingMsgId, role: 'assistant', type: 'loading' })
  pendingRequests.set(requestId, loadingMsgId)

  loading.value = true
  await scrollToBottom()

  try {
    const modelMapping: { [key: string]: string } = { 'gpt-3.5': 'chatgpt3.5', 'deepseek': 'deepseek' }
    const provider = modelMapping[selectedModel.value] || selectedModel.value
    // 尝试获取 Todo 上下文（仅当用户允许且当前在 /dashboard/todos 页面）
    const currentPath = router.currentRoute.value?.path || ''
    let answer: any = null
    try {
      // 若当前在待办页但本地 todoStore 尚未填充（可能尚未完成页面同步），尝试从后端拉取视图范围内的任务并导入，确保 AI 能拿到中/低优任务
      if ((currentPath.includes('/dashboard/todos') || currentPath.includes('/dashboard/tasks')) && toArray((todoStore as any).tasks).length === 0) {
        try {
          const start = dayjs().subtract(3, 'day').format('YYYY-MM-DD')
          const end = dayjs().add(3, 'day').format('YYYY-MM-DD')
          await taskStore.fetchTasks({ startDeadline: start, endDeadline: end, status: 'all' })
          todoStore.importFromTasks(taskStore.tasks as any)
        } catch (e) {
          console.warn('[AI助手] 从后端拉取任务并导入 todoStore 失败', e)
        }
      }

      const context = (() => {
        if (!includeContext.value) return null
        // 支持在 /dashboard/todos 与 /dashboard/tasks 两个页面附加上下文
        if (!(currentPath.includes('/dashboard/todos') || currentPath.includes('/dashboard/tasks'))) return null

        // 使用 store 中的视图分组，保证与页面显示一致（包含日期窗口过滤）
        const highArr = toArray((todoStore as any).highPriorityTasks)
        const mediumArr = toArray((todoStore as any).mediumPriorityTasks)
        const lowArr = toArray((todoStore as any).procrastinateTasks)
        const completedArr = toArray((todoStore as any).completedTasks)

        const highList = highArr.map((t:any) => ({ id: String(t.id), title: t.title, deadline: t.deadline, estimatedMinutes: t.estimatedMinutes, urgency: t.urgency, originTaskId: t.originTaskId }))
        const mediumList = mediumArr.map((t:any) => ({ id: String(t.id), title: t.title, deadline: t.deadline, estimatedMinutes: t.estimatedMinutes, urgency: t.urgency, originTaskId: t.originTaskId }))
        const lowList = lowArr.map((t:any) => ({ id: String(t.id), title: t.title, deadline: t.deadline, estimatedMinutes: t.estimatedMinutes, urgency: t.urgency, originTaskId: t.originTaskId }))
        const completedList = completedArr.map((t:any) => ({ id: String(t.id), title: t.title, completedAt: t.completedAt || t.updatedAt || t.createdAt }))

        const counts = {
          high: highList.length,
          medium: mediumList.length,
          low: lowList.length,
          today: toArray((todoStore as any).tasks).filter((t:any) => !t.completed && (t.deadline === dayjs().format('YYYY-MM-DD'))).length
        }
        const overload = counts.today >= 5
        const weekly = parseInt(localStorage.getItem('weeklyCoreDone') || '0') || 0

        // 调试日志：便于前端控制台确认传递了哪些分组
        console.debug('[AI助手] 构建上下文分组 counts=', counts, 'samples:', { high: highList.slice(0,3).map(t=>t.title), medium: mediumList.slice(0,3).map(t=>t.title), low: lowList.slice(0,3).map(t=>t.title), completed: completedList.slice(0,3).map(t=>t.title) })
        return { hasContext: true, high_priority_tasks: highList, medium_priority_tasks: mediumList, procrastinate_tasks: lowList, completed_tasks: completedList, counts, overload, weekly_core_done: weekly }
      })()
      // 调试：在发送前打印完整上下文预览（包含 page context 与会话历史预览）
      try {
        console.log('[AI助手] 将发送 promote 的上下文预览：', {
          question: userMsg,
          model: provider,
          includeContext: includeContext.value,
          path: currentPath,
          counts: context && context.counts,
          mediumSample: context && context.medium_priority_tasks ? (context.medium_priority_tasks as any).slice(0,8).map((t:any)=>t.title) : []
        })
      } catch (e) { console.warn('[AI助手] 上下文打印失败', e) }

      // 构建发送给后端的最近历史：优先使用组件内消息；若为空则回退到 localStorage 中的会话历史
      const recentSource = (messages.value && messages.value.length > 0) ? messages.value : (() => {
        try {
          const key = 'ai_chat_history_' + sessionId.value
          const raw = localStorage.getItem(key)
          if (raw) {
            const parsed = JSON.parse(raw)
            if (Array.isArray(parsed)) return parsed
          }
        } catch (e) { /* ignore */ }
        return []
      })()

      const recent = (recentSource || [])
        .slice(-12)
        .filter((m:any) => {
          if (m.role !== 'user' && m.role !== 'assistant') return false
          if (typeof m.content !== 'string') return false
          const s = (m.content || '').trim()
          if (s.startsWith('{') && s.includes('"type"')) return false
          return true
        })
        .map((m:any) => ({ role: m.role, content: m.content }))

      // 打印 recent 预览，便于排查历史是否被正确附加
      try { console.debug('[AI助手] recent messages preview:', recent.map((r:any)=>({ role: r.role, content: String(r.content || '').substring(0,200) }))) } catch (e) {}

      // 始终尝试使用 promote（并传入最近历史）；promote 失败时先尝试仅使用 messages 再回退到 chat
      try {
        answer = await userAiApi.promote({ question: userMsg, context: context || null, model: provider, messages: recent })
      } catch (promoteErr) {
        console.warn('[AI助手] promote 接口调用失败，尝试仅用 messages 重试 promote：', promoteErr)
        try {
          // 有时 context（结构化页面数据）可能导致后端解析失败，尝试不传 context 仅发送 messages
          answer = await userAiApi.promote({ question: userMsg, model: provider, messages: recent })
        } catch (promoteErr2) {
          console.warn('[AI助手] promote (no context) 也失败，回退到 chat：', promoteErr2)
          answer = await userAiApi.chat({ message: userMsg, model: provider })
        }
      }
    } catch (ctxErr) {
      console.warn('[AI助手] 上下文增强调用失败，回退到普通 chat：', ctxErr)
      answer = await userAiApi.chat({ message: userMsg, model: provider })
    }
    // 去重：如果此 request 已被处理过则忽略后续响应
    if (processedRequestIds.has(requestId)) {
      const lid = pendingRequests.get(requestId)
      if (lid) messages.value = messages.value.filter(m => m.id !== lid)
      pendingRequests.delete(requestId)
      return
    }
    processedRequestIds.add(requestId)
    // 清理 loading 提示
    const lid = pendingRequests.get(requestId)
    if (lid) { messages.value = messages.value.filter(m => m.id !== lid); pendingRequests.delete(requestId) }
    console.log('[AI助手] 收到回复:', { userMsg, answer, type: typeof answer, model: provider })
    messages.value = messages.value.filter(m => m.id !== loadingMsgId)

    // 支持后端返回结构化 JSON：{ type, content, data }，并兼容数组与 update_schedule
    try {
      // helper: 从可能的字段集合中选取第一个存在的日期字段
      const pickDateField = (obj: any, keys: string[]) => {
        if (!obj) return null
        for (const k of keys) {
          if (Object.prototype.hasOwnProperty.call(obj, k) && obj[k]) return obj[k]
        }
        return null
      }

      const handleCreateTask = async (parsed: any, content: string) => {
        const rawStart = pickDateField(parsed, ['startTime','start_time','start','startDate','start_date','deadline'])
        const rawDeadline = pickDateField(parsed, ['deadline','dueDate','due_date'])
        const estMin = parsed?.estimatedMinutes ?? (parsed?.estimatedTime ? Math.round(parsed.estimatedTime * 60) : null)
        const suggestion = {
          title: parsed.title || (content ? String(content).substring(0,120) : '新任务'),
          startTime: rawStart ? new Date(rawStart) : (rawDeadline ? new Date(rawDeadline) : null),
          deadline: rawDeadline ? new Date(rawDeadline) : null,
          estimatedMinutes: estMin ?? 0,
          description: parsed.description || parsed.note || parsed.notes || '',
          categoryName: parsed.categoryName || parsed.category || null,
        }

        if (content && String(content).trim().length > 0) {
          messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'text', content: String(content) })
        }

        try {
          const thash = hashTaskSuggestion(suggestion)
          if (!recentLocalSuggestionHashes.has(thash)) {
            messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'task', taskData: suggestion })
            recentLocalSuggestionHashes.add(thash)
            setTimeout(() => recentLocalSuggestionHashes.delete(thash), 2 * 60 * 1000)
          } else {
            console.debug('[AI助手] 跳过重复的任务建议', thash)
          }
        } catch (e) { messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'task', taskData: suggestion }) }

        const payload: any = {
          title: suggestion.title,
          startTime: suggestion.startTime ? new Date(suggestion.startTime).toISOString() : null,
          deadline: suggestion.deadline ? new Date(suggestion.deadline).toISOString() : null,
          estimatedMinutes: suggestion.estimatedMinutes,
          description: suggestion.description,
          categoryName: suggestion.categoryName,
        }

        if (isCreateTaskIntent(userMsg)) {
          // 自动创建：优先使用 taskStore API，若失败再降级为打开创建表单
          try {
            const thash = hashTaskSuggestion(suggestion)
            if (!recentLocalSuggestionHashes.has(thash)) {
              await taskStore.createTask(payload)
              messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'text', content: `已创建任务：${payload.title}` })
              recentLocalSuggestionHashes.add(thash)
              setTimeout(() => recentLocalSuggestionHashes.delete(thash), 2 * 60 * 1000)
            } else {
              console.debug('[AI助手] 跳过重复创建任务', payload.title)
            }
          } catch (err) {
            console.warn('[AI助手] 批量创建任务失败，降级为打开表单', err)
            try { sessionStorage.setItem('ai_pending_create_task', JSON.stringify(payload)) } catch (e) {}
            try { await router.push('/dashboard/tasks') } catch (e) {}
            setTimeout(() => { window.dispatchEvent(new CustomEvent('ai-create-task', { detail: payload })); try { sessionStorage.removeItem('ai_pending_create_task') } catch (e) {} }, 220)
          }
        }
      }

      // 从自然语言文本尝试解析日期区间，例如 “22到24号” / “5月22到24号” 等
      const parseDateRangeFromText = (text: string) => {
        if (!text || typeof text !== 'string') return null
        const t = text.replace(/\s+/g, '')
        // 带年带月：2026年5月22到24号
        let m = t.match(/(\d{4})年(\d{1,2})月(\d{1,2})[日号]?[至到\-~](\d{1,2})[日号]?/)
        if (m) {
          const year = Number(m[1])
          const month = Number(m[2])
          const d1 = Number(m[3])
          const d2 = Number(m[4])
          const s = dayjs().year(year).month(month - 1).date(d1)
          const e = dayjs().year(year).month(month - 1).date(d2)
          return { start: s, end: e }
        }
        // 带月不带年：5月22到24号
        m = t.match(/(\d{1,2})月(\d{1,2})[日号]?[至到\-~](\d{1,2})[日号]?/)
        if (m) {
          const month = Number(m[1])
          const d1 = Number(m[2])
          const d2 = Number(m[3])
          const year = dayjs().year()
          const s = dayjs().year(year).month(month - 1).date(d1)
          const e = dayjs().year(year).month(month - 1).date(d2)
          return { start: s, end: e }
        }
        // 仅日范围：22到24号（默认同月同年）
        m = t.match(/(\d{1,2})[日号]?[至到\-~](\d{1,2})[日号]?/)
        if (m) {
          const d1 = Number(m[1])
          const d2 = Number(m[2])
          const now = dayjs()
          const s = now.date(d1)
          const e = now.date(d2)
          return { start: s, end: e }
        }
        return null
      }

      const handleCreateSchedule = async (parsed: any, content: string) => {
        const rawStart = pickDateField(parsed, ['startTime','start_time','start','startDate','start_date','begin'])
        const rawEnd = pickDateField(parsed, ['endTime','end_time','end','endDate','end_date','finish'])
        const suggestion = {
          title: parsed.title || (content ? String(content).substring(0,120) : '日程'),
          startTime: rawStart ? new Date(rawStart) : null,
          endTime: rawEnd ? new Date(rawEnd) : null,
          reminderTime: parsed.reminderTime ?? parsed.reminder ?? 15,
          description: parsed.description || parsed.note || '',
        }

        // 如果 AI 只给出了一个日期区间（如 “22到24号”）但没有具体时间，尝试从原始文本解析并预填默认时段
        try {
          if ((!suggestion.startTime || !suggestion.endTime) && content && String(content).trim().length > 0) {
            const range = parseDateRangeFromText(String(content))
            if (range) {
              // 默认开始时间 09:00，结束时间 18:00（可按需改为全天）
              const s = range.start.hour(9).minute(0).second(0)
              const e = range.end.hour(18).minute(0).second(0)
              if (!suggestion.startTime) suggestion.startTime = s.toDate()
              if (!suggestion.endTime) suggestion.endTime = e.toDate()
            }
            // 如果文本包含地点（如“在天津”），把地点加入描述以便用户查阅
            const locMatch = String(content).match(/在([^，,。\s]+)/)
            if (locMatch && locMatch[1]) {
              const loc = locMatch[1]
              if (suggestion.description) suggestion.description = suggestion.description + '；地点：' + loc
              else suggestion.description = '地点：' + loc
            }
          }
        } catch (ex) { console.warn('[AI助手] 解析日期区间失败', ex) }

        if (content && String(content).trim().length > 0) {
          messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'text', content: String(content) })
        }

        try {
          const shash = hashScheduleSuggestion(suggestion)
          if (!recentLocalSuggestionHashes.has(shash)) {
            messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'schedule', scheduleData: suggestion })
            recentLocalSuggestionHashes.add(shash)
            setTimeout(() => recentLocalSuggestionHashes.delete(shash), 2 * 60 * 1000)
          } else {
            console.debug('[AI助手] 跳过重复的日程建议', shash)
          }
        } catch (e) { messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'schedule', scheduleData: suggestion }) }

        const payload: any = {
          title: suggestion.title,
          startTime: suggestion.startTime ? new Date(suggestion.startTime).toISOString() : null,
          endTime: suggestion.endTime ? new Date(suggestion.endTime).toISOString() : null,
          reminderTime: suggestion.reminderTime,
          description: suggestion.description,
        }

        if (isCreateScheduleIntent(userMsg)) {
          try {
            await scheduleStore.createSchedule(payload)
            messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'text', content: `已创建日程：${payload.title}` })
          } catch (err) {
            console.warn('[AI助手] 批量创建日程失败，降级为打开表单', err)
            try { sessionStorage.setItem('ai_pending_create_schedule', JSON.stringify(payload)) } catch (e) {}
            try { await router.push('/dashboard/schedules') } catch (e) {}
            setTimeout(() => { window.dispatchEvent(new CustomEvent('ai-create-schedule', { detail: payload })); try { sessionStorage.removeItem('ai_pending_create_schedule') } catch (e) {} }, 220)
          }
        }
      }

      if (Array.isArray(answer)) {
        // 收集任务与日程项，任务使用队列逐一弹窗
        const taskItems: any[] = []
        const scheduleItems: any[] = []
        for (const item of answer) {
          if (!item || typeof item !== 'object' || !item.type) continue
          const atype = String(item.type)
          const content = item.content || ''
          const data = item.data || {}
          if (atype === 'create_task') {
            taskItems.push({ data, content })
          } else if (atype === 'create_schedule') {
            scheduleItems.push({ data, content })
          } else if (atype === 'update_schedule') {
            messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'text', content: content || '检测到日程变动，已为您打开日程页面以便确认。' })
            try { sessionStorage.setItem('ai_pending_update_schedule', JSON.stringify(data)) } catch (e) {}
            try { await router.push('/dashboard/schedules') } catch (e) {}
            setTimeout(() => { window.dispatchEvent(new CustomEvent('ai-update-schedule', { detail: data })); try { sessionStorage.removeItem('ai_pending_update_schedule') } catch (e) {} }, 220)
          } else {
            messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'text', content: content || String(item) })
          }
        }

        // 多任务：写入队列并触发首个创建表单
        if (taskItems.length > 0) {
          const payloads = taskItems.map(it => {
            const parsed = it.data || {}
            const st = parsed.startTime || parsed.start_date || parsed.start
            const dl = parsed.deadline || parsed.dueDate || parsed.end_date
            return {
              title: parsed.title || (it.content ? String(it.content).substring(0,120) : '新任务'),
              startTime: st ? (dayjs(st).isValid() ? dayjs(st).toISOString() : null) : (dl ? (dayjs(dl).isValid() ? dayjs(dl).toISOString() : null) : null),
              deadline: dl ? (dayjs(dl).isValid() ? dayjs(dl).toISOString() : null) : null,
              estimatedMinutes: parsed.estimatedMinutes ?? (parsed.estimatedTime ? Math.round(parsed.estimatedTime * 60) : null),
              description: parsed.description || parsed.note || parsed.notes || '',
              categoryName: parsed.categoryName || null,
            }
          })
          try { sessionStorage.setItem('ai_pending_create_tasks', JSON.stringify(payloads)) } catch (e) { console.warn('store queue failed', e) }
          try { await router.push('/dashboard/tasks') } catch (e) {}
          setTimeout(() => { try { window.dispatchEvent(new CustomEvent('ai-create-task', { detail: payloads[0] })) } catch (e) { console.warn('dispatch first queued task failed', e) } }, 220)
          messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'text', content: `已为您识别 ${taskItems.length} 个任务，将依次打开创建表单。` })
        }

        // 逐个处理日程建议（仍沿用原有逻辑）
        for (const it of scheduleItems) {
          await handleCreateSchedule(it.data, it.content)
        }
      } else if (answer && typeof answer === 'object' && (answer as any).type) {
        const atype = String((answer as any).type)
        const content = (answer as any).content || ''
        const data = (answer as any).data || null

        if (atype === 'create_task') {
          await handleCreateTask(data || {}, content)
        } else if (atype === 'create_schedule') {
          await handleCreateSchedule(data || {}, content)
        } else if (atype === 'update_schedule') {
          messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'text', content: content || '检测到日程变动，已为您打开日程页面以便确认。' })
          try { sessionStorage.setItem('ai_pending_update_schedule', JSON.stringify(data || {})) } catch (e) {}
          try { await router.push('/dashboard/schedules') } catch (e) {}
          setTimeout(() => { window.dispatchEvent(new CustomEvent('ai-update-schedule', { detail: data || {} })); try { sessionStorage.removeItem('ai_pending_update_schedule') } catch (e) {} }, 220)
        } else {
          const text = (content && String(content).trim()) || '🤖 暂无回复内容，请稍后重试或换种说法。'
          messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'text', content: text })
        }
      } else {
        messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'text', content: (answer && String(answer).trim()) || '🤖 暂无回复内容，请稍后重试或换种说法。' })
      }
    } catch (e) {
      console.warn('[AI助手] 处理结构化响应时出错', e)
      messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'text', content: (answer && String(answer).trim()) || '🤖 暂无回复内容，请稍后重试或换种说法。' })
    }
  } catch (error: any) {
    messages.value = messages.value.filter(m => m.id !== loadingMsgId)
    console.error('[AI助手] 请求出错:', error)
    messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'text', content: '❌ 出错了，请稍后重试: ' + (error.message || '未知错误') })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

// 配置 marked，关闭导致警告的两个特性
marked.setOptions({
  breaks: true,
  gfm: true,
  mangle: false,
  headerIds: false
})

// 渲染 Markdown 为安全的 HTML
const renderMarkdown = (text: string | undefined | null) => {
  if (!text) return ''
  const rawHtml = marked.parse(String(text))
  return DOMPurify.sanitize(rawHtml)
}

const escapeHtml = (text: string | undefined | null) => {
  if (text == null) return ''
  return String(text).replace(/[&<>]/g, (m) => (m === '&' ? '&amp;' : m === '<' ? '&lt;' : '&gt;'))
}

const scrollToBottom = async () => {
  await nextTick()
  try {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  } catch (e) { /* ignore */ }
}

onMounted(async () => {
  await loadChatHistory()
  await scrollToBottom()
})

// 将消息持久化到 localStorage（按 sessionId 分隔）
watch(messages, (nv) => {
  try {
    const key = 'ai_chat_history_' + sessionId.value
    localStorage.setItem(key, JSON.stringify(nv))
  } catch (e) { /* ignore */ }
}, { deep: true })

// 清空聊天记录：清空内存消息、重置计数，并移除本地/会话存储中相关条目
const clearChatHistory = () => {
  try {
    messages.value = []
    messageIdCounter = 0
    const key = 'ai_chat_history_' + sessionId.value
    try { localStorage.removeItem(key) } catch (e) { /* ignore */ }
    try { sessionStorage.removeItem('ai_pending_create_task') } catch (e) { /* ignore */ }
    try { sessionStorage.removeItem('ai_pending_create_schedule') } catch (e) { /* ignore */ }
    try { sessionStorage.removeItem('ai_pending_create_tasks') } catch (e) { /* ignore */ }
    ElMessage.success('已清空聊天记录')
  } catch (e) {
    console.warn('[AI助手] 清空聊天记录失败', e)
    ElMessage.error('清空聊天记录失败')
  }
}

const quickAction = (action: string) => {
  if (action === 'create') {
    userInput.value = '帮我创建一个任务：'
  } else if (action === 'summary') {
    // 直接获取总结
    getTodaySummary()
  } else if (action === 'help') {
    userInput.value = '你能帮我做什么？'
  }
}

const getTodaySummary = async () => {
  const loadingMsgId = String(messageIdCounter++)
  messages.value.push({
    id: loadingMsgId,
    role: 'assistant',
    type: 'loading'
  })
  
  loading.value = true
  await scrollToBottom()
  
  try {
    const summary = await userAiApi.getTodaySummary()
    messages.value = messages.value.filter(m => m.id !== loadingMsgId)
    messages.value.push({
      id: String(messageIdCounter++),
      role: 'assistant',
      type: 'text',
      content: '📊 今日总结:\n\n' + summary
    })
  } catch (error) {
    messages.value = messages.value.filter(m => m.id !== loadingMsgId)
    messages.value.push({
      id: String(messageIdCounter++),
      role: 'assistant',
      type: 'text',
      content: '❌ 获取总结失败'
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

const confirmTask = async (_taskData: any) => {
  try {
    const payload = {
      title: _taskData.title,
      startTime: _taskData.startTime ? new Date(_taskData.startTime).toISOString() : (_taskData.deadline ? new Date(_taskData.deadline).toISOString() : null),
      deadline: _taskData.deadline ? new Date(_taskData.deadline).toISOString() : null,
      estimatedMinutes: _taskData.estimatedMinutes ?? null,
      description: _taskData.description ?? '',
      categoryName: _taskData.categoryName ?? null,
    }
    sessionStorage.setItem('ai_pending_create_task', JSON.stringify(payload))
    await router.push('/dashboard/tasks')
    setTimeout(() => {
      window.dispatchEvent(new CustomEvent('ai-create-task', { detail: payload }))
      sessionStorage.removeItem('ai_pending_create_task')
    }, 220)
    ElMessage.success('已将建议的任务填入创建表单')
  } catch (error) {
    ElMessage.error('❌ 填入任务表单失败')
  }
}
</script>

<style scoped lang="scss">
.ai-chat-drawer {
  position: fixed;
  bottom: 0;
  right: 20px;
  width: 380px;
  height: 600px;
  background: #f5efe0; /* 使用仪表盘的纯米色背景 (#f5efe0) */
  border-radius: 12px 12px 0 0;
  box-shadow: 0 -2px 12px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  z-index: 1000;
  border: 1px solid #e8e8e8;
  
    .drawer-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(135deg, #C39BD4 0%, #9BB7D4 45%, #D6B77A 90%);
    color: white;
    border-radius: 12px 12px 0 0;
    
    .title-bar {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;
      
      .title {
        font-weight: 600;
        font-size: 14px;
      }
    }
    
    .close-btn {
      background: none;
      border: none;
      font-size: 18px;
      cursor: pointer;
      color: white;
      opacity: 0.9;
      
      &:hover {
        opacity: 1;
      }
    }
  }
  
  .messages-container {
    flex: 1;
    overflow-y: auto;
    padding: 12px 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    
    .message {
      display: flex;
      gap: 8px;
      animation: messageSlide 0.3s ease-in;
      
      &.user {
        justify-content: flex-end;
        
        .avatar {
          order: 2;
        }
        
        .content {
          background: #F7EAD9; /* 复古米色 - 用户消息 */
          order: 1;
          color: #333;
        }
      }
      
      &.assistant {
        justify-content: flex-start;
        
        .content {
          background: #FFF9F2; /* 更暖的复古背景供助手消息使用 */
          color: #333;
        }
      }
      
      .avatar {
        font-size: 20px;
        flex-shrink: 0;
        width: 28px;
        height: 28px;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      
      .content {
        max-width: 280px;
        padding: 10px 12px;
        border-radius: 8px;
        word-wrap: break-word;
        font-size: 13px;
        line-height: 1.6;
        
        .text {
          white-space: pre-wrap;
        }
        
        .task-suggestion {
          h4 {
            margin: 0 0 12px 0;
            font-size: 13px;
            font-weight: 600;
          }
        }
        
        .loading {
          display: flex;
          gap: 4px;
          height: 6px;
          
          span {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #999;
            animation: bounce 1.4s infinite;
            
            &:nth-child(1) { animation-delay: 0s; }
            &:nth-child(2) { animation-delay: 0.2s; }
            &:nth-child(3) { animation-delay: 0.4s; }
          }
          
          @keyframes bounce {
            0%, 80%, 100% { opacity: 0.3; }
            40% { opacity: 1; }
          }
        }
      }
    }
  }
  
  .shortcuts {
    display: flex;
    gap: 8px;
    padding: 0 16px;
    margin-bottom: 12px;
    
      .shortcut-btn {
      flex: 1;
      height: 32px;
      border: 1px solid #e6dede;
      border-radius: 6px;
      background: white;
      cursor: pointer;
      font-size: 12px;
      transition: all 0.3s;
      font-weight: 500;
      color: #6b4a6a;
      
      &:hover {
        background: #fff7f0;
        border-color: #C39BD4;
        color: #C39BD4;
      }
      
      &:active {
        transform: scale(0.98);
      }
    }
  }
  
  .input-area {
    display: flex;
    gap: 8px;
    padding: 12px 16px;
    border-top: 1px solid #f0f0f0;
    background: #fafafa;
  }
}

.chat-fab {
  position: fixed;
  bottom: 30px;
  right: 30px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  /* 彩虹复古渐变，呼应仪表盘的配色 */
  background: linear-gradient(90deg, #F7D6C1 0%, #E0AFA0 20%, #D6B77A 40%, #9BB7D4 60%, #C39BD4 80%, #F6D6A9 100%);
  border: none;
  color: #fff;
  font-size: 22px;
  cursor: pointer;
  box-shadow: 0 8px 28px rgba(0,0,0,0.18);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  z-index: 999;

  &:hover {
    transform: scale(1.08) translateY(-2px);
    box-shadow: 0 10px 30px rgba(0,0,0,0.22);
  }

  &:active {
    transform: scale(0.98);
  }
}

@keyframes messageSlide {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 深色模式支持
@media (prefers-color-scheme: dark) {
  .ai-chat-drawer {
    background: #1e1e1e;
    border-color: #333;
    
    .drawer-header {
      border-bottom-color: #333;
    }
    
    .messages-container {
      .message {
        &.user {
          .content {
            background: #2d5a2d;
            color: #e0e0e0;
          }
        }
        
        &.assistant {
          .content {
            background: #2a2a2a;
            color: #e0e0e0;
          }
        }
      }
    }
    
    .input-area {
      background: #2a2a2a;
      border-top-color: #333;
    }
  }
}
</style>
