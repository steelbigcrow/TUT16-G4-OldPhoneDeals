import { useEffect, useMemo } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { verifyEmail, resendVerification } from '../../api/auth'
import { getApiErrorMessage } from '../../api'
import { setZodFormErrors } from '../../utils/zodFormErrors'
import { useNotifications } from '../../contexts/NotificationContext'

const schema = z.object({
  email: z.string().trim().email(),
  code: z.string().trim().min(1, 'Code is required'),
})

type FormValues = z.infer<typeof schema>

export function VerifyEmailPage() {
  const notifications = useNotifications()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const prefillEmail = useMemo(() => searchParams.get('email') ?? '', [searchParams])

  const form = useForm<FormValues>({
    defaultValues: { email: prefillEmail, code: '' },
  })

  useEffect(() => {
    if (prefillEmail) {
      form.setValue('email', prefillEmail)
    }
  }, [prefillEmail, form])

  return (
    <div className='mx-auto max-w-md space-y-6'>
      <div className='space-y-1'>
        <h1 className='text-xl font-semibold'>Verify email</h1>
        <p className='text-sm text-slate-600'>Enter the code sent to your email.</p>
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
            const res = await verifyEmail(parsed.data)
            if (!res.success) {
              notifications.error(res.message ?? 'Verification failed')
              return
            }

            notifications.success('Email verified. You can now log in.')
            navigate('/login')
          } catch (err) {
            notifications.error(getApiErrorMessage(err))
          }
        })}
      >
        <div>
          <label htmlFor='verify_email_email' className='text-xs font-medium text-slate-700'>
            Email
          </label>
          <input
            id='verify_email_email'
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
          <label htmlFor='verify_email_code' className='text-xs font-medium text-slate-700'>
            Code
          </label>
          <input
            id='verify_email_code'
            {...form.register('code')}
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          />
          {form.formState.errors.code?.message ? (
            <div className='mt-1 text-xs text-rose-700'>{form.formState.errors.code.message}</div>
          ) : null}
        </div>

        <button
          type='submit'
          disabled={form.formState.isSubmitting}
          className='w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50'
        >
          Verify
        </button>

        <button
          type='button'
          onClick={async () => {
            const email = form.getValues('email')
            const parsed = z.string().trim().email().safeParse(email)
            if (!parsed.success) {
              form.setError('email', { type: 'validate', message: 'Enter a valid email' })
              return
            }

            try {
              const res = await resendVerification({ email })
              if (res.success) notifications.info('Verification email resent')
              else notifications.error(res.message ?? 'Failed to resend')
            } catch (err) {
              notifications.error(getApiErrorMessage(err))
            }
          }}
          className='w-full rounded-md border border-slate-200 bg-white px-4 py-2 text-sm font-medium hover:bg-slate-50'
        >
          Resend code
        </button>
      </form>

      <div className='text-sm text-slate-700'>
        Back to{' '}
        <Link to='/login' className='text-slate-900 underline'>
          login
        </Link>
        .
      </div>
    </div>
  )
}
