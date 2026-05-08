<template>
  <div class="task-item" :class="{ 'future-task': isFutureByDate, 'highlight-gold': highlight, 'today-task': isTodayProp }" @contextmenu.prevent="onContextMenu($event)">
    <div class="item-header">
      <el-checkbox v-model="isCompleted" :disabled="loading" @change="handleComplete" />
      <span class="task-title" :class="{ completed: task.completed }" @dblclick="enableEdit" v-if="!editingTitle" v-html="renderedTitle"></span>
      <el-input v-else v-model="editTitle" size="small" class="inline-edit" @blur="saveTitle" @keyup.enter="saveTitle" />
      <div class="header-right">
        <div class="tags">
          <el-tag v-for="t in task.tags || []" :key="t" size="small">{{ t }}</el-tag>
        </div>
        <!-- 显示估时（若有）或提示灯泡 -->
        <div class="estimate-box" v-if="hasEstimate">
          <span class="estimate-text">{{ formattedEstimate }}</span>
        </div>
        <button v-else class="estimate-bulb" title="未填写预估时长，点击完善" @click.stop="requestEditEstimate">💡</button>
        <button class="star-btn" @click.stop="toggleStarLocal">{{ starred ? '★' : '☆' }}</button>
        <button class="pomodoro-btn" @click.stop="togglePomodoro" :title="task.completed ? '已完成任务不可开始番茄钟' : (pomodoroActive ? '停止番茄钟' : '开始番茄钟')" :disabled="task.completed">🍅</button>
        <el-tag :type="getPriorityType(task.priority)" size="small">{{ task.priority }}</el-tag>
      </div>
    </div>
    <div class="item-body">
      <div class="task-info">
        <template v-if="categoryObj">
          <span class="category-badge" :style="categoryStyle">{{ categoryObj.name }}</span>
        </template>
        <template v-else>
          <span class="category">{{ task.categoryName || (task as any).category || '未分类' }}</span>
        </template>
        <span v-if="task.startTime || task.deadline" class="deadline">
          <el-icon><Calendar /></el-icon>
          {{ formattedTime }}
        </span>
      </div>
    </div>
    <div class="item-actions">
      <el-button type="primary" link size="small" @click="handleEdit">编辑</el-button>
      <el-button type="danger" link size="small" @click="handleDelete">删除</el-button>
    </div>

    <div v-if="showMenu" class="context-menu" :style="{ left: menuX + 'px', top: menuY + 'px' }">
      <div class="cm-item" @click="onEdit">编辑</div>
      <div class="cm-item" @click="onDelete">删除</div>
      <div class="cm-item" @click="onCopyLink">复制任务链接</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import dayjs from 'dayjs'
import { Calendar } from '@element-plus/icons-vue'
import { useTaskStore } from '@/store/task'
import { ElMessage } from 'element-plus'
import { usePomodoroStore } from '@/store/pomodoro'

interface Task {
  id?: string | number
  title: string
  category?: string
  categoryId?: string | number
  categoryName?: string
  startTime?: string
  priority: 'high' | 'medium' | 'low'
  deadline?: string
  completed: boolean
}

const props = defineProps<{
  task: Task
  highlight?: boolean
  isToday?: boolean
}>()

const emit = defineEmits<{
  (e: 'edit', task: Task): void
  (e: 'edit-require-estimate', task: Task): void
  (e: 'delete', id?: string | number): void
  (e: 'complete', id?: string | number, completed?: boolean): void
}>()

const loading = ref(false)
const taskStore = useTaskStore()
const starred = computed(() => taskStore.isStarred(props.task.id))
const showMenu = ref(false)
const menuX = ref(0)
const menuY = ref(0)
const pomodoroStore = usePomodoroStore()
const pomodoroActive = computed(() => String(pomodoroStore.activeTaskId) === String(props.task.id))

// inline edit
const editingTitle = ref(false)
const editTitle = ref(props.task.title)

const enableEdit = () => {
  editingTitle.value = true
  editTitle.value = props.task.title
}

const saveTitle = async () => {
  if (!editingTitle.value) return
  editingTitle.value = false
  if (editTitle.value !== props.task.title) {
    try {
      await taskStore.updateTask(props.task.id as string | number, { title: editTitle.value })
      ElMessage.success('更新成功')
    } catch (e: any) {
      // 若后端返回 Task not found（前端临时 id 或已被删除），尝试刷新数据并友好提示
      if (e && e.message && e.message.includes('Task not found')) {
        try { await taskStore.fetchTasks() } catch (err) { /* ignore */ }
        ElMessage.info('任务不存在或已被删除，已刷新任务列表')
        return
      }
      ElMessage.error('更新失败')
    }
  }
}

