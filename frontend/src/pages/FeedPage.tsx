import { useInfiniteQuery } from '@tanstack/react-query'
import { useInView } from 'react-intersection-observer'
import { useEffect } from 'react'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Typography from '@mui/material/Typography'
import CircularProgress from '@mui/material/CircularProgress'
import { articlesApi } from '../api/articles'
import { qk } from '../hooks/queryKeys'
import { ArticleCard } from '../components/article/ArticleCard'
import { CreatePost } from '../components/article/CreatePost'

export function FeedPage() {
  const { ref, inView } = useInView()

  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
  } = useInfiniteQuery({
    queryKey: qk.articles.list(),
    queryFn: ({ pageParam = 0 }) => articlesApi.list({ page: pageParam as number, size: 10 }),
    getNextPageParam: (last) => last.hasNext ? last.page + 1 : undefined,
    initialPageParam: 0,
  })

  useEffect(() => {
    if (inView && hasNextPage) fetchNextPage()
  }, [inView, hasNextPage, fetchNextPage])

  const articles = data?.pages.flatMap((p) => p.items) ?? []

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <CreatePost />

      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress size={32} />
        </Box>
      )}

      {!isLoading && articles.length === 0 && (
        <Card sx={{ py: 8, textAlign: 'center', color: 'text.disabled' }}>
          <Typography fontSize={18} fontWeight={500}>No posts yet</Typography>
          <Typography variant="body2" sx={{ mt: 0.5 }}>Be the first to share something!</Typography>
        </Card>
      )}

      {articles.map((a) => <ArticleCard key={a.id} article={a} />)}

      {/* Infinite scroll trigger */}
      <Box ref={ref} sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
        {isFetchingNextPage && <CircularProgress size={24} />}
      </Box>
    </Box>
  )
}
