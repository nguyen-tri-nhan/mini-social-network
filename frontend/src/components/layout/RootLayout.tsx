import { Outlet, Navigate } from 'react-router-dom'
import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Navbar } from './Navbar'
import { Sidebar, BottomNav } from './Sidebar'
import { useAuthStore, isAuthenticated } from '../../stores/authStore'
import { usersApi } from '../../api/articles'
import { qk } from '../../hooks/queryKeys'
import { Spinner } from '../ui'

export function RootLayout() {
  const { setUser } = useAuthStore()

  const { data: me, isLoading } = useQuery({
    queryKey: qk.users.me,
    queryFn: usersApi.me,
    enabled: isAuthenticated(),
    staleTime: 5 * 60_000,
  })

  useEffect(() => { if (me) setUser(me) }, [me, setUser])

  if (!isAuthenticated()) return <Navigate to="/login" replace />

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <div className="mx-auto flex max-w-5xl gap-6 px-4 pt-6 pb-20 lg:pb-6">
        <Sidebar />
        <main className="min-w-0 flex-1">
          <Outlet />
        </main>
      </div>
      <BottomNav />
    </div>
  )
}
