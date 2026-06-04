import { client, api } from './client'
import type { AuthResponse } from '../types'

export const authApi = {
  signup: (data: {
    username: string; email: string; password: string
    firstname: string; lastname: string
  }) => api<AuthResponse>(() => client.post('/api/auth/signup', data)),

  signin: (data: { identifier: string; password: string }) =>
    api<AuthResponse>(() => client.post('/api/auth/signin', data)),
}
