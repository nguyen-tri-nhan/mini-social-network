import { Outlet, Navigate } from 'react-router-dom'
import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import Box from '@mui/material/Box'
import CircularProgress from '@mui/material/CircularProgress'
import { Navbar } from './Navbar'
import { Sidebar, BottomNav } from './Sidebar'
import { useAuthStore, isAuthenticated } from '../../stores/authStore'
import { usersApi } from '../../api/articles'
import { qk } from '../../hooks/queryKeys'

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
      <Box sx={{ display: 'flex', height: '100vh', alignItems: 'center', justifyContent: 'center' }}>
        <CircularProgress size={32} />
      </Box>
    )
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <Navbar />
      <Box sx={{ mx: 'auto', maxWidth: 960, display: 'flex', gap: 3, px: 2, pt: 3, pb: { xs: 10, lg: 3 } }}>
        <Sidebar />
        <Box component="main" sx={{ minWidth: 0, flex: 1 }}>
          <Outlet />
        </Box>
      </Box>
      <BottomNav />
    </Box>
  )
}
