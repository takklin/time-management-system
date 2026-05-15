import { defineStore } from 'pinia'
import { ref } from 'vue'
import dayjs from 'dayjs'
import { getTimeRecords, createTimeRecord as apiCreateTimeRecord, updateTimeRecord as apiUpdateTimeRecord, deleteTimeRecord as apiDeleteTimeRecord } from '@/api/time-records'

export interface TimeRecord {
  id?: string | number
  taskTitle?: string | null
  taskId: string | number
  startTime: string
  endTime: string
  duration: number
  note?: string
}

export const useTimeRecordStore = defineStore('timeRecord', () => {
  const records = ref<TimeRecord[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)


  async function fetchRecords(startDate?: string, endDate?: string, retries = 2) {
    loading.value = true
    error.value = null
    try {
        const today = dayjs()
        const firstDay = startDate || today.startOf('month').format('YYYY-MM-DD')
        const lastDay = endDate || today.endOf('month').format('YYYY-MM-DD')

      let attempt = 0
      while (attempt <= retries) {
        try {
          const response = await getTimeRecords({ startDate: firstDay, endDate: lastDay })
          // utils/request 已在拦截器里返回 data 或 data.data，直接使用返回值
          const raw = Array.isArray(response) ? response : (response && response.data ? response.data : [])
          // Normalize backend fields for frontend consistency
          records.value = (raw || []).map((r: any) => {
            const rec: any = { ...(r || {}) }
            rec.note = r.note ?? r.notes
            // try common server-side fields for task title
            rec.taskTitle = r.taskTitle ?? r.task_title ?? r.taskName ?? (r.task && (r.task.title || r.task.name)) ?? undefined

            // normalize startTime/endTime: convert numeric timestamps to ISO strings
            if (rec.startTime && typeof rec.startTime === 'number') {
              rec.startTime = new Date(rec.startTime).toISOString()
            }
            if (rec.endTime && typeof rec.endTime === 'number') {
              rec.endTime = new Date(rec.endTime).toISOString()
            }

            // normalize duration: prefer common fields like duration, durationMinutes
            const durCandidates = [r.duration, r.durationMinutes, r.duration_minutes, r.duration_minute, r.durationMin, r.durationMinues, r.duration_seconds, r.durationSeconds]
            let dur: any = undefined
            for (const d of durCandidates) {
              if (d !== undefined && d !== null) { dur = d; break }
            }
            if (dur === undefined || dur === null || Number.isNaN(Number(dur))) {
              // try to compute from start/end time (minutes)
              try {
                if (rec.startTime && rec.endTime) {
                  const s = new Date(rec.startTime).getTime()
                  const e = new Date(rec.endTime).getTime()
                  if (!Number.isNaN(s) && !Number.isNaN(e) && e >= s) {
                    rec.duration = Math.round((e - s) / 60000)
                  } else {
                    rec.duration = 0
                  }
                } else {
                  rec.duration = 0
                }
              } catch (e) {
                rec.duration = 0
              }
            } else {
              // cast to number; backend commonly returns minutes
              rec.duration = Math.round(Number(dur))
            }

            return rec
          })
          return records.value
        } catch (err: any) {
          attempt++
          if (attempt > retries) {
            // 最后一次失败，记录错误信息
            const msg = (err && err.message) ? err.message : '加载时间记录失败'
            error.value = msg
            throw err
          }
          // 指数/线性退避
          await new Promise((r) => setTimeout(r, 1000 * attempt))
        }
      }
    } finally {
      loading.value = false
    }
  }

  async function createRecord(record: TimeRecord) {
    try {
      const response: any = await apiCreateTimeRecord(record)
      // 后端 create 接口通常返回空 body（Result.success），因此不强制推入本地缓存
      return response
    } catch (error) {
      console.error('Failed to create time record:', error)
      throw error
    }
  }

  async function updateRecord(id: number, updates: Partial<TimeRecord>) {
    try {
      const response = await apiUpdateTimeRecord(id, updates)
      const index = records.value.findIndex(r => r.id === id)
      if (index !== -1) {
        records.value[index] = { ...records.value[index], ...response }
      }
      return response
    } catch (error) {
      console.error('Failed to update time record:', error)
      throw error
    }
  }

  async function deleteRecord(id: string | number) {
    try {
      await apiDeleteTimeRecord(id)
      // 重新从后端拉取，确保后端实际删除或逻辑删除已生效
      try {
        await fetchRecords()
      } catch (e) {
        // 如果刷新失败，依然尝试本地移除以改善 UX
        records.value = records.value.filter(r => r.id !== id)
      }
    } catch (error) {
      console.error('Failed to delete time record:', error)
      // 若删除失败，尝试重新同步列表以避免前端与后端不一致
      try { await fetchRecords() } catch (e) { /* ignore */ }
      throw error
    }
  }

  function reset() {
    records.value = []
    loading.value = false
    error.value = null
  }

  return {
    records,
    loading,
    error,
    fetchRecords,
    createRecord,
    updateRecord,
    deleteRecord,
    reset,
  }
})
