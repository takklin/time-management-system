import service from '@/utils/request'

export interface Task {
  id?: number
  title: string
  category: string
  priority: 'high' | 'medium' | 'low'
  deadline?: string
  estimatedTime?: number
  actualTime?: number
  description?: string
  completed: boolean
}

export interface TaskQuery {
  categoryId?: number
  priority?: string
  status?: string
  dateRange?: [string, string]
  search?: string
  page?: number
  pageSize?: number
}

/**
 * 获取任务列表
 */
export function getTasks(query?: TaskQuery) {
  return service.get('/v1/tasks', { params: query })
}

/**
 * 获取单个任务详情
 */
export function getTask(id: number) {
  return service.get(`/v1/tasks/${id}`)
}

/**
 * 创建任务
 */
export function createTask(data: Task) {
  return service.post('/v1/tasks', data)
}

/**
 * 更新任务
 */
export function updateTask(id: number, data: Partial<Task>) {
  return service.put(`/v1/tasks/${id}`, data)
}

/**
 * 删除任务
 */
export function deleteTask(id: number) {
  return service.delete(`/v1/tasks/${id}`)
}

/**
 * 批量删除任务
 */
export function deleteTasks(ids: number[]) {
  return service.post('/v1/tasks/batch-delete', { ids })
}

/**
 * 标记任务为完成
 */
export function completeTask(id: number) {
  return service.post(`/v1/tasks/${id}/complete`)
}

/**
 * 获取任务分类
 */
export function getCategories() {
  return service.get('/v1/categories')
}

/**
 * 创建分类
 */
export function createCategory(data: { name: string; color?: string }) {
  return service.post('/v1/categories', data)
}

/**
 * 更新分类
 */
export function updateCategory(id: number, data: { name: string; color?: string }) {
  return service.put(`/v1/categories/${id}`, data)
}

/**
 * 删除分类
 */
export function deleteCategory(id: number) {
  return service.delete(`/v1/categories/${id}`)
}
