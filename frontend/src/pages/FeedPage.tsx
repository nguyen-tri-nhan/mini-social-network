import { useInfiniteQuery } from '@tanstack/react-query'
import { useInView } from 'react-intersection-observer'
import { useEffect } from 'react'
import { articlesApi } from '../api/articles'
import { qk } from '../hooks/queryKeys'
import { ArticleCard } from '../components/article/ArticleCard'
import { CreatePost } from '../components/article/CreatePost'
import { Spinner } from '../components/ui'

// react-intersection-observer needs to be installed:
// npm i react-intersection-observer
// For now we use a simple scroll-based approach via IntersectionObserver API directly

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
    <div className="flex flex-col gap-4">
      <CreatePost />

      {isLoading && (
        <div className="flex justify-center py-8">
          <Spinner className="h-8 w-8" />
        </div>
      )}

      {!isLoading && articles.length === 0 && (
        <div className="rounded-xl bg-white py-16 text-center text-gray-400 shadow-sm">
          <p className="text-lg font-medium">No posts yet</p>
          <p className="mt-1 text-sm">Be the first to share something!</p>
        </div>
      )}

      {articles.map((a) => <ArticleCard key={a.id} article={a} />)}

      {/* Infinite scroll trigger */}
      <div ref={ref} className="flex justify-center py-4">
        {isFetchingNextPage && <Spinner />}
      </div>
    </div>
  )
}
