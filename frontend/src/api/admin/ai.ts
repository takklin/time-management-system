import request from '@/utils/request'

/**
 * 管理员 AI 功能 API
 */

export interface QueryRequest {
  question: string
  sessionId?: string  // 会话ID，用于维持对话上下文；不提供则自动生成
}

export interface QueryResponse {
  answer: string
  rawData: string | null
}

export interface AiAlert {
  id: number
  alertType: string
  severity: 'HIGH' | 'MEDIUM' | 'LOW'
  title: string
  description: string
  suggestion: string
  relatedLogIds: string | null
  isHandled: number
  handledAt: string | null
  createdAt: string
}

export interface AiConfig {
  id: number
  provider: string
  model: string
  isActive: number
  maxTokens: number
  temperature: number
}

export interface HandleAlertRequest {
  note?: string
}

/**
 * 自然语言查询
 */
export interface AdminQueryRequest extends QueryRequest {
  context?: any
  messages?: Array<{ role: string; content: string }>
}

export function queryData(data: AdminQueryRequest): Promise<QueryResponse> {
  return request.post('/v1/admin/ai/query', data)
}

/**
 * 获取危险日志摘要（用于在 AI 提示中附带）
 */
export interface DangerLogSummary {
  summary: string
  logs?: any[]
}

export function getDangerSummary(): Promise<DangerLogSummary> {
  return request.get('/v1/admin/ai/alerts/danger-summary')
}

/**
 * 获取未处理的预警
 */
export function getAlerts(): Promise<AiAlert[]> {
  return request.get('/v1/admin/ai/alerts/unhandled')
}

/**
 * 获取完整系统统计数据（供管理员助手附带上下文使用）
 */
export function getSystemStatistics(days = 30): Promise<any> {
  return request.get(`/v1/admin/system/statistics?days=${days}`)
}

/**
 * 标记预警已处理
 */
export function markAlertHandled(alertId: number, data?: HandleAlertRequest): Promise<void> {
  return request.put(`/v1/admin/ai/alert/${alertId}/handle`, data || {})
}

/**
 * 手动触发日志扫描
 */
export function triggerLogScan(): Promise<void> {
  return request.post('/v1/admin/ai/scan-logs', {})
}

/**
 * ========== AI 配置管理 ==========
 */

/**
 * 获取所有 AI 配置
 */
export function listConfigs(): Promise<AiConfig[]> {
  return request.get('/v1/admin/ai-config/list')
}

/**
 * 切换 AI 提供商
 */
export function switchProvider(provider: string): Promise<any> {
  return request.post(`/v1/admin/ai-config/switch/${provider}`)
}

/**
 * 测试连接
 */
export function testConnection(provider: string): Promise<any> {
  return request.post(`/v1/admin/ai-config/test-connection/${provider}`)
}

/**
 * 获取当前激活配置
 */
export function getCurrentConfig(): Promise<any> {
  return request.get('/v1/admin/ai-config/current')
}
