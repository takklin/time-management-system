import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTimeRecords, createTimeRecord as apiCreateTimeRecord, updateTimeRecord as apiUpdateTimeRecord, deleteTimeRecord as apiDeleteTimeRecord } from '@/api/time-records'

export interface TimeRecord {
  id?: number
  taskId: number
  startTime: string
  endTime: string
  duration: number
  notes?: string
}

export const useTimeRecordStore = defineStore('timeRecord', () => {
  const records = ref<TimeRecord[]>([])
  const loading = ref(false)

  async function fetchRecords() {
    loading.value = true
    try {
      const response = await getTimeRecords()
      records.value = response.data || response
    } finally {
      loading.value = false
    }
  }

  async function createRecord(record: TimeRecord) {
    try {
      const response: any = await apiCreateTimeRecord(record)
      records.value.push(response)
      return response
    } catch (error) {
      console.error('Failed to create time record:', error)
      throw error
    }
  }

  async function updateRecord(id: number, updates: Partial<TimeRecord>) {
    try {
      const response = await apiUpdateTimeRecord(id, updates)
      const index = records.value.findIndex(r => r.id === id)
      if (index !== -1) {
        records.value[index] = { ...records.value[index], ...response }
      }
      return response
    } catch (error) {
      console.error('Failed to update time record:', error)
      throw error
    }
  }

  async function deleteRecord(id: number) {
    try {
      await apiDeleteTimeRecord(id)
      records.value = records.value.filter(r => r.id !== id)
    } catch (error) {
      console.error('Failed to delete time record:', error)
      throw error
    }
  }

  return {
    records,
    loading,
    fetchRecords,
    createRecord,
    updateRecord,
    deleteRecord,
  }
})
