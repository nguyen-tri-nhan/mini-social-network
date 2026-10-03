import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useInfiniteQuery, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, RotateCw } from 'lucide-react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import IconButton from '@mui/material/IconButton'
import MuiAvatar from '@mui/material/Avatar'
import Typography from '@mui/material/Typography'
import { chatApi } from '../../api/chat'
import { qk } from '../../hooks/queryKeys'
import { chatUserName, chronological, unconfirmed, upsertMessage, type MessagesData, type PendingMessage } from '../../lib/chat'
import { initials, relativeTime } from '../../lib/utils'
import { useAuthStore } from '../../stores/authStore'
import { MessageInput } from './MessageInput'

interface Props { conversationId: string; onBack?: () => void }

export function MessageThread({ conversationId, onBack }: Props) {
  const me = useAuthStore((s) => s.user?.id)
  const qc = useQueryClient()
  const bottomRef = useRef<HTMLDivElement>(null)
  const [pending, setPending] = useState<PendingMessage[]>([])

  const { data: conversation, isError } = useQuery({
    queryKey: qk.chat.conversation(conversationId),
    queryFn:  () => chatApi.get(conversationId),
    retry: false,
  })

  const thread = useInfiniteQuery({
    queryKey: qk.chat.messages(conversationId),
    queryFn: ({ pageParam }) => chatApi.messages(conversationId, pageParam),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (last) => (last.hasMore ? last.items[last.items.length - 1]?.id : undefined),
  })

  const messages = chronological(thread.data)
  const waiting = unconfirmed(pending, messages)
  const newestId = messages[messages.length - 1]?.id

  useEffect(() => {
    bottomRef.current?.scrollIntoView?.({ block: 'end' })
  }, [newestId, waiting.length])

  // Đánh dấu đã đọc khi mở thread, khi có tin mới lúc tab đang hiện, và khi tab quay lại (§14.6).
  useEffect(() => {
    if (!newestId) return
    const markRead = () => {
      if (document.visibilityState !== 'visible') return
      chatApi.markRead(conversationId, newestId).then(() => {
        qc.invalidateQueries({ queryKey: qk.chat.unread })
        qc.invalidateQueries({ queryKey: qk.chat.conversations })
      })
    }
    markRead()
    document.addEventListener('visibilitychange', markRead)
    return () => document.removeEventListener('visibilitychange', markRead)
  }, [conversationId, newestId, qc])

  // Gửi lại dùng đúng clientMessageId cũ → server trả tin đã có thay vì tạo tin trùng (§14.3).
  function send(clientMessageId: string, content: string) {
    setPending((p) => [...p.filter((x) => x.clientMessageId !== clientMessageId), { clientMessageId, content, failed: false }])
    chatApi.send(conversationId, clientMessageId, content)
      .then((msg) => {
        qc.setQueryData<MessagesData>(qk.chat.messages(conversationId), (d) => upsertMessage(d, msg))
        setPending((p) => p.filter((x) => x.clientMessageId !== clientMessageId))
        qc.invalidateQueries({ queryKey: qk.chat.conversations })
      })
      .catch(() => setPending((p) => p.map((x) => (x.clientMessageId === clientMessageId ? { ...x, failed: true } : x))))
  }

  if (isError) {
    return <Typography sx={{ p: 3, color: 'text.disabled', textAlign: 'center' }}>Conversation not found</Typography>
  }

  const other = conversation?.otherUser

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, p: 1.5, borderBottom: 1, borderColor: 'divider' }}>
        {onBack && (
          <IconButton aria-label="Back" size="small" onClick={onBack}><ArrowLeft size={18} /></IconButton>
        )}
        {other && (
          <Box component={Link} to={`/users/${other.id}`} sx={{ display: 'flex', alignItems: 'center', gap: 1.5, color: 'inherit', textDecoration: 'none' }}>
            <MuiAvatar src={other.avatarUrl} sx={{ width: 36, height: 36, fontSize: 13 }}>
              {!other.avatarUrl && initials(other.firstname ?? 'U', other.lastname ?? '')}
            </MuiAvatar>
            <Typography fontWeight={600}>{chatUserName(other)}</Typography>
          </Box>
        )}
      </Box>

      <Box sx={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 0.75, p: 2 }}>
        {thread.hasNextPage && (
          <Button size="small" onClick={() => thread.fetchNextPage()} disabled={thread.isFetchingNextPage} sx={{ alignSelf: 'center' }}>
            {thread.isFetchingNextPage ? 'Loading…' : 'Load older messages'}
          </Button>
        )}
        {thread.isLoading && <CircularProgress size={24} sx={{ alignSelf: 'center', my: 4 }} />}
        {!thread.isLoading && messages.length === 0 && waiting.length === 0 && (
          <Typography variant="body2" color="text.disabled" sx={{ textAlign: 'center', my: 4 }}>
            Say hi to {chatUserName(other)} 👋
          </Typography>
        )}

        {messages.map((m) => (
          <Bubble key={m.id} mine={m.senderId === me} content={m.content} caption={relativeTime(m.createdAt)} />
        ))}
        {waiting.map((p) => (
          <Bubble
            key={p.clientMessageId}
            mine
            content={p.content}
            faded={!p.failed}
            caption={p.failed ? 'Failed to send' : 'Sending…'}
            onRetry={p.failed ? () => send(p.clientMessageId, p.content) : undefined}
          />
        ))}
        <div ref={bottomRef} />
      </Box>

      <MessageInput onSend={(content) => send(crypto.randomUUID(), content)} />
    </Box>
  )
}

interface BubbleProps { mine: boolean; content: string; caption: string; faded?: boolean; onRetry?: () => void }

function Bubble({ mine, content, caption, faded, onRetry }: BubbleProps) {
  return (
    <Box sx={{ alignSelf: mine ? 'flex-end' : 'flex-start', maxWidth: '75%', opacity: faded ? 0.6 : 1 }}>
      <Box
        sx={{
          px: 1.5, py: 1, borderRadius: 3, whiteSpace: 'pre-wrap', wordBreak: 'break-word',
          bgcolor: mine ? 'primary.main' : 'grey.100', color: mine ? 'primary.contrastText' : 'text.primary',
        }}
      >
        <Typography variant="body2">{content}</Typography>
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, justifyContent: mine ? 'flex-end' : 'flex-start', px: 0.5 }}>
        <Typography variant="caption" color={onRetry ? 'error' : 'text.disabled'}>{caption}</Typography>
        {onRetry && (
          <IconButton aria-label="Retry" size="small" onClick={onRetry}><RotateCw size={12} /></IconButton>
        )}
      </Box>
    </Box>
  )
}
