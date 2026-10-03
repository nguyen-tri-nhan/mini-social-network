import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { MessageCircle } from 'lucide-react'
import Badge from '@mui/material/Badge'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import IconButton from '@mui/material/IconButton'
import Popover from '@mui/material/Popover'
import Typography from '@mui/material/Typography'
import { chatApi } from '../../api/chat'
import { qk } from '../../hooks/queryKeys'
import { ConversationList } from './ConversationList'

// Icon chat tách khỏi chuông: badge = số conversation chưa đọc, không đếm từng tin (§13.3).
export function ChatMenu() {
  const navigate = useNavigate()
  const [anchor, setAnchor] = useState<HTMLElement | null>(null)

  const { data: unread } = useQuery({
    queryKey: qk.chat.unread,
    queryFn: chatApi.unreadCount,
    refetchInterval: 60_000,
  })

  // Key con của conversations: invalidate cùng lúc với MessagesPage, nhưng phải là entry riêng —
  // bên đó là infinite query, dữ liệu khác hình dạng.
  const { data, isLoading } = useQuery({
    queryKey: [...qk.chat.conversations, 'menu'],
    queryFn: () => chatApi.list(),
    enabled: !!anchor,
  })

  const count = unread?.count ?? 0

  function go(path: string) {
    setAnchor(null)
    navigate(path)
  }

  return (
    <>
      <IconButton aria-label="Messages" size="small" onClick={(e) => setAnchor(e.currentTarget)}>
        <Badge badgeContent={count > 9 ? '9+' : count} color="error" invisible={count === 0}>
          <MessageCircle size={20} />
        </Badge>
      </IconButton>
      <Popover
        open={!!anchor}
        anchorEl={anchor}
        onClose={() => setAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        slotProps={{ paper: { sx: { width: 340, maxHeight: 480, display: 'flex', flexDirection: 'column' } } }}
      >
        <Typography variant="subtitle1" fontWeight={700} sx={{ px: 2, pt: 1.5, pb: 1 }}>Chats</Typography>
        <Box sx={{ flex: 1, overflowY: 'auto', px: 1 }}>
          {isLoading && <CircularProgress size={20} sx={{ display: 'block', mx: 'auto', my: 3 }} />}
          {data && data.items.length === 0 && (
            <Typography variant="body2" color="text.disabled" sx={{ textAlign: 'center', my: 3 }}>No messages yet</Typography>
          )}
          {data && <ConversationList conversations={data.items} onSelect={(id) => go(`/messages/${id}`)} />}
        </Box>
        <Button onClick={() => go('/messages')} sx={{ borderTop: 1, borderColor: 'divider', borderRadius: 0 }}>
          See all in Messages
        </Button>
      </Popover>
    </>
  )
}
