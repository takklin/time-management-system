import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useUserStore } from '@/store/user'
import { getMyMessages, markMessageRead, deleteMessage } from '@/api/message'

export const useMessageStore = defineStore('message', () => {
  const messages = ref<any[]>([])

  const unreadCount = computed(() => {
    return messages.value.filter(m => !m.isRead && !m.isDeleted).length
  })

  function normalize(item: any) {
    const obj: any = Object.assign({}, item)
    // 解析后端持久化 id（dbId）为字符串，避免 JS 对大整数的精度丢失
    const rawId = obj.id ?? obj.messageId ?? obj.message_id ?? null
    obj.dbId = rawId !== null && rawId !== undefined ? String(rawId) : null
    // UI id 使用 dbId（若存在）或退回到本地时间戳（临时 id），保持为字符串
    obj.id = obj.dbId ?? String(Date.now())
    obj.title = obj.title || obj.subject || ''
    obj.content = obj.content || obj.message || obj.body || ''
    obj.createdAt = obj.createdAt || obj.created_at || obj.created || new Date().toISOString()
    if (obj.isRead === undefined) obj.isRead = !!obj.is_read
    if (obj.isDeleted === undefined) obj.isDeleted = !!obj.is_deleted
    return obj
  }

  function addMessage(payload: any) {
    try {
      const item = normalize(Object.assign({ receivedAt: new Date() }, payload))
      const exists = messages.value.some(m => m.id === item.id)
      if (exists) return
      messages.value.unshift(item)
    } catch (e) { console.warn('addMessage error', e) }
  }

  async function fetchMessages(params: any = { page: 1, size: 50 }) {
    try {
      const res: any = await getMyMessages(params)
      let fetched: any[] = []
      if (res && res.content) fetched = res.content.map(normalize)
      else if (Array.isArray(res)) fetched = res.map(normalize)

      const existingIds = new Set(fetched.map((a: any) => a.id))
      for (const a of messages.value) {
        if (!existingIds.has(a.id)) fetched.unshift(a)
      }
      messages.value = fetched
      return messages.value
    } catch (e) {
      console.warn('fetchMessages failed', e)
      return []
    }
  }

  async function markRead(id: number | string) {
    const key = id === null || id === undefined ? null : String(id)
    if (!key) return false
    try {
      // 尝试找到本地消息并优先使用 dbId（字符串）调用后端
      const idx = messages.value.findIndex(m => String(m.id) === key || String(m.dbId) === key)
      let dbId: string | null = null
      if (idx >= 0) {
        const local = messages.value[idx]
        if (local.dbId !== undefined && local.dbId !== null) dbId = String(local.dbId)
      } else {
        // 没有本地记录，尝试把 key 当作 dbId 使用
        dbId = key
      }

      // 如果没有持久化 id，则本地标记并不向后端发起调用
      if (dbId === null) {
        if (idx >= 0) messages.value[idx].isRead = true
        return true
      }

      await markMessageRead(dbId)
      const idx2 = messages.value.findIndex(m => String(m.id) === dbId || String(m.dbId) === dbId)
      if (idx2 >= 0) messages.value[idx2].isRead = true
      return true
    } catch (e) {
      console.warn('markRead failed', e)
      try {
        const status = (e && (e as any).response && (e as any).response.status) || (String(e).includes('404') ? 404 : null)
        if (status === 404) {
          const idx = messages.value.findIndex(m => String(m.id) === key || String(m.dbId) === key)
          if (idx >= 0) messages.value[idx].isRead = true
          try { console.info('[MessageStore] markRead fallback: marked locally for missing id', key) } catch (err) {}
          return true
        }
      } catch (err) {}
      return false
    }
  }

  async function removeMessage(id: number | string) {
    try {
      const key = id === null || id === undefined ? null : String(id)
      if (!key) return false
      const idx = messages.value.findIndex(m => String(m.id) === key || String(m.dbId) === key)
      let dbId: string | null = null
      if (idx >= 0) dbId = messages.value[idx].dbId ? String(messages.value[idx].dbId) : null
      if (dbId === null) {
        // 无持久化 id，直接从本地移除
        if (idx >= 0) messages.value.splice(idx, 1)
        return true
      }
      await deleteMessage(dbId)
      const idx2 = messages.value.findIndex(m => String(m.id) === key || String(m.dbId) === dbId)
      if (idx2 >= 0) messages.value.splice(idx2, 1)
      return true
    } catch (e) {
      console.warn('removeMessage failed', e)
      return false
    }
  }

  return { messages, unreadCount, addMessage, fetchMessages, markRead, removeMessage }
})

