import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { usersApi, articlesApi } from '../api/articles'
import { useAuthStore } from '../stores/authStore'
import { qk } from '../hooks/queryKeys'
import { relativeTime } from '../lib/utils'
import { Avatar, Button, Card, Input, Spinner } from '../components/ui'
import { ArticleCard } from '../components/article/ArticleCard'

const schema = z.object({
  firstname: z.string().min(1),
  lastname:  z.string().min(1),
})
type Form = z.infer<typeof schema>

export function ProfilePage() {
  const { user, setUser } = useAuthStore()
  const qc = useQueryClient()
  const [editing, setEditing] = useState(false)

  const { register, handleSubmit, reset, formState: { errors } } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: { firstname: user?.firstname ?? '', lastname: user?.lastname ?? '' },
  })

  // My articles
  const { data: articlesData, isLoading } = useQuery({
    queryKey: qk.articles.list(`authorId==${user?.id}`),
    queryFn:  () => articlesApi.list({ filter: `authorId==${user?.id}`, size: 20 }),
    enabled:  !!user?.id,
  })

  const update = useMutation({
    mutationFn: (data: Form) => usersApi.update(data),
    onSuccess: (updated) => {
      setUser(updated)
      qc.invalidateQueries({ queryKey: qk.users.me })
      setEditing(false)
      toast.success('Profile updated')
    },
  })

  if (!user) return <div className="flex justify-center py-12"><Spinner className="h-8 w-8" /></div>

  const fallback = `${user.firstname[0]}${user.lastname[0]}`.toUpperCase()

  return (
    <div className="flex flex-col gap-4">
      {/* Profile card */}
      <Card className="p-6">
        <div className="flex items-center gap-4">
          <Avatar fallback={fallback} src={user.avatarUrl} size="lg" />
          <div className="flex-1">
            <h2 className="text-xl font-bold text-gray-900">{user.firstname} {user.lastname}</h2>
            <p className="text-sm text-gray-400">@{user.username}</p>
          </div>
          <Button variant="secondary" size="sm" onClick={() => { setEditing((v) => !v); reset() }}>
            {editing ? 'Cancel' : 'Edit'}
          </Button>
        </div>

        {editing && (
          <form onSubmit={handleSubmit((d) => update.mutate(d))} className="mt-4 flex flex-col gap-3">
            <div className="grid grid-cols-2 gap-3">
              <Input label="First name" error={errors.firstname?.message} {...register('firstname')} />
              <Input label="Last name"  error={errors.lastname?.message}  {...register('lastname')} />
            </div>
            <Button type="submit" loading={update.isPending} className="self-end">Save</Button>
          </form>
        )}
      </Card>

      {/* My posts */}
      <h3 className="font-semibold text-gray-700">My posts</h3>

      {isLoading && <div className="flex justify-center py-8"><Spinner className="h-8 w-8" /></div>}

      {!isLoading && (articlesData?.items.length === 0) && (
        <Card className="py-12 text-center text-gray-400">
          <p>No posts yet</p>
        </Card>
      )}

      {articlesData?.items.map((a) => <ArticleCard key={a.id} article={a} />)}
    </div>
  )
}
