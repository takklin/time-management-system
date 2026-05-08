import request from '@/utils/request'

export interface CreateBackupReq {
  format?: 'json' | 'sql'
  tables?: string[]
}

export function createBackup(payload: CreateBackupReq) {
  return request.post('/v1/admin/backup/create', payload)
}

export function listBackups(page = 1, size = 10) {
  return request.get('/v1/admin/backup/list', { params: { current: page, size } })
}

export function downloadBackup(id: number) {
  return request.get(`/v1/admin/backup/download/${id}`, { responseType: 'blob' })
}

export function deleteBackup(id: number) {
  return request.delete(`/v1/admin/backup/${id}`)
}

export function restoreUpload(file: File, confirm = false) {
  const fd = new FormData()
  fd.append('file', file)
  fd.append('confirm', String(confirm))
  return request.post('/v1/admin/backup/restore/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export function getBackupStats() {
  return request.get('/v1/admin/backup/stats')
}
