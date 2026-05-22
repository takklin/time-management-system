// 动态按需加载依赖，避免在打包或页面加载时因模块在顶层访问 Node 全局变量（如 `global`）导致错误
export async function createAlertClient(token: string | null, onMessage: (payload: any) => void, isAdmin = false) {
  // 动态导入 stompjs 与 sockjs-client
  const stomp = await import('@stomp/stompjs')
  // 某些第三方包会访问 `global`，在浏览器上做兼容
  try { if (typeof window !== 'undefined' && (window as any).global === undefined) (window as any).global = window } catch (e) {}
  const SockJSMod = await import('sockjs-client')
  const Client = (stomp as any).Client || (stomp as any).default?.Client || (stomp as any).Stomp?.Client
  const SockJS = (SockJSMod as any).default || SockJSMod

  // 计算 SockJS / WS 地址
  let sockJsUrl = '/ws-alert'
  try {
    if (typeof window !== 'undefined' && window.location && window.location.port && window.location.port !== '8081') {
      sockJsUrl = `${window.location.protocol}//${window.location.hostname}:8081/ws-alert`
    }
  } catch (e) {}

  const isDev = typeof window !== 'undefined' && window.location && window.location.port && window.location.port !== '8081'
  let client: any

  if (isDev) {
    const wsProto = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const wsUrl = `${wsProto}://${window.location.hostname}:8081/ws-alert/websocket`
    client = new Client({
      brokerURL: wsUrl,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: (str: any) => { try { console.debug('[STOMP]', str) } catch (e) {} },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        try {
          console.info(`[AlertSocket] connected (admin=${isAdmin})`)
          try { (window as any).__tm_alertClient = client } catch (e) {}
          client.subscribe('/user/queue/alerts', (msg: any) => {
            try { console.debug('[AlertSocket] /user/queue/alerts received', msg.body) } catch (e) {}
            try { onMessage(JSON.parse(msg.body)) } catch (e) { onMessage(msg.body) }
          })
          // 订阅管理员发送给用户的私信/收件箱消息
          try {
            client.subscribe('/user/queue/messages', (msg: any) => {
              try { console.debug('[AlertSocket] /user/queue/messages received', msg.body) } catch (e) {}
              try { window.dispatchEvent(new CustomEvent('tm:message', { detail: JSON.parse(msg.body) })) } catch (e) { try { window.dispatchEvent(new CustomEvent('tm:message', { detail: msg.body })) } catch (err) {} }
            })
            try { console.info('[AlertSocket] subscribed to /user/queue/messages') } catch (e) {}
          } catch (e) {}
          try { console.info('[AlertSocket] subscribed to /user/queue/alerts') } catch (e) {}
          if (isAdmin) {
            client.subscribe('/topic/admin/alerts', (msg: any) => {
              try { console.debug('[AlertSocket] /topic/admin/alerts received', msg.body) } catch (e) {}
              try { onMessage(JSON.parse(msg.body)) } catch (e) { onMessage(msg.body) }
            })
            try { console.info('[AlertSocket] subscribed to /topic/admin/alerts') } catch (e) {}
          }
        } catch (err) { console.error('subscribe error', err) }
      },
      onStompError: (frame: any) => { console.error('STOMP error', frame) },
      onWebSocketClose: (evt: any) => { console.warn('[AlertSocket] websocket closed', evt) }
    })
  } else {
    client = new Client({
      webSocketFactory: () => new SockJS(sockJsUrl),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: (str: any) => { try { console.debug('[STOMP]', str) } catch (e) {} },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        try {
          console.info(`[AlertSocket] connected (admin=${isAdmin})`)
          try { (window as any).__tm_alertClient = client } catch (e) {}
          client.subscribe('/user/queue/alerts', (msg: any) => {
            try { console.debug('[AlertSocket] /user/queue/alerts received', msg.body) } catch (e) {}
            try {
              onMessage(JSON.parse(msg.body))
            } catch (e) {
              onMessage(msg.body)
            }
          })
          // 订阅管理员向用户发送的消息（收件箱）
          try {
            client.subscribe('/user/queue/messages', (msg: any) => {
              try { console.debug('[AlertSocket] /user/queue/messages received', msg.body) } catch (e) {}
              try { window.dispatchEvent(new CustomEvent('tm:message', { detail: JSON.parse(msg.body) })) } catch (e) { try { window.dispatchEvent(new CustomEvent('tm:message', { detail: msg.body })) } catch (err) {} }
            })
            try { console.info('[AlertSocket] subscribed to /user/queue/messages') } catch (e) {}
          } catch (e) {}
          try { console.info('[AlertSocket] subscribed to /user/queue/alerts') } catch (e) {}
          if (isAdmin) {
            client.subscribe('/topic/admin/alerts', (msg: any) => {
              try { console.debug('[AlertSocket] /topic/admin/alerts received', msg.body) } catch (e) {}
              try {
                onMessage(JSON.parse(msg.body))
              } catch (e) {
                onMessage(msg.body)
              }
            })
            try { console.info('[AlertSocket] subscribed to /topic/admin/alerts') } catch (e) {}
          }
        } catch (err) {
          console.error('subscribe error', err)
        }
      },
      onStompError: (frame: any) => {
        console.error('STOMP error', frame)
      },
      onWebSocketClose: (evt: any) => {
        console.warn('[AlertSocket] websocket closed', evt)
      }
    })
  }

  client.activate()
  try { (window as any).__tm_alertClient = client } catch (e) {}
  return client
}

export function disconnectClient(client: any) {
  try {
    if (client && client.deactivate) client.deactivate()
  } catch (e) {
    console.warn('disconnect error', e)
  }
}
