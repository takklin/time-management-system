import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSchedules, createSchedule as apiCreateSchedule, updateSchedule as apiUpdateSchedule, deleteSchedule as apiDeleteSchedule } from '@/api/schedules'

export interface Schedule {
  id?: number
  title: string
  startTime: string
  endTime: string
  taskId?: number
  reminderTime?: string
  description?: string
}

export const useScheduleStore = defineStore('schedule', () => {
  const schedules = ref<Schedule[]>([])
  const loading = ref(false)
  const selectedDate = ref(new Date().toISOString().split('T')[0])

  async function fetchSchedules(startDate?: string, endDate?: string) {
    loading.value = true
    try {
      const today = new Date()
      const firstDay = startDate || new Date(today.getFullYear(), today.getMonth(), 1).toISOString().split('T')[0]
      const lastDay = endDate || new Date(today.getFullYear(), today.getMonth() + 1, 0).toISOString().split('T')[0]

      const response = await getSchedules({ startDate: firstDay, endDate: lastDay })
      // 合并新获取到的日程到现有列表，按 id 去重并以最新数据为准
      const incoming = (response && (response.data || response)) || []
      const map = new Map<string, any>()
      // 先放已有的
      for (const s of schedules.value || []) {
        if (s && s.id != null) map.set(String(s.id), s)
      }
      // 再放新的（覆盖同 id 的旧项）
      for (const s of incoming || []) {
        if (s && s.id != null) map.set(String(s.id), s)
      }
      schedules.value = Array.from(map.values())
      // 尝试同步任何本地回退的待上传日程
      try { await syncLocalSchedules() } catch (e) { console.warn('syncLocalSchedules failed', e) }
    } finally {
      loading.value = false
    }
  }

  async function createSchedule(schedule: Schedule) {
    try {
      const toUpload: any = { ...schedule }
      if (toUpload.taskId === undefined || toUpload.taskId === null) {
        delete toUpload.taskId
      }
      const resp: any = await apiCreateSchedule(toUpload)
      const created = resp?.data || resp
      schedules.value.push(created)
      return created
    } catch (error: any) {
      console.error('Failed to create schedule:', error)
      // 从后端尝试读取消息
      const serverMsg = error?.response?.data?.msg || error?.message || '创建日程失败（服务器错误）'
      // 若缺少开始/结束时间，则不要把不完整数据放到自动重试队列（会导致反复同步失败）
      if (!schedule || !schedule.startTime || !schedule.endTime) {
        ElMessage.warning(`${serverMsg}；日程缺少开始或结束时间，已保存为待处理项，请在日程页面补全并保存。`)
        try { sessionStorage.setItem('ai_pending_create_schedule', JSON.stringify(schedule || {})) } catch (e) { /* ignore */ }
        return schedule
      }

      ElMessage.warning(`${serverMsg}；已在本地保存，稍后会尝试同步。`)
      // 本地回退：生成临时负 id 并标记为 _local
      const localId = Date.now() * -1
      const localSch = { ...schedule, id: localId, _local: true }
      schedules.value.push(localSch)
      const pending = JSON.parse(localStorage.getItem('tm_local_schedules') || '[]')
      pending.push(localSch)
      localStorage.setItem('tm_local_schedules', JSON.stringify(pending))
      return localSch
    }
  }

  // 尝试把本地 pending 的日程同步到后端（非阻塞）
  async function syncLocalSchedules() {
    const pending = JSON.parse(localStorage.getItem('tm_local_schedules') || '[]') as any[]
    if (!pending || pending.length === 0) return
    for (const ps of pending.slice()) {
      try {
        // 如果 pending 条目缺少必要的时间字段，移交为人工待处理，避免反复失败
        if (!ps.startTime || !ps.endTime) {
          console.warn('syncLocalSchedules: skipping invalid pending schedule (missing start/end)', ps)
          try { sessionStorage.setItem('ai_pending_create_schedule', JSON.stringify(ps)) } catch (e) { /* ignore */ }
          const i = pending.findIndex(x => x.id === ps.id)
          if (i !== -1) pending.splice(i, 1)
          continue
        }

        // 移除本地临时字段再上传
        const toUpload = { ...ps }
        delete (toUpload as any)._local
        const resp = await apiCreateSchedule(toUpload)
        const created = resp?.data || resp
        // 替换本地列表中的临时项
        const idx = schedules.value.findIndex(s => s.id === ps.id)
        if (idx !== -1) schedules.value[idx] = created
        // 从 pending 中移除
        const i = pending.findIndex(x => x.id === ps.id)
        if (i !== -1) pending.splice(i, 1)
      } catch (e) {
        console.warn('sync one schedule failed', e)
      }
    }
    localStorage.setItem('tm_local_schedules', JSON.stringify(pending))
  }

  async function updateSchedule(id: number | string, updates: Partial<Schedule>) {
    try {
      const response = await apiUpdateSchedule(id, updates)
      const index = schedules.value.findIndex(s => String(s.id) === String(id))
      if (index !== -1) {
        // 优先使用后端返回的数据（若有），再覆盖为本次提交的 updates，确保本地状态立即反映用户修改
        const merged = { ...schedules.value[index], ...(response || {}), ...updates }
        schedules.value[index] = merged
      } else if (response && (response as any).id != null) {
        // 若本地未找到，但后端返回了完整对象，则加入列表
        schedules.value.push(response as any)
      } else {
        // 兜底：未找到且后端未返回对象，尝试后台刷新以同步最新数据
        try { await fetchSchedules() } catch (e) { console.warn('fetchSchedules after update fallback failed', e) }
      }

      // 若更新涉及时间范围变化，后台异步刷新以保证跨天/跨月变动被正确展示（不阻塞调用方）
      if (updates.startTime || updates.endTime) {
        fetchSchedules().catch(() => {})
      }

      return response
    } catch (error) {
      console.error('Failed to update schedule:', error)
      throw error
    }
  }

  async function deleteSchedule(id: number | string) {
    try {
      await apiDeleteSchedule(id)
      schedules.value = schedules.value.filter(s => String(s.id) !== String(id))
    } catch (error) {
      console.error('Failed to delete schedule:', error)
      throw error
    }
  }

  function reset() {
    schedules.value = []
    selectedDate.value = new Date().toISOString().split('T')[0]
    try { localStorage.setItem('tm_local_schedules', JSON.stringify([])) } catch (e) { /* ignore */ }
  }

  function setSelectedDate(date: string) {
    selectedDate.value = date
  }

  return {
    schedules,
    loading,
    selectedDate,
    fetchSchedules,
    createSchedule,
    updateSchedule,
    deleteSchedule,
    setSelectedDate,
    syncLocalSchedules,
    reset,
  }
})
