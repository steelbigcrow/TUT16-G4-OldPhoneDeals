import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { getApiErrorMessage } from '../../api'
import { requestPasswordReset, resetPassword } from '../../api/auth'
import { setZodFormErrors } from '../../utils/zodFormErrors'
import { useNotifications } from '../../contexts/NotificationContext'

const requestSchema = z.object({
  email: z.string().trim().email(),
})

const resetSchema = z.object({
  email: z.string().trim().email(),
  code: z.string().trim().min(1, 'Code is required'),
  newPassword: z.string().min(6, 'Password must be at least 6 characters'),
})

type RequestValues = z.infer<typeof requestSchema>
type ResetValues = z.infer<typeof resetSchema>

export function ResetPasswordPage() {
  const notifications = useNotifications()
  const navigate = useNavigate()

  const requestForm = useForm<RequestValues>({
    defaultValues: { email: '' },
  })

  const resetForm = useForm<ResetValues>({
    defaultValues: { email: '', code: '', newPassword: '' },
  })

  return (
    <div className='mx-auto max-w-md space-y-6'>
      <div className='space-y-1'>
        <h1 className='text-xl font-semibold'>Reset password</h1>
        <p className='text-sm text-slate-600'>Request a reset code and set a new password.</p>
      </div>

      <div className='space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
        <h2 className='text-sm font-semibold'>1) Request reset code</h2>

        <form
          className='space-y-3'
          onSubmit={requestForm.handleSubmit(async (values) => {
            const parsed = requestSchema.safeParse(values)
            if (!parsed.success) {
              setZodFormErrors(parsed.error, requestForm.setError)
              return
            }

            try {
              const res = await requestPasswordReset(parsed.data)
              if (res.success) {
                notifications.info('If the email exists, a reset code has been sent.')
                resetForm.setValue('email', parsed.data.email)
              } else {
                notifications.error(res.message ?? 'Failed to request reset code')
              }
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
              {...requestForm.register('email')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
            />
            {requestForm.formState.errors.email?.message ? (
              <div className='mt-1 text-xs text-rose-700'>
                {requestForm.formState.errors.email.message}
              </div>
            ) : null}
          </div>

          <button
            type='submit'
            disabled={requestForm.formState.isSubmitting}
            className='w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50'
          >
            Send code
          </button>
        </form>
      </div>

      <div className='space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
        <h2 className='text-sm font-semibold'>2) Set new password</h2>

        <form
          className='space-y-3'
          onSubmit={resetForm.handleSubmit(async (values) => {
            const parsed = resetSchema.safeParse(values)
            if (!parsed.success) {
              setZodFormErrors(parsed.error, resetForm.setError)
              return
            }

            try {
              const res = await resetPassword(parsed.data)
              if (res.success) {
                notifications.success('Password reset. Please log in.')
                navigate('/login')
              } else {
                notifications.error(res.message ?? 'Failed to reset password')
              }
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
              {...resetForm.register('email')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
            />
            {resetForm.formState.errors.email?.message ? (
              <div className='mt-1 text-xs text-rose-700'>{resetForm.formState.errors.email.message}</div>
            ) : null}
          </div>

          <div>
            <label className='text-xs font-medium text-slate-700'>Code</label>
            <input
              {...resetForm.register('code')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
            />
            {resetForm.formState.errors.code?.message ? (
              <div className='mt-1 text-xs text-rose-700'>{resetForm.formState.errors.code.message}</div>
            ) : null}
          </div>

          <div>
            <label className='text-xs font-medium text-slate-700'>New password</label>
            <input
              type='password'
              autoComplete='new-password'
              {...resetForm.register('newPassword')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
            />
            {resetForm.formState.errors.newPassword?.message ? (
              <div className='mt-1 text-xs text-rose-700'>
                {resetForm.formState.errors.newPassword.message}
              </div>
            ) : null}
          </div>

          <button
            type='submit'
            disabled={resetForm.formState.isSubmitting}
            className='w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50'
          >
            Reset password
          </button>
        </form>
      </div>

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
