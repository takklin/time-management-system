import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getTasks, getCategories, createTask as apiCreateTask, updateTask as apiUpdateTask, deleteTask as apiDeleteTask, completeTask as apiCompleteTask } from '@/api/tasks'

export interface Task {
  id?: string | number
  title: string
  categoryId: number | string | null
  categoryName?: string
  priority: 'high' | 'medium' | 'low'
  deadline?: string
  startTime?: string
  estimatedTime?: number
  estimatedMinutes?: number
  actualTime?: number
  description?: string
  completed: boolean
}

export interface Category {
  id: string | number
  name: string
  color?: string
}

// 将后端任务实体规范化为前端使用的 Task 结构
function normalizeTaskFromServer(t: any) {
  if (!t) return t
  const id = t.id ?? t._id ?? t.taskId ?? null
  const categoryId = t.categoryId ?? (t.category && (t.category.id ?? t.category._id)) ?? null
  const categoryName = t.categoryName ?? (t.category && (t.category.name ?? null)) ?? null
  const priority = t.priority ? String(t.priority).toLowerCase() : 'medium'

  const estimatedTime = t.estimatedTime != null
    ? Number(t.estimatedTime)
    : (t.estimatedMinutes != null ? Math.round(Number(t.estimatedMinutes) / 60 * 100) / 100 : (t.estimated_hours != null ? Number(t.estimated_hours) : 0))

  const estimatedMinutes = t.estimatedMinutes != null
    ? Number(t.estimatedMinutes)
    : (t.estimatedTime != null ? Math.round(Number(t.estimatedTime) * 60) : undefined)

  const completed = Boolean(t.completed === true || t.status === 1 || t.status === 'done' || t.status === 'completed')

  const normalized: any = {
    id,
    title: t.title ?? t.name ?? '',
    categoryId,
    categoryName,
    priority: (priority as 'high'|'medium'|'low'),
    deadline: t.deadline ?? t.dueDate ?? t.due_date,
    startTime: t.startTime ?? t.start_time ?? t.startedAt ?? undefined,
    estimatedTime,
    estimatedMinutes,
    actualTime: t.actualTime != null ? Number(t.actualTime) : (t.actualMinutes != null ? Number(t.actualMinutes) / 60 : undefined),
    description: t.description ?? t.note ?? '',
    completed,
  }

  // 保留后端可能携带的 completedAt 字段
  if (t.completedAt || t.completed_at) normalized.completedAt = t.completedAt ?? t.completed_at

  return normalized
}

