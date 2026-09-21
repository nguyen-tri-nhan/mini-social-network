import { useQuery } from '@tanstack/react-query'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Typography from '@mui/material/Typography'
import CircularProgress from '@mui/material/CircularProgress'
import { ArrowLeft } from 'lucide-react'
import { articlesApi } from '../api/articles'
import { qk } from '../hooks/queryKeys'
import { ArticleCard } from '../components/article/ArticleCard'

export function ArticleDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [searchParams] = useSearchParams()
  const highlightCommentId = searchParams.get('comment') ?? undefined

  const { data: article, isLoading, isError } = useQuery({
    queryKey: qk.articles.detail(id!),
    queryFn:  () => articlesApi.getById(id!),
    enabled:  !!id,
  })

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography
        component={Link}
        to="/"
        variant="body2"
        sx={{ display: 'flex', alignItems: 'center', gap: 0.5, color: 'text.secondary', textDecoration: 'none', '&:hover': { color: 'primary.main' } }}
      >
        <ArrowLeft size={16} /> Back to feed
      </Typography>

      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress size={32} />
        </Box>
      )}

      {isError && (
        <Card sx={{ py: 6, textAlign: 'center', color: 'text.disabled' }}>
          <Typography>Post not found</Typography>
        </Card>
      )}

      {article && (
        <ArticleCard article={article} defaultShowComments highlightCommentId={highlightCommentId} />
      )}
    </Box>
  )
}
