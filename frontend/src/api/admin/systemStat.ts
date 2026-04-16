import request from '@/utils/request'

export function getSystemStat() {
  return request.get('/v1/admin/system/stat')
}

export function getFullStatistics(days: number = 30) {
  return request.get('/v1/admin/system/statistics', { params: { days } })
}

export function getUserGrowth(days: number = 30) {
  return request.get('/v1/admin/system/user-growth', { params: { days } })
}

export function getCompletionRate(days: number = 30) {
  return request.get('/v1/admin/system/completion-rate', { params: { days } })
}

export function getUserRanking(limit: number = 10) {
  return request.get('/v1/admin/system/user-ranking', { params: { limit } })
}
