import request from '@/utils/request'

export interface SysConfig {
  id?: number
  configKey: string
  configValue: string
  description?: string
}

export function listConfigs() {
  return request.get('/v1/admin/config/list')
}

export function updateConfig(configKey: string, configValue: string) {
  return request.post('/v1/admin/config/update', { configKey, configValue })
}

export default { listConfigs, updateConfig }
