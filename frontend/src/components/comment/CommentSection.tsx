import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Send, Trash2 } from 'lucide-react'
import Box from '@mui/material/Box'
import MuiAvatar from '@mui/material/Avatar'
import Typography from '@mui/material/Typography'
import IconButton from '@mui/material/IconButton'
import CircularProgress from '@mui/material/CircularProgress'
import InputBase from '@mui/material/InputBase'
import { commentsApi } from '../../api/interactions'
import { useAuthStore } from '../../stores/authStore'
import { qk } from '../../hooks/queryKeys'
import { relativeTime } from '../../lib/utils'

interface Props { targetId: string; targetType: string }

export function CommentSection({ targetId, targetType }: Props) {
  const { user } = useAuthStore()
  const qc = useQueryClient()
  const [text, setText] = useState('')
  const fallback = user ? `${user.firstname[0]}${user.lastname[0]}`.toUpperCase() : '?'

  const { data, isLoading } = useQuery({
    queryKey: qk.comments.byTarget(targetId, targetType),
    queryFn: () => commentsApi.list(targetId, targetType),
  })

  const add = useMutation({
    mutationFn: () => commentsApi.create({ targetId, targetType, description: text.trim() }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.comments.byTarget(targetId, targetType) })
      setText('')
    },
  })

  const del = useMutation({
    mutationFn: (id: string) => commentsApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.comments.byTarget(targetId, targetType) }),
  })

  const comments = data?.items ?? []

  return (
    <Box sx={{ px: 2, pb: 2 }}>
      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
          <CircularProgress size={20} />
        </Box>
      )}

      <Box sx={{ mt: 1.5, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
        {comments.map((c) => (
          <Box key={c.id} sx={{ display: 'flex', gap: 1 }}>
            <MuiAvatar sx={{ width: 32, height: 32, fontSize: 12 }}>U</MuiAvatar>
            <Box sx={{ flex: 1 }}>
              <Box sx={{ bgcolor: 'grey.50', borderRadius: 3, px: 1.5, py: 1 }}>
                <Typography variant="caption" fontWeight={600} color="text.secondary" component="p">User</Typography>
                <Typography variant="body2">{c.description}</Typography>
              </Box>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, px: 0.5, mt: 0.25 }}>
                <Typography variant="caption" color="text.disabled">{relativeTime(c.createdAt)}</Typography>
                {user?.id === c.authorId && (
                  <Box
                    component="button"
                    onClick={() => del.mutate(c.id)}
                    sx={{
                      display: 'flex', alignItems: 'center', gap: 0.5, border: 0, bgcolor: 'transparent',
                      cursor: 'pointer', color: 'text.disabled', fontSize: 12, p: 0,
                      '&:hover': { color: 'error.main' },
                    }}
                  >
                    <Trash2 size={12} /> Delete
                  </Box>
                )}
              </Box>
            </Box>
          </Box>
        ))}
      </Box>

      <Box sx={{ mt: 1.5, display: 'flex', gap: 1 }}>
        <MuiAvatar src={user?.avatarUrl} sx={{ width: 32, height: 32, fontSize: 12 }}>
          {!user?.avatarUrl && fallback}
        </MuiAvatar>
        <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', gap: 1, bgcolor: 'grey.100', borderRadius: 999, px: 2, py: 0.5 }}>
          <InputBase
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey && text.trim()) { e.preventDefault(); add.mutate() } }}
            placeholder="Write a comment..."
            sx={{ flex: 1, fontSize: 14 }}
          />
          <IconButton
            aria-label="Send comment"
            size="small"
            onClick={() => add.mutate()}
            disabled={!text.trim() || add.isPending}
            sx={{ color: 'primary.main' }}
          >
            {add.isPending ? <CircularProgress size={16} /> : <Send size={16} />}
          </IconButton>
        </Box>
      </Box>
    </Box>
  )
}
