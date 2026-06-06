import { beforeEach, describe, it, expect } from 'vitest'
import { useAuthStore, isAuthenticated } from './authStore'
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
    it('returns true when jwt is in localStorage', () => {
      localStorage.setItem('jwt', 'some-token')
      expect(isAuthenticated()).toBe(true)
    })

    it('returns false when no jwt in localStorage', () => {
      expect(isAuthenticated()).toBe(false)
    })

    it('returns false after logout', () => {
      useAuthStore.getState().setAuth('my-token', mockUser)
      useAuthStore.getState().logout()
      expect(isAuthenticated()).toBe(false)
    })
  })
})
