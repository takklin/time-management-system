<template>
  <div class="dashboard-container">
    <div class="welcome-card">
        <div class="welcome-left">
          <h2>欢迎回来，{{ userStore.user?.username }}</h2>
          <p>{{ currentDate }} | 今天是个美好的一天</p>
          <p class="quote">“{{ quote }}”</p>
          <div class="streak">连续打卡：<strong>{{ streak }}</strong> 天</div>
        </div>
        <div class="welcome-right">
                    <div class="upcoming-title">未来3天日程</div>
                    <div class="upcoming-list upcoming-highlight">
                <div v-if="upcomingSchedules.length === 0" class="empty">暂无日程</div>
                <div v-for="s in upcomingSchedules" :key="s.id" class="upcoming-item">
                  <div class="time">{{ formatScheduleDate(s) }}</div>
                  <div class="title">{{ s.title }}</div>
                </div>
              </div>
              <!-- 全局番茄钟（侧边） -->
              <!-- 使用全局布局中的 TomatoClock，避免页面内重复实例 -->
            </div>
        <div class="smiley-corner" aria-hidden="true">😊</div>
      </div>

    <div class="stats-row">
      <el-card class="stat-card">
        <template #header>
          <div class="card-header">
            <span>任务总数</span>
          </div>
        </template>
        <div class="stat-value">{{ taskStats.total }}</div>
      </el-card>

      <el-card class="stat-card">
        <template #header>
          <div class="card-header">
            <span>完成率</span>
          </div>
        </template>
        <div class="stat-value">{{ taskStats.completionRate }}%</div>
      </el-card>

      <el-card class="stat-card">
        <template #header>
          <div class="card-header">
            <span>本周专注时长</span>
          </div>
        </template>
        <div class="stat-value">{{ taskStats.weeklyFocusHours }}h</div>
      </el-card>
    </div>

    <div class="quick-add">
      <el-input v-model="quickInput" placeholder="快速添加任务，例如：明天下午3点 开会" clearable @keyup.enter="handleQuickAdd" />
      <el-button type="primary" @click="handleQuickAdd">快速添加</el-button>
    </div>

    <div class="content-row">
      <ChartCard title="今日待办任务" :class="['flex-1', todayTasks.length > 0 ? 'today-has-tasks' : 'today-empty']">
        <!-- 自定义空状态，确保在 ElementPlus 图标/样式缺失时也能可靠展示 -->
        <div v-if="todayTasks.length === 0" class="custom-empty-state">
          <div class="empty-svg">
            <svg class="empty-svg-icon" viewBox="0 0 79 86" xmlns="http://www.w3.org/2000/svg">
              <g fill="none" fill-rule="evenodd">
                <rect x="6" y="12" width="67" height="62" rx="6" fill="#FCFDFF" stroke="#D5DEE8" stroke-width="1.4"/>
                <path d="M14 26 L65 26" stroke="#E2E8F0" stroke-width="1.2" stroke-dasharray="2 2"/>
                <path d="M22 66 L58 66" stroke="#D0D9E4" stroke-width="1.2" stroke-linecap="round"/>
                <path d="M26 72 L54 72" stroke="#D0D9E4" stroke-width="1.2" stroke-linecap="round"/>
                <g transform="translate(44,28)">
                  <circle cx="14" cy="10" r="7" fill="#FFF" stroke="#C0CCDA" stroke-width="1.2"/>
                  <path d="M14 7 L14 13 M11 10 L17 10" stroke="#C0CCDA" stroke-width="1.2" stroke-linecap="round"/>
                </g>
              </g>
            </svg>
          </div>
          <div class="empty-description">今日暂无待办任务</div>
        </div>

        <!-- 今日任务列表（若存在） -->
        <div v-else class="today-list">
          <TaskItem
            v-for="task in todayTasks"
            :key="task.id"
            :task="task"
            :isToday="true"
            @complete="completeTask"
            @delete="deleteTask"
            @edit="editTask"
            @edit-require-estimate="editTaskRequireEstimate"
          />
        </div>

        <!-- 明日待办（始终显示在今日区域下方） -->
        <div v-if="tomorrowTasksSorted.length > 0" class="tomorrow-list" style="margin-top: 12px">
          <div class="section-title">明日优先任务</div>
          <TaskItem
            v-for="task in tomorrowTasksSorted"
            :key="task.id"
            :task="task"
            :highlight="true"
            @complete="completeTask"
            @delete="deleteTask"
            @edit="editTask"
            @edit-require-estimate="editTaskRequireEstimate"
          />
        </div>

        <!-- 未分类任务 -->
        <div v-if="unclassifiedTasks.length > 0" class="unclassified-list" style="margin-top: 12px">
          <div class="section-title">未分类 / 其他</div>
          <TaskItem
            v-for="task in unclassifiedTasks"
            :key="task.id"
            :task="task"
            @complete="completeTask"
            @delete="deleteTask"
            @edit="editTask"
            @edit-require-estimate="editTaskRequireEstimate"
          />
        </div>
      </ChartCard>

      <ChartCard title="任务与专注趋势" class="flex-1">
        <div class="range-switch">
          <el-button-group>
            <el-button :type="timeRange === 'week' ? 'primary' : 'default'" @click="setRange('week')">本周</el-button>
            <el-button :type="timeRange === 'month' ? 'primary' : 'default'" @click="setRange('month')">本月</el-button>
          </el-button-group>
        </div>
        <div id="combinedChart" style="height: 320px"></div>
      </ChartCard>
    </div>

    <div class="check-anim" v-if="showCheck">✓</div>

    <!-- AI 助手浮窗（仅普通用户显示，管理员不显示） -->
    <AIChatAssistant v-if="userStore.user?.role !== 'admin'" />

    <!-- 编辑任务对话框（与 /dashboard/tasks 的新增表单保持一致） -->
    <el-dialog v-model="editDialogVisible" title="编辑任务" width="520px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="editForm.title" placeholder="输入任务标题" />
        </el-form-item>

        <el-form-item label="分类">
          <el-select v-model="editForm.categoryId" placeholder="选择分类" clearable>
            <el-option v-for="cat in taskStore.categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="优先级">
          <el-radio-group v-model="editForm.priority">
            <el-radio value="high">高</el-radio>
            <el-radio value="medium">中</el-radio>
            <el-radio value="low">低</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="起始时间">
          <el-date-picker v-model="editForm.startTime" type="datetime" placeholder="选择日期时间" style="width:100%" />
        </el-form-item>

        <el-form-item label="预估时长">
          <DurationSlider ref="editDurationSlider" v-model="editEstimatedMinutes" :min="0" :max="480" :step="5" :invalid="editRequireEstimateFocus && editEstimatedMinutes === 0" placeholder="请完善预估时长（必填）" />
        </el-form-item>

        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeEditDialog">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 完成任务弹窗（填写开始时间与持续时长） -->
    <el-dialog v-model="completeDialogVisible" title="完成任务 - 填写时间" width="520px">
      <el-form label-width="120px">
        <el-form-item label="开始时间">
          <el-date-picker v-model="completeStartTime" type="datetime" placeholder="选择开始时间" style="width:100%" />
        </el-form-item>
        <el-form-item label="持续时长（分钟）">
          <el-input-number v-model="completeDuration" :min="1" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmComplete">确认并完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { useUserStore } from '@/store/user'
