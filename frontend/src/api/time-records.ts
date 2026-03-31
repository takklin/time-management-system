import service from '@/utils/request'

export interface TimeRecord {
  id?: number
  taskId: number
  startTime: string
  endTime: string
  duration: number
  notes?: string
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
export function getTimeRecord(id: number) {
  return service.get(`/v1/time-records/${id}`)
}

/**
 * 创建时间记录
 */
export function createTimeRecord(data: TimeRecord) {
  return service.post('/v1/time-records', data)
}

/**
 * 更新时间记录
 */
export function updateTimeRecord(id: number, data: Partial<TimeRecord>) {
  return service.put(`/v1/time-records/${id}`, data)
}

/**
 * 删除时间记录
 */
export function deleteTimeRecord(id: number) {
  return service.delete(`/v1/time-records/${id}`)
}

/**
 * 获取统计数据
 */
export function getStatistics(dateRange?: [string, string]) {
  return service.get('/v1/statistics', { params: { dateRange } })
}
