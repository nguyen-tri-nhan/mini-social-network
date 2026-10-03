import axios from 'axios'
import { toast } from 'sonner'
import { useAuthStore } from '../stores/authStore'

const BASE_URL = import.meta.env.VITE_API_URL ?? ''  // vite proxy '/api' → localhost:8080/api

export const client = axios.create({ baseURL: BASE_URL })

// Token của chính tab này, không đọc lại localStorage: localStorage dùng chung giữa các tab,
// tab khác đăng nhập acc khác là request ở đây đổi danh tính ngay trong khi UI vẫn là người cũ.
client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Global error handler
client.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status
    const message: string = err.response?.data?.error?.errorMessage ?? err.message

    if (status === 401) {
      localStorage.removeItem('jwt')
      window.location.href = '/login'
    } else if (status !== 404) {
      toast.error(message)
    }

    return Promise.reject(err)
  },
)

export async function api<T>(fn: () => Promise<{ data: { data: T } }>): Promise<T> {
  const res = await fn()
  return res.data.data as T
}
