// Luôn resolve, không bao giờ reject — lỗi WS hoặc hết timeout vẫn cho gọi
// tiếp /me như fallback thay vì kẹt vô hạn.
export function waitForUserReady(userId: string, timeoutMs = 8000): Promise<void> {
  return new Promise((resolve) => {
    let settled = false
    let ws: WebSocket | null = null

    const finish = () => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      ws?.close()
      resolve()
    }

    const timer = setTimeout(finish, timeoutMs)

    try {
      const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
      ws = new WebSocket(`${protocol}://${location.host}/ws`)

      ws.onopen = () => {
        ws?.send(JSON.stringify({ type: 'SUBSCRIBE', topic: `user_${userId}_ready` }))
      }

      ws.onmessage = (e) => {
        try {
          const msg = JSON.parse(e.data)
          if (msg.type === 'USER_READY') finish()
        } catch {
          // ignore malformed message, để timeout tự xử lý
        }
      }

      ws.onerror = finish
    } catch {
      finish()
    }
  })
}
