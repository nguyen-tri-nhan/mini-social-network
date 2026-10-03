import { client, api } from './client'
import type { ChatMessage, Conversation, CursorPage } from '../types'

export const chatApi = {
  open: (targetUserId: string) =>
    api<Conversation>(() => client.post('/api/conversations', { targetUserId })),

  list: (before?: string) =>
    api<CursorPage<Conversation>>(() => client.get('/api/conversations', { params: { before } })),

  get: (id: string) =>
    api<Conversation>(() => client.get(`/api/conversations/${id}`)),

  unreadCount: () =>
    api<{ count: number }>(() => client.get('/api/conversations/unread-count')),

  markRead: (id: string, messageId: string) =>
    client.post(`/api/conversations/${id}/read`, { messageId }),

  messages: (id: string, before?: string) =>
    api<CursorPage<ChatMessage>>(() => client.get(`/api/conversations/${id}/messages`, { params: { before } })),

  send: (id: string, clientMessageId: string, content: string) =>
    api<ChatMessage>(() => client.post(`/api/conversations/${id}/messages`, { clientMessageId, content })),
}
