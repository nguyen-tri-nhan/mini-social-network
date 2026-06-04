import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Send, Trash2 } from 'lucide-react'
import { commentsApi } from '../../api/interactions'
import { useAuthStore } from '../../stores/authStore'
import { qk } from '../../hooks/queryKeys'
import { relativeTime } from '../../lib/utils'
import { Avatar, Spinner } from '../ui'

interface Props { targetId: string; targetType: string }

export function CommentSection({ targetId, targetType }: Props) {
  const { user } = useAuthStore()
  const qc = useQueryClient()
  const [text, setText] = useState('')
  const fallback = user ? `${user.firstname[0]}${user.lastname[0]}`.toUpperCase() : '?'

  const { data, isLoading } = useQuery({
    queryKey: qk.comments.byTarget(targetId, targetType),
    queryFn: () => commentsApi.list(targetId, targetType),
  })

  const add = useMutation({
    mutationFn: () => commentsApi.create({ targetId, targetType, description: text.trim() }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.comments.byTarget(targetId, targetType) })
      setText('')
    },
  })

  const del = useMutation({
    mutationFn: (id: string) => commentsApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.comments.byTarget(targetId, targetType) }),
  })

  const comments = data?.items ?? []

  return (
    <div className="border-t border-gray-100 px-4 pb-4">
      {/* Existing comments */}
      {isLoading && <div className="flex justify-center py-4"><Spinner /></div>}

      <div className="mt-3 flex flex-col gap-3">
        {comments.map((c) => (
          <div key={c.id} className="flex gap-2">
            <Avatar fallback="U" size="sm" />
            <div className="flex-1">
              <div className="rounded-2xl bg-gray-50 px-3 py-2">
                <p className="text-xs font-semibold text-gray-700">User</p>
                <p className="text-sm text-gray-800">{c.description}</p>
              </div>
              <div className="mt-0.5 flex items-center gap-3 px-1 text-xs text-gray-400">
                <span>{relativeTime(c.createdAt)}</span>
                {user?.id === c.authorId && (
                  <button
                    onClick={() => del.mutate(c.id)}
                    className="flex items-center gap-1 hover:text-red-500"
                  >
                    <Trash2 className="h-3 w-3" /> Delete
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Input */}
      <div className="mt-3 flex gap-2">
        <Avatar fallback={fallback} src={user?.avatarUrl} size="sm" />
        <div className="flex flex-1 items-center gap-2 rounded-full bg-gray-100 px-4 py-2">
          <input
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey && text.trim()) { e.preventDefault(); add.mutate() } }}
            placeholder="Write a comment..."
            className="flex-1 bg-transparent text-sm outline-none placeholder-gray-400"
          />
          <button
            onClick={() => add.mutate()}
            disabled={!text.trim() || add.isPending}
            className="text-brand disabled:opacity-40"
          >
            {add.isPending ? <Spinner className="h-4 w-4" /> : <Send className="h-4 w-4" />}
          </button>
        </div>
      </div>
    </div>
  )
}
