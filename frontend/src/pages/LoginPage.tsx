import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { authApi } from '../api/auth'
import { usersApi } from '../api/articles'
import { useAuthStore } from '../stores/authStore'
import { Button, Input } from '../components/ui'

const schema = z.object({
  identifier: z.string().min(1, 'Required'),
  password:   z.string().min(6, 'Min 6 characters'),
})
type Form = z.infer<typeof schema>

export function LoginPage() {
  const navigate  = useNavigate()
  const { setAuth } = useAuthStore()

  const { register, handleSubmit, formState: { errors } } = useForm<Form>({
    resolver: zodResolver(schema),
  })

  const mutation = useMutation({
    mutationFn: authApi.signin,
    onSuccess: async (auth) => {
      const user = await usersApi.me()
      setAuth(auth.accessToken, user)
      navigate('/')
    },
    onError: () => toast.error('Invalid credentials'),
  })

  return (
    <div className="flex min-h-screen">
      {/* Left — branding */}
      <div className="hidden flex-1 items-center justify-center bg-brand lg:flex">
        <div className="text-center text-white">
          <div className="mb-4 text-6xl font-bold">S</div>
          <h1 className="text-3xl font-bold">Social</h1>
          <p className="mt-2 text-blue-100">Connect and share moments</p>
        </div>
      </div>

      {/* Right — form */}
      <div className="flex flex-1 items-center justify-center p-8">
        <div className="w-full max-w-sm">
          <h2 className="mb-6 text-2xl font-bold text-gray-900">Sign in</h2>

          <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="flex flex-col gap-4">
            <Input
              label="Username or email"
              placeholder="username or email"
              error={errors.identifier?.message}
              {...register('identifier')}
            />
            <Input
              label="Password"
              type="password"
              placeholder="••••••"
              error={errors.password?.message}
              {...register('password')}
            />
            <Button type="submit" size="lg" loading={mutation.isPending} className="w-full mt-2">
              Sign in
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500">
            No account?{' '}
            <Link to="/signup" className="font-medium text-brand hover:underline">
              Sign up
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
