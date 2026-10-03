export interface ServerMessage {
  topic: string
  type: string
  payload?: Record<string, unknown>
}

type Handler = (msg: ServerMessage) => void

const BASE_RECONNECT_MS = 3000
const MAX_RECONNECT_MS = 30_000

// 1 kết nối WebSocket/tab dùng chung cho mọi tính năng (notification, chờ USER_READY, chat).
// Token đi qua Sec-WebSocket-Protocol vì browser không set được header Authorization — ADR 0009.
export class WsClient {
  private ws: WebSocket | null = null
  private handlers = new Map<string, Set<Handler>>()
  private reconnectListeners = new Set<() => void>()
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private attempts = 0
  private wanted = false
  private everOpened = false
  private tokenProvider: () => string | null = () => localStorage.getItem('jwt')

  // authStore import wsClient nên wsClient không import ngược lại được — store tự đăng ký vào đây.
  setTokenProvider(provider: () => string | null): void {
    this.tokenProvider = provider
  }

  subscribe(topic: string, handler: Handler): () => void {
    let set = this.handlers.get(topic)
    if (!set) {
      set = new Set()
      this.handlers.set(topic, set)
    }
    const firstForTopic = set.size === 0
    set.add(handler)

    this.wanted = true
    if (!this.ws) this.connect()
    else if (firstForTopic) this.send({ type: 'SUBSCRIBE', topic })

    return () => {
      const current = this.handlers.get(topic)
      if (!current?.delete(handler) || current.size > 0) return
      this.handlers.delete(topic)
      this.send({ type: 'UNSUBSCRIBE', topic })
    }
  }

  // Tin đến lúc mất kết nối sẽ không bao giờ được push lại — caller phải tự tải lại dữ liệu.
  onReconnect(listener: () => void): () => void {
    this.reconnectListeners.add(listener)
    return () => this.reconnectListeners.delete(listener)
  }

  close(): void {
    this.wanted = false
    this.everOpened = false
    this.attempts = 0
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    this.reconnectTimer = null
    this.handlers.clear()
    this.ws?.close()
    this.ws = null
  }

  private connect(): void {
    const token = this.tokenProvider()
    if (!token) return

    const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
    const ws = new WebSocket(`${protocol}://${location.host}/ws`, [
      'bearer-token-carrier',
      encodeURIComponent(`quarkus-http-upgrade#Authorization#Bearer ${token}`),
    ])
    this.ws = ws

    ws.onopen = () => {
      this.attempts = 0
      for (const topic of this.handlers.keys()) this.send({ type: 'SUBSCRIBE', topic })
      if (this.everOpened) this.reconnectListeners.forEach((l) => l())
      this.everOpened = true
    }

    ws.onmessage = (e) => {
      let msg: ServerMessage
      try {
        msg = JSON.parse(e.data)
      } catch {
        return
      }
      this.handlers.get(msg.topic)?.forEach((h) => h(msg))
    }

    ws.onclose = () => {
      if (this.ws !== ws) return
      this.ws = null
      if (!this.wanted) return
      const delay = Math.min(BASE_RECONNECT_MS * 2 ** this.attempts, MAX_RECONNECT_MS)
      this.attempts++
      this.reconnectTimer = setTimeout(() => this.connect(), delay)
    }

    ws.onerror = () => ws.close()
  }

  private send(msg: { type: string; topic: string }): void {
    if (this.ws?.readyState === WebSocket.OPEN) this.ws.send(JSON.stringify(msg))
  }
}

export const wsClient = new WsClient()
