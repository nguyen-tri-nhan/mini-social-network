import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { qk } from './queryKeys'
import { NOTIFICATION_TYPE_LABEL } from '../lib/notificationLabels'
import { resolveNotificationTarget } from '../lib/notificationTarget'
import { wsClient } from '../lib/wsClient'

export function useNotificationSocket(userId: string | undefined) {
  const qc = useQueryClient()
  const navigate = useNavigate()

  useEffect(() => {
    if (!userId) return

    const refresh = () => {
      qc.invalidateQueries({ queryKey: qk.notifications.unread })
      qc.invalidateQueries({ queryKey: qk.notifications.list })
    }

    const unsubscribe = wsClient.subscribe(`user_${userId}_notification`, (msg) => {
      if (msg.type !== 'NOTIFICATION') return
      const p = (msg.payload ?? {}) as Record<string, string | undefined>

      const eventType = p.eventType
      const label = eventType
        ? NOTIFICATION_TYPE_LABEL[eventType] ?? eventType.toLowerCase()
        : 'sent you a notification'

      // interaction-service đã nhúng sẵn tên actor vào payload (từ
      // user_ref cục bộ của nó) — xem specs/decisions/0006.
      const actorName = p.actorFirstname ? `${p.actorFirstname} ${p.actorLastname}` : 'Someone'

      const target = eventType
        ? resolveNotificationTarget({ type: eventType, articleId: p.articleId, commentId: p.commentId })
        : null

      toast(`${actorName} ${label}`, target ? {
        action: {
          label: 'View',
          onClick: () => navigate(target.highlightCommentId ? `${target.path}?comment=${target.highlightCommentId}` : target.path),
        },
      } : undefined)

      refresh()
    })
    const offReconnect = wsClient.onReconnect(refresh)

    return () => {
      unsubscribe()
      offReconnect()
    }
  }, [userId, qc, navigate])
}
