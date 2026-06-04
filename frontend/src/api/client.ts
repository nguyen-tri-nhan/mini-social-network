import axios from 'axios'
import { toast } from 'sonner'

const BASE_URL = import.meta.env.VITE_API_URL ?? ''  // vite proxy '/api' → localhost:8080/api

export const client = axios.create({ baseURL: BASE_URL })

// Attach JWT from localStorage on every request
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt')
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
