import { client, api } from './client'
import type { Notification, PageResponse } from '../types'

export const notificationsApi = {
  list: (page = 0, size = 20) =>
    api<PageResponse<Notification>>(() =>
      client.get('/api/notifications', { params: { page, size } }),
    ),

  unreadCount: () =>
    api<{ count: number }>(() => client.get('/api/notifications/unread-count')),

  markSeen: (id: string) => client.patch(`/api/notifications/${id}/seen`),

  markAllSeen: () => client.patch('/api/notifications/seen-all'),
}