// simple markdown renderer (支持 **bold** 和 *italic*)
const escapeHtml = (str: string) => str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
const renderMarkdown = (text: string) => {
  if (!text) return ''
  let s = escapeHtml(text)
  s = s.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  s = s.replace(/\*(.+?)\*/g, '<em>$1</em>')
  return s
}
const renderedTitle = computed(() => renderMarkdown(props.task.title))

const toggleStarLocal = () => {
  taskStore.toggleStar(props.task.id)
  ElMessage.success(taskStore.isStarred(props.task.id) ? '已收藏' : '已取消收藏')
}

const togglePomodoro = () => {
  if (!props.task.id) return
  if ((props.task as any).completed) {
    ElMessage.info('已完成任务不能开始番茄钟')
    return
  }
  if (pomodoroActive.value) pomodoroStore.stop()
  else pomodoroStore.start(props.task.id as string | number)
}

const isCompleted = computed({
  get: () => props.task.completed,
  set: () => {
    // This will be handled in handleComplete
  },
})

const getPriorityType = (priority: string) => {
  const map: Record<string, string> = {
    high: 'danger',
    medium: 'warning',
    low: 'success',
  }
  return map[priority] || 'info'
}

const categoryObj = computed(() => {
  const t: any = props.task
  if (!t) return null
  // 优先根据 categoryId 查找
  if (t.categoryId != null) {
    const found = taskStore.categories.find((c: any) => String(c.id) === String(t.categoryId))
    if (found) return found
  }
  // 再尝试 categoryName
  if (t.categoryName) {
    const found = taskStore.categories.find((c: any) => c.name === t.categoryName)
    if (found) return found
    return { name: t.categoryName, color: '#999' }
  }
  // 最后尝试兼容旧字段 category
  if ((t as any).category) {
    const found = taskStore.categories.find((c: any) => c.name === (t as any).category)
    if (found) return found
    return { name: (t as any).category, color: '#999' }
  }
  return null
})

const categoryStyle = computed(() => {
  if (!categoryObj.value) return {}
  const color = categoryObj.value.color || '#999'
  return { backgroundColor: color, color: '#fff', padding: '2px 8px', borderRadius: '6px' }
})

const hasEstimate = computed(() => {
  const t: any = props.task
  if (t == null) return false
  // 优先使用 estimatedMinutes（整数分钟），只有大于 0 时认为已填写
  if (Object.prototype.hasOwnProperty.call(t, 'estimatedMinutes') && t.estimatedMinutes != null) {
    const mins = Number(t.estimatedMinutes)
    return !Number.isNaN(mins) && mins > 0
  }
  // fallback 使用 estimatedTime（小时小数），仅当转换后的分钟数大于 0 时认为已填写
  if (Object.prototype.hasOwnProperty.call(t, 'estimatedTime') && t.estimatedTime != null) {
    const hours = Number(t.estimatedTime)
    const mins = Number.isNaN(hours) ? 0 : Math.round(hours * 60)
    return mins > 0
  }
  return false
})

const formattedEstimate = computed(() => {
  const t: any = props.task
  const mins = t.estimatedMinutes != null ? Number(t.estimatedMinutes) : (t.estimatedTime != null ? Math.round(Number(t.estimatedTime) * 60) : null)
  if (mins == null) return ''
  if (mins === 0) return '0分钟'
  const h = Math.floor(mins / 60)
  const r = mins % 60
  if (h === 0) return `${mins}分钟`
  if (r === 0) return `${h}小时`
  return `${h}小时${r}分钟`
})

// 判断是否为“未来（非今日）”的任务：优先使用 startTime，fallback 使用 deadline
const isFutureByDate = computed(() => {
  const st = (props.task as any).startTime ?? (props.task as any).deadline
  if (!st) return false
  try {
    return dayjs(st).isAfter(dayjs(), 'day')
  } catch {
    return false
  }
})

const formattedTime = computed(() => {
  const raw = (props.task as any).startTime || (props.task as any).deadline
  if (!raw) return ''
  try {
    // 尝试解析并以本地时区格式化为常见展示格式（去掉 'T'）
    return dayjs(raw).format('YYYY-MM-DD HH:mm')
  } catch (e) {
    return String(raw)
  }
})

