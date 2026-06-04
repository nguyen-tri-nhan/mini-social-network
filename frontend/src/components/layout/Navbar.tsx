import { Bell, LogOut, User } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { notificationsApi } from '../../api/notifications'
import { useAuthStore } from '../../stores/authStore'
import { qk } from '../../hooks/queryKeys'
import { Avatar } from '../ui'

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
    <header className="sticky top-0 z-40 flex h-14 items-center border-b border-gray-200 bg-white px-4 shadow-sm">
      {/* Logo */}
      <Link to="/" className="mr-6 text-xl font-bold text-brand">Social</Link>

      <div className="flex-1" />

      {/* Actions */}
      <div className="flex items-center gap-2">
        {/* Notifications */}
        <Link to="/notifications" className="relative rounded-full p-2 hover:bg-gray-100">
          <Bell className="h-5 w-5 text-gray-600" />
          {count > 0 && (
            <span className="absolute right-1 top-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white">
              {count > 9 ? '9+' : count}
            </span>
          )}
        </Link>

        {/* Profile */}
        <Link to="/profile" className="rounded-full p-1 hover:bg-gray-100">
          <Avatar fallback={fallback} src={user?.avatarUrl} size="sm" />
        </Link>

        {/* Logout */}
        <button onClick={handleLogout} className="rounded-full p-2 hover:bg-gray-100" title="Logout">
          <LogOut className="h-5 w-5 text-gray-600" />
        </button>
      </div>
    </header>
  )
}
