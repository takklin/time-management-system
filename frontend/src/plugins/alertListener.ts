import { App } from 'vue'
import { ElNotification } from 'element-plus'
import { useAlertStore } from '@/store/alert'
import { useMessageStore } from '@/store/message'
import { useUserStore } from '@/store/user'

export default function install(app: App) {
  if (typeof window === 'undefined') return
  if ((window as any).__tm_alert_listener_installed) return

  // 短期去重：在短时间窗口内合并多条实时推送，统一调用 fetchUnhandled 来以服务端为准去重
  let alertBuffer: any[] = []
  let alertFetchTimer: any = null

  window.addEventListener('tm:alert', (e: any) => {
    const payload = e?.detail ?? e
    try { console.info('[AlertListener] tm:alert', payload) } catch (e) {}
    try {
      const userStore = useUserStore()
      // 如果用户尚未登录，缓存在全局临时数组，登录后回放到 store（避免丢失登录失败时的告警）
      if (!userStore || !userStore.isLoggedIn) {
        try { console.info('[AlertListener] tm:alert received before login -> buffering', payload) } catch (ignore) {}
        try {
          let buf = (window as any).__tm_pending_alerts
          if (!Array.isArray(buf)) buf = []
          buf.push(payload)
          (window as any).__tm_pending_alerts = buf
        } catch (e) {
          console.warn('[AlertListener] buffer pending alert failed', e)
        }
        return
      }

      const alertStore = useAlertStore()
      try {
        // 将实时推送放入短期缓冲，并延迟调用 fetchUnhandled 以避免重复告警
        alertBuffer.push(payload)
        if (alertFetchTimer) clearTimeout(alertFetchTimer)
        alertFetchTimer = setTimeout(() => {
          try {
            alertStore.fetchUnhandled().catch(() => {})
          } catch (e) { console.warn('[AlertListener] fetchUnhandled failed', e) }
          alertBuffer = []
          alertFetchTimer = null
        }, 800)
      } catch (err) { console.warn('[AlertListener] buffering alert failed', err) }
    } catch (err) {
      console.warn('[AlertListener] unexpected error handling tm:alert', err)
    }

    const title = payload.title || payload.alertType || '告警'
    const message = payload.message || payload.description || ''
    const sev = (payload.severity || '').toString().toLowerCase()
    let type: 'success' | 'warning' | 'info' | 'error' = 'info'
    if (sev === 'critical' || sev === 'high') type = 'error'
    else if (sev === 'medium' || sev === 'warn' || sev === 'warning') type = 'warning'

    try {
      ElNotification({ title, message, type })
    } catch (e) {
      console.warn('ElNotification failed', e)
    }
  })

  // 处理管理员发给用户的私信/收件箱消息
  window.addEventListener('tm:message', (e: any) => {
    const payload = e?.detail ?? e
    try { console.info('[AlertListener] tm:message', payload) } catch (e) {}
    try {
      const userStore = useUserStore()
      const messageStore = useMessageStore()
      if (!userStore || !userStore.isLoggedIn) {
        try { console.info('[AlertListener] tm:message received before login -> buffering', payload) } catch (ignore) {}
        try {
          let buf = (window as any).__tm_pending_messages
          if (!Array.isArray(buf)) buf = []
          buf.push(payload)
          (window as any).__tm_pending_messages = buf
        } catch (e) { console.warn('[AlertListener] buffer pending message failed', e) }
        return
      }

      try { messageStore.addMessage(payload) } catch (err) { console.warn('[AlertListener] add message to store failed', err) }
    } catch (err) {
      console.warn('[AlertListener] unexpected error handling tm:message', err)
    }
  })

  try { (window as any).__tm_alert_listener_installed = true } catch (e) {}
}