import { useTaskStore } from '@/store/task'
import { useTimeRecordStore } from '@/store/time-record'
import { useScheduleStore } from '@/store/schedule'
import ChartCard from '@/components/ChartCard.vue'
import TaskItem from '@/components/TaskItem.vue'
import DurationSlider from '@/components/DurationSlider.vue'
import AIChatAssistant from '@/components/user/AIChatAssistant.vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const taskStore = useTaskStore()
const timeRecordStore = useTimeRecordStore()
const scheduleStore = useScheduleStore()

const currentDate = ref('')
const taskStats = reactive({
  total: 0,
  completionRate: 0,
  weeklyFocusHours: 0,
})

const quote = ref('')
const quotes = [
  '今日努力一点，明日更靠近目标。',
  '专注是一种习惯，也是一种能力。',
  '把复杂事情拆成小任务，逐个击破。',
  '休息也是高效工作的关键。',
]

const streak = ref(0)
const timeRange = ref<'week'|'month'>('week')
const quickInput = ref('')
const upcomingSchedules = ref<any[]>([])
// 编辑任务对话与表单
const editDialogVisible = ref(false)
const editingTask = ref<any>(null)
const editForm = reactive({
  id: null as number | null,
  title: '',
  categoryId: null as number | null,
  priority: 'medium' as 'high'|'medium'|'low',
  startTime: null as any,
  estimatedTime: 0,
  description: '',
  completed: false,
})

