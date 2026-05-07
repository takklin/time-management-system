import request from '@/utils/request'

/**
 * 用户 AI 功能 API
 */

export interface ChatRequest {
  message: string
  model?: string  // 可选：指定 AI 模型 (e.g., "chatgpt3.5", "deepseek")
}

export interface TaskParseRequest {
  message: string
}

export interface ParsedTask {
  title: string | null
  startTime?: string | null
  deadline: string | null
  estimatedMinutes: number | null
  categoryName: string | null
}

export interface TaskSuggestionRequest {
  mainTask: string
}

export interface PromoteRequest {
  question: string
  context?: any
  model?: string
  messages?: Array<{ role: string; content: string }>
}

/**
 * 基础对话
 */
export function chat(data: ChatRequest): Promise<string> {
  return request.post('/v1/user/ai/chat', data)
}

/**
 * 自然语言解析任务
 */
export function parseTask(data: TaskParseRequest): Promise<ParsedTask> {
  return request.post('/v1/user/ai/parse-task', data)
}

/**
 * 获取今日总结
 */
export function getTodaySummary(): Promise<string> {
  return request.get('/v1/user/ai/summary/today')
}

/**
 * 生成任务分解建议
 */
export function getTaskSuggestions(data: TaskSuggestionRequest): Promise<string> {
  return request.post('/v1/user/ai/task-suggestions', data)
}

/**
 * 向后端发送用户问题与（可选）页面上下文，后端会把上下文拼接到 prompt 中调用 LLM
 */
export function promote(data: PromoteRequest): Promise<any> {
  return request.post('/v1/user/ai/promote', data)
}
