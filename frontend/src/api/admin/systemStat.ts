import request from '@/utils/request'

export function getSystemStat() {
  return request.get('/api/v1/admin/system/stat')
}
