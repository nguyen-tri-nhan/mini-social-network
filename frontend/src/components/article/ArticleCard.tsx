import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { MessageCircle, ThumbsDown, ThumbsUp, Trash2 } from 'lucide-react'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import MuiAvatar from '@mui/material/Avatar'
import Typography from '@mui/material/Typography'
import IconButton from '@mui/material/IconButton'
import ButtonBase from '@mui/material/ButtonBase'
import CircularProgress from '@mui/material/CircularProgress'
import Divider from '@mui/material/Divider'
import { articlesApi, usersApi } from '../../api/articles'
import { votesApi } from '../../api/interactions'
import { useAuthStore } from '../../stores/authStore'
import { qk } from '../../hooks/queryKeys'
import { relativeTime } from '../../lib/utils'
import { CommentSection } from '../comment/CommentSection'
import type { Article } from '../../types'

interface Props { article: Article }

export function ArticleCard({ article }: Props) {
  const { user } = useAuthStore()
  const qc = useQueryClient()
  const [showComments, setShowComments] = useState(false)
  const [voteCount, setVoteCount] = useState(article.voteCount)

  const { data: author } = useQuery({
    queryKey: qk.users.detail(article.authorId),
    queryFn:  () => usersApi.getById(article.authorId),
    staleTime: 10 * 60_000,
  })

  const authorName = author ? `${author.firstname} ${author.lastname}` : 'User'
  const authorFallback = author ? `${author.firstname[0]}${author.lastname[0]}`.toUpperCase() : 'U'

  const isOwner = user?.id === article.authorId

  const deleteMutation = useMutation({
    mutationFn: () => articlesApi.delete(article.id),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.articles.all }),
  })

  const voteMutation = useMutation({
    mutationFn: (value: -1 | 0 | 1) => votesApi.cast(article.id, 'ARTICLE', value),
    onMutate: (value) => setVoteCount((c) => c + value),
    onError: (_, value) => setVoteCount((c) => c - value),
  })

  return (
    <Card>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', p: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <MuiAvatar src={author?.avatarUrl} sx={{ width: 40, height: 40, fontSize: 14 }}>
            {!author?.avatarUrl && authorFallback}
          </MuiAvatar>
          <Box>
            <Typography variant="body2" fontWeight={600}>{authorName}</Typography>
            <Typography variant="caption" color="text.disabled">{relativeTime(article.createdAt)}</Typography>
          </Box>
        </Box>
        {isOwner && (
          <IconButton
            aria-label="Delete post"
            size="small"
            onClick={() => deleteMutation.mutate()}
            disabled={deleteMutation.isPending}
            sx={{ color: 'text.disabled', '&:hover': { color: 'error.main', bgcolor: 'error.50' } }}
          >
            {deleteMutation.isPending ? <CircularProgress size={16} /> : <Trash2 size={16} />}
          </IconButton>
        )}
      </Box>

      {/* Content */}
      {article.description && (
        <Typography variant="body2" sx={{ px: 2, pb: 1.5, lineHeight: 1.6 }}>
          {article.description}
        </Typography>
      )}
      {article.imageUrl && (
        <Box component="img" src={article.imageUrl} alt="" sx={{ maxHeight: 384, width: '100%', objectFit: 'cover' }} />
      )}

      {/* Counts */}
      <Box sx={{ display: 'flex', gap: 2, px: 2, py: 1, borderTop: 1, borderColor: 'grey.100' }}>
        <Typography variant="caption" color="text.disabled">{voteCount} votes</Typography>
        <Typography variant="caption" color="text.disabled">{article.commentCount} comments</Typography>
      </Box>

      {/* Actions */}
      <Box sx={{ display: 'flex', borderTop: 1, borderColor: 'grey.100' }}>
        <ButtonBase
          onClick={() => voteMutation.mutate(1)}
          sx={{ flex: 1, display: 'flex', gap: 1, py: 1.25, fontSize: 14, fontWeight: 500, color: 'text.secondary', '&:hover': { bgcolor: 'grey.50', color: 'primary.main' } }}
        >
          <ThumbsUp size={16} /> Like
        </ButtonBase>
        <ButtonBase
          onClick={() => voteMutation.mutate(-1)}
          sx={{ flex: 1, display: 'flex', gap: 1, py: 1.25, fontSize: 14, fontWeight: 500, color: 'text.secondary', '&:hover': { bgcolor: 'grey.50', color: 'error.main' } }}
        >
          <ThumbsDown size={16} /> Dislike
        </ButtonBase>
        <ButtonBase
          onClick={() => setShowComments((v) => !v)}
          sx={{ flex: 1, display: 'flex', gap: 1, py: 1.25, fontSize: 14, fontWeight: 500, color: 'text.secondary', '&:hover': { bgcolor: 'grey.50', color: 'success.main' } }}
        >
          <MessageCircle size={16} /> Comment
        </ButtonBase>
      </Box>

      {showComments && (
        <>
          <Divider />
          <CommentSection targetId={article.id} targetType="ARTICLE" />
        </>
      )}
    </Card>
  )
}
