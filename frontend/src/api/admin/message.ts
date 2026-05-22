import request from '@/utils/request'

export function sendMessageToUser(payload: { userId: number; title?: string; content?: string }) {
  return request.post('/v1/admin/messages/send', payload)
}
