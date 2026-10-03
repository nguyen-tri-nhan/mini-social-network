import { useNavigate, useParams } from 'react-router-dom'
import { useInfiniteQuery } from '@tanstack/react-query'
import { MessageCircle } from 'lucide-react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CircularProgress from '@mui/material/CircularProgress'
import Typography from '@mui/material/Typography'
import { chatApi } from '../api/chat'
import { qk } from '../hooks/queryKeys'
import { ConversationList } from '../components/chat/ConversationList'
import { MessageThread } from '../components/chat/MessageThread'

export function MessagesPage() {
  const { conversationId } = useParams()
  const navigate = useNavigate()

  const list = useInfiniteQuery({
    queryKey: qk.chat.conversations,
    queryFn: ({ pageParam }) => chatApi.list(pageParam),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (last) => (last.hasMore ? last.items[last.items.length - 1]?.lastMessage?.id : undefined),
  })
  const conversations = list.data?.pages.flatMap((p) => p.items) ?? []

  return (
    <Card sx={{ display: 'flex', height: 'calc(100vh - 140px)', minHeight: 420 }}>
      {/* Mobile: chỉ hiện danh sách hoặc thread */}
      <Box
        sx={{
          width: { xs: '100%', md: 300 }, flexShrink: 0, borderRight: { md: 1 }, borderColor: { md: 'divider' },
          display: { xs: conversationId ? 'none' : 'flex', md: 'flex' }, flexDirection: 'column', minHeight: 0,
        }}
      >
        <Typography variant="h6" fontWeight={700} sx={{ px: 2, py: 1.5 }}>Messages</Typography>
        <Box sx={{ flex: 1, overflowY: 'auto', px: 1 }}>
          {list.isLoading && <CircularProgress size={24} sx={{ display: 'block', mx: 'auto', my: 4 }} />}
          {!list.isLoading && conversations.length === 0 && (
            <Typography variant="body2" color="text.disabled" sx={{ textAlign: 'center', my: 4, px: 2 }}>
              No conversations yet. Open someone's profile and press "Message".
            </Typography>
          )}
          <ConversationList
            conversations={conversations}
            activeId={conversationId}
            onSelect={(id) => navigate(`/messages/${id}`)}
          />
          {list.hasNextPage && (
            <Button fullWidth size="small" onClick={() => list.fetchNextPage()} disabled={list.isFetchingNextPage}>
              Load more
            </Button>
          )}
        </Box>
      </Box>

      <Box sx={{ flex: 1, minWidth: 0, display: { xs: conversationId ? 'flex' : 'none', md: 'flex' }, flexDirection: 'column' }}>
        {conversationId ? (
          <MessageThread key={conversationId} conversationId={conversationId} onBack={() => navigate('/messages')} />
        ) : (
          <Box sx={{ m: 'auto', textAlign: 'center', color: 'text.disabled' }}>
            <MessageCircle size={40} />
            <Typography sx={{ mt: 1 }}>Select a conversation</Typography>
          </Box>
        )}
      </Box>
    </Card>
  )
}
