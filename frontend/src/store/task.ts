import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTasks, getCategories, createTask as apiCreateTask, updateTask as apiUpdateTask, deleteTask as apiDeleteTask, completeTask as apiCompleteTask } from '@/api/tasks'

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

export interface Category {
  id: number
  name: string
  color?: string
}

export const useTaskStore = defineStore('task', () => {
  const tasks = ref<Task[]>([])
  const categories = ref<Category[]>([])
  const loading = ref(false)
  const selectedFilters = ref({
    category: '',
    priority: '',
    status: 'all',
  })

  async function fetchTasks() {
    loading.value = true
    try {
      const response = await getTasks()
      tasks.value = response.data || response
    } finally {
      loading.value = false
    }
  }

  async function fetchCategories() {
    try {
      const response = await getCategories()
      categories.value = response.data || response
    } catch (error) {
      console.error('Failed to fetch categories:', error)
    }
  }

  async function createTask(task: Task) {
    try {
      const response: any = await apiCreateTask(task)
      tasks.value.push(response)
      return response
    } catch (error) {
      console.error('Failed to create task:', error)
      throw error
    }
  }

  async function updateTask(id: number, updates: Partial<Task>) {
    try {
      const response = await apiUpdateTask(id, updates)
      const index = tasks.value.findIndex(t => t.id === id)
      if (index !== -1) {
        tasks.value[index] = { ...tasks.value[index], ...response }
      }
      return response
    } catch (error) {
      console.error('Failed to update task:', error)
      throw error
    }
  }

  async function deleteTask(id: number) {
    try {
      await apiDeleteTask(id)
      tasks.value = tasks.value.filter(t => t.id !== id)
    } catch (error) {
      console.error('Failed to delete task:', error)
      throw error
    }
  }

  async function completeTask(id: number) {
    try {
      const response = await apiCompleteTask(id)
      const index = tasks.value.findIndex(t => t.id === id)
      if (index !== -1) {
        tasks.value[index].completed = true
      }
      return response
    } catch (error) {
      console.error('Failed to complete task:', error)
      throw error
    }
  }

  function setSelectedFilters(filters: any) {
    selectedFilters.value = { ...selectedFilters.value, ...filters }
  }

  return {
    tasks,
    categories,
    loading,
    selectedFilters,
    fetchTasks,
    fetchCategories,
    createTask,
    updateTask,
    deleteTask,
    completeTask,
    setSelectedFilters,
  }
})
