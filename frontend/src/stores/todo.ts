import { defineStore } from 'pinia'
import { ref } from 'vue'
import dayjs from 'dayjs'

export type Todo = {
  id?: number | string
  title: string
  priority?: 'high'|'medium'|'low'
  startTime?: string | null
  deadline?: string | null
  completed: boolean
  isCoreAligned?: boolean
  urgency?: 'today'|'week'|'later'
  estimatedEffort?: 'large'|'medium'|'small'
  estimatedMinutes?: number
  dependency?: 'blocking'|'independent'|'blocked'
  originTaskId?: string | number
  createdAt?: string
}

export const useTodoStore = defineStore('todo', () => {
  // 初始任务列表为空，避免示例/种子数据在生产环境中出现
  const tasks = ref<Todo[]>([])

  // 计算优先级并归一化到 0-10 区间，便于页面使用固定阈值
  const computePriorityScore = (t: Todo) => {
    const align = t.isCoreAligned ? 3 : 1
    const urgency = t.urgency === 'today' ? 3 : (t.urgency === 'week' ? 2 : 1)
    const effort = t.estimatedEffort === 'large' ? 1 : (t.estimatedEffort === 'medium' ? 2 : 3)
    const dependency = t.dependency === 'blocking' ? 3 : (t.dependency === 'independent' ? 2 : 1)

    const raw = align * 1.5 + urgency * 1.2 + effort * 0.5 + dependency * 1.0
    // raw 最小值 = 1*1.5 + 1*1.2 + 1*0.5 + 1*1 = 4.2
    // raw 最大值 = 3*1.5 + 3*1.2 + 3*0.5 + 3*1 = 12.6
    const minRaw = 4.2
    const maxRaw = 12.6
    const normalized = Math.max(0, Math.min(10, ((raw - minRaw) / (maxRaw - minRaw)) * 10))
    // 返回 0-10 数值，便于前端用固定阈值判断（>=7 高优，>=4 中优，<4 低优）
    return Math.round(normalized * 10) / 10
  }

  function addTask(payload: Partial<Todo>){
    const next: Todo = {
      id: Date.now(),
      title: payload.title || '新任务',
      completed: false,
      deadline: payload.deadline ?? null,
      isCoreAligned: !!payload.isCoreAligned,
      urgency: (payload.urgency as any) ?? 'later',
      estimatedEffort: (payload.estimatedEffort as any) ?? 'small',
      estimatedMinutes: payload.estimatedMinutes ?? ((payload.estimatedEffort === 'large') ? 180 : (payload.estimatedEffort === 'medium' ? 60 : 15)),
      dependency: (payload.dependency as any) ?? 'independent',
      createdAt: dayjs().toISOString(),
    }
    tasks.value.push(next)
    return next
  }

  function deleteTask(id?: number | string){
    const idx = tasks.value.findIndex(t => String(t.id) === String(id))
    if (idx !== -1) tasks.value.splice(idx, 1)
  }

  function delayTask(id?: number | string, days = 1){
    const t = tasks.value.find(x => String(x.id) === String(id))
    if (!t) return
    t.deadline = t.deadline ? dayjs(t.deadline).add(days, 'day').format('YYYY-MM-DD') : dayjs().add(days, 'day').format('YYYY-MM-DD')
  }

  function delayLowPriorityTasks(){
    tasks.value.forEach(t => {
      const score = computePriorityScore(t)
      if (!t.completed && score < 4){
        t.deadline = t.deadline ? dayjs(t.deadline).add(1,'day').format('YYYY-MM-DD') : dayjs().add(1,'day').format('YYYY-MM-DD')
      }
    })
  }

  function markComplete(id?: number | string){
    const t = tasks.value.find(x => String(x.id) === String(id))
    if (!t) return false
    t.completed = true
    return computePriorityScore(t) >= 7
  }

  function setTasksOrder(newOrder: Todo[]){
    tasks.value.splice(0, tasks.value.length, ...newOrder)
  }

  // 从任务管理页导入任务（例如 useTaskStore.tasks）
  function importFromTasks(taskItems: any[]){
    if (!Array.isArray(taskItems)) return
    for (const raw of taskItems){
      const t = raw || {}
      const originId = t.id ?? t._id ?? t.taskId
      if (originId == null) continue

      // 估算耗时（分钟），优先使用 estimatedMinutes，然后 estimatedTime(小时)
      const estMin = t.estimatedMinutes ?? (t.estimatedTime != null ? Math.round(Number(t.estimatedTime)*60) : undefined)
      const startTimeVal = t.startTime ?? t.start_time ?? null
      const estimatedEffort = estMin != null ? (estMin > 120 ? 'large' : (estMin >= 30 ? 'medium' : 'small')) : 'small'

      // 计算截止时间：优先使用后端 deadline/dueDate，否则若存在 startTime 与 estimatedMinutes，则使用 startTime + estimatedMinutes
      const deadlineVal = t.deadline ?? t.dueDate ?? null
      let computedDeadline = deadlineVal
      if (!computedDeadline && startTimeVal && estMin != null) {
        try { computedDeadline = dayjs(startTimeVal).add(estMin, 'minute').toISOString() } catch (e) { computedDeadline = startTimeVal }
      }

      // 紧急度：有截止日按距离天数判断；无则 later
      let urgency: 'today'|'week'|'later' = 'later'
      if (computedDeadline) {
        const diff = dayjs(computedDeadline).startOf('day').diff(dayjs().startOf('day'), 'day')
        if (diff <= 0) urgency = 'today'
        else if (diff <= 7) urgency = 'week'
        else urgency = 'later'
      }
      

      // 与 OKR 对齐判断：优先使用后端显式字段（支持多种命名），否则启发式检测关键词或 priority === 'high'
      let isCoreAligned = false
      const backendCoreFlags = [t.isCoreAligned, t._isCoreAligned, t.is_core_aligned, t.isOKR, t.is_okr]
      for (const f of backendCoreFlags) {
        if (f === true || String(f) === '1') { isCoreAligned = true; break }
      }
      const textProbe = `${t.title || ''} ${t.description || ''} ${t.categoryName || ''}`.toLowerCase()
      if (!isCoreAligned) {
        if (/okr|objective|关键成果|目标|核心|key result|keyresult/.test(textProbe)) isCoreAligned = true
        else if (String(t.priority || '').toLowerCase() === 'high') isCoreAligned = true
      }

      // 依赖关系判定：检查后端字段并根据描述关键词判断是否阻塞或被阻塞
      let dependency: 'blocking'|'independent'|'blocked' = 'independent'
      try {
        // 后端常见字段：blockers / dependencies / blocks / blockedBy
        if (Array.isArray(t.blockers) && t.blockers.length) {
          dependency = 'blocked' // 有 blockers 表示此任务被阻塞
        } else if (Array.isArray(t.dependencies) && t.dependencies.length) {
          dependency = 'blocked' // depends on other tasks
        } else if (Array.isArray(t.blocks) && t.blocks.length) {
          dependency = 'blocking' // blocks others
        } else if (Array.isArray(t.blockedBy) && t.blockedBy.length) {
          dependency = 'blocked'
        } else {
          // 关键字检测：被阻塞倾向 -> 'blocked'，否则若出现阻塞类关键词认为是 'blocking'
          if (/(被阻塞|blocked by|blocked|等待|等待中)/i.test(textProbe)) dependency = 'blocked'
          else if (/(阻塞|blocker|blocking|阻塞他人)/i.test(textProbe)) dependency = 'blocking'
        }
      } catch (e) { console.warn('dependency detection failed', e) }

      const todo: Todo = {
        id: Date.now() + Math.floor(Math.random()*1000),
        title: t.title || t.name || '未命名',
        priority: (t.priority as any) || 'medium',
        startTime: startTimeVal,
        deadline: computedDeadline,
        completed: !!t.completed,
        isCoreAligned,
        urgency,
        estimatedEffort: estimatedEffort as any,
        dependency,
        originTaskId: originId,
        createdAt: dayjs().toISOString(),
      }
      // 若本地已存在与后端任务对应的 mapping，则更新该条并跳过新增，保持本地 id 不变
      const existingIdx = tasks.value.findIndex(x => x.originTaskId != null && String(x.originTaskId) === String(originId))
      if (existingIdx !== -1) {
        const ex = tasks.value[existingIdx]
        tasks.value[existingIdx] = { ...ex, title: todo.title, priority: todo.priority, startTime: todo.startTime, deadline: todo.deadline, completed: todo.completed, isCoreAligned: todo.isCoreAligned, urgency: todo.urgency, estimatedEffort: todo.estimatedEffort, estimatedMinutes: todo.estimatedMinutes, dependency: todo.dependency }
      } else {
        tasks.value.push(todo)
      }
    }
  }

  return { tasks, computePriorityScore, addTask, deleteTask, delayTask, delayLowPriorityTasks, markComplete, setTasksOrder, importFromTasks }
})

export default useTodoStore
