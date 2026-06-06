import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'
import { Navbar } from './Navbar'
import { useAuthStore } from '../../stores/authStore'
import { notificationsApi } from '../../api/notifications'
import type { UserProfile } from '../../types'

vi.mock('../../api/notifications', () => ({
  notificationsApi: { unreadCount: vi.fn() },
}))

const mockUser: UserProfile = {
  id: 'user-1',
  username: 'nhan',
  email: 'nhan@example.com',
  firstname: 'Nhan',
  lastname: 'Nguyen',
  createdAt: '2024-01-01T00:00:00Z',
}

function renderNavbar() {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <Navbar />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('Navbar', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'token', user: mockUser })
  })

  afterEach(() => {
    vi.clearAllMocks()
    useAuthStore.setState({ token: null, user: null })
    localStorage.clear()
  })

  describe('notification badge', () => {
    it('shows count when there are unread notifications', async () => {
      vi.mocked(notificationsApi.unreadCount).mockResolvedValue({ count: 5 })
      renderNavbar()
      expect(await screen.findByText('5')).toBeInTheDocument()
    })

    it('shows 9+ when count exceeds 9', async () => {
      vi.mocked(notificationsApi.unreadCount).mockResolvedValue({ count: 42 })
      renderNavbar()
      expect(await screen.findByText('9+')).toBeInTheDocument()
    })

    it('shows exact count up to 9', async () => {
      vi.mocked(notificationsApi.unreadCount).mockResolvedValue({ count: 9 })
      renderNavbar()
      expect(await screen.findByText('9')).toBeInTheDocument()
    })

    it('hides badge when no unread notifications', async () => {
      vi.mocked(notificationsApi.unreadCount).mockResolvedValue({ count: 0 })
      renderNavbar()
      await vi.waitFor(() => {
        expect(screen.queryByText('0')).not.toBeInTheDocument()
      })
    })
  })

  describe('logout', () => {
    it('clears auth state when logout button is clicked', async () => {
      vi.mocked(notificationsApi.unreadCount).mockResolvedValue({ count: 0 })
      renderNavbar()

      fireEvent.click(screen.getByTitle('Logout'))

      expect(useAuthStore.getState().token).toBeNull()
      expect(useAuthStore.getState().user).toBeNull()
    })
  })
})
