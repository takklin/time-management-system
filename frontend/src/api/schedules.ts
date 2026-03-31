import service from '@/utils/request'

export interface Schedule {
  id?: number
  title: string
  startTime: string
  endTime: string
  taskId?: number
  reminderTime?: string
  description?: string
}

export interface ScheduleQuery {
  startDate?: string
  endDate?: string
  date?: string
  taskId?: number
  page?: number
  pageSize?: number
}

/**
 * 获取日程列表
 */
export function getSchedules(query?: ScheduleQuery) {
  return service.get('/v1/schedules', { params: query })
}

/**
 * 获取单个日程详情
 */
export function getSchedule(id: number) {
  return service.get(`/v1/schedules/${id}`)
}

/**
 * 创建日程
 */
export function createSchedule(data: Schedule) {
  return service.post('/v1/schedules', data)
}

/**
 * 更新日程
 */
export function updateSchedule(id: number, data: Partial<Schedule>) {
  return service.put(`/v1/schedules/${id}`, data)
}

/**
 * 删除日程
 */
export function deleteSchedule(id: number) {
  return service.delete(`/v1/schedules/${id}`)
}

/**
 * 获取指定日期的日程
 */
export function getSchedulesByDate(date: string) {
  return service.get(`/v1/schedules/date/${date}`)
}
