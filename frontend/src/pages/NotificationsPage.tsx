import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bell, CheckCheck } from 'lucide-react'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Typography from '@mui/material/Typography'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import ButtonBase from '@mui/material/ButtonBase'
import { notificationsApi } from '../api/notifications'
import { qk } from '../hooks/queryKeys'
import { relativeTime } from '../lib/utils'

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
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="h5" fontWeight={700}>Notifications</Typography>
        {items.some((n) => !n.seen) && (
          <Button
            size="small"
            startIcon={<CheckCheck size={16} />}
            onClick={() => markAll.mutate()}
            disabled={markAll.isPending}
          >
            Mark all read
          </Button>
        )}
      </Box>

      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress size={32} />
        </Box>
      )}

      {!isLoading && items.length === 0 && (
        <Card sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 8, color: 'text.disabled' }}>
          <Bell size={40} style={{ marginBottom: 12 }} />
          <Typography fontWeight={500}>No notifications yet</Typography>
        </Card>
      )}

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
        {items.map((n) => (
          <ButtonBase
            key={n.id}
            onClick={() => { if (!n.seen) markOne.mutate(n.id) }}
            sx={{
              display: 'flex', alignItems: 'flex-start', gap: 1.5, borderRadius: 3, p: 2,
              justifyContent: 'flex-start', textAlign: 'left', boxShadow: 1,
              bgcolor: n.seen ? 'white' : 'primary.50',
            }}
          >
            {!n.seen && <Box sx={{ mt: 0.75, height: 10, width: 10, flexShrink: 0, borderRadius: '50%', bgcolor: 'primary.main' }} />}
            {n.seen && <Box sx={{ mt: 0.75, height: 10, width: 10, flexShrink: 0 }} />}
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="body2">
                <Typography component="span" fontWeight={500}>Someone </Typography>
                {TYPE_LABEL[n.type] ?? n.type.toLowerCase()}
              </Typography>
              <Typography variant="caption" color="text.disabled" sx={{ mt: 0.25, display: 'block' }}>
                {relativeTime(n.createdAt)}
              </Typography>
            </Box>
          </ButtonBase>
        ))}
      </Box>
    </Box>
  )
}
