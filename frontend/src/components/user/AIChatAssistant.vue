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
const todoStore = useTodoStore()
const taskStore = useTaskStore()
const userStore = useUserStore()

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
const loadChatHistory = () => {
  initSessionId()

  // 恢复用户之前选择的模型，如果没有则默认为 'gpt-3.5'
  const savedModel = localStorage.getItem('ai_user_selected_model')
  if (savedModel) {
    selectedModel.value = savedModel
  }

  // 恢复会话消息（按 sessionId）
  try {
    const key = 'ai_chat_history_' + sessionId.value
    const raw = localStorage.getItem(key)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) {
        messages.value = parsed
        // ensure messageIdCounter is ahead
        try { messageIdCounter = Math.max(...parsed.map((m:any) => Number(m.id) || 0)) + 1 } catch(e) {}
        return
      }
    }
  } catch (e) { console.warn('[AI助手] 恢复聊天记录失败', e) }

  // 默认欢迎消息
  messages.value = [{
    id: String(messageIdCounter++),
    role: 'assistant',
    type: 'text',
    content: '👋 你好！我是你的 AI 助手。我可以帮你：\n• 💡 用自然语言创建任务\n• 📊 生成今日总结\n• 🔍 查询任务信息\n\n试试输入"帮我创建一个任务"吧！'
  }]
}

onMounted(async () => {
  loadChatHistory()
})

// 监听用户切换/登录状态变化，重新加载当前用户的会话与历史
watch(() => userStore.user?.id, (newId, oldId) => {
  try {
    if (newId !== oldId) {
      loadChatHistory()
    }
  } catch (e) { console.warn('[AI助手] 用户切换时重新加载聊天历史失败', e) }
})

// 持久化消息，当 messages 变化时保存（按 session）
watch(messages, (val) => {
  try {
    const key = 'ai_chat_history_' + sessionId.value
    localStorage.setItem(key, JSON.stringify(val))
  } catch (e) { console.warn('[AI助手] 保存聊天记录失败', e) }
}, { deep: true })

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

/**
 * 处理模型选择变更
 * 保存用户选择到 localStorage，下次打开浮窗时恢复
 */
const onModelChange = (newModel: string) => {
  localStorage.setItem('ai_user_selected_model', newModel)
  console.log('[AI助手] 用户选择了模型:', newModel)
  // 发送一条提示消息
  messages.value.push({
    id: String(messageIdCounter++),
    role: 'assistant',
    type: 'text',
    content: `已切换到 ${newModel === 'gpt-3.5' ? 'ChatGPT3.5' : 'DeepSeek'} 模型`
  })
}

const clearChatHistory = async () => {
  try {
    const res = await ElMessageBox.confirm('确认要清空当前会话的聊天记录吗？该操作不可恢复。', '清空聊天记录', { confirmButtonText: '清空', cancelButtonText: '取消', type: 'warning' })
    const key = 'ai_chat_history_' + sessionId.value
    messages.value = []
    localStorage.removeItem(key)
    ElMessage.success('聊天记录已清空')
  } catch (e) {
    // 用户取消或操作失败，什么都不做
  }
}

const router = useRouter()

// 帮助将可能为 computed/ref 的分组统一为数组
const toArray = (v: any) => {
  try {
    if (!v) return []
    if (Array.isArray(v)) return v
    if (v.value && Array.isArray(v.value)) return v.value
    // 支持 Vue 的响应式 Proxy
    const maybe = Array.from(v || [])
    return Array.isArray(maybe) ? maybe : []
  } catch (e) { return [] }
}

