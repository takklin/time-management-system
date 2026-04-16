import request from '@/utils/request'

export interface UserQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number
  orderBy?: string
  orderType?: string
}

export function getUserList(query: UserQuery) {
  return request.get('/v1/admin/users', { params: query })
}

export function getUserDetail(userId: number) {
  return request.get(`/v1/admin/users/${userId}/detail`)
}

export function getUserAnalytics(userId: number) {
  return request.get(`/v1/admin/users/${userId}/analytics`)
}

export function updateUserStatus(userId: number, status: number) {
  return request.put(`/v1/admin/users/${userId}/status`, { status })
}

export function resetUserPassword(userId: number, password = '123456') {
  return request.put(`/v1/admin/users/${userId}/reset-password`, { password })
}

export function deleteUser(userId: number) {
  return request.delete(`/v1/admin/users/${userId}`)
}
