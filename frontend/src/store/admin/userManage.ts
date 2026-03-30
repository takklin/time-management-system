import { defineStore } from 'pinia'
import * as api from '@/api/admin/userManage'

export const useAdminUserManageStore = defineStore('adminUserManage', {
  state: () => ({
    users: [] as any[],
    total: 0,
    loading: false,
    detail: null as any
  }),
  actions: {
    async loadUsers(query: api.UserQuery) {
      this.loading = true
      try {
        const res: any = await api.getUserList(query)
        const data = res.data || res
        this.users = data.rows || []
        this.total = data.total || this.users.length
        return { rows: this.users, total: this.total }
      } finally {
        this.loading = false
      }
    },
    async updateStatus(userId: number, status: number) {
      await api.updateUserStatus(userId, status)
    },
    async resetPassword(userId: number) {
      await api.resetUserPassword(userId)
    },
    async loadUserDetail(userId: number) {
      const res: any = await api.getUserDetail(userId)
      this.detail = res.data || res
      return this.detail
    }
  }
})
