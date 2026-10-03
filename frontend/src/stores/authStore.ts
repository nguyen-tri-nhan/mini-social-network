import { create } from 'zustand'
import type { UserProfile } from '../types'
import { queryClient } from '../lib/queryClient'
import { wsClient } from '../lib/wsClient'

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

  // Gọi trước khi fetch profile ngay sau signin/signup — request lấy token từ store.
  // localStorage chỉ để giữ phiên qua reload.
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
    // qk.users.me / qk.notifications.* không scope theo user id — không clear
    // thì data user cũ còn "fresh" trong staleTime, hiện lại tới khi refetch.
    queryClient.clear()
    // Kết nối WS mang token cũ — user kế tiếp trong cùng tab phải mở kết nối mới.
    wsClient.close()
  },
}))

wsClient.setTokenProvider(() => useAuthStore.getState().token)

export const isAuthenticated = () => !!useAuthStore.getState().token

function subjectOf(token: string): string | null {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(payload)).sub ?? null
  } catch {
    return null
  }
}

// Mọi tab cùng trình duyệt (kể cả mọi cửa sổ ẩn danh đang mở) dùng chung localStorage.
// Tab khác đăng nhập acc khác → reload để store, cache, WS của tab này cùng đổi theo một lúc;
// tự dọn từng phần thì dễ sót và để lại trạng thái nửa người này nửa người kia.
export function syncWithOtherTab(newToken: string | null, navigate: (path: string) => void) {
  const { token, logout } = useAuthStore.getState()
  if (newToken === token) return

  if (!newToken) {
    logout()
    navigate('/login')
    return
  }

  const sub = subjectOf(newToken)
  if (token && sub !== null && sub === subjectOf(token)) {
    useAuthStore.setState({ token: newToken })
    return
  }
  navigate('/')
}

// 'storage' chỉ bắn ở các tab KHÁC tab vừa ghi; key null = localStorage.clear().
window.addEventListener('storage', (e) => {
  if (e.key !== 'jwt' && e.key !== null) return
  syncWithOtherTab(localStorage.getItem('jwt'), (path) => window.location.assign(path))
})
