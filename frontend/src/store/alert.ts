import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUnhandledAlerts, getAlerts, markAlertAsRead, confirmAlert, batchConfirmAlerts } from '@/api/admin/alert'
import { getMyAlerts, markMyAlertAsRead, ignoreMyAlert, batchReadMyAlerts, clearAllMyAlerts } from '@/api/alert'
import { useUserStore } from '@/store/user'

export const useAlertStore = defineStore('alert', () => {
  const alerts = ref<any[]>([])

  const unreadCount = computed(() => {
    return alerts.value.filter(a => {
      // 支持后端不同字段：status === 0 或 isHandled === 0 或 read === false
      if (a.read === true) return false
      if (a.isHandled !== undefined) return a.isHandled === 0
      if (a.status !== undefined) return a.status === 0
      return true
    }).length
  })

  function normalize(item: any) {
    const obj: any = Object.assign({}, item)
    // 标准字段
    obj.id = obj.id ?? obj.alertId ?? Date.now()
    // 尝试把字符串数字转为 number
    if (typeof obj.id === 'string') {
      const parsed = parseInt(obj.id, 10)
      if (!isNaN(parsed)) obj.id = parsed
    }
    obj.title = obj.title || obj.alertType || obj.type || obj.alert_type || ''
    obj.message = obj.message || obj.description || obj.content || ''
    obj.severity = obj.severity || obj.severityLevel || 'low'
    obj.createdAt = obj.createdAt || obj.created_at || obj.created || new Date().toISOString()
    // read / handled 状态
    if (obj.read === undefined) {
      if (obj.isHandled !== undefined) obj.read = obj.isHandled === 1
      else if (obj.status !== undefined) obj.read = obj.status !== 0
      else obj.read = false
    }
    // 风险分与 AI 建议（兼容多种字段名）
    obj.riskScore = obj.riskScore ?? obj.risk_score ?? obj.score ?? 0
    obj.aiSuggestion = obj.aiSuggestion ?? obj.ai_suggestion ?? obj.suggestion ?? null
    return obj
  }

  function addAlert(payload: any) {
    try {
      const item = normalize(Object.assign({ receivedAt: new Date() }, payload))
      // 去重：如果本地已有相同 id，则跳过
      const exists = alerts.value.some(a => a.id === item.id)
      if (exists) {
        try { console.info('[AlertStore] addAlert skipped duplicate', item.id) } catch (e) {}
        return
      }
      try { console.info('[AlertStore] addAlert', item.id, item.title) } catch (e) {}
      alerts.value.unshift(item)
    } catch (e) {
      console.warn('addAlert error', e)
    }
  }

  async function fetchUnhandled(limit = 20) {
    try {
      const userStore = useUserStore()
      // 普通用户通过用户接口拉取自己的未读告警（支持在登录后把登录期间产生的告警同步过来）
      if (!userStore.user || userStore.user.role !== 'admin') {
        const res: any = await getMyAlerts({ page: 1, size: limit })
        let fetched: any[] = []
        if (res && res.content) fetched = res.content.map(normalize)
        else if (Array.isArray(res)) fetched = res.map(normalize)

        // 合并本地实时告警
        const existingIds = new Set(fetched.map((a: any) => a.id))
        for (const a of alerts.value) {
          if (!existingIds.has(a.id)) fetched.unshift(a)
        }
        alerts.value = fetched
        return alerts.value
      }

      const res: any = await getUnhandledAlerts(limit)
      let fetched: any[] = []
      if (Array.isArray(res)) fetched = res.map(normalize)
      else if (res && res.content) fetched = res.content.map(normalize)

      // 合并：保留本地（实时）告警中不在服务端返回中的项，避免打开面板时丢失未持久化的实时告警
      const existingIds = new Set(fetched.map((a: any) => a.id))
      for (const a of alerts.value) {
        if (!existingIds.has(a.id)) fetched.unshift(a)
      }
      alerts.value = fetched
      try { console.info('[AlertStore] fetchUnhandled -> loaded', fetched.length) } catch (e) {}
      return alerts.value
    } catch (e) {
      console.warn('fetchUnhandled failed', e)
      return []
    }
  }

  async function fetchAlerts(params: any = { page: 1, size: 50 }) {
    try {
      const userStore = useUserStore()
      if (!userStore.user || userStore.user.role !== 'admin') {
        const res: any = await getMyAlerts(params)
        let fetched: any[] = []
        if (res && res.content) fetched = res.content.map(normalize)
        else if (Array.isArray(res)) fetched = res.map(normalize)

        const existingIds = new Set(fetched.map((a: any) => a.id))
        for (const a of alerts.value) {
          if (!existingIds.has(a.id)) fetched.unshift(a)
        }
        alerts.value = fetched
        return alerts.value
      }

      const res: any = await getAlerts(params)
      let fetched: any[] = []
      if (res && res.content) fetched = res.content.map(normalize)
      else if (Array.isArray(res)) fetched = res.map(normalize)

      // 合并本地实时告警（优先保留服务端数据，但保留本地中不在服务端的项）
      const existingIds = new Set(fetched.map((a: any) => a.id))
      for (const a of alerts.value) {
        if (!existingIds.has(a.id)) fetched.unshift(a)
      }
      alerts.value = fetched
      try { console.info('[AlertStore] fetchAlerts -> loaded', fetched.length) } catch (e) {}
      return alerts.value
    } catch (e) {
      console.warn('fetchAlerts failed', e)
      return []
    }
  }

  async function markAsRead(id: number | string) {
    try {
      // 确保 id 为 number
      const numericId = typeof id === 'string' ? parseInt(id as string, 10) : id
      if (Number.isNaN(numericId as any)) {
        console.warn('[AlertStore] markAsRead invalid id', id)
        return false
      }
      const userStore = useUserStore()
      if (!userStore.user || userStore.user.role !== 'admin') {
        await markMyAlertAsRead(numericId as number)
      } else {
        await markAlertAsRead(numericId as number)
      }
      const idx = alerts.value.findIndex(a => a.id === numericId)
      if (idx >= 0) alerts.value[idx].read = true
      else try { console.warn('[AlertStore] markAsRead: id not found locally', numericId) } catch (e) {}
      try { console.info('[AlertStore] markAsRead success', numericId) } catch (e) {}
      return true
    } catch (e) {
      console.warn('markAsRead failed', e)
      try {
        const status = (e && (e as any).response && (e as any).response.status) || (String(e).includes('404') ? 404 : null)
        if (status === 404) {
          const idx = alerts.value.findIndex(a => a.id === numericId)
          if (idx >= 0) alerts.value[idx].read = true
          try { console.info('[AlertStore] markAsRead fallback: marked locally for missing id', numericId) } catch (e) {}
          return true
        }
      } catch (err) {}
      return false
    }
  }

  async function ignoreAlert(id: number | string) {
    try {
      const numericId = typeof id === 'string' ? parseInt(id as string, 10) : id
      if (Number.isNaN(numericId as any)) {
        console.warn('[AlertStore] ignoreAlert invalid id', id)
        return false
      }
      // 标记为已读以表示忽略
      const userStore = useUserStore()
      if (!userStore.user || userStore.user.role !== 'admin') {
        await ignoreMyAlert(numericId as number)
      } else {
        await markAlertAsRead(numericId as number)
      }
      const idx = alerts.value.findIndex(a => a.id === numericId)
      if (idx >= 0) alerts.value.splice(idx, 1)
      try { console.info('[AlertStore] ignoreAlert success', numericId) } catch (e) {}
      return true
    } catch (e) {
      console.warn('ignoreAlert failed', e)
      try {
        const status = (e && (e as any).response && (e as any).response.status) || (String(e).includes('404') ? 404 : null)
        if (status === 404) {
          const numericId = typeof id === 'string' ? parseInt(id as string, 10) : id
          const idx = alerts.value.findIndex(a => a.id === numericId)
          if (idx >= 0) alerts.value.splice(idx, 1)
          try { console.info('[AlertStore] ignoreAlert fallback: removed locally for missing id', numericId) } catch (e) {}
          return true
        }
      } catch (err) {}
      return false
    }
  }

  async function markAllAsRead() {
    try {
      const unreadIds = alerts.value.filter(a => !a.read).map(a => typeof a.id === 'string' ? parseInt(a.id, 10) : a.id).filter(i => !Number.isNaN(i))
      const userStore = useUserStore()
      if (!userStore.user || userStore.user.role !== 'admin') {
        if (unreadIds.length > 0) await batchReadMyAlerts(unreadIds as number[])
      } else {
        await Promise.all((unreadIds as number[]).map(id => markAlertAsRead(id)))
      }
      alerts.value.forEach(a => { a.read = true })
      try { console.info('[AlertStore] markAllAsRead', unreadIds.length) } catch (e) {}
      return true
    } catch (e) {
      console.warn('markAllAsRead failed', e)
      return false
    }
  }

  async function confirm(id: number, handledBy?: string, remark?: string) {
    try {
      await confirmAlert(id, handledBy, remark)
      const idx = alerts.value.findIndex(a => a.id === id)
      if (idx >= 0) alerts.value[idx].read = true
      return true
    } catch (e) {
      console.warn('confirm failed', e)
      return false
    }
  }

  function clear() {
    alerts.value = []
  }

  return { alerts, unreadCount, addAlert, fetchUnhandled, fetchAlerts, markAsRead, ignoreAlert, markAllAsRead, confirm, clear }
})
