import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import TextField from '@mui/material/TextField'
import Button from '@mui/material/Button'
import { authApi } from '../api/auth'
import { usersApi } from '../api/articles'
import { useAuthStore } from '../stores/authStore'

const schema = z.object({
  firstname: z.string().min(1, 'Required'),
  lastname:  z.string().min(1, 'Required'),
  username:  z.string().min(1, 'Required').max(50),
  email:     z.string().email('Invalid email'),
  password:  z.string().min(6, 'Min 6 characters'),
})
type Form = z.infer<typeof schema>

export function SignUpPage() {
  const navigate   = useNavigate()
  const { setToken, setAuth } = useAuthStore()

  const { register, handleSubmit, formState: { errors } } = useForm<Form>({
    resolver: zodResolver(schema),
  })

  const mutation = useMutation({
    mutationFn: authApi.signup,
    onSuccess: async (auth) => {
      setToken(auth.accessToken)
      const user = await usersApi.me()
      setAuth(auth.accessToken, user)
      navigate('/')
    },
  })

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', alignItems: 'center', justifyContent: 'center', bgcolor: 'background.default', p: 2 }}>
      <Box sx={{ width: '100%', maxWidth: 448, borderRadius: 4, bgcolor: 'white', p: 4, boxShadow: 1 }}>
        <Box sx={{ mb: 3, textAlign: 'center' }}>
          <Typography variant="h5" fontWeight={700} color="primary.main">Social</Typography>
          <Typography variant="subtitle1" fontWeight={600} color="text.secondary" sx={{ mt: 0.5 }}>
            Create your account
          </Typography>
        </Box>

        <Box component="form" onSubmit={handleSubmit((d) => mutation.mutate(d))} sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1.5 }}>
            <TextField label="First name" placeholder="Nhan" error={!!errors.firstname} helperText={errors.firstname?.message} {...register('firstname')} />
            <TextField label="Last name" placeholder="Nguyen" error={!!errors.lastname} helperText={errors.lastname?.message} {...register('lastname')} />
          </Box>
          <TextField label="Username" placeholder="nhan123" fullWidth error={!!errors.username} helperText={errors.username?.message} {...register('username')} />
          <TextField label="Email" placeholder="nhan@mail.com" fullWidth error={!!errors.email} helperText={errors.email?.message} {...register('email')} />
          <TextField label="Password" type="password" placeholder="••••••" fullWidth error={!!errors.password} helperText={errors.password?.message} {...register('password')} />

          <Button type="submit" size="large" variant="contained" fullWidth disabled={mutation.isPending} sx={{ mt: 1 }}>
            {mutation.isPending ? 'Creating…' : 'Create account'}
          </Button>
        </Box>

        <Typography variant="body2" color="text.secondary" sx={{ mt: 2.5, textAlign: 'center' }}>
          Already have an account?{' '}
          <Typography component={Link} to="/login" variant="body2" fontWeight={500} sx={{ color: 'primary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}>
            Sign in
          </Typography>
        </Typography>
      </Box>
    </Box>
  )
}
