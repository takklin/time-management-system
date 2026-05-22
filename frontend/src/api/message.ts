import request from '@/utils/request'

export function getMyMessages(params: { page?: number; size?: number } = { page: 1, size: 50 }) {
  return request.get('/v1/user/messages', { params })
}

export function markMessageRead(id: string | number) {
  // silent: true 避免在拦截器中弹出错误提示，调用方可自行处理 404 回退逻辑
  return request.put(`/v1/user/messages/${id}/read`, null, { silent: true })
}

export function deleteMessage(id: string | number) {
  // silent: true 避免在拦截器中弹出错误提示，调用方可自行处理回退逻辑
  return request.delete(`/v1/user/messages/${id}`, { silent: true })
}
