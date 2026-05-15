import request from '@/utils/request'
import axios from 'axios'

// 当 dev proxy 不可用时的回退后端地址（可通过 VITE_FALLBACK_BACKEND 覆盖）
const FALLBACK_BACKEND = import.meta.env.VITE_FALLBACK_BACKEND || 'http://localhost:8081/api'

async function fallbackPost(path: string, data?: any) {
  const url = `${FALLBACK_BACKEND}${path}`
  const res = await axios.post(url, data, { headers: { 'Content-Type': 'application/json' } })
  const d = res.data
  if (d && (d.code === 200 || d.code === 0)) {
    return d.data || d
  }
  throw new Error(d?.msg || 'Fallback request failed')
}

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
  return request.post('/v1/user/ai/chat', data).catch(async (err: any) => {
    // 若为网络层错误，尝试回退到显式后端地址（避免 dev proxy 未启动导致的 Network Error）
    if (err && err.message && err.message.includes('Network Error')) {
      try { return await fallbackPost('/v1/user/ai/chat', data) } catch (e) { /* fallback failed, rethrow original */ }
    }
    throw err
  })
}

/**
 * 自然语言解析任务
 */
// export function parseTask(data: TaskParseRequest): Promise<ParsedTask> {
//   return request.post('/v1/user/ai/parse-task', data)
// }

/**
 * 获取今日总结
 */
export function getTodaySummary(): Promise<string> {
  return request.get('/v1/user/ai/summary/today').catch(async (err: any) => {
    if (err && err.message && err.message.includes('Network Error')) {
      try { return await fallbackPost('/v1/user/ai/summary/today') } catch (e) { }
    }
    throw err
  })
}

/**
 * 生成任务分解建议
 */
export function getTaskSuggestions(data: TaskSuggestionRequest): Promise<string> {
  return request.post('/v1/user/ai/task-suggestions', data).catch(async (err: any) => {
    if (err && err.message && err.message.includes('Network Error')) {
      try { return await fallbackPost('/v1/user/ai/task-suggestions', data) } catch (e) { }
    }
    throw err
  })
}

/**
 * 向后端发送用户问题与（可选）页面上下文，后端会把上下文拼接到 prompt 中调用 LLM
 */
export function promote(data: PromoteRequest): Promise<any> {
  return request.post('/v1/user/ai/promote', data).catch(async (err: any) => {
    if (err && err.message && err.message.includes('Network Error')) {
      try { return await fallbackPost('/v1/user/ai/promote', data) } catch (e) { }
    }
    throw err
  })
}
