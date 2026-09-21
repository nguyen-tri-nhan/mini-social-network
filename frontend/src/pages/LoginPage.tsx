import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import TextField from '@mui/material/TextField'
import Button from '@mui/material/Button'
import { authApi } from '../api/auth'
import { usersApi } from '../api/articles'
import { useAuthStore } from '../stores/authStore'

const schema = z.object({
  identifier: z.string().min(1, 'Required'),
  password:   z.string().min(6, 'Min 6 characters'),
})
type Form = z.infer<typeof schema>

export function LoginPage() {
  const navigate  = useNavigate()
  const { setToken, setAuth } = useAuthStore()

  const { register, handleSubmit, formState: { errors } } = useForm<Form>({
    resolver: zodResolver(schema),
  })

  const mutation = useMutation({
    mutationFn: authApi.signin,
    onSuccess: async (auth) => {
      setToken(auth.accessToken)
      const user = await usersApi.me()
      setAuth(auth.accessToken, user)
      navigate('/')
    },
    onError: () => toast.error('Invalid credentials'),
  })

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      {/* Left — branding */}
      <Box
        sx={{
          flex: 1, display: { xs: 'none', lg: 'flex' }, alignItems: 'center', justifyContent: 'center',
          bgcolor: 'primary.main',
        }}
      >
        <Box sx={{ textAlign: 'center', color: 'white' }}>
          <Typography sx={{ mb: 2, fontSize: 64, fontWeight: 700 }}>S</Typography>
          <Typography variant="h4" fontWeight={700}>Social</Typography>
          <Typography sx={{ mt: 1, color: 'rgba(255,255,255,0.85)' }}>Connect and share moments</Typography>
        </Box>
      </Box>

      {/* Right — form */}
      <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', p: 4 }}>
        <Box sx={{ width: '100%', maxWidth: 384 }}>
          <Typography variant="h5" fontWeight={700} sx={{ mb: 3 }}>Sign in</Typography>

          <Box component="form" onSubmit={handleSubmit((d) => mutation.mutate(d))} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              label="Username or email"
              placeholder="username or email"
              error={!!errors.identifier}
              helperText={errors.identifier?.message}
              fullWidth
              {...register('identifier')}
            />
            <TextField
              label="Password"
              type="password"
              placeholder="••••••"
              error={!!errors.password}
              helperText={errors.password?.message}
              fullWidth
              {...register('password')}
            />
            <Button type="submit" size="large" variant="contained" fullWidth disabled={mutation.isPending} sx={{ mt: 1 }}>
              {mutation.isPending ? 'Signing in…' : 'Sign in'}
            </Button>
          </Box>

          <Typography variant="body2" color="text.secondary" sx={{ mt: 3, textAlign: 'center' }}>
            No account?{' '}
            <Typography component={Link} to="/signup" variant="body2" fontWeight={500} sx={{ color: 'primary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}>
              Sign up
            </Typography>
          </Typography>
        </Box>
      </Box>
    </Box>
  )
}
