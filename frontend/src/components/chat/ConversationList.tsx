import Box from '@mui/material/Box'
import ButtonBase from '@mui/material/ButtonBase'
import MuiAvatar from '@mui/material/Avatar'
import Typography from '@mui/material/Typography'
import { chatUserName } from '../../lib/chat'
import { initials, relativeTime } from '../../lib/utils'
import { useAuthStore } from '../../stores/authStore'
import type { Conversation } from '../../types'

interface Props {
  conversations: Conversation[]
  activeId?: string
  onSelect: (id: string) => void
}

export function ConversationList({ conversations, activeId, onSelect }: Props) {
  const me = useAuthStore((s) => s.user?.id)

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column' }}>
      {conversations.map((c) => {
        const name = chatUserName(c.otherUser)
        const last = c.lastMessage
        return (
          <ButtonBase
            key={c.id}
            onClick={() => onSelect(c.id)}
            sx={{
              display: 'flex', alignItems: 'center', gap: 1.5, px: 1.5, py: 1, borderRadius: 2,
              justifyContent: 'flex-start', textAlign: 'left',
              bgcolor: c.id === activeId ? 'action.selected' : 'transparent',
              '&:hover': { bgcolor: 'action.hover' },
            }}
          >
            <MuiAvatar src={c.otherUser.avatarUrl} sx={{ width: 40, height: 40, fontSize: 14 }}>
              {!c.otherUser.avatarUrl && initials(c.otherUser.firstname ?? 'U', c.otherUser.lastname ?? '')}
            </MuiAvatar>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="body2" fontWeight={c.unread ? 700 : 500} noWrap>{name}</Typography>
              {last && (
                <Typography
                  variant="caption"
                  noWrap
                  component="p"
                  color={c.unread ? 'text.primary' : 'text.secondary'}
                  fontWeight={c.unread ? 600 : 400}
                >
                  {last.senderId === me ? 'You: ' : ''}{last.content} · {relativeTime(last.createdAt)}
                </Typography>
              )}
            </Box>
            {c.unread && <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: 'primary.main', flexShrink: 0 }} />}
          </ButtonBase>
        )
      })}
    </Box>
  )
}
