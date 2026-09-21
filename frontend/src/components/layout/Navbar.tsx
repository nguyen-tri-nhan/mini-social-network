import { Bell, LogOut } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import AppBar from '@mui/material/AppBar'
import Toolbar from '@mui/material/Toolbar'
import Box from '@mui/material/Box'
import IconButton from '@mui/material/IconButton'
import Badge from '@mui/material/Badge'
import MuiAvatar from '@mui/material/Avatar'
import Typography from '@mui/material/Typography'
import { notificationsApi } from '../../api/notifications'
import { useAuthStore } from '../../stores/authStore'
import { qk } from '../../hooks/queryKeys'

export function Navbar() {
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()

  const { data: unread } = useQuery({
    queryKey: qk.notifications.unread,
    queryFn: notificationsApi.unreadCount,
    refetchInterval: 30_000,
  })

  function handleLogout() {
    logout()
    navigate('/login')
  }

  const count = unread?.count ?? 0
  const fallback = user ? `${user.firstname[0]}${user.lastname[0]}`.toUpperCase() : '?'

  return (
    <AppBar
      position="sticky"
      color="inherit"
      elevation={0}
      sx={{ borderBottom: 1, borderColor: 'divider', zIndex: (t) => t.zIndex.appBar }}
    >
      <Toolbar sx={{ height: 56, minHeight: 56 }}>
        <Typography
          component={Link}
          to="/"
          variant="h6"
          sx={{ mr: 3, fontWeight: 700, color: 'primary.main', textDecoration: 'none' }}
        >
          Social
        </Typography>

        <Box sx={{ flex: 1 }} />

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <IconButton aria-label="Notifications" component={Link} to="/notifications" size="small">
            <Badge badgeContent={count > 9 ? '9+' : count} color="error" invisible={count === 0}>
              <Bell size={20} />
            </Badge>
          </IconButton>

          <IconButton aria-label="Profile" component={Link} to="/profile" size="small">
            <MuiAvatar src={user?.avatarUrl} sx={{ width: 32, height: 32, fontSize: 13 }}>
              {!user?.avatarUrl && fallback}
            </MuiAvatar>
          </IconButton>

          <IconButton aria-label="Logout" onClick={handleLogout} size="small" title="Logout">
            <LogOut size={20} />
          </IconButton>
        </Box>
      </Toolbar>
    </AppBar>
  )
}
