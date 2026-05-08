import service from '@/utils/request'

export function getTimeDistribution(startDate: string, endDate: string) {
  return service.get('/v1/stats/time-distribution', { params: { startDate, endDate } })
}

export function getCompletionTrend(startDate: string, endDate: string) {
  return service.get('/v1/stats/completion-trend', { params: { startDate, endDate } })
}

export function getDailyFocus(startDate: string, endDate: string) {
  return service.get('/v1/stats/daily-focus', { params: { startDate, endDate } })
}

export function getEstimateVsActual(startDate: string, endDate: string) {
  return service.get('/v1/stats/estimate-vs-actual', { params: { startDate, endDate } })
}

export function getTopTasks(startDate: string, endDate: string, limit: number = 10) {
  return service.get('/v1/stats/top-tasks', { params: { startDate, endDate, limit } })
}

export default {
  getTimeDistribution,
  getCompletionTrend,
  getDailyFocus,
  getEstimateVsActual,
}
