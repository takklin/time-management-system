import request from '@/utils/request'

/**
 * 获取未处理的预警列表
 */
export function getUnhandledAlerts(limit: number = 10) {
  return request.get('/v1/admin/alerts/unhandled', {
    params: { limit }
  })
}

/**
 * 获取所有预警（支持分页和筛选）
 */
export function getAlerts(params: {
  page?: number
  size?: number
  alertType?: string
  severity?: string
  status?: number
}) {
  return request.get('/v1/admin/alerts', { params })
}

/**
 * 标记预警为已读
 */
export function markAlertAsRead(id: number) {
  return request.post(`/v1/admin/alerts/${id}/read`)
}

/**
 * 标记预警为已确认
 */
export function confirmAlert(id: number, handledBy?: string, remark?: string) {
  return request.post(`/v1/admin/alerts/${id}/confirm`, {
    handledBy,
    remark
  })
}

/**
 * 获取预警统计
 */
export function getAlertStatistics() {
  return request.get('/v1/admin/alerts/statistics')
}

/**
 * 批量确认预警
 */
export function batchConfirmAlerts(alertIds: number[], handledBy?: string) {
  return request.post('/v1/admin/alerts/batch-confirm', alertIds, {
    params: { handledBy }
  })
}

/**
 * 管理员主动通知用户（点对点）
 */
export function notifyUser(alertId: number, message?: string, username?: string) {
  return request.post(`/v1/admin/alerts/${alertId}/notify`, null, {
    params: { message, username }
  })
}

/**
 * 管理员清空所有告警（归档）
 */
export function clearAllAlerts() {
  return request.delete('/v1/admin/alerts/all')
}

/**
 * 删除单条告警（管理员）
 */
export function deleteAlert(id: number) {
  return request.delete(`/v1/admin/alerts/${id}`)
}

/**
 * 批量删除告警（管理员）
 * 传递 body 为 id 数组
 */
export function batchDeleteAlerts(ids: number[]) {
  return request.delete('/v1/admin/alerts', { data: ids })
}