const highlight = computed(() => !!props.highlight)
const isTodayProp = computed(() => !!props.isToday)

const handleEdit = () => {
  emit('edit', props.task)
}

const requestEditEstimate = () => {
  emit('edit-require-estimate', props.task)
}

const handleDelete = () => {
  if (props.task.id) {
    emit('delete', props.task.id)
  }
}

const onContextMenu = (e: MouseEvent) => {
  showMenu.value = true
  menuX.value = e.clientX
  menuY.value = e.clientY
  window.addEventListener('click', onWindowClick)
}

const onWindowClick = () => {
  showMenu.value = false
  window.removeEventListener('click', onWindowClick)
}

const onEdit = () => { showMenu.value = false; handleEdit() }
const onDelete = () => { showMenu.value = false; handleDelete() }
const onCopyLink = async () => {
  try {
    const url = `${window.location.origin}/dashboard/tasks/${props.task.id}`
    await navigator.clipboard.writeText(url)
    ElMessage.success('任务链接已复制')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

const handleComplete = async (val: boolean) => {
  loading.value = true
  try {
    emit('complete', props.task.id, val)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.task-item {
  background-color: #fff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid #ebeef5;
  transition: all 0.3s;
}

.task-item.future-task {
  border: 1px solid rgba(255, 197, 79, 0.9); /* 淡金色边框 */
  background: #fffaf0; /* 轻微金色背景 */
}

.task-item.today-task {
  border-left: 5px solid #ff7b72;
  border-right: 1px solid #ffe1db;
  border-top: 1px solid #ffe1db;
  border-bottom: 1px solid #ffe1db;
  background: #fffefc;
  box-shadow: 0 2px 8px rgba(255, 90, 70, 0.08);
}
.task-item.today-task:hover {
  border-color: #ffaa9e;
  box-shadow: 0 6px 14px rgba(255, 80, 50, 0.12);
}

.task-item.highlight-gold {
  border: 2px solid #FFC54F;
  background: linear-gradient(90deg, rgba(255,245,230,0.6), rgba(255,255,255,0.6));
  box-shadow: 0 6px 22px rgba(255, 181, 26, 0.12);
  animation: goldPulse 1.6s ease-in-out infinite;
}

@keyframes goldPulse {
  0% { box-shadow: 0 6px 22px rgba(255, 181, 26, 0.06); }
  50% { box-shadow: 0 12px 34px rgba(255, 181, 26, 0.18); }
  100% { box-shadow: 0 6px 22px rgba(255, 181, 26, 0.06); }
}

.task-item:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.task-title {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.task-title.completed {
  color: #ccc;
  text-decoration: line-through;
}

.item-body {
  margin-bottom: 8px;
}

.task-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #999;
}

.category {
  background-color: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
}

.deadline {
  display: flex;
  align-items: center;
  gap: 4px;
}

.item-actions {
  display: flex;
  gap: 10px;
}

.header-right { display:flex; align-items:center; gap:8px }
.star-btn { background:none; border:none; cursor:pointer; font-size:18px; color:#f5a623 }
.pomodoro-btn { background:none; border:none; cursor:pointer; font-size:18px; margin-left:6px; color: #FF6347 }
.pomodoro-btn:hover { transform: scale(1.05); }
.inline-edit { max-width: 60%; }
.tags el-tag { margin-right:4px }
.context-menu {
  position: fixed;
  z-index: 2000;
  background: #fff;
  border: 1px solid #e6e6e6;
  box-shadow: 0 8px 20px rgba(0,0,0,0.08);
  border-radius: 6px;
  overflow: hidden;
}
.cm-item { padding: 8px 12px; cursor: pointer; font-size: 13px }
.cm-item:hover { background: #f5f5f5 }

.estimate-box { margin-right: 8px; display:flex; align-items:center }
.estimate-text { font-size:12px; color:#666; padding:2px 6px; background:#f5f7fa; border-radius:4px }
.estimate-bulb { width:28px; height:28px; border-radius:50%; background:linear-gradient(180deg,#fff8e1,#ffd54f); border:none; cursor:pointer; display:inline-flex; align-items:center; justify-content:center; margin-right:8px; box-shadow:0 0 8px rgba(255,213,79,0.9); animation: bulbBlink 1s infinite }
@keyframes bulbBlink { 0% { transform:scale(1); opacity:1 } 50% { transform:scale(1.08); opacity:0.85 } 100% { transform:scale(1); opacity:1 } }
</style>
