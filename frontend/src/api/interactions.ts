import { client, api } from './client'
import type { Comment, Vote, PageResponse } from '../types'

export const commentsApi = {
  list: (targetId: string, targetType = 'ARTICLE', page = 0, size = 20) =>
    api<PageResponse<Comment>>(() =>
      client.get('/api/comments', { params: { targetId, targetType, page, size } }),
    ),

  create: (data: { targetId: string; targetType: string; description: string }) =>
    api<Comment>(() => client.post('/api/comments', data)),

  delete: (id: string) => client.delete(`/api/comments/${id}`),
}

export const votesApi = {
  cast: (targetId: string, targetType: string, value: -1 | 0 | 1) =>
    api<Vote>(() => client.post('/api/votes', { targetId, targetType, value })),
}
