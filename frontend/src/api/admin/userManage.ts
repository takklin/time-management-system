import request from '@/utils/request'

export interface UserQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number
}

export function getUserList(query: UserQuery) {
  return request.get('/api/v1/admin/users', { params: query })
}

export function getUserDetail(userId: number) {
  return request.get(`/api/v1/admin/users/${userId}`)
}

export function updateUserStatus(userId: number, status: number) {
  return request.put(`/api/v1/admin/users/${userId}/status`, { status })
}

export function resetUserPassword(userId: number, password = '123456') {
  return request.put(`/api/v1/admin/users/${userId}/reset-password`, { password })
}
