import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { register } from '../../api/auth'
import { getApiErrorMessage } from '../../api'
import { setZodFormErrors } from '../../utils/zodFormErrors'
import { useNotifications } from '../../contexts/NotificationContext'

const schema = z.object({
  firstName: z.string().trim().min(2, 'First name is required'),
  lastName: z.string().trim().min(2, 'Last name is required'),
  email: z.string().trim().email(),
  password: z.string().min(6, 'Password must be at least 6 characters'),
})

type FormValues = z.infer<typeof schema>

export function RegisterPage() {
  const notifications = useNotifications()
  const navigate = useNavigate()

  const form = useForm<FormValues>({
    defaultValues: { firstName: '', lastName: '', email: '', password: '' },
  })

  return (
    <div className='mx-auto max-w-md space-y-6'>
      <div className='space-y-1'>
        <h1 className='text-xl font-semibold'>Register</h1>
        <p className='text-sm text-slate-600'>Create a new account.</p>
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
            const res = await register(parsed.data)
            if (!res.success) {
              notifications.error(res.message ?? 'Registration failed')
              return
            }

            notifications.success('Registered. Please verify your email.')
            const qs = new URLSearchParams({ email: parsed.data.email }).toString()
            navigate(`/verify-email?${qs}`)
          } catch (err) {
            notifications.error(getApiErrorMessage(err))
          }
        })}
      >
        <div className='grid grid-cols-1 gap-3 sm:grid-cols-2'>
          <div>
            <label className='text-xs font-medium text-slate-700'>First name</label>
            <input
              {...form.register('firstName')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
            />
            {form.formState.errors.firstName?.message ? (
              <div className='mt-1 text-xs text-rose-700'>
                {form.formState.errors.firstName.message}
              </div>
            ) : null}
          </div>

          <div>
            <label className='text-xs font-medium text-slate-700'>Last name</label>
            <input
              {...form.register('lastName')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
            />
            {form.formState.errors.lastName?.message ? (
              <div className='mt-1 text-xs text-rose-700'>
                {form.formState.errors.lastName.message}
              </div>
            ) : null}
          </div>
        </div>

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
            autoComplete='new-password'
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
          Create account
        </button>
      </form>

      <div className='text-sm text-slate-700'>
        Already have an account?{' '}
        <Link to='/login' className='text-slate-900 underline'>
          Log in
        </Link>
      </div>
    </div>
  )
}

