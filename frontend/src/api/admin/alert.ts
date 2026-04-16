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
