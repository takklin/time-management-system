import service from '@/utils/request'
import dayjs from 'dayjs'

export interface TimeRecord {
  id?: string | number
  taskId: string | number
  startTime: string
  endTime: string
  duration: number
  note?: string
}

export interface TimeRecordQuery {
  startDate?: string
  endDate?: string
  taskId?: number
  dateRange?: [string, string]
  page?: number
  pageSize?: number
}

/**
 * 获取时间记录列表
 */
export function getTimeRecords(query?: TimeRecordQuery) {
  return service.get('/v1/time-records', { params: query })
}

/**
 * 获取单个时间记录
 */
export function getTimeRecord(id: string | number) {
  return service.get(`/v1/time-records/${id}`)
}

/**
 * 创建时间记录
 */
export function createTimeRecord(data: TimeRecord) {
  // normalize payload to backend expectations:
  // - duration -> durationMinutes (DB NOT NULL)
  // - startTime/endTime -> 'YYYY-MM-DDTHH:mm:ss' (LocalDateTime)
  // - recordDate derived from startTime if not provided
  const payload: any = { ...data }
  if (payload.duration !== undefined && payload.durationMinutes === undefined) {
    payload.durationMinutes = payload.duration
    delete payload.duration
  }
  if (payload.startTime) {
    payload.recordDate = payload.recordDate || dayjs(payload.startTime).format('YYYY-MM-DD')
    payload.startTime = dayjs(payload.startTime).format('YYYY-MM-DDTHH:mm:ss')
  }
  if (payload.endTime) {
    payload.endTime = dayjs(payload.endTime).format('YYYY-MM-DDTHH:mm:ss')
  }
  return service.post('/v1/time-records', payload)
}

/**
 * 更新时间记录
 */
export function updateTimeRecord(id: string | number, data: Partial<TimeRecord>) {
  const payload: any = { ...data }
  if (payload.duration !== undefined && payload.durationMinutes === undefined) {
    payload.durationMinutes = payload.duration
    delete payload.duration
  }
  if (payload.startTime) payload.startTime = dayjs(payload.startTime).format('YYYY-MM-DDTHH:mm:ss')
  if (payload.endTime) payload.endTime = dayjs(payload.endTime).format('YYYY-MM-DDTHH:mm:ss')
  return service.put(`/v1/time-records/${id}`, payload)
}

/**
 * 删除时间记录
 */
export function deleteTimeRecord(id: string | number) {
  return service.delete(`/v1/time-records/${id}`)
}

/**
 * 获取统计数据
 */
export function getStatistics(dateRange?: [string, string]) {
  return service.get('/v1/statistics', { params: { dateRange } })
}
