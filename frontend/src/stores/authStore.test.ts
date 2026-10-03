import { beforeEach, describe, it, expect, vi } from 'vitest'
import { useAuthStore, isAuthenticated, syncWithOtherTab } from './authStore'
import type { UserProfile } from '../types'

const mockUser: UserProfile = {
  id: 'user-1',
  username: 'nhan',
  email: 'nhan@example.com',
  firstname: 'Nhan',
  lastname: 'Nguyen',
  createdAt: '2024-01-01T00:00:00Z',
}

describe('authStore', () => {
  beforeEach(() => {
    localStorage.clear()
    useAuthStore.setState({ token: null, user: null })
  })

  describe('setToken', () => {
    it('stores token in state and localStorage without touching user', () => {
      useAuthStore.getState().setToken('my-token')

      expect(useAuthStore.getState().token).toBe('my-token')
      expect(useAuthStore.getState().user).toBeNull()
      expect(localStorage.getItem('jwt')).toBe('my-token')
    })
  })

  describe('setAuth', () => {
    it('stores token in state and localStorage', () => {
      useAuthStore.getState().setAuth('my-token', mockUser)

      expect(useAuthStore.getState().token).toBe('my-token')
      expect(localStorage.getItem('jwt')).toBe('my-token')
    })

    it('stores user in state', () => {
      useAuthStore.getState().setAuth('my-token', mockUser)

      expect(useAuthStore.getState().user).toEqual(mockUser)
    })
  })

  describe('logout', () => {
    it('clears token from state and localStorage', () => {
      useAuthStore.getState().setAuth('my-token', mockUser)
      useAuthStore.getState().logout()

      expect(useAuthStore.getState().token).toBeNull()
      expect(localStorage.getItem('jwt')).toBeNull()
    })

    it('clears user from state', () => {
      useAuthStore.getState().setAuth('my-token', mockUser)
      useAuthStore.getState().logout()

      expect(useAuthStore.getState().user).toBeNull()
    })
  })

  describe('setUser', () => {
    it('updates user without touching token', () => {
      useAuthStore.setState({ token: 'existing-token', user: null })
      useAuthStore.getState().setUser(mockUser)

      expect(useAuthStore.getState().token).toBe('existing-token')
      expect(useAuthStore.getState().user).toEqual(mockUser)
    })
  })

  describe('isAuthenticated', () => {
    it('returns true when this tab holds a token', () => {
      useAuthStore.getState().setToken('some-token')
      expect(isAuthenticated()).toBe(true)
    })

    it('returns false when this tab holds no token', () => {
      expect(isAuthenticated()).toBe(false)
    })

    it('returns false after logout', () => {
      useAuthStore.getState().setAuth('my-token', mockUser)
      useAuthStore.getState().logout()
      expect(isAuthenticated()).toBe(false)
    })
  })

  describe('syncWithOtherTab', () => {
    const jwt = (sub: string, n = 0) =>
      `h.${btoa(JSON.stringify({ sub, n })).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')}.s`

    it('another tab logged in as a different user → reload this tab', () => {
      useAuthStore.getState().setAuth(jwt('old'), mockUser)
      const navigate = vi.fn()

      syncWithOtherTab(jwt('new'), navigate)

      expect(navigate).toHaveBeenCalledWith('/')
    })

    it('another tab logged out → this tab logs out too', () => {
      useAuthStore.getState().setAuth(jwt('old'), mockUser)
      const navigate = vi.fn()

      syncWithOtherTab(null, navigate)

      expect(useAuthStore.getState().token).toBeNull()
      expect(useAuthStore.getState().user).toBeNull()
      expect(navigate).toHaveBeenCalledWith('/login')
    })

    it('same user signed in again elsewhere → adopt the new token without reload', () => {
      useAuthStore.getState().setAuth(jwt('me', 1), mockUser)
      const navigate = vi.fn()

      syncWithOtherTab(jwt('me', 2), navigate)

      expect(useAuthStore.getState().token).toBe(jwt('me', 2))
      expect(useAuthStore.getState().user).toEqual(mockUser)
      expect(navigate).not.toHaveBeenCalled()
    })

    it('unreadable token is treated as a different user', () => {
      useAuthStore.getState().setAuth(jwt('me'), mockUser)
      const navigate = vi.fn()

      syncWithOtherTab('garbage', navigate)

      expect(navigate).toHaveBeenCalledWith('/')
    })

    it('a logged-out tab follows a login elsewhere', () => {
      const navigate = vi.fn()

      syncWithOtherTab(jwt('someone'), navigate)

      expect(navigate).toHaveBeenCalledWith('/')
    })
  })
})
