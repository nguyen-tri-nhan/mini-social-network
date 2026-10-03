import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { MessageCircle } from 'lucide-react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CircularProgress from '@mui/material/CircularProgress'
import MuiAvatar from '@mui/material/Avatar'
import Typography from '@mui/material/Typography'
import { articlesApi, usersApi } from '../api/articles'
import { chatApi } from '../api/chat'
import { useAuthStore } from '../stores/authStore'
import { qk } from '../hooks/queryKeys'
import { initials } from '../lib/utils'
import { ArticleCard } from '../components/article/ArticleCard'

export function UserProfilePage() {
  const { id = '' } = useParams()
  const me = useAuthStore((s) => s.user?.id)
  const navigate = useNavigate()
  const isSelf = id === me

  const { data: profile, isLoading, isError } = useQuery({
    queryKey: qk.users.detail(id),
    queryFn:  () => usersApi.getById(id),
    enabled:  !isSelf,
    retry: false,
  })

  const { data: articles } = useQuery({
    queryKey: qk.articles.list(`authorId==${id}`),
    queryFn:  () => articlesApi.list({ filter: `authorId==${id}`, size: 20 }),
    enabled:  !isSelf && !!profile,
  })

  const openChat = useMutation({
    mutationFn: () => chatApi.open(id),
    onSuccess: (c) => navigate(`/messages/${c.id}`),
  })

  if (isSelf) return <Navigate to="/profile" replace />

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress size={32} />
      </Box>
    )
  }

  if (isError || !profile) {
    return (
      <Card sx={{ py: 6, textAlign: 'center', color: 'text.disabled' }}>
        <Typography>User not found</Typography>
      </Card>
    )
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Card sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <MuiAvatar src={profile.avatarUrl} sx={{ width: 56, height: 56, fontSize: 16 }}>
            {!profile.avatarUrl && initials(profile.firstname, profile.lastname)}
          </MuiAvatar>
          <Box sx={{ flex: 1 }}>
            <Typography variant="h6" fontWeight={700}>{profile.firstname} {profile.lastname}</Typography>
            <Typography variant="body2" color="text.disabled">@{profile.username}</Typography>
          </Box>
          <Button
            variant="contained"
            size="small"
            startIcon={<MessageCircle size={16} />}
            onClick={() => openChat.mutate()}
            disabled={openChat.isPending}
          >
            Message
          </Button>
        </Box>
      </Card>

      <Typography fontWeight={600} color="text.secondary">Posts</Typography>
      {articles?.items.length === 0 && (
        <Card sx={{ py: 6, textAlign: 'center', color: 'text.disabled' }}>
          <Typography>No posts yet</Typography>
        </Card>
      )}
      {articles?.items.map((a) => <ArticleCard key={a.id} article={a} />)}
    </Box>
  )
}
