import { create } from 'zustand'
import type { UserProfile } from '../types'

interface AuthState {
  token: string | null
  user: UserProfile | null
  setToken: (token: string) => void
  setAuth: (token: string, user: UserProfile) => void
  setUser: (user: UserProfile) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('jwt'),
  user: null,

  // Persist token trước khi gọi API cần auth (vd fetch profile ngay sau
  // signin/signup) — client.ts đọc token từ localStorage tại thời điểm gửi
  // request, gọi API trước khi persist sẽ mất Authorization header.
  setToken: (token) => {
    localStorage.setItem('jwt', token)
    set({ token })
  },

  setAuth: (token, user) => {
    localStorage.setItem('jwt', token)
    set({ token, user })
  },

  setUser: (user) => set({ user }),

  logout: () => {
    localStorage.removeItem('jwt')
    set({ token: null, user: null })
  },
}))

export const isAuthenticated = () => !!localStorage.getItem('jwt')