export const useTaskStore = defineStore('task', () => {
  const tasks = ref<Task[]>([])
  const categories = ref<Category[]>([])
  const loading = ref(false)
  const selectedFilters = ref({
    category: '',
    priority: '',
    status: 'all',
  })
  // 收藏（星标）id，本地持久化
  const starredIds = ref<string[]>(JSON.parse(localStorage.getItem('tm_starred_ids') || '[]').map((x: any) => String(x)))

  // 已归档任务 id 本地持久化（仅前端展现用）
  const archivedIds = ref<string[]>(JSON.parse(localStorage.getItem('tm_archived_ids') || '[]').map((x: any) => String(x)))

  function persistArchived() {
    localStorage.setItem('tm_archived_ids', JSON.stringify(archivedIds.value))
  }

  function persistStarred() {
    localStorage.setItem('tm_starred_ids', JSON.stringify(starredIds.value))
  }

  async function fetchTasks(filters?: any) {
    loading.value = true
    try {
      const response = await getTasks(filters)
      const raw = response.data || response
      // Normalize server tasks: ensure estimatedTime (hours) exists from estimatedMinutes
      try {
        tasks.value = (Array.isArray(raw) ? raw : (raw && raw.data ? raw.data : raw))
          .map((t: any) => normalizeTaskFromServer(t))
      } catch (e) {
        // fallback
        tasks.value = raw
      }
      // 在获取任务后自动归档已完成超过7天的任务（本地标记）
      try { archiveCompletedOlderThan(7) } catch (e) { console.warn('archiveCompletedOlderThan failed', e) }
    } finally {
      loading.value = false
    }
  }

  async function fetchCategories() {
    try {
      const response = await getCategories()
      const raw = response.data || response
      const arr = Array.isArray(raw) ? raw : (raw && raw.data ? raw.data : [])
      // 保持后端返回的 id 类型（number 或 string），避免不必要的类型转换导致匹配失败
      categories.value = (arr as any[]).map((c: any) => ({ id: c.id, name: c.name, color: c.color }))
    } catch (error) {
      console.error('Failed to fetch categories:', error)
    }
  }

  function isStarred(id?: string | number) {
    if (id == null) return false
    return starredIds.value.includes(String(id))
  }

  function toggleStar(id?: string | number) {
    if (id == null) return
    const sid = String(id)
    const idx = starredIds.value.indexOf(sid)
    if (idx === -1) starredIds.value.push(sid)
    else starredIds.value.splice(idx, 1)
    persistStarred()
  }

  // 任务元数据（子任务/评论/历史）使用 localStorage 存储，键名 tm_task_meta_{id}
  const metaPrefix = 'tm_task_meta_'
  function _metaKey(id: string | number) { return `${metaPrefix}${id}` }
  function readMeta(id: string | number) {
    try {
      const raw = localStorage.getItem(_metaKey(id))
      return raw ? JSON.parse(raw) : { subtasks: [], comments: [], history: [] }
    } catch {
      return { subtasks: [], comments: [], history: [] }
    }
  }
  function writeMeta(id: string | number, meta: any) {
    localStorage.setItem(_metaKey(id), JSON.stringify(meta))
  }
  function pushHistory(id: string | number, entry: { action: string; detail?: string; time?: string }) {
    try {
      const meta = readMeta(id)
      meta.history = meta.history || []
      meta.history.unshift({ ...entry, time: entry.time || dayjs().toISOString() })
      writeMeta(id, meta)
    } catch (e) { console.warn('pushHistory failed', e) }
  }

  async function createTask(task: Task) {
    try {
      // normalize outgoing payload: prefer estimatedMinutes
      const out: any = { ...(task as any) }
      if (Object.prototype.hasOwnProperty.call(out, 'estimatedTime')) {
        out.estimatedMinutes = Math.round(Number(out.estimatedTime) * 60)
        delete out.estimatedTime
      }
      const response: any = await apiCreateTask(out)
      if (response && response.id) {
        const normalized = normalizeTaskFromServer(response)
        tasks.value.push(normalized)
        pushHistory(response.id, { action: 'created', detail: JSON.stringify(normalized) })
        return normalized
      }
      // 后端未返回实体（或返回 null），为稳健性触发列表刷新以获取后端真实数据
      try { await fetchTasks() } catch (e) { console.warn('fetchTasks fallback failed', e) }
      ElMessage.info('创建已提交，已刷新任务列表以获取最新数据')
      return null
    } catch (error) {
      console.error('Failed to create task:', error)
      throw error
    }
  }

  async function updateTask(id: string | number, updates: Partial<Task>) {
    try {
      const index = tasks.value.findIndex(t => String(t.id) === String(id))

      // 规范化更新字段：将前端的 completed/estimatedTime 映射为后端期望的 status/estimatedMinutes
      const normalizedUpdates: any = { ...(updates as any) }
      if (Object.prototype.hasOwnProperty.call(normalizedUpdates, 'completed')) {
        normalizedUpdates.status = normalizedUpdates.completed ? 1 : 0
        normalizedUpdates.completedAt = normalizedUpdates.completed ? new Date().toISOString() : null
        delete normalizedUpdates.completed
      }
      if (Object.prototype.hasOwnProperty.call(normalizedUpdates, 'estimatedTime')) {
        const hours = Number(normalizedUpdates.estimatedTime) || 0
        normalizedUpdates.estimatedMinutes = Math.round(hours * 60)
        delete normalizedUpdates.estimatedTime
      }

      // 构建完整实体：优先使用本地缓存的任务字段，然后覆盖为 normalizedUpdates
      const base = index !== -1 ? { ...tasks.value[index] } : { id }
      // 如果本地有 estimatedTime（小时），优先转换为 estimatedMinutes
      if ((base as any).estimatedTime != null && (base as any).estimatedMinutes == null) {
        (base as any).estimatedMinutes = Math.round(Number((base as any).estimatedTime) * 60)
      }
      const fullPayload: any = { ...base, ...normalizedUpdates }
      fullPayload.id = String(id)

      const response = await apiUpdateTask(String(id), fullPayload)
      if (index !== -1) {
        // 用后端返回的数据更新本地缓存（若后端仅返回实体，则先 normalize）
        if (response && typeof response === 'object') tasks.value[index] = { ...tasks.value[index], ...normalizeTaskFromServer(response) }
        else tasks.value[index] = { ...tasks.value[index], ...fullPayload }
      }
      pushHistory(id, { action: 'updated', detail: JSON.stringify(fullPayload) })
      return response
    } catch (error) {
      // 不在此处执行 fetchTasks 回退；由上层调用者（UI）负责根据错误类型决定是否刷新列表并提示用户，
      // 避免重复刷新和产生额外的错误日志（例如在后端也返回错误时触发二次失败）。
      console.error('Failed to update task:', error)
      throw error
    }
  }

  async function deleteTask(id: string | number) {
    try {
      await apiDeleteTask(String(id))
      tasks.value = tasks.value.filter(t => String(t.id) !== String(id))
      try { pushHistory(id, { action: 'deleted' }) } catch {}
    } catch (error) {
      console.error('Failed to delete task:', error)
      try {
        // 处理业务层 404（message）或 HTTP 404
        // @ts-ignore
        if (error && error.message && String(error.message).includes('Task not found')) {
          await fetchTasks()
          ElMessage.info('任务不存在，已刷新任务列表')
          return
        }
        // @ts-ignore
        if (error && error.response && error.response.status === 404) {
          await fetchTasks()
          ElMessage.info('任务不存在，已刷新任务列表')
          return
        }
      } catch (e) { console.warn('fetchTasks fallback failed', e) }
      throw error
    }
  }

  async function completeTask(id: string | number, completed = true) {
    const index = tasks.value.findIndex(t => String(t.id) === String(id))
    const originalCompleted = index !== -1 ? tasks.value[index].completed : undefined
    if (index !== -1) {
      tasks.value[index].completed = completed
      ;(tasks.value[index] as any).completedAt = completed ? new Date().toISOString() : undefined
    }

    try {
      // 使用 updateTask 保证发送完整实体
      await updateTask(id, { completed })
      try { pushHistory(id, { action: completed ? 'completed' : 'uncompleted' }) } catch {}
      return { success: true }
    } catch (error) {
      console.error('updateTask failed, try fallback complete endpoint:', error)
      // 回退：若后端仍支持旧的 /complete 接口且是标记完成的场景，可尝试回退
      if (completed) {
        try {
          const response = await apiCompleteTask(String(id))
          try { pushHistory(id, { action: 'completed' }) } catch {}
          return response
        } catch (err2) {
          console.error('Failed to complete task (fallback):', err2)
          if (index !== -1) {
            tasks.value[index].completed = originalCompleted as boolean
            if (!originalCompleted) delete (tasks.value[index] as any).completedAt
          }
          ElMessage.error('完成任务失败，请稍后重试')
          throw err2
        }
      }

      // 若为取消完成且回退失败，回滚本地
      if (index !== -1) {
        tasks.value[index].completed = originalCompleted as boolean
        if (!originalCompleted) delete (tasks.value[index] as any).completedAt
      }
      ElMessage.error('更新完成状态失败，请稍后重试')
      throw error
    }
  }

  // 归档相关：将已完成超过 days 的任务本地标记为已归档
  function archiveCompletedOlderThan(days = 7) {
    const now = dayjs()
    for (const t of tasks.value) {
      if (t.id == null) continue
      if ((t as any).completed && (t as any).completedAt) {
        const completedAt = dayjs((t as any).completedAt)
        if (now.diff(completedAt, 'day') >= days) {
          if (!archivedIds.value.includes(String(t.id))) {
            archivedIds.value.push(String(t.id))
            try { pushHistory(t.id, { action: 'archived' }) } catch {}
          }
        }
      }
    }
    persistArchived()
  }

  function isArchived(id?: string | number) {
    if (id == null) return false
    return archivedIds.value.includes(String(id))
  }

  function archiveTask(id?: string | number) {
    if (id == null) return
    const sid = String(id)
    if (!archivedIds.value.includes(sid)) {
      archivedIds.value.push(sid)
      persistArchived()
      try { pushHistory(id, { action: 'archived' }) } catch {}
    }
  }
  function unarchiveTask(id?: string | number) {
    if (id == null) return
    const sid = String(id)
    const idx = archivedIds.value.indexOf(sid)
    if (idx !== -1) {
      archivedIds.value.splice(idx, 1)
      persistArchived()
      try { pushHistory(id, { action: 'unarchived' }) } catch {}
    }
  }

  // 当前视图在页面上显示的任务子集（供番茄钟等组件使用）
  const displayedTasks = ref<any[]>([])
  const displayedTasksSource = ref<string | null>(null)

  function setDisplayedTasks(newTasks: any[], source?: string | null) {
    try {
      displayedTasks.value = Array.isArray(newTasks) ? newTasks : []
      displayedTasksSource.value = source ?? null
    } catch (e) {
      console.warn('setDisplayedTasks failed', e)
    }
  }

  function setSelectedFilters(filters: any) {
    selectedFilters.value = { ...selectedFilters.value, ...filters }
  }

  function reset() {
    tasks.value = []
    categories.value = []
    selectedFilters.value = { category: '', priority: '', status: 'all' }
  }

  // 清理本地持久化项（例如 starred/archived IDs），以避免用户切换时残留
  function resetPersisted() {
    try {
      starredIds.value = []
      archivedIds.value = []
      persistStarred()
      persistArchived()
    } catch (e) { console.warn('resetPersisted failed', e) }
  }

  return {
    tasks,
    categories,
    loading,
    selectedFilters,
    starredIds,
    fetchTasks,
    fetchCategories,
    createTask,
    updateTask,
    deleteTask,
    completeTask,
    isStarred,
    toggleStar,
    readMeta,
    writeMeta,
    pushHistory,
    setSelectedFilters,
    displayedTasks,
    displayedTasksSource,
    setDisplayedTasks,
    archivedIds,
    isArchived,
    archiveTask,
    unarchiveTask,
    archiveCompletedOlderThan,
    reset,
    resetPersisted,
  }
})
