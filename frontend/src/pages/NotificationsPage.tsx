import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bell, CheckCheck } from 'lucide-react'
import { notificationsApi } from '../api/notifications'
import { qk } from '../hooks/queryKeys'
import { relativeTime, cn } from '../lib/utils'
import { Button, Card, Spinner } from '../components/ui'

const TYPE_LABEL: Record<string, string> = {
  COMMENT_CREATED: 'commented on your post',
  VOTE_CAST:       'voted on your post',
  ARTICLE_CREATED: 'published a new post',
}

export function NotificationsPage() {
  const qc = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: qk.notifications.list,
    queryFn:  () => notificationsApi.list(),
  })

  const markAll = useMutation({
    mutationFn: notificationsApi.markAllSeen,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.notifications.list })
      qc.invalidateQueries({ queryKey: qk.notifications.unread })
    },
  })

  const markOne = useMutation({
    mutationFn: (id: string) => notificationsApi.markSeen(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.notifications.list })
      qc.invalidateQueries({ queryKey: qk.notifications.unread })
    },
  })

  const items = data?.items ?? []

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-gray-900">Notifications</h1>
        {items.some((n) => !n.seen) && (
          <Button variant="ghost" size="sm" onClick={() => markAll.mutate()} loading={markAll.isPending}>
            <CheckCheck className="h-4 w-4" /> Mark all read
          </Button>
        )}
      </div>

      {isLoading && <div className="flex justify-center py-12"><Spinner className="h-8 w-8" /></div>}

      {!isLoading && items.length === 0 && (
        <Card className="flex flex-col items-center py-16 text-gray-400">
          <Bell className="mb-3 h-10 w-10" />
          <p className="font-medium">No notifications yet</p>
        </Card>
      )}

      <div className="flex flex-col gap-2">
        {items.map((n) => (
          <button
            key={n.id}
            onClick={() => { if (!n.seen) markOne.mutate(n.id) }}
            className={cn(
              'flex items-start gap-3 rounded-xl p-4 text-left transition-colors',
              n.seen ? 'bg-white shadow-sm' : 'bg-blue-50 shadow-sm',
            )}
          >
            {!n.seen && <span className="mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full bg-brand" />}
            {n.seen  && <span className="mt-1.5 h-2.5 w-2.5 shrink-0" />}
            <div className="flex-1 min-w-0">
              <p className="text-sm text-gray-800">
                <span className="font-medium">Someone </span>
                {TYPE_LABEL[n.type] ?? n.type.toLowerCase()}
              </p>
              <p className="mt-0.5 text-xs text-gray-400">{relativeTime(n.createdAt)}</p>
            </div>
          </button>
        ))}
      </div>
    </div>
  )
}
