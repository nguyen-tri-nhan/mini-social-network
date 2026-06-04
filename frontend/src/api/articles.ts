import { client, api } from './client'
import type { Article, PageResponse, UserProfile } from '../types'

export const articlesApi = {
  list: (params?: { page?: number; size?: number; filter?: string; sort?: string }) =>
    api<PageResponse<Article>>(() => client.get('/api/articles', { params })),

  getById: (id: string) =>
    api<Article>(() => client.get(`/api/articles/${id}`)),

  create: (data: { description?: string; imageUrl?: string }) =>
    api<Article>(() => client.post('/api/articles', data)),

  delete: (id: string) => client.delete(`/api/articles/${id}`),

  presignUpload: (filename: string, contentType = 'image/jpeg') =>
    api<{ uploadUrl: string; imageUrl: string }>(() =>
      client.post('/api/articles/images/presign', null, {
        params: { filename, contentType },
      }),
    ),
}

export const usersApi = {
  me: () => api<UserProfile>(() => client.get('/api/users/me')),

  getById: (id: string) =>
    api<UserProfile>(() => client.get(`/api/users/${id}`)),

  update: (data: { firstname?: string; lastname?: string; avatarUrl?: string }) =>
    api<UserProfile>(() => client.patch('/api/users/me', data)),
}
