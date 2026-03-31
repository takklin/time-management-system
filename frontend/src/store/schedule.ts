import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getSchedules, createSchedule as apiCreateSchedule, updateSchedule as apiUpdateSchedule, deleteSchedule as apiDeleteSchedule } from '@/api/schedules'

export interface Schedule {
  id?: number
  title: string
  startTime: string
  endTime: string
  taskId?: number
  reminderTime?: string
  description?: string
}

export const useScheduleStore = defineStore('schedule', () => {
  const schedules = ref<Schedule[]>([])
  const loading = ref(false)
  const selectedDate = ref(new Date().toISOString().split('T')[0])

  async function fetchSchedules(startDate?: string, endDate?: string) {
    loading.value = true
    try {
      const today = new Date()
      const firstDay = startDate || new Date(today.getFullYear(), today.getMonth(), 1).toISOString().split('T')[0]
      const lastDay = endDate || new Date(today.getFullYear(), today.getMonth() + 1, 0).toISOString().split('T')[0]

      const response = await getSchedules({ startDate: firstDay, endDate: lastDay })
      schedules.value = response.data || response
    } finally {
      loading.value = false
    }
  }

  async function createSchedule(schedule: Schedule) {
    try {
      const response: any = await apiCreateSchedule(schedule)
      schedules.value.push(response)
      return response
    } catch (error) {
      console.error('Failed to create schedule:', error)
      throw error
    }
  }

  async function updateSchedule(id: number, updates: Partial<Schedule>) {
    try {
      const response = await apiUpdateSchedule(id, updates)
      const index = schedules.value.findIndex(s => s.id === id)
      if (index !== -1) {
        schedules.value[index] = { ...schedules.value[index], ...response }
      }
      return response
    } catch (error) {
      console.error('Failed to update schedule:', error)
      throw error
    }
  }

  async function deleteSchedule(id: number) {
    try {
      await apiDeleteSchedule(id)
      schedules.value = schedules.value.filter(s => s.id !== id)
    } catch (error) {
      console.error('Failed to delete schedule:', error)
      throw error
    }
  }

  function setSelectedDate(date: string) {
    selectedDate.value = date
  }

  return {
    schedules,
    loading,
    selectedDate,
    fetchSchedules,
    createSchedule,
    updateSchedule,
    deleteSchedule,
    setSelectedDate,
  }
})