// 将 editForm.estimatedTime（小时）与滑动条 minutes 绑定互转
const editEstimatedMinutes = computed({
  get: () => Math.round((editForm.estimatedTime ?? 0) * 60),
  set: (v: number) => { editForm.estimatedTime = Math.round((v || 0) / 60 * 100) / 100 },
})

const editRequireEstimateFocus = ref(false)
const editDurationSlider = ref<any>(null)
const showCheck = ref(false)

// 今日任务：仅包含未完成且 startTime 精确等于今天的任务
const todayTasks = computed(() => {
  const today = dayjs()
  return taskStore.tasks
    .filter((t: any) => {
      if (t.completed) return false
      if (!t.startTime) return false // 无起始时间的任务视为未分类，不属于今日
      try {
        return dayjs(t.startTime).isSame(today, 'day')
      } catch (error) {
        return false
      }
    })
    .sort((a: any, b: any) => dayjs(a.startTime).valueOf() - dayjs(b.startTime).valueOf())
    .slice(0, 5)
})

// 明日任务：未完成且 startTime 在明天的任务，按优先级/收藏/时间排序，最多 3 条
const tomorrowTasksSorted = computed(() => {
  const tomorrow = dayjs().add(1, 'day')
  const mapPriority = (p: any) => (p === 'high' ? 3 : p === 'medium' ? 2 : 1)
  return taskStore.tasks
    .filter((t: any) => {
      if (t.completed) return false
      if (!t.startTime) return false
      return dayjs(t.startTime).isSame(tomorrow, 'day')
    })
    .sort((a: any, b: any) => {
      const pa = mapPriority(a.priority)
      const pb = mapPriority(b.priority)
      if (pa !== pb) return pb - pa
      const sa = taskStore.isStarred(a.id) ? 1 : 0
      const sb = taskStore.isStarred(b.id) ? 1 : 0
      if (sa !== sb) return sb - sa
      return dayjs(a.startTime).valueOf() - dayjs(b.startTime).valueOf()
    })
    .slice(0, 3)
})

// 未分类 / 无固定日期：不在今天/明天的任务，或者没有截止日期的任务
const unclassifiedTasks = computed(() => {
  const today = dayjs()
  const tomorrow = dayjs().add(1, 'day')
  return taskStore.tasks
    .filter((t: any) => {
      if (t.completed) return false
      if (t.startTime) {
        const dl = dayjs(t.startTime)
        // 过滤掉已过期的
        if (dl.isBefore(today, 'day')) return false
        // 不属于今日和明日的，视为未分类/其他
        if (dl.isSame(today, 'day') || dl.isSame(tomorrow, 'day')) return false
        return true
      }
      // 没有 startTime 的任务归为未分类
      return true
    })
    .slice(0, 5)
})

let combinedChartInst: any = null

// fetchRecordsForRange 已在文件顶部定义，避免重复声明

