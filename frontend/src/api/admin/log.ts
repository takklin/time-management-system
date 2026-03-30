import request from '@/utils/request'

export function getLogs(params: { page?: number; size?: number; keyword?: string }) {
  return request.get('/api/v1/admin/logs', { params })
}