const cnNumMap: Record<string, number> = { '零':0,'一':1,'二':2,'三':3,'四':4,'五':5,'六':6,'七':7,'八':8,'九':9,'十':10 }
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
    // 先在消息区展示建议，但只有当用户有明确创建意图时才自动打开日程创建表单
    messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'schedule', scheduleData: { ...scheduleSuggestion, startTime: new Date(scheduleSuggestion.startTime), endTime: new Date(scheduleSuggestion.endTime) } })
    await scrollToBottom()
    try {
      if (isCreateScheduleIntent(userMsg)) {
        sessionStorage.setItem('ai_pending_create_schedule', JSON.stringify(scheduleSuggestion))
        await router.push('/dashboard/schedules')
        setTimeout(() => {
          window.dispatchEvent(new CustomEvent('ai-create-schedule', { detail: scheduleSuggestion }))
          sessionStorage.removeItem('ai_pending_create_schedule')
        }, 220)
      }
    } catch (err) { console.warn('导航或分发日程建议失败', err) }
  }

  // 先尝试调用 parseTask 接口（若存在），若有解析结果则生成 task 建议并预填表单
  let parsedSuggestion: any = null
  try {
    const parsed = await userAiApi.parseTask({ message: userMsg })
    if (parsed && (parsed.title || parsed.startTime || parsed.deadline || parsed.estimatedMinutes)) {
      parsedSuggestion = buildSuggestionFromParsed(parsed)
    }
  } catch (e) {
    // 后端无解析能力或出错，使用本地解析作为回退
    try {
      const local = localParseTask(userMsg)
      if (local && (local.title || local.startTime || local.deadline || local.estimatedMinutes)) {
        parsedSuggestion = buildSuggestionFromParsed(local)
      }
    } catch (err) { /* ignore */ }
  }

  // 如果有建议，则先将建议消息展示，并准备跳转/填表
  if (parsedSuggestion) {
    // 显示建议；仅当用户明确表达创建意图时才自动打开创建表单
    messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'task', taskData: parsedSuggestion })
    await scrollToBottom()

    try {
      const payload = {
        title: parsedSuggestion.title,
        startTime: parsedSuggestion.startTime || parsedSuggestion.deadline || null,
        deadline: parsedSuggestion.deadline || null,
        estimatedMinutes: parsedSuggestion.estimatedMinutes,
        estimatedTime: parsedSuggestion.estimatedTime,
        description: parsedSuggestion.description,
        categoryName: parsedSuggestion.categoryName,
      }
      if (isCreateTaskIntent(userMsg)) {
        sessionStorage.setItem('ai_pending_create_task', JSON.stringify(payload))
        await router.push('/dashboard/tasks')
        setTimeout(() => {
          window.dispatchEvent(new CustomEvent('ai-create-task', { detail: payload }))
          sessionStorage.removeItem('ai_pending_create_task')
        }, 220)
      }
    } catch (err) {
      console.warn('导航或分发建议失败', err)
    }
  }

  // 继续正常发送对话请求到后端（不依赖于 parse 结果）
  // 添加加载指示器
  const loadingMsgId = String(messageIdCounter++)
  messages.value.push({ id: loadingMsgId, role: 'assistant', type: 'loading' })

  loading.value = true
  await scrollToBottom()

  try {
    const modelMapping: { [key: string]: string } = { 'gpt-3.5': 'chatgpt3.5', 'deepseek': 'deepseek' }
    const provider = modelMapping[selectedModel.value] || selectedModel.value
    // 尝试获取 Todo 上下文（仅当用户允许且当前在 /dashboard/todos 页面）
    const currentPath = router.currentRoute.value?.path || ''
    let answer: string | null = null
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
      // 调试：在发送前打印完整上下文预览，便于在浏览器控制台确认 medium_priority_tasks 是否被传递
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
      if (context) {
        // 使用 promote 接口，将用户问题、上下文和最近的消息历史发送给后端
        const recent = (messages.value || []).slice(-12).map((m:any) => {
          let content = ''
          try {
            if (m.type === 'text') content = m.content || ''
            else if (m.type === 'task') content = JSON.stringify({ type: 'task_suggestion', taskData: m.taskData || {} })
            else if (m.type === 'schedule') content = JSON.stringify({ type: 'schedule_suggestion', scheduleData: m.scheduleData || {} })
            else content = JSON.stringify(m)
          } catch (e) {
            content = String(m.content || '')
          }
          return { role: m.role, content }
        })
        answer = await userAiApi.promote({ question: userMsg, context, model: provider, messages: recent })
      } else {
        answer = await userAiApi.chat({ message: userMsg, model: provider })
      }
    } catch (ctxErr) {
      console.warn('[AI助手] 上下文增强调用失败，回退到普通 chat：', ctxErr)
      answer = await userAiApi.chat({ message: userMsg, model: provider })
    }
    messages.value = messages.value.filter(m => m.id !== loadingMsgId)
    console.log('[AI助手] 收到回复:', { userMsg, answer, type: typeof answer, model: provider })

    // 支持后端返回结构化 JSON：{ type, content, data }
    try {
      if (answer && typeof answer === 'object' && (answer as any).type) {
        const atype = String((answer as any).type)
        const content = (answer as any).content || ''
        const data = (answer as any).data || null

        if (atype === 'create_task') {
          const parsed: any = data || {}
          const suggestion = {
            title: parsed.title || (typeof content === 'string' ? content.substring(0, 120) : '新任务'),
            startTime: parsed.startTime ? new Date(parsed.startTime) : (parsed.deadline ? new Date(parsed.deadline) : null),
            deadline: parsed.deadline ? new Date(parsed.deadline) : null,
            estimatedMinutes: parsed.estimatedMinutes ?? (parsed.estimatedTime ? Math.round(parsed.estimatedTime * 60) : null),
            description: parsed.description || parsed.note || '',
            categoryName: parsed.categoryName || null,
          }
          messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'task', taskData: suggestion })

          try {
            const payload = {
              title: suggestion.title,
              startTime: suggestion.startTime ? new Date(suggestion.startTime).toISOString() : (suggestion.deadline ? new Date(suggestion.deadline).toISOString() : null),
              deadline: suggestion.deadline ? new Date(suggestion.deadline).toISOString() : null,
              estimatedMinutes: suggestion.estimatedMinutes,
              description: suggestion.description,
              categoryName: suggestion.categoryName,
            }
            // 二次校验：仅当用户明确表达创建意图时才自动打开任务创建窗口
            if (isCreateTaskIntent(userMsg)) {
              sessionStorage.setItem('ai_pending_create_task', JSON.stringify(payload))
              await router.push('/dashboard/tasks')
              setTimeout(() => {
                window.dispatchEvent(new CustomEvent('ai-create-task', { detail: payload }))
                sessionStorage.removeItem('ai_pending_create_task')
              }, 220)
            } else {
              // 当模型误判为创建但用户并未明确要求时，降级为普通文本回复（并记录日志）
              console.warn('[AI助手] 模型返回 create_task，但用户消息未包含创建关键词，已降级为普通回复。用户消息:', userMsg)
            }
          } catch (err) { console.warn('导航或分发建议失败', err) }

        } else if (atype === 'create_schedule') {
          const parsed: any = data || {}
          const suggestion = {
            title: parsed.title || (typeof content === 'string' ? content.substring(0, 120) : '日程'),
            startTime: parsed.startTime ? new Date(parsed.startTime) : null,
            endTime: parsed.endTime ? new Date(parsed.endTime) : null,
            reminderTime: parsed.reminderTime ?? 15,
            description: parsed.description || ''
          }
          messages.value.push({ id: String(messageIdCounter++), role: 'assistant', type: 'schedule', scheduleData: suggestion })
          try {
            const payload = {
              title: suggestion.title,
              startTime: suggestion.startTime ? new Date(suggestion.startTime).toISOString() : null,
              endTime: suggestion.endTime ? new Date(suggestion.endTime).toISOString() : null,
              reminderTime: suggestion.reminderTime,
              description: suggestion.description,
            }
            if (isCreateScheduleIntent(userMsg)) {
              sessionStorage.setItem('ai_pending_create_schedule', JSON.stringify(payload))
              await router.push('/dashboard/schedules')
              setTimeout(() => { window.dispatchEvent(new CustomEvent('ai-create-schedule', { detail: payload })); sessionStorage.removeItem('ai_pending_create_schedule') }, 220)
            } else {
              console.warn('[AI助手] 模型返回 create_schedule，但用户消息未包含创建关键词，已降级为普通回复。用户消息:', userMsg)
            }
          } catch (err) { console.warn('导航或分发日程失败', err) }

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
