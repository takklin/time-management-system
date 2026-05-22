import request from '@/utils/request'

/**
 * 获取当前用户的告警（分页）
 */
export function getMyAlerts(params: { page?: number; size?: number } = { page: 1, size: 50 }) {
  return request.get('/v1/alerts', { params })
}

export function markMyAlertAsRead(id: number) {
  return request.post(`/v1/alerts/${id}/read`)
}

export function ignoreMyAlert(id: number) {
  return request.delete(`/v1/alerts/${id}`)
}

export function batchReadMyAlerts(ids: number[]) {
  return request.post('/v1/alerts/batch-read', ids)
}

export function clearAllMyAlerts() {
  return request.delete('/v1/alerts/all')
}
