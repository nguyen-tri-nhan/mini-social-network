import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import MuiAvatar from '@mui/material/Avatar'
import Typography from '@mui/material/Typography'
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
import CircularProgress from '@mui/material/CircularProgress'
import { usersApi, articlesApi } from '../api/articles'
import { useAuthStore } from '../stores/authStore'
import { qk } from '../hooks/queryKeys'
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

  if (!user) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress size={32} />
      </Box>
    )
  }

  const fallback = `${user.firstname[0]}${user.lastname[0]}`.toUpperCase()

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {/* Profile card */}
      <Card sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <MuiAvatar src={user.avatarUrl} sx={{ width: 56, height: 56, fontSize: 16 }}>
            {!user.avatarUrl && fallback}
          </MuiAvatar>
          <Box sx={{ flex: 1 }}>
            <Typography variant="h6" fontWeight={700}>{user.firstname} {user.lastname}</Typography>
            <Typography variant="body2" color="text.disabled">@{user.username}</Typography>
          </Box>
          <Button variant="outlined" size="small" onClick={() => { setEditing((v) => !v); reset() }}>
            {editing ? 'Cancel' : 'Edit'}
          </Button>
        </Box>

        {editing && (
          <Box component="form" onSubmit={handleSubmit((d) => update.mutate(d))} sx={{ mt: 2, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
            <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1.5 }}>
              <TextField label="First name" error={!!errors.firstname} helperText={errors.firstname?.message} {...register('firstname')} />
              <TextField label="Last name" error={!!errors.lastname} helperText={errors.lastname?.message} {...register('lastname')} />
            </Box>
            <Button type="submit" variant="contained" disabled={update.isPending} sx={{ alignSelf: 'flex-end' }}>
              {update.isPending ? 'Saving…' : 'Save'}
            </Button>
          </Box>
        )}
      </Card>

      {/* My posts */}
      <Typography fontWeight={600} color="text.secondary">My posts</Typography>

      {isLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress size={32} />
        </Box>
      )}

      {!isLoading && articlesData?.items.length === 0 && (
        <Card sx={{ py: 6, textAlign: 'center', color: 'text.disabled' }}>
          <Typography>No posts yet</Typography>
        </Card>
      )}

      {articlesData?.items.map((a) => <ArticleCard key={a.id} article={a} />)}
    </Box>
  )
}
