import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { MessageCircle, ThumbsDown, ThumbsUp, Trash2 } from 'lucide-react'
import { articlesApi, usersApi } from '../../api/articles'
import { votesApi } from '../../api/interactions'
import { useAuthStore } from '../../stores/authStore'
import { qk } from '../../hooks/queryKeys'
import { relativeTime } from '../../lib/utils'
import { Avatar, Spinner } from '../ui'
import { CommentSection } from '../comment/CommentSection'
import type { Article, UserProfile } from '../../types'

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

  // Use author name from cache or placeholder
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
    <div className="rounded-xl bg-white shadow-sm">
      {/* Header */}
      <div className="flex items-start justify-between p-4">
        <div className="flex items-center gap-3">
          <Avatar fallback={authorFallback} src={author?.avatarUrl} />
          <div>
            <p className="text-sm font-semibold text-gray-900">{authorName}</p>
            <p className="text-xs text-gray-400">{relativeTime(article.createdAt)}</p>
          </div>
        </div>
        {isOwner && (
          <button
            onClick={() => deleteMutation.mutate()}
            disabled={deleteMutation.isPending}
            className="rounded-full p-1.5 text-gray-400 hover:bg-red-50 hover:text-red-500"
          >
            {deleteMutation.isPending ? <Spinner className="h-4 w-4" /> : <Trash2 className="h-4 w-4" />}
          </button>
        )}
      </div>

      {/* Content */}
      {article.description && (
        <p className="px-4 pb-3 text-sm text-gray-800 leading-relaxed">{article.description}</p>
      )}
      {article.imageUrl && (
        <img src={article.imageUrl} alt="" className="max-h-96 w-full object-cover" />
      )}

      {/* Counts */}
      <div className="flex items-center gap-4 px-4 py-2 text-xs text-gray-400 border-t border-gray-50">
        <span>{voteCount} votes</span>
        <span>{article.commentCount} comments</span>
      </div>

      {/* Actions */}
      <div className="flex border-t border-gray-100">
        <button
          onClick={() => voteMutation.mutate(1)}
          className="flex flex-1 items-center justify-center gap-2 py-2.5 text-sm font-medium text-gray-500 hover:bg-gray-50 hover:text-brand"
        >
          <ThumbsUp className="h-4 w-4" /> Like
        </button>
        <button
          onClick={() => voteMutation.mutate(-1)}
          className="flex flex-1 items-center justify-center gap-2 py-2.5 text-sm font-medium text-gray-500 hover:bg-gray-50 hover:text-red-500"
        >
          <ThumbsDown className="h-4 w-4" /> Dislike
        </button>
        <button
          onClick={() => setShowComments((v) => !v)}
          className="flex flex-1 items-center justify-center gap-2 py-2.5 text-sm font-medium text-gray-500 hover:bg-gray-50 hover:text-green-600"
        >
          <MessageCircle className="h-4 w-4" /> Comment
        </button>
      </div>

      {/* Comments inline */}
      {showComments && <CommentSection targetId={article.id} targetType="ARTICLE" />}
    </div>
  )
}
