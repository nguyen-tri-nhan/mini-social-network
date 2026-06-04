import { create } from 'zustand'
import type { UserProfile } from '../types'

interface AuthState {
  token: string | null
  user: UserProfile | null
  setAuth: (token: string, user: UserProfile) => void
  setUser: (user: UserProfile) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('jwt'),
  user: null,

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