onMounted(async () => {
  // 日期
  const now = new Date()
  currentDate.value = now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  })

  // 拉取数据
  await taskStore.fetchTasks()
  // 确保已加载分类供编辑对话使用
  try { await taskStore.fetchCategories() } catch (e) { console.warn('fetchCategories failed', e) }
  await fetchRecordsForRange(timeRange.value)
  await scheduleStore.fetchSchedules()

  // 统计
  taskStats.total = taskStore.tasks.length
  taskStats.completionRate = Math.round((taskStore.tasks.filter(t => t.completed).length / taskStats.total) * 100 || 0)
  // 计算本周专注时长（分钟）
  taskStats.weeklyFocusHours = Math.round(
    (timeRecordStore.records.reduce((s, r) => s + (r.duration || 0), 0) / 60) || 0
  )

  // 随机名言
  quote.value = quotes[Math.floor(Math.random() * quotes.length)]

  // 计算连续打卡
  computeStreak()

  // upcoming schedules
  computeUpcoming()

  // 初始化图表
  initCombinedChart()
})

const computeStreak = () => {
  const dates = new Set(timeRecordStore.records.map(r => dayjs(r.startTime).format('YYYY-MM-DD')))
  let cnt = 0
  let d = dayjs()
  while (dates.has(d.format('YYYY-MM-DD'))) {
    cnt++
    d = d.subtract(1, 'day')
  }
  streak.value = cnt
}

// 计算未来三天内（含正在进行）的日程并按起始时间排序
const computeUpcoming = () => {
  const todayStart = dayjs().startOf('day')
  const threeDaysEnd = dayjs().add(3, 'day').endOf('day')
  const arr = (scheduleStore.schedules || []).filter((s: any) => {
    if (!s.startTime) return false
    const st = dayjs(s.startTime)
    const en = s.endTime ? dayjs(s.endTime) : st
    // 若区间 [st,en] 与 [todayStart, threeDaysEnd] 有交集，则认为在未来三天范围内
    return !(en.isBefore(todayStart) || st.isAfter(threeDaysEnd))
  })
  arr.sort((a: any, b: any) => dayjs(a.startTime).valueOf() - dayjs(b.startTime).valueOf())
  upcomingSchedules.value = arr.slice(0, 3)
}

// 将日程格式化为“YYYY-MM-DD” 或 “YYYY-MM-DD 至 YYYY-MM-DD” 的字符串（不展示具体时间）
const formatScheduleDate = (s: any) => {
  if (!s || !s.startTime) return ''
  const st = dayjs(s.startTime)
  const en = s.endTime ? dayjs(s.endTime) : st
  if (st.isSame(en, 'day')) return st.format('YYYY-MM-DD')
  return `${st.format('YYYY-MM-DD')} 至 ${en.format('YYYY-MM-DD')}`
}

const formatShortDateTime = (iso: string) => {
  try { return dayjs(iso).format('MM-DD HH:mm') } catch { return '' }
}

const initCombinedChart = () => {
  const el = document.getElementById('combinedChart')
  if (!el) return
  if (!combinedChartInst) combinedChartInst = echarts.init(el)
  updateCombinedChart()
}

const updateCombinedChart = () => {
  if (!combinedChartInst) return
  const days = timeRange.value === 'week' ? 7 : 30
  const labels: string[] = []
  const finished: number[] = []
  const focusMinutes: number[] = []

  for (let i = days - 1; i >= 0; i--) {
    const d = dayjs().subtract(i, 'day')
    labels.push(d.format('MM-DD'))
    const key = d.format('YYYY-MM-DD')
    const recs = timeRecordStore.records.filter(r => dayjs(r.startTime).format('YYYY-MM-DD') === key)
    focusMinutes.push(recs.reduce((s, r) => s + (r.duration || 0), 0))
    // use unique taskIds in time records as proxy for finished count
    const finishedCount = new Set(recs.map(r => r.taskId)).size
    finished.push(finishedCount)
  }

  combinedChartInst.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['完成任务数', '专注时长（分钟）'] },
    xAxis: [{ type: 'category', data: labels }],
    yAxis: [{ type: 'value', name: '任务数' }, { type: 'value', name: '分钟' }],
    series: [
      { name: '完成任务数', type: 'line', data: finished, smooth: true, itemStyle: { color: '#409EFF' } },
      { name: '专注时长（分钟）', type: 'bar', data: focusMinutes, yAxisIndex: 1, itemStyle: { color: '#67C23A' } },
    ],
  })
}

