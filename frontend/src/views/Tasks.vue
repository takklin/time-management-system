<template>
  <div class="tasks-container">
    <div class="tasks-header">
      <h1>任务管理</h1>
      <div class="header-actions">
        <el-button-group>
          <el-button :type="viewMode === 'list' ? 'primary' : 'default'" @click="viewMode = 'list'">列表</el-button>
          <el-button :type="viewMode === 'kanban' ? 'primary' : 'default'" @click="viewMode = 'kanban'">看板</el-button>
        </el-button-group>
        <el-button type="primary" @click="showCreateDialog">+ 新增任务</el-button>
      </div>
    </div>

    <div class="tasks-content">
      <div class="filters-sidebar">
        <div class="filter-group">
          <h3>分类</h3>
          <el-select v-model="selectedFilters.categoryId" placeholder="选择分类" clearable @change="fetchTasksWithFilters">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </div>

        <div class="filter-group">
          <h3>优先级</h3>
          <el-checkbox-group v-model="selectedFilters.priority">
            <el-checkbox value="high">高</el-checkbox>
            <el-checkbox value="medium">中</el-checkbox>
            <el-checkbox value="low">低</el-checkbox>
          </el-checkbox-group>
        </div>

        <div class="filter-group">
          <h3>状态</h3>
          <el-radio-group v-model="selectedFilters.status">
            <el-radio value="all">全部</el-radio>
            <el-radio value="active">未完成</el-radio>
            <el-radio value="completed">已完成</el-radio>
          </el-radio-group>
        </div>

        <div class="filter-group">
          <el-checkbox v-model="showOnlyStarred">仅显示我关注的</el-checkbox>
        </div>

        <div class="filter-group">
          <h3>标签</h3>
          <el-select v-model="selectedTags" multiple filterable clearable placeholder="选择标签">
            <el-option v-for="t in uniqueTags" :key="t" :label="t" :value="t" />
          </el-select>
        </div>

        <div class="filter-group">
          <el-button :type="showExpired ? 'danger' : 'default'" plain @click="showExpired = !showExpired">已过期</el-button>
        </div>
      </div>

      <div class="tasks-main">
        <el-empty v-if="filteredTasks.length === 0" description="暂无任务" />

        <div v-if="viewMode === 'list'">
          <!-- 今日任务（使用仪表盘样式的卡片） -->
          <ChartCard title="今日任务" :class="[ groupedTasks.today.length > 0 ? 'today-has-tasks' : 'today-empty' ]">
            <div v-if="groupedTasks.today.length === 0" class="custom-empty-state">
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

            <div v-else class="today-list">
              <div v-for="task in groupedTasks.today" :key="task.id" class="task-row">
                <el-checkbox :model-value="selectedIds.includes(task.id)" @change="(val)=>toggleSelect(task.id, val)" />
                <TaskItem :task="task" :isToday="true" @complete="completeTask" @delete="deleteTask" @edit="editTask" @edit-require-estimate="editTaskRequireEstimate" />
              </div>
            </div>

            <!-- 明日任务 显示在今日卡片下方 -->
            <div v-if="groupedTasks.tomorrow.length > 0" class="tomorrow-list" style="margin-top:12px">
              <div class="section-title">明日任务</div>
              <div v-for="task in groupedTasks.tomorrow" :key="task.id" class="task-row">
                <el-checkbox :model-value="selectedIds.includes(task.id)" @change="(val)=>toggleSelect(task.id, val)" />
                <TaskItem :task="task" :highlight="true" @complete="completeTask" @delete="deleteTask" @edit="editTask" @edit-require-estimate="editTaskRequireEstimate" />
              </div>
            </div>
          </ChartCard>

          <!-- 过期任务（显示在今日/明日之后，且仅保留最近三天内的过期项） -->
          <div v-if="groupedTasks.expired.length > 0" class="group-section">
            <div class="group-title expired">过期任务（{{ groupedTasks.expired.length }}）</div>
            <div v-for="task in groupedTasks.expired" :key="task.id" class="task-row">
              <el-checkbox :model-value="selectedIds.includes(task.id)" @change="(val)=>toggleSelect(task.id, val)" />
              <TaskItem :task="task" @complete="completeTask" @delete="deleteTask" @edit="editTask" @edit-require-estimate="editTaskRequireEstimate" />
            </div>
          </div>

          <!-- Future -->
          <div v-if="groupedTasks.future.length > 0" class="group-section">
            <div class="group-title">未来（{{ groupedTasks.future.length }}）</div>
            <div v-for="task in groupedTasks.future" :key="task.id" class="task-row">
              <el-checkbox :model-value="selectedIds.includes(task.id)" @change="(val)=>toggleSelect(task.id, val)" />
              <TaskItem :task="task" @complete="completeTask" @delete="deleteTask" @edit="editTask" @edit-require-estimate="editTaskRequireEstimate" />
            </div>
          </div>

          <!-- Completed -->
          <div v-if="groupedTasks.completed.length > 0" class="group-section">
            <div class="group-title">已完成（{{ groupedTasks.completed.length }}）</div>
            <div v-for="task in groupedTasks.completed" :key="task.id" class="task-row">
              <el-checkbox :model-value="selectedIds.includes(task.id)" @change="(val)=>toggleSelect(task.id, val)" />
              <TaskItem :task="task" @complete="completeTask" @delete="deleteTask" @edit="editTask" @edit-require-estimate="editTaskRequireEstimate" />
            </div>
          </div>

          <div id="tasks-sentinel" style="height:24px"></div>
          <div class="pagination-actions" v-if="filteredTasks.length > visibleCount">
            <el-button link @click="loadMore">加载更多</el-button>
            <el-pagination :total="filteredTasks.length" :page-size="visibleCount" layout="prev, pager, next" />
          </div>
        </div>

        <div v-else class="kanban-board">
          <!-- 左侧：使用 ChartCard 展示今日待办 / 明日 / 过期（仅保留最近三天） -->
          <div class="kanban-column left-column" @dragover.prevent @drop="(e)=>onDropToColumn(e,'todo')">
            <ChartCard title="今日待办任务" class="left-chartcard">
              <div v-if="groupedTasks.today.length === 0" class="custom-empty-state">
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

              <div v-else class="today-list">
                <div v-for="task in groupedTasks.today" :key="task.id" class="kanban-card" draggable @dragstart="(e)=>onDragStartCard(e,task.id)">
                  <TaskItem :task="task" :isToday="true" @complete="completeTask" @delete="deleteTask" @edit="editTask" @edit-require-estimate="editTaskRequireEstimate" />
                </div>
              </div>

              <!-- 明日任务 -->
              <div v-if="groupedTasks.tomorrow.length > 0" class="tomorrow-list" style="margin-top:12px">
                <div class="section-title">明日任务</div>
                <div v-for="task in groupedTasks.tomorrow" :key="task.id" class="kanban-card" draggable @dragstart="(e)=>onDragStartCard(e,task.id)">
                  <TaskItem :task="task" :highlight="true" @complete="completeTask" @delete="deleteTask" @edit="editTask" @edit-require-estimate="editTaskRequireEstimate" />
                </div>
              </div>

              <!-- 过期任务（最近三天） -->
              <div v-if="groupedTasks.expired.length > 0" class="group-section" style="margin-top:12px">
                <div class="group-title expired">过期任务（{{ groupedTasks.expired.length }}）</div>
                <div v-for="task in groupedTasks.expired" :key="task.id" class="kanban-card" draggable @dragstart="(e)=>onDragStartCard(e,task.id)">
                  <TaskItem :task="task" @complete="completeTask" @delete="deleteTask" @edit="editTask" @edit-require-estimate="editTaskRequireEstimate" />
                </div>
              </div>
            </ChartCard>
          </div>

          <!-- 右侧：进行中（仅 1 个）+ 已完成（剩余空间） -->
          <div class="kanban-column right-column">
            <div class="inprogress-area" @dragover.prevent @drop="(e)=>onDropToColumn(e,'inprogress')">
              <div class="col-title">进行中</div>
              <div v-if="activeTask" class="kanban-card inprogress-card" draggable @dragstart="(e)=>onDragStartCard(e, activeTask.id)">
                <TaskItem :task="activeTask" @complete="completeTask" @delete="deleteTask" @edit="editTask" @edit-require-estimate="editTaskRequireEstimate" />
              </div>
              <div v-else class="empty">暂无进行中任务</div>
            </div>

            <div class="completed-area" @dragover.prevent @drop="(e)=>onDropToColumn(e,'done')">
              <div class="col-title">已完成</div>
              <div class="col-list">
                <div v-for="t in completedSorted" :key="t.id" class="kanban-card" draggable @dragstart="(e)=>onDragStartCard(e,t.id)">
                  <TaskItem :task="t" @complete="completeTask" @delete="deleteTask" @edit="editTask" @edit-require-estimate="editTaskRequireEstimate" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="selectedIds.length > 0" class="bulk-bar">
          <div>{{ selectedIds.length }} 个已选</div>
          <el-button type="danger" @click="bulkDelete">批量删除</el-button>
          <el-select placeholder="批量修改分类" @change="bulkChangeCategory" clearable>
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-select placeholder="批量修改优先级" @change="bulkChangePriority" clearable>
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 编辑/创建对话框 -->
    <el-dialog v-model="showDialog" :title="editingTaskId ? '编辑任务' : '新增任务'" width="600px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="formData.title" placeholder="输入任务标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="formData.categoryName" placeholder="选择分类">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="formData.priority">
            <el-radio value="high">高</el-radio>
            <el-radio value="medium">中</el-radio>
            <el-radio value="low">低</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="起始时间">
          <el-date-picker v-model="formData.startTime" type="datetime" placeholder="选择日期时间" />
        </el-form-item>
        <el-form-item label="预估时长">
          <DurationSlider ref="formDurationSlider" v-model="formEstimatedMinutes" :min="0" :max="480" :step="5" :invalid="requireEstimateFocus && formEstimatedMinutes === 0" placeholder="请完善预估时长（必填）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="4" placeholder="输入任务描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="saveTask">保存</el-button>
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
import { ref, reactive, computed, onMounted, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { useTaskStore } from '@/store/task'
import { usePomodoroStore } from '@/store/pomodoro'
import { useTimeRecordStore } from '@/store/time-record'
import { ElMessage, ElMessageBox } from 'element-plus'
import TaskItem from '@/components/TaskItem.vue'
import DurationSlider from '@/components/DurationSlider.vue'
import ChartCard from '@/components/ChartCard.vue'

// 额外 UI 状态
const viewMode = ref<'list'|'kanban'>('list')
const showOnlyStarred = ref(false)
const selectedTags = ref<string[]>([])
const showExpired = ref(false)
const selectedIds = ref<number[]>([])
const visibleCount = ref(20)

const loadMore = () => { visibleCount.value += 20 }

const taskStore = useTaskStore()
const pomodoroStore = usePomodoroStore()
const timeRecordStore = useTimeRecordStore()
const route = useRoute()
const categories = ref<any[]>([])
const uniqueTags = computed(() => {
  const s = new Set<string>()
  taskStore.tasks.forEach(t => {
    if ((t as any).tags) (t as any).tags.forEach((x: string) => s.add(x))
  })
  return Array.from(s)
})
const showDialog = ref(false)
const editingTaskId = ref<number | null>(null)
const requireEstimateFocus = ref(false)
const sentinelObserver = ref<IntersectionObserver | null>(null)

onBeforeUnmount(() => {
  if (sentinelObserver.value) {
    try { sentinelObserver.value.disconnect() } catch (e) { /* ignore */ }
  }
  try { window.removeEventListener('ai-create-task', aiCreateHandler as EventListener) } catch (e) { /* ignore */ }
})

const aiCreateHandler = (e: any) => {
  const payload = (e && e.detail) ? e.detail : null
  let data = payload
  if (!data) {
    const pending = sessionStorage.getItem('ai_pending_create_task')
    if (pending) data = JSON.parse(pending)
  }
  if (!data) return

  editingTaskId.value = null
  formData.title = data.title || ''
  // 将 categoryName 填入编辑表单（前端显示名称，保存时再映射为 id）
  if (data.categoryName) {
    formData.categoryName = data.categoryName
  } else {
    formData.categoryName = categories.value[0]?.name || null
  }
  formData.priority = data.priority || 'medium'
  // 保证 date-picker 得到 Date 对象，避免后续 Date.toISOString 导致时区偏移
  formData.startTime = data.startTime ? dayjs(data.startTime).toDate() : (data.deadline ? dayjs(data.deadline).toDate() : null)
  if (data.estimatedMinutes != null) {
    formData.estimatedTime = Math.round((data.estimatedMinutes / 60) * 100) / 100
  } else if (data.estimatedTime != null) {
    formData.estimatedTime = data.estimatedTime
  } else {
    formData.estimatedTime = 0
  }
  formData.description = data.description || ''
  showDialog.value = true
  // remove the pending flag
  try { sessionStorage.removeItem('ai_pending_create_task') } catch (err) {}
}
// voice feature removed: startVoice/stopVoice previously existed here

// 过滤器：categoryId、priority、status
const selectedFilters = reactive({
  categoryId: null as number | null,
  priority: [] as string[],
  status: 'all',
})

// 表单数据：categoryName（前端编辑显示为名称，保存时映射为 id）
const formData = reactive({
  title: '',
  categoryName: null as string | null,
  priority: 'medium' as const,
  startTime: null as any,
  estimatedTime: 0,
  description: '',
})

// 将表单的小时（小数）与滑动条的分钟互转，保持后续逻辑兼容
const formEstimatedMinutes = computed({
  get: () => Math.round((formData.estimatedTime ?? 0) * 60),
  set: (v: number) => { formData.estimatedTime = Math.round((v || 0) / 60 * 100) / 100 },
})

const formDurationSlider = ref<any>(null)

const activeTask = computed(() => {
  const aid = pomodoroStore.activeTaskId
  if (aid == null) return null
  return taskStore.tasks.find((t: any) => String(t.id) === String(aid)) || null
})

// 任务列表（已过滤）
const filteredTasks = computed(() => {
  let tasks = taskStore.tasks.slice()
  // 仅收藏
  if (showOnlyStarred.value) {
    tasks = tasks.filter(t => taskStore.isStarred(t.id as number))
  }
  // 标签筛选
  if (selectedTags.value.length > 0) {
    tasks = tasks.filter(t => {
      const tags = (t as any).tags || []
      return selectedTags.value.every(tag => tags.includes(tag))
    })
  }
  // 已过期
  if (showExpired.value) {
    const now = new Date()
    tasks = tasks.filter(t => (t.startTime || t.deadline) && new Date(t.startTime || t.deadline) < now && !t.completed)
  }
  // 本地过滤优先级
  if (selectedFilters.priority.length > 0) {
    tasks = tasks.filter(t => selectedFilters.priority.includes(t.priority))
  }
  // 本地过滤状态
  if (selectedFilters.status === 'active') {
    tasks = tasks.filter(t => !t.completed)
  } else if (selectedFilters.status === 'completed') {
    tasks = tasks.filter(t => t.completed)
  }
  // 分类过滤已由后端处理
  // 过滤掉本地已归档
  tasks = tasks.filter(t => !taskStore.isArchived(t.id))
  return tasks
})

// 将页面上当前过滤后的任务同步到 store，供其他组件（如番茄钟）使用
// NOTE: displayedTasks will be updated below based on current view (visibleTasks or kanban groups)

// 分组：expired / today / tomorrow / future / completed
const groupedTasks = computed(() => {
  const now = dayjs()
  const todayStr = now.format('YYYY-MM-DD')
  const tomorrowStr = now.add(1,'day').format('YYYY-MM-DD')
  const threeDaysAgo = now.subtract(3, 'day').startOf('day')

  const expired: any[] = []
  const today: any[] = []
  const tomorrow: any[] = []
  const future: any[] = []
  const completed: any[] = []

  const priorityOrder = { high: 0, medium: 1, low: 2 }

  for (const t of filteredTasks.value) {
    if ((t as any).completed) { completed.push(t); continue }
    const timeKey = t.startTime || t.deadline
    if (timeKey) {
      const d = dayjs(timeKey)
      // 过期并且在最近三天范围内才保留
      if (d.isBefore(now, 'day')) {
        if (d.isAfter(threeDaysAgo) || d.isSame(threeDaysAgo, 'day')) expired.push(t)
        // 太早的过期任务不展示（丢弃）
      } else if (d.format('YYYY-MM-DD') === todayStr) {
        today.push(t)
      } else if (d.format('YYYY-MM-DD') === tomorrowStr) {
        tomorrow.push(t)
      } else {
        future.push(t)
      }
    } else {
      future.push(t)
    }
  }

  // 先按重要性排序（high > medium > low），重要性相同则按时间升序
  const sortByPriorityThenTime = (arr: any[]) => arr.sort((a: any, b: any) => {
    const pa = priorityOrder[(a as any).priority] ?? 3
    const pb = priorityOrder[(b as any).priority] ?? 3
    if (pa !== pb) return pa - pb
    const ta = (a as any).startTime || (a as any).deadline
    const tb = (b as any).startTime || (b as any).deadline
    if (ta && tb) return dayjs(ta).valueOf() - dayjs(tb).valueOf()
    if (ta) return -1
    if (tb) return 1
    return 0
  })

  sortByPriorityThenTime(today)
  sortByPriorityThenTime(tomorrow)
  // 过期任务按时间降序（最新的过期项先显示）
  expired.sort((a: any, b: any) => {
    const ta = dayjs((a as any).startTime || (a as any).deadline).valueOf()
    const tb = dayjs((b as any).startTime || (b as any).deadline).valueOf()
    return tb - ta
  })
  sortByPriorityThenTime(future)

  return { expired, today, tomorrow, future, completed }
})

// 公共排序器：重要性 -> 时间升序
const sortByPriorityThenTime = (arr: any[]) => {
  const order: Record<string, number> = { high: 0, medium: 1, low: 2 }
  return (arr || []).slice().sort((a: any, b: any) => {
    const pa = order[a.priority] ?? 3
    const pb = order[b.priority] ?? 3
    if (pa !== pb) return pa - pb
    const ta = a.startTime || a.deadline || ''
    const tb = b.startTime || b.deadline || ''
    try {
      const va = ta ? dayjs(ta).valueOf() : 0
      const vb = tb ? dayjs(tb).valueOf() : 0
      return va - vb
    } catch {
      return 0
    }
  })
}

const completedSorted = computed(() => sortByPriorityThenTime(groupedTasks.value.completed || []))

// 获取分类和任务
const fetchCategories = async () => {
  await taskStore.fetchCategories()
  categories.value = taskStore.categories
}

const fetchTasksWithFilters = async () => {
  await taskStore.fetchTasks({
    categoryId: selectedFilters.categoryId,
    status: selectedFilters.status,
  })
  // ensure any open dialog/focus flags reset after fetching
  showDialog.value = false
  requireEstimateFocus.value = false
}

// 监听过滤器变化
watch(
  () => [selectedFilters.categoryId, selectedFilters.priority, selectedFilters.status],
  () => {
    fetchTasksWithFilters()
  },
  { deep: true }
)

onMounted(async () => {
  await fetchCategories()
  await fetchTasksWithFilters()
  // 支持按需加载更多的简易无限滚动：监听 sentinel
  await nextTick()
  const sentinel = document.getElementById('tasks-sentinel')
  if (sentinel && 'IntersectionObserver' in window) {
    const obs = new IntersectionObserver((entries) => {
      entries.forEach(en => { if (en.isIntersecting) loadMore() })
    }, { root: null, threshold: 0.2 })
    obs.observe(sentinel)
    sentinelObserver.value = obs
  }
  // 监听来自 AI 助手的创建任务事件
  try {
    window.addEventListener('ai-create-task', aiCreateHandler as EventListener)
  } catch (e) { /* ignore */ }

  // 如果存在挂起的 AI 创建任务数据，立即填充
  try {
    const pending = sessionStorage.getItem('ai_pending_create_task')
    if (pending) {
      const payload = JSON.parse(pending)
      aiCreateHandler({ detail: payload })
      sessionStorage.removeItem('ai_pending_create_task')
    }
    // 支持队列：ai_pending_create_tasks
    const queued = sessionStorage.getItem('ai_pending_create_tasks')
    if (queued) {
      try {
        const arr = JSON.parse(queued)
        if (Array.isArray(arr) && arr.length > 0) {
          aiCreateHandler({ detail: arr[0] })
          // 不移除队列，保存由 saveTask 继续处理
        }
      } catch (e) { console.warn('failed parse queued tasks', e) }
    }
  } catch (e) { /* ignore */ }
})

const showCreateDialog = () => {
  editingTaskId.value = null
  formData.title = ''
  formData.categoryName = null
  formData.priority = 'medium'
  formData.startTime = null
  formData.estimatedTime = 0
  formData.description = ''
  requireEstimateFocus.value = false
  showDialog.value = true
}

const saveTask = async () => {
  if (!formData.title) {
    ElMessage.error('请输入任务标题')
    return
  }
  if (!formData.categoryName) {
    ElMessage.error('请选择分类')
    return
  }
  if (formEstimatedMinutes.value == null || Number(formEstimatedMinutes.value) <= 0) {
    ElMessage.error('预估时长为必填项')
    return
  }
  try {
    // 将前端的 categoryName 映射为 categoryId 用于后端
    const selectedCat = categories.value.find((c: any) => c.name === formData.categoryName)
    const catId = selectedCat ? selectedCat.id : null
    const payloadBase: any = {
      title: formData.title,
      categoryId: catId,
      priority: formData.priority,
      // 后端期望以分钟为单位：使用 estimatedMinutes 字段传递整数分钟
      estimatedMinutes: formEstimatedMinutes.value,
      description: formData.description,
      completed: false,
    }
    // 以本地 LocalDateTime 字符串发送（不带时区后缀），避免 Date.toISOString() 导致的 UTC 偏移
    if (formData.startTime) {
      payloadBase.startTime = dayjs(formData.startTime).format('YYYY-MM-DDTHH:mm:ss')
    }
    if (editingTaskId.value) {
      await taskStore.updateTask(editingTaskId.value, payloadBase)
      ElMessage.success('任务更新成功')
    } else {
      await taskStore.createTask(payloadBase)
      ElMessage.success('任务创建成功')
    }
    showDialog.value = false
    await fetchTasksWithFilters()
    // 处理 AI 队列：如果存在 ai_pending_create_tasks 队列，则弹出下一个待创建项
    try {
      const queued = sessionStorage.getItem('ai_pending_create_tasks')
      if (queued) {
        const arr = JSON.parse(queued)
        if (Array.isArray(arr) && arr.length > 0) {
          // 当前已保存第一个，移除已处理项
          arr.shift()
          if (arr.length > 0) {
            sessionStorage.setItem('ai_pending_create_tasks', JSON.stringify(arr))
            // 打开下一个任务表单
            setTimeout(() => {
              try { window.dispatchEvent(new CustomEvent('ai-create-task', { detail: arr[0] })) } catch (e) { console.warn('dispatch next queued task failed', e) }
            }, 220)
          } else {
            sessionStorage.removeItem('ai_pending_create_tasks')
          }
        }
      }
    } catch (e) { console.warn('process queued tasks failed', e) }
  } catch (error) {
    try {
      // @ts-ignore
      if (error && error.message && error.message.includes('Task not found')) {
        await taskStore.fetchTasks()
        ElMessage.info('任务不存在或已被删除，已刷新任务列表')
        showDialog.value = false
        return
      }
    } catch (err) { /* ignore */ }
    ElMessage.error('操作失败')
  }
}

const toggleSelect = (id: number | undefined, checked: boolean) => {
  if (!id) return
  const idx = selectedIds.value.indexOf(id)
  if (checked && idx === -1) selectedIds.value.push(id)
  if (!checked && idx !== -1) selectedIds.value.splice(idx, 1)
}

const bulkDelete = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定删除 ${selectedIds.value.length} 个任务吗？`, '提示', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    for (const id of selectedIds.value.slice()) {
      try { await taskStore.deleteTask(id) } catch (e) { console.error(e) }
    }
    selectedIds.value = []
    await fetchTasksWithFilters()
  } catch (e) {
    // cancel or failed
  }
}

const bulkChangeCategory = async (catId: number | null) => {
  if (selectedIds.value.length === 0) return
  for (const id of selectedIds.value) {
    try { await taskStore.updateTask(id, { categoryId: catId }) } catch (e) { console.error(e) }
  }
  selectedIds.value = []
  await fetchTasksWithFilters()
}

const bulkChangePriority = async (prio: 'high'|'medium'|'low') => {
  if (selectedIds.value.length === 0) return
  for (const id of selectedIds.value) {
    try { await taskStore.updateTask(id, { priority: prio }) } catch (e) { console.error(e) }
  }
  selectedIds.value = []
  await fetchTasksWithFilters()
}

// 简易无限滚动：显示前 visibleCount 个任务
const visibleTasks = computed(() => filteredTasks.value.slice(0, visibleCount.value))

// 将当前页面实际展示的任务（基于视图模式）同步到 store，供番茄钟使用
watch(
  () => ({ mode: viewMode.value, visible: visibleTasks.value, groups: groupedTasks.value }),
  () => {
    try {
      let published: any[] = []
      if (viewMode.value === 'list') {
        // 优先使用 今日 列表（groupedTasks.today）作为页面上最显著的展示内容
        const todayList = (groupedTasks.value && groupedTasks.value.today) || []
        if (Array.isArray(todayList) && todayList.length > 0) {
          // 仅发布最显著的前 3 条（与页面 summary 匹配）
          published = (todayList || []).filter((t: any) => !t.completed && !taskStore.isArchived(t.id)).slice(0, 3)
          try { console.debug('[DEBUG] Tasks.vue publishing TODAY items only (limit 3), count=', published.length) } catch (_) {}
        } else {
          published = (visibleTasks.value || []).filter((t: any) => !t.completed && !taskStore.isArchived(t.id)).slice(0, 3)
          try { console.debug('[DEBUG] Tasks.vue publishing VISIBLE items (limit 3), count=', published.length) } catch (_) {}
        }
      } else {
        // kanban: 合并今日/明日/未来/过期（不包含已完成）
        const groups = groupedTasks.value || { today: [], tomorrow: [], future: [], expired: [] }
        const merged = ([] as any[]).concat(groups.today || [], groups.tomorrow || [], groups.future || [], groups.expired || [])
        const map = new Map<string, any>()
        for (const t of merged) {
          if (!t) continue
          const id = String((t as any).id)
          if (taskStore.isArchived(t.id)) continue
          if (!map.has(id)) map.set(id, t)
        }
        published = Array.from(map.values()).filter((t: any) => !t.completed).slice(0, 3)
        try { console.debug('[DEBUG] Tasks.vue publishing KANBAN merged items (limit 3), count=', published.length) } catch (_) {}
      }
      taskStore.setDisplayedTasks(published, route.path)
    } catch (e) { console.warn('setDisplayedTasks failed', e) }
  },
  { immediate: true, deep: true }
)

// 看板拖拽
const onDragStartCard = (e: DragEvent, id: number | undefined) => {
  if (!id) return
  e.dataTransfer?.setData('text/task-id', String(id))
}

const onDropToColumn = async (e: DragEvent, column: string) => {
  e.preventDefault()
  const idStr = e.dataTransfer?.getData('text/task-id')
  if (!idStr) return
  const id = Number(idStr)
  try {
    if (column === 'done') await taskStore.updateTask(id, { completed: true })
    else await taskStore.updateTask(id, { completed: false, status: column })
    await fetchTasksWithFilters()
  } catch (err) { console.error(err) }
}

const deleteTask = (id: number | undefined) => {
  if (id) {
    taskStore.deleteTask(id)
  }
}

const editTask = (task: any) => {
  editingTaskId.value = task.id
  formData.title = task.title
  formData.categoryName = task.categoryName || (categories.value.find((c:any) => String(c.id) === String(task.categoryId))?.name) || null
  formData.priority = task.priority
  // 将后端返回的时间字符串解析为 Date 对象，确保 el-date-picker 使用本地时间
  formData.startTime = task.startTime ? dayjs(task.startTime).toDate() : (task.deadline ? dayjs(task.deadline).toDate() : null)
  formData.estimatedTime = task.estimatedTime != null
    ? Number(task.estimatedTime)
    : (task.estimatedMinutes != null ? Math.round(Number(task.estimatedMinutes) / 60 * 100) / 100 : 0)
  formData.description = task.description
  requireEstimateFocus.value = false
  showDialog.value = true
}

const editTaskRequireEstimate = (task: any) => {
  editingTaskId.value = task.id
  formData.title = task.title
  formData.categoryName = task.categoryName || (categories.value.find((c:any) => String(c.id) === String(task.categoryId))?.name) || null
  formData.priority = task.priority
  formData.startTime = task.startTime ? dayjs(task.startTime).toDate() : (task.deadline ? dayjs(task.deadline).toDate() : null)
  formData.estimatedTime = task.estimatedTime != null
    ? Number(task.estimatedTime)
    : (task.estimatedMinutes != null ? Math.round(Number(task.estimatedMinutes) / 60 * 100) / 100 : 0)
  formData.description = task.description
  requireEstimateFocus.value = true
  showDialog.value = true
  // focus slider after dialog opens
  nextTick(() => {
    if (formDurationSlider.value && typeof formDurationSlider.value.focusRange === 'function') {
      try { formDurationSlider.value.focusRange() } catch (_) { /* ignore */ }
    }
  })
}

const closeDialog = () => {
  showDialog.value = false
  requireEstimateFocus.value = false
}

// 完成任务弹窗（用于手动填写开始时间与持续时长）
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
    await fetchRecordsForRange('month')
    await fetchTasksWithFilters()
    ElMessage.success('已记录时间并完成任务')
    completeDialogVisible.value = false
    completingTaskId.value = null
  } catch (err) {
    console.error(err)
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

// 完成任务：与 Dashboard 一致的校验逻辑
const completeTask = async (id: number | undefined, newCompleted?: boolean) => {
  if (!id) return
  const markCompleted = typeof newCompleted === 'boolean' ? newCompleted : true
  if (!markCompleted) {
    // 取消完成
    try {
      await taskStore.updateTask(id, { completed: false })
      await fetchTasksWithFilters()
    } catch (e) {
      console.error(e)
      ElMessage.error('取消完成失败')
    }
    return
  }

  try {
    // refresh time records
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
        ElMessage.warning('该任务已有时间记录，但未填写预估时长，请先完善预估时长后再完成任务')
        if (taskObj) editTaskRequireEstimate(taskObj)
        return
      }
      const total = existing.reduce((s, r) => s + (Number((r as any).duration) || 0), 0)
      await taskStore.updateTask(id, { completed: true, estimatedTime: total / 60, actualMinutes: total })
      await fetchRecordsForRange('month')
      await fetchTasksWithFilters()
      ElMessage.success('任务已完成，时间已记录')
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
</script>

<style scoped>
.tasks-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.tasks-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.tasks-header h1 {
  font-size: 24px;
  margin: 0;
}

.tasks-content {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 20px;
  flex: 1;
}

.filters-sidebar {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  height: fit-content;
  position: sticky;
  top: 0;
}

.filter-group {
  margin-bottom: 20px;
}

.filter-group h3 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
}

.tasks-main {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
}

.header-actions { display:flex; gap:12px; align-items:center }
.task-row { display:flex; gap:12px; align-items:flex-start; margin-bottom:10px }
.pagination-actions { display:flex; justify-content:center; padding:12px 0 }
.kanban-board { display:flex; gap:12px }
.kanban-column { flex:1; background:#fafafa; border-radius:8px; padding:12px; min-height:200px }
.col-title { font-weight:700; margin-bottom:8px }
.kanban-card { margin-bottom:8px }
.bulk-bar { position:fixed; left:260px; right:24px; bottom:18px; background:#fff; border-radius:8px; padding:10px 16px; display:flex; gap:12px; align-items:center; box-shadow:0 8px 24px rgba(0,0,0,0.08) }

.group-section { margin-bottom: 18px }
.group-title { font-size: 16px; font-weight: 700; margin-bottom: 8px }
.group-title.expired { color: #f56c6c }

@media (max-width: 768px) {
  .tasks-content {
    grid-template-columns: 1fr;
  }

  .filters-sidebar {
    position: static;
  }
}

/* Kanban two-column layout styles */
.kanban-board { display:flex; gap:12px; align-items:stretch }
.kanban-column { flex:1; display:flex; flex-direction:column; background:transparent }
.left-column { min-width: 360px }
.right-column { min-width: 320px }
.left-chartcard { height: 100%; }
.inprogress-area { margin-bottom:12px }
.inprogress-card { margin-bottom:8px }
.completed-area { flex:1; overflow:auto; display:flex; flex-direction:column }
.completed-area .col-list { overflow:auto }
.kanban-card { margin-bottom:8px }
.col-title { font-weight:700; margin-bottom:8px }

</style>
