import request from '@/utils/request'

export interface LogQuery {
  page?: number
  size?: number
  operator?: string
  action?: string
  riskLevel?: string
  result?: string
  startDate?: string
  endDate?: string
}

export function getLogs(params: LogQuery) {
  return request.get('/v1/admin/logs', { params })
}

export function getLogDetail(logId: number) {
  return request.get(`/v1/admin/logs/${logId}`)
}

export function getUserOperationStats(days: number = 7) {
  return request.get('/v1/admin/logs/stats/user-operations', { params: { days } })
}

export function getHighRiskOperationStats(days: number = 7) {
  return request.get('/v1/admin/logs/stats/high-risk', { params: { days } })
}

export function exportLogs(params: LogQuery) {
  return request.get('/v1/admin/logs/export', { params, responseType: 'blob' })
}

export function cleanupLogs(retentionDays: number = 90) {
  return request.post('/v1/admin/logs/cleanup', { retentionDays })
}

export function seedDemoLogs() {
  return request.post('/v1/admin/logs/seed-demo')
}