const setRange = (r: 'week'|'month') => {
  timeRange.value = r
  // 拉取对应范围的数据后再刷新图表
  fetchRecordsForRange(r).then(() => updateCombinedChart())
}

function parseNaturalDatetime(text: string) {
  const t = text || ''
  let base = dayjs()
  if (t.includes('明天')) base = base.add(1, 'day')
  else if (t.includes('后天')) base = base.add(2, 'day')
  else if (t.includes('今天')) base = base

  const m = t.match(/(?:凌晨|早上|上午|中午|下午|晚上)?\s*(\d{1,2})(?:[:点：](\d{1,2}))?/) 
  if (m) {
    let hour = parseInt(m[1])
    const minute = m[2] ? parseInt(m[2]) : 0
    if ((t.includes('下午') || t.includes('晚上')) && hour < 12) hour += 12
    return base.hour(hour).minute(minute).second(0)
  }
  return null
}

const handleQuickAdd = async () => {
  const text = quickInput.value && quickInput.value.trim()
  if (!text) return ElMessage.warning('请输入任务描述')
  let title = text
  let startTime: any = null
  const parsed = parseNaturalDatetime(text)
  if (parsed) {
    startTime = parsed.toISOString()
    // remove time words from title
    title = text.replace(/(今天|明天|后天|下午|晚上|上午|早上|中午|\d{1,2}[:点：]?\d{0,2})/g, '').trim()
  }

  try {
    await taskStore.createTask({
      title: title || '新任务',
      categoryId: null,
      priority: 'medium',
      startTime: startTime || undefined,
      estimatedTime: 1,
      completed: false,
    })
    ElMessage.success('任务已添加')
    quickInput.value = ''
    await taskStore.fetchTasks()
    updateCombinedChart()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}



const deleteTask = async (id: number | undefined) => {
  if (!id) return
  try {
    await ElMessageBox.confirm('确认删除该任务？', '提示', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    await taskStore.deleteTask(id)
    await taskStore.fetchTasks()
    ElMessage.success('任务已删除')
    updateCombinedChart()
  } catch (e) {
    // user cancelled or delete failed
    try {
      // 如果是业务层返回的 Task not found，deleteTask 已处理并刷新列表，这里忽略
      // 其他错误则提示
      if (e && e.message && String(e.message).includes('Task not found')) {
        return
      }
    } catch (err) { /* ignore */ }
    console.error(e)
    ElMessage.error('删除失败')
  }
}

const editTask = (task: any) => {
  if (!task) return
  editingTask.value = task
  editForm.id = task.id || null
  editForm.title = task.title || ''
  editForm.categoryId = task.categoryId != null ? task.categoryId : null
  editForm.priority = (task.priority && (task.priority as string).toLowerCase()) || 'medium'
  editForm.startTime = task.startTime ? dayjs(task.startTime).toDate() : (task.deadline ? dayjs(task.deadline).toDate() : null)
  editForm.estimatedTime = task.estimatedTime != null
    ? Number(task.estimatedTime)
    : (task.estimatedMinutes != null ? Math.round(Number(task.estimatedMinutes) / 60 * 100) / 100 : 0)
  editForm.description = task.description || ''
  editForm.completed = !!task.completed
  editDialogVisible.value = true
}

const editTaskRequireEstimate = (task: any) => {
  if (!task) return
  editingTask.value = task
  editForm.id = task.id || null
  editForm.title = task.title || ''
  editForm.categoryId = task.categoryId != null ? task.categoryId : null
  editForm.priority = (task.priority && (task.priority as string).toLowerCase()) || 'medium'
  editForm.startTime = task.startTime ? dayjs(task.startTime).toDate() : (task.deadline ? dayjs(task.deadline).toDate() : null)
  editForm.estimatedTime = task.estimatedTime != null
    ? Number(task.estimatedTime)
    : (task.estimatedMinutes != null ? Math.round(Number(task.estimatedMinutes) / 60 * 100) / 100 : 0)
  editForm.description = task.description || ''
  editForm.completed = !!task.completed
  editRequireEstimateFocus.value = true
  editDialogVisible.value = true
  nextTick(() => {
    if (editDurationSlider.value && typeof editDurationSlider.value.focusRange === 'function') {
      try { editDurationSlider.value.focusRange() } catch (_) { /* ignore */ }
    }
  })
}

const saveEdit = async () => {
  if (!editingTask.value) return
  if (!editForm.title || !editForm.title.trim()) { ElMessage.warning('任务标题不能为空'); return }
  if (editEstimatedMinutes.value == null || Number(editEstimatedMinutes.value) <= 0) { ElMessage.warning('预估时长为必填项'); return }
  try {
    const orig: any = editingTask.value
    const updates: any = {}

    // title
    if (String(editForm.title || '') !== String(orig.title || '')) updates.title = editForm.title

    // category
    if (String(editForm.categoryId ?? '') !== String(orig.categoryId ?? '')) updates.categoryId = editForm.categoryId

    // priority
    if ((editForm.priority || 'medium') !== (orig.priority || 'medium')) updates.priority = editForm.priority

    // startTime: compare via dayjs to avoid accidental timezone shifts
    const origSt = orig.startTime ? dayjs(orig.startTime) : (orig.deadline ? dayjs(orig.deadline) : null)
    const newSt = editForm.startTime ? dayjs(editForm.startTime) : null
    if (origSt && newSt) {
      if (!origSt.isSame(newSt)) updates.startTime = newSt.format('YYYY-MM-DDTHH:mm:ss')
    } else if (!origSt && newSt) {
      updates.startTime = newSt.format('YYYY-MM-DDTHH:mm:ss')
    } else if (origSt && !newSt) {
      updates.startTime = null
    }

    // estimated time (hours)
    const origEstimatedHours = orig.estimatedTime != null ? Number(orig.estimatedTime) : (orig.estimatedMinutes != null ? Number(orig.estimatedMinutes) / 60 : 0)
    if (Number(editForm.estimatedTime || 0) !== Number(origEstimatedHours || 0)) {
      updates.estimatedTime = Number(editForm.estimatedTime) || 0
    }

    // description
    if (String(editForm.description || '') !== String(orig.description || '')) updates.description = editForm.description

    // completed
    if (!!editForm.completed !== !!orig.completed) updates.completed = !!editForm.completed

    if (Object.keys(updates).length === 0) {
      ElMessage.info('未修改任何字段')
      editDialogVisible.value = false
      return
    }

    await taskStore.updateTask(editingTask.value.id, updates)
    await taskStore.fetchTasks()
    ElMessage.success('任务已更新')
    editDialogVisible.value = false
    editRequireEstimateFocus.value = false
    updateCombinedChart()
  } catch (e) {
    try {
      // @ts-ignore
      if (e && e.message && e.message.includes('Task not found')) {
        await taskStore.fetchTasks()
        ElMessage.info('任务不存在或已被删除，已刷新任务列表')
        editDialogVisible.value = false
        updateCombinedChart()
        return
      }
    } catch (err) { /* ignore */ }
    console.error(e)
    ElMessage.error('更新失败')
  }
}

const closeEditDialog = () => {
  editDialogVisible.value = false
  editRequireEstimateFocus.value = false
}

// 完成任务流程：优先复用已有时间记录；若无则弹窗要求开始时间与持续时长
const completeDialogVisible = ref(false)
const completingTaskId = ref<string | number | null>(null)
const completeStartTime = ref<any>(null)
const completeDuration = ref<number | null>(null)

const confirmComplete = async () => {
  if (!completingTaskId.value) return
  if (!completeStartTime.value || !completeDuration.value) {
    ElMessage.error('请填写开始时间和持续时长（分钟）')
    return
  }
  try {
    const sid = String(completingTaskId.value)
    // 创建时间记录（手动填写）
    await timeRecordStore.createRecord({
      taskId: sid,
      startTime: dayjs(completeStartTime.value).toISOString(),
      endTime: dayjs(completeStartTime.value).add(Number(completeDuration.value), 'minute').toISOString(),
      duration: Number(completeDuration.value),
      note: '手动记录',
    })

    // 将预估时长改为持续时长（小时），同时设置实际分钟数
    await taskStore.updateTask(completingTaskId.value, { completed: true, estimatedTime: Number(completeDuration.value) / 60, actualMinutes: Number(completeDuration.value) })
    await fetchRecordsForRange(timeRange.value)
    await taskStore.fetchTasks()
    ElMessage.success('已记录时间并完成任务')
    completeDialogVisible.value = false
    completingTaskId.value = null
    updateCombinedChart()
  } catch (err) {
    console.error(err)
    ElMessage.error('完成操作失败')
  }
}

const completeTask = async (id: number | undefined, newCompleted?: boolean) => {
  if (!id) return
  const markCompleted = typeof newCompleted === 'boolean' ? newCompleted : true
  if (!markCompleted) {
    // 取消完成
    try {
      await taskStore.updateTask(id, { completed: false })
      await taskStore.fetchTasks()
      updateCombinedChart()
    } catch (e) {
      console.error(e)
      ElMessage.error('取消完成失败')
    }
    return
  }

    try {
      // refresh time records to ensure latest (拉取最近 30 天的记录以覆盖大部分场景)
      try { await fetchRecordsForRange('month') } catch (e) { /* ignore */ }
      const taskObj: any = taskStore.tasks.find((t: any) => String(t.id) === String(id))
      const existing = timeRecordStore.records.filter(r => String(r.taskId) === String(id) && r.endTime)

      // 检查任务是否有预估时长（兼容 estimatedMinutes / estimatedTime）
      const hasEstimate = (() => {
        if (!taskObj) return false
        if (Object.prototype.hasOwnProperty.call(taskObj, 'estimatedMinutes') && taskObj.estimatedMinutes != null) {
          const mins = Number(taskObj.estimatedMinutes)
          if (!Number.isNaN(mins) && mins > 0) return true
        }
        if (Object.prototype.hasOwnProperty.call(taskObj, 'estimatedTime') && taskObj.estimatedTime != null) {
          const hours = Number(taskObj.estimatedTime)
          const mins = Number.isNaN(hours) ? 0 : Math.round(hours * 60)
          if (mins > 0) return true
        }
        return false
      })()

      if (existing && existing.length > 0) {
        if (!hasEstimate) {
          // 任务已有时间记录但未填写预估时长
          ElMessage.warning('该任务已有时间记录，但未填写预估时长，请先完善预估时长后再完成任务')
          if (taskObj) editTaskRequireEstimate(taskObj)
          return
        }
        const total = existing.reduce((s, r) => s + (Number((r as any).duration) || 0), 0)
        await taskStore.updateTask(id, { completed: true, estimatedTime: total / 60, actualMinutes: total })
        await fetchRecordsForRange(timeRange.value)
        await taskStore.fetchTasks()
        ElMessage.success('任务已完成，时间已记录')
        updateCombinedChart()
        return
      }

      // 无已结束的时间记录，弹窗要求用户填写
      completingTaskId.value = id
      completeStartTime.value = null
      completeDuration.value = null
      completeDialogVisible.value = true
  } catch (e) {
    console.error(e)
    ElMessage.error('完成操作失败')
  }
}

// fetch time records for range (week/month)
const fetchRecordsForRange = async (range: 'week'|'month') => {
  const days = range === 'week' ? 7 : 30
  const start = dayjs().subtract(days - 1, 'day').format('YYYY-MM-DD')
  const end = dayjs().format('YYYY-MM-DD')
  try {
    await timeRecordStore.fetchRecords(start, end)
  } catch (err) {
    console.warn('fetchRecordsForRange failed', err)
  }
}
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}

.welcome-card {
  position: relative;
  background: #f5efe0; /* 纯米色背景 */
  color: #2f2f2f;
  padding: 24px;
  border-radius: 8px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.welcome-card h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
}

.welcome-card p {
  margin: 0;
  opacity: 0.8;
}

.welcome-left { max-width: 60%; }
.quote { margin-top: 12px; font-style: italic; opacity: 0.95 }
.streak { margin-top: 8px; font-size: 14px }

.welcome-right { width: 260px; }
.upcoming-title { font-weight: 600; margin-bottom: 8px }
.upcoming-list { background: rgba(255,255,255,0.06); padding: 10px; border-radius: 6px }
.smiley-corner {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 52px;
  height: 52px;
  border-radius: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
  box-shadow: 0 6px 18px rgba(0,0,0,0.12);
  background: linear-gradient(90deg, #F7D6C1 0%, #E0AFA0 20%, #D6B77A 40%, #9BB7D4 60%, #C39BD4 80%, #F6D6A9 100%);
}
.upcoming-item { padding: 6px 8px; border-radius: 4px; background: rgba(255,255,255,0.02); margin-bottom: 8px }

.upcoming-list.upcoming-highlight {
  padding: 6px 10px;
  border-radius: 8px;
}
.upcoming-list.upcoming-highlight .upcoming-item {
  border-left: 6px solid #FFC54F; /* 更明显的左侧金色线 */
  padding-left: 10px;
  background: transparent;
  box-shadow: none;
  animation: goldBlink 1.6s ease-in-out infinite;
}

@keyframes goldBlink {
  0% { box-shadow: 0 0 0 rgba(255,197,79,0.0); }
  50% { box-shadow: 0 6px 18px rgba(255,197,79,0.18); }
  100% { box-shadow: 0 0 0 rgba(255,197,79,0.0); }
}
.upcoming-item .time { font-size: 12px; opacity: 0.8 }
.upcoming-item .title { font-weight: 600 }

.quick-add { display:flex; gap:8px; margin: 12px 0 20px 0 }

.fallback-container { margin-top: 8px }
.fallback-container .section-title { font-weight: 600; margin: 6px 0; color: #333 }
.fallback-container .tomorrow-list .task-item.highlight-gold { margin-bottom: 8px }
.fallback-container .unclassified-list .task-item { margin-bottom: 8px }

.check-anim {
  position: fixed;
  right: 24px;
  bottom: 24px;
  background: #67C23A;
  color: white;
  width: 56px;
  height: 56px;
  border-radius: 28px;
  display:flex;
  align-items:center;
  justify-content:center;
  font-size: 28px;
  box-shadow: 0 8px 20px rgba(0,0,0,0.2);
  animation: pop 0.6s ease;
}

@keyframes pop { 0% { transform: scale(0.2); opacity: 0 } 60% { transform: scale(1.05); opacity: 1 } 100% { transform: scale(1); opacity: 1 } }

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
}

/* Custom empty state for 今日暂无待办任务 */
.custom-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 28px 12px;
  border-radius: 8px;
  background: #fbfdff;
  margin-bottom: 8px;
}
.empty-svg-icon { width: 120px; height: 120px; margin-bottom: 12px; opacity: 0.9 }
.empty-description { font-size: 15px; color: #9aa9bb; font-weight: 600; background: rgba(255,255,255,0.6); padding: 6px 14px; border-radius: 20px }

.today-list .task-item, .tomorrow-list .task-item, .unclassified-list .task-item { margin-bottom: 8px }

.content-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.flex-1 {
  flex: 1;
}

@media (max-width: 1024px) {
  .content-row {
    grid-template-columns: 1fr;
  }
}
</style>
