import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'
import { usersApi } from '../api/articles'
import { useAuthStore } from '../stores/authStore'
import { Button, Input } from '../components/ui'

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
  const { setAuth } = useAuthStore()

  const { register, handleSubmit, formState: { errors } } = useForm<Form>({
    resolver: zodResolver(schema),
  })

  const mutation = useMutation({
    mutationFn: authApi.signup,
    onSuccess: async (auth) => {
      const user = await usersApi.me()
      setAuth(auth.accessToken, user)
      navigate('/')
    },
  })

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-sm">
        <div className="mb-6 text-center">
          <div className="text-3xl font-bold text-brand">Social</div>
          <h2 className="mt-1 text-lg font-semibold text-gray-700">Create your account</h2>
        </div>

        <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="flex flex-col gap-3">
          <div className="grid grid-cols-2 gap-3">
            <Input label="First name" placeholder="Nhan" error={errors.firstname?.message} {...register('firstname')} />
            <Input label="Last name"  placeholder="Nguyen" error={errors.lastname?.message}  {...register('lastname')} />
          </div>
          <Input label="Username"  placeholder="nhan123"        error={errors.username?.message} {...register('username')} />
          <Input label="Email"     placeholder="nhan@mail.com"  error={errors.email?.message}    {...register('email')} />
          <Input label="Password"  type="password" placeholder="••••••" error={errors.password?.message} {...register('password')} />

          <Button type="submit" size="lg" loading={mutation.isPending} className="mt-2 w-full">
            Create account
          </Button>
        </form>

        <p className="mt-5 text-center text-sm text-gray-500">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-brand hover:underline">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
