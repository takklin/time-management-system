import { defineStore } from 'pinia'
import { getLogs } from '@/api/admin/log'

export const useAdminLogsStore = defineStore('adminLogs', {
  state: () => ({
    logs: [] as any[],
    loading: false,
    total: 0
  }),
  actions: {
    async loadLogs(params: { page?: number; size?: number; keyword?: string }) {
      this.loading = true
      try {
        const res: any = await getLogs(params)
        const data = res.data || res
        this.logs = data.rows || []
        this.total = data.total || this.logs.length
      } finally {
        this.loading = false
      }
    }
  }
})
