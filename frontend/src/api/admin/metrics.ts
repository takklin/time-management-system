import request from '@/utils/request'

/**
 * 获取系统健康度指标
 */
export function getHealthMetrics(timeRange: number = 60) {
  return request.get('/v1/admin/metrics/health', {
    params: { timeRange }
  })
}

/**
 * 获取最慢的API列表
 */
export function getSlowestApis(timeRange: number = 60) {
  return request.get('/v1/admin/metrics/slowest-apis', {
    params: { timeRange }
  })
}

/**
 * 获取失败的请求列表
 */
export function getFailedRequests(timeRange: number = 60) {
  return request.get('/v1/admin/metrics/failed-requests', {
    params: { timeRange }
  })
}

/**
 * 获取实时QPS
 */
export function getQps() {
  return request.get('/v1/admin/metrics/qps')
}

/**
 * 清空指标数据（仅用于测试）
 */
export function clearMetrics() {
  return request.post('/v1/admin/metrics/clear')
}
