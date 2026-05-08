<template>
  <div class="todo-item" :class="priorityClass">
    <el-checkbox :model-value="isCompleted" @change="handleComplete" />
    <div class="todo-content">
      <div class="title">
        {{ task.title }}
        <span v-if="priorityClass === 'high-priority'" class="fire">🔥</span>
        <span v-if="task.isCoreAligned" class="badge okr">🎯</span>
        <span class="badge urgency">{{ urgencyLabel }}</span>
        <span class="badge effort">{{ effortLabel }}</span>
      </div>
      <div class="meta">
          <span v-if="formattedDeadline">截止: {{ formattedDeadline }}</span>
        </div>
    </div>
    <div class="actions">
      <el-button size="small" @click.stop="handleEdit">编辑</el-button>
      <el-button size="small" @click="$emit('delay', task)">推迟</el-button>
      <el-button size="small" type="danger" @click="$emit('delete', task)">删除</el-button>
      <el-button v-if="task.isCoreAligned" size="small" type="warning" @click="$emit('request-priority')">请求拍优</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineProps } from 'vue'
import dayjs from 'dayjs'

const props = defineProps({ task: Object, showProcrastinateBadge: { type: Boolean, default: false } })

const isCompleted = computed({
  get: () => (props.task as any).completed,
  set: () => {},
})

function handleComplete(val: boolean) {
  const t = props.task as any
  // emit the whole task for parent to handle completion logic
  // keep the checkbox controlled by parent
  // val is the new checked state
  // Pass both task and new state
  // @ts-ignore
  emitComplete(t, val)
}

const emit = defineEmits<{
  (e: 'edit', task: any): void
  (e: 'delay', task: any): void
  (e: 'delete', task: any): void
  (e: 'request-priority', task?: any): void
  (e: 'complete', task: any, checked: boolean): void
}>()

function emitComplete(task: any, checked: boolean) { emit('complete', task, checked) }
function handleEdit() { emit('edit', (props.task as any)) }
// debug helper
function handleEditDebug() {
  try { console.debug('[TodoItem] edit clicked', props.task) } catch (e) {}
  handleEdit()
}

const priorityClass = computed(() => {
  const t: any = props.task
  if (t.isCoreAligned && t.urgency === 'today') return 'high-priority'
  if (!t.isCoreAligned) return 'low-priority'
  return ''
})

const urgencyLabel = computed(() => {
  const t: any = props.task
  if (!t.urgency) return ''
  if (t.urgency === 'today') return '⏰ 今天必须'
  if (t.urgency === 'week') return '📅 本周内'
  return '📆 可推迟'
})

const effortLabel = computed(() => {
  const t: any = props.task
  if (!t.estimatedEffort) return ''
  if (t.estimatedEffort === 'large') return '🐘 大块任务'
  if (t.estimatedEffort === 'medium') return '⚖️ 中等'
  return '🐜 碎片任务'
})

const formattedEstimate = computed(() => {
  const t: any = props.task
  const mins = t.estimatedMinutes != null ? Number(t.estimatedMinutes) : (t.estimatedEffort === 'large' ? 180 : (t.estimatedEffort === 'medium' ? 60 : 15))
  if (!mins) return '0分钟'
  const h = Math.floor(mins/60)
  const r = mins % 60
  if (h === 0) return `${mins}分钟`
  if (r === 0) return `${h}小时`
  return `${h}小时${r}分钟`
})

const formattedDeadline = computed(() => {
  const t: any = props.task
  // 优先使用 起始时间 + 预估时长 计算截止时间
  try {
    const start = t.startTime
    const mins = t.estimatedMinutes != null ? Number(t.estimatedMinutes) : (t.estimatedTime != null ? Math.round(Number(t.estimatedTime)*60) : null)
    if (start && mins != null && !Number.isNaN(mins)) {
      return dayjs(start).add(mins, 'minute').format('YYYY-MM-DD HH:mm')
    }
  } catch (e) {
    // ignore and fallback
  }
  const tDeadline = t.deadline || t.dueDate || null
  if (!tDeadline) return null
  try { return dayjs(tDeadline).format('YYYY-MM-DD HH:mm') } catch { return tDeadline }
})
</script>

<style scoped>
.todo-item{
  display:flex;
  align-items:center;
  gap:14px;
  padding:12px;
  background: #ffffff;
  border-radius:10px;
  border: 1px solid rgba(16,24,40,0.04);
}
.todo-item:hover{ transform:translateY(-2px); box-shadow:0 10px 24px rgba(16,24,40,0.06); }
.high-priority{ border-left:4px solid #f56c6c; background:linear-gradient(90deg, rgba(245,108,108,0.04), rgba(255,255,255,0)); }
.low-priority{ opacity:0.85; color:#909399; font-style:italic }
.todo-content{ flex:1; min-width:0 }
.title{ font-weight:600; margin-bottom:6px; color:#2c3e50; display:flex; align-items:center; gap:8px; }
.badge{ font-size:12px; margin-left:6px; padding:4px 8px; border-radius:12px; white-space:nowrap }
.badge.okr{ background:#ecf5ff; color:#409eff }
.badge.urgency{ background:#fff7ed; color:#e6a23c }
.badge.effort{ background:#f0f9eb; color:#67c23a }
.meta{ font-size:12px; color:#8a94a6 }
.actions{ display:flex; gap:8px }
.fire{ margin-left:6px; font-size:14px }
</style>
