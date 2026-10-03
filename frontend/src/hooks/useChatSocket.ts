import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { qk } from './queryKeys'
import { upsertMessage, type MessagesData } from '../lib/chat'
import { wsClient } from '../lib/wsClient'
import type { ChatMessage } from '../types'

// 1 topic/người nhận cho mọi conversation (messaging-plan §6) — badge cập nhật cả khi thread đóng.
export function useChatSocket(userId: string | undefined) {
  const qc = useQueryClient()

  useEffect(() => {
    if (!userId) return

    const unsubscribe = wsClient.subscribe(`user_${userId}_chat`, (msg) => {
      if (msg.type !== 'CHAT_MESSAGE') return
      const m = msg.payload as ChatMessage
      qc.setQueryData<MessagesData>(qk.chat.messages(m.conversationId), (d) => upsertMessage(d, m))
      qc.invalidateQueries({ queryKey: qk.chat.conversations })
      qc.invalidateQueries({ queryKey: qk.chat.conversation(m.conversationId) })
      qc.invalidateQueries({ queryKey: qk.chat.unread })
    })
    // WS không đảm bảo giao tin — rớt mạng thì tải lại toàn bộ (§14.6).
    const offReconnect = wsClient.onReconnect(() => qc.invalidateQueries({ queryKey: qk.chat.all }))

    return () => {
      unsubscribe()
      offReconnect()
    }
  }, [userId, qc])
}
