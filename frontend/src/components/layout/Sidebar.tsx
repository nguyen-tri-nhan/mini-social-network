import { Bell, Home, User } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import Box from '@mui/material/Box'
import List from '@mui/material/List'
import ListItemButton from '@mui/material/ListItemButton'
import ListItemIcon from '@mui/material/ListItemIcon'
import ListItemText from '@mui/material/ListItemText'
import Paper from '@mui/material/Paper'
import BottomNavigation from '@mui/material/BottomNavigation'
import BottomNavigationAction from '@mui/material/BottomNavigationAction'

const links = [
  { to: '/',              icon: Home,  label: 'Feed' },
  { to: '/notifications', icon: Bell,  label: 'Notifications' },
  { to: '/profile',       icon: User,  label: 'Profile' },
]

export function Sidebar() {
  return (
    <Box component="nav" sx={{ width: 224, flexShrink: 0, pt: 2, display: { xs: 'none', lg: 'block' } }}>
      <List sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
        {links.map(({ to, icon: Icon, label }) => (
          <ListItemButton
            key={to}
            component={NavLink}
            to={to}
            end={to === '/'}
            sx={{
              borderRadius: 2,
              '&.active': { bgcolor: 'primary.50', color: 'primary.main' },
            }}
          >
            <ListItemIcon sx={{ minWidth: 36 }}><Icon size={20} /></ListItemIcon>
            <ListItemText primary={label} primaryTypographyProps={{ fontSize: 14, fontWeight: 500 }} />
          </ListItemButton>
        ))}
      </List>
    </Box>
  )
}

// Bottom nav for mobile
export function BottomNav() {
  return (
    <Paper
      elevation={0}
      sx={{
        position: 'fixed', bottom: 0, left: 0, right: 0, zIndex: (t) => t.zIndex.appBar,
        borderTop: 1, borderColor: 'divider', display: { xs: 'block', lg: 'none' },
      }}
    >
      <BottomNavigation showLabels>
        {links.map(({ to, icon: Icon, label }) => (
          <BottomNavigationAction
            key={to}
            component={NavLink}
            to={to}
            label={label}
            icon={<Icon size={20} />}
          />
        ))}
      </BottomNavigation>
    </Paper>
  )
}
