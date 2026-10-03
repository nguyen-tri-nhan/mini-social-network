import { wsClient } from './wsClient'

// Luôn resolve, không bao giờ reject — lỗi WS hoặc hết timeout vẫn cho gọi
// tiếp /me như fallback thay vì kẹt vô hạn.
export function waitForUserReady(userId: string, timeoutMs = 8000): Promise<void> {
  return new Promise((resolve) => {
    let settled = false
    let unsubscribe: () => void = () => {}

    const finish = () => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      unsubscribe()
      resolve()
    }

    const timer = setTimeout(finish, timeoutMs)
    unsubscribe = wsClient.subscribe(`user_${userId}_ready`, (msg) => {
      if (msg.type === 'USER_READY') finish()
    })
  })
}
