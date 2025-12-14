import { useMemo } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { useQueryClient } from '@tanstack/react-query'
import { getApiErrorMessage } from '../../api'
import { login } from '../../api/auth'
import { queryKeys } from '../../queryKeys'
import { USER_TOKEN_KEY, safeSetToken } from '../../auth/tokens'
import { getSafeReturnUrl } from '../../auth/returnUrl'
import { setZodFormErrors } from '../../utils/zodFormErrors'
import { useNotifications } from '../../contexts/NotificationContext'

const schema = z.object({
  email: z.string().trim().email(),
  password: z.string().min(1, 'Password is required'),
})

type FormValues = z.infer<typeof schema>

export function LoginPage() {
  const notifications = useNotifications()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()

  const returnUrl = useMemo(() => getSafeReturnUrl(location.search), [location.search])

  const form = useForm<FormValues>({
    defaultValues: { email: '', password: '' },
  })

  return (
    <div className='mx-auto max-w-md space-y-6'>
      <div className='space-y-1'>
        <h1 className='text-xl font-semibold'>Login</h1>
        <p className='text-sm text-slate-600'>Welcome back.</p>
      </div>

      <form
        className='space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'
        onSubmit={form.handleSubmit(async (values) => {
          const parsed = schema.safeParse(values)
          if (!parsed.success) {
            setZodFormErrors(parsed.error, form.setError)
            return
          }

          try {
            const res = await login(parsed.data)
            if (!res.success || !res.data?.token) {
              notifications.error(res.message ?? 'Login failed')
              return
            }

            safeSetToken(USER_TOKEN_KEY, res.data.token)
            await queryClient.invalidateQueries({ queryKey: queryKeys.auth.me })
            notifications.success('Logged in')

            navigate(returnUrl ?? '/home', { replace: true })
          } catch (err) {
            notifications.error(getApiErrorMessage(err))
          }
        })}
      >
        <div>
          <label className='text-xs font-medium text-slate-700'>Email</label>
          <input
            type='email'
            autoComplete='email'
            {...form.register('email')}
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          />
          {form.formState.errors.email?.message ? (
            <div className='mt-1 text-xs text-rose-700'>{form.formState.errors.email.message}</div>
          ) : null}
        </div>

        <div>
          <label className='text-xs font-medium text-slate-700'>Password</label>
          <input
            type='password'
            autoComplete='current-password'
            {...form.register('password')}
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          />
          {form.formState.errors.password?.message ? (
            <div className='mt-1 text-xs text-rose-700'>
              {form.formState.errors.password.message}
            </div>
          ) : null}
        </div>

        <button
          type='submit'
          disabled={form.formState.isSubmitting}
          className='w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50'
        >
          Log in
        </button>

        {returnUrl ? (
          <div className='text-xs text-slate-500'>
            After login you will be redirected to <span className='font-mono'>{returnUrl}</span>
          </div>
        ) : null}
      </form>

      <div className='flex items-center justify-between text-sm'>
        <Link to='/register' className='text-slate-700 hover:underline'>
          Create account
        </Link>
        <Link to='/reset-password' className='text-slate-700 hover:underline'>
          Forgot password
        </Link>
      </div>
    </div>
  )
}

