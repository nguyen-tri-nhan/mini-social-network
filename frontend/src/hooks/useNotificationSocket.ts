import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { qk } from './queryKeys'
import { NOTIFICATION_TYPE_LABEL } from '../lib/notificationLabels'
import { resolveNotificationTarget } from '../lib/notificationTarget'

const RECONNECT_DELAY_MS = 3000

// Kết nối WS sống suốt session (khác waitForUserReady — one-shot rồi đóng).
export function useNotificationSocket(userId: string | undefined) {
  const qc = useQueryClient()
  const navigate = useNavigate()

  useEffect(() => {
    if (!userId) return

    let ws: WebSocket | null = null
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null
    let cancelled = false

    function connect() {
      const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
      ws = new WebSocket(`${protocol}://${location.host}/ws`)

      ws.onopen = () => {
        ws?.send(JSON.stringify({ type: 'SUBSCRIBE', topic: `user_${userId}_notification` }))
      }

      ws.onmessage = (e) => {
        try {
          const msg = JSON.parse(e.data)
          if (msg.type !== 'NOTIFICATION') return

          const eventType = msg.payload?.eventType as string | undefined
          const label = eventType
            ? NOTIFICATION_TYPE_LABEL[eventType] ?? eventType.toLowerCase()
            : 'sent you a notification'

          const target = eventType
            ? resolveNotificationTarget({ type: eventType, articleId: msg.payload?.articleId, commentId: msg.payload?.commentId })
            : null

          toast(`Someone ${label}`, target ? {
            action: {
              label: 'View',
              onClick: () => navigate(target.highlightCommentId ? `${target.path}?comment=${target.highlightCommentId}` : target.path),
            },
          } : undefined)

          qc.invalidateQueries({ queryKey: qk.notifications.unread })
          qc.invalidateQueries({ queryKey: qk.notifications.list })
        } catch {
          // ignore malformed message
        }
      }

      // Không phân biệt lỗi mạng thoáng qua với logout — reconnect vô điều
      // kiện khi còn userId, cleanup effect (userId đổi/unmount) sẽ tự huỷ.
      ws.onclose = () => {
        if (!cancelled) reconnectTimer = setTimeout(connect, RECONNECT_DELAY_MS)
      }
      ws.onerror = () => ws?.close()
    }

    connect()

    return () => {
      cancelled = true
      if (reconnectTimer) clearTimeout(reconnectTimer)
      ws?.close()
    }
  }, [userId, qc, navigate])
}
