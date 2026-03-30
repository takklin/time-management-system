import { defineStore } from 'pinia'
import { getSystemStat } from '@/api/admin/systemStat'

export const useAdminSystemStatStore = defineStore('adminSystemStat', {
  state: () => ({
    stat: { userCount: 0, taskCount: 0, scheduleCount: 0 },
    loading: false
  }),
  actions: {
    async loadSystemStat() {
      this.loading = true
      try {
        const res: any = await getSystemStat()
        this.stat = res.data || res
      } finally {
        this.loading = false
      }
    }
  }
})
