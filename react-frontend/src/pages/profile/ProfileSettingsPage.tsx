import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { getApiErrorMessage } from '../../api'
import { useChangePassword, useProfile, useUpdateProfile } from '../../hooks'
import { setZodFormErrors } from '../../utils/zodFormErrors'
import { useNotifications } from '../../contexts/NotificationContext'

const updateProfileSchema = z.object({
  firstName: z.string().trim().min(2),
  lastName: z.string().trim().min(2),
  email: z.string().trim().email(),
  currentPassword: z.string().optional(),
})

const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, 'Current password is required'),
  newPassword: z.string().min(6, 'New password must be at least 6 characters'),
})

type UpdateProfileValues = z.infer<typeof updateProfileSchema>
type ChangePasswordValues = z.infer<typeof changePasswordSchema>

export function ProfileSettingsPage() {
  const notifications = useNotifications()

  const profile = useProfile()
  const updateProfile = useUpdateProfile()
  const changePassword = useChangePassword()

  const updateForm = useForm<UpdateProfileValues>({
    defaultValues: { firstName: '', lastName: '', email: '', currentPassword: '' },
  })

  const passwordForm = useForm<ChangePasswordValues>({
    defaultValues: { currentPassword: '', newPassword: '' },
  })

  useEffect(() => {
    if (profile.data?.success && profile.data.data) {
      updateForm.reset({
        firstName: profile.data.data.firstName,
        lastName: profile.data.data.lastName,
        email: profile.data.data.email,
        currentPassword: '',
      })
    }
  }, [profile.data, updateForm])

  return (
    <div className='grid grid-cols-1 gap-6 lg:grid-cols-2'>
      <div className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
        <h2 className='text-base font-semibold'>Update profile</h2>

        {profile.isError ? (
          <div className='mt-3 rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
            {getApiErrorMessage(profile.error)}
          </div>
        ) : null}

        <form
          className='mt-4 space-y-3'
          onSubmit={updateForm.handleSubmit(async (values) => {
            const parsed = updateProfileSchema.safeParse(values)
            if (!parsed.success) {
              setZodFormErrors(parsed.error, updateForm.setError)
              return
            }

            try {
              const res = await updateProfile.mutateAsync(parsed.data)
              if (res.success) notifications.success('Profile updated')
              else notifications.error(res.message ?? 'Failed to update')
            } catch (err) {
              notifications.error(getApiErrorMessage(err))
            }
          })}
        >
          <div className='grid grid-cols-1 gap-3 sm:grid-cols-2'>
            <div>
              <label htmlFor='profile_settings_first_name' className='text-xs font-medium text-slate-700'>
                First name
              </label>
              <input
                id='profile_settings_first_name'
                {...updateForm.register('firstName')}
                className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
              />
              {updateForm.formState.errors.firstName?.message ? (
                <div className='mt-1 text-xs text-rose-700'>
                  {updateForm.formState.errors.firstName.message}
                </div>
              ) : null}
            </div>
            <div>
              <label htmlFor='profile_settings_last_name' className='text-xs font-medium text-slate-700'>
                Last name
              </label>
              <input
                id='profile_settings_last_name'
                {...updateForm.register('lastName')}
                className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
              />
              {updateForm.formState.errors.lastName?.message ? (
                <div className='mt-1 text-xs text-rose-700'>
                  {updateForm.formState.errors.lastName.message}
                </div>
              ) : null}
            </div>
          </div>

          <div>
            <label htmlFor='profile_settings_email' className='text-xs font-medium text-slate-700'>
              Email
            </label>
            <input
              id='profile_settings_email'
              type='email'
              autoComplete='email'
              {...updateForm.register('email')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
            />
            {updateForm.formState.errors.email?.message ? (
              <div className='mt-1 text-xs text-rose-700'>{updateForm.formState.errors.email.message}</div>
            ) : null}
          </div>

          <div>
            <label
              htmlFor='profile_settings_email_change_current_password'
              className='text-xs font-medium text-slate-700'
            >
              Current password (required when changing email)
            </label>
            <input
              id='profile_settings_email_change_current_password'
              type='password'
              autoComplete='current-password'
              {...updateForm.register('currentPassword')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
            />
          </div>

          <button
            type='submit'
            disabled={updateProfile.isPending}
            className='w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50'
          >
            Save changes
          </button>
        </form>
      </div>

      <div className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
        <h2 className='text-base font-semibold'>Change password</h2>

        <form
          className='mt-4 space-y-3'
          onSubmit={passwordForm.handleSubmit(async (values) => {
            const parsed = changePasswordSchema.safeParse(values)
            if (!parsed.success) {
              setZodFormErrors(parsed.error, passwordForm.setError)
              return
            }

            try {
              const res = await changePassword.mutateAsync(parsed.data)
              if (res.success) {
                notifications.success('Password changed')
                passwordForm.reset({ currentPassword: '', newPassword: '' })
              } else {
                notifications.error(res.message ?? 'Failed to change password')
              }
            } catch (err) {
              notifications.error(getApiErrorMessage(err))
            }
          })}
        >
          <div>
            <label
              htmlFor='profile_settings_password_current'
              className='text-xs font-medium text-slate-700'
            >
              Current password
            </label>
            <input
              id='profile_settings_password_current'
              type='password'
              autoComplete='current-password'
              {...passwordForm.register('currentPassword')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
            />
            {passwordForm.formState.errors.currentPassword?.message ? (
              <div className='mt-1 text-xs text-rose-700'>
                {passwordForm.formState.errors.currentPassword.message}
              </div>
            ) : null}
          </div>

          <div>
            <label htmlFor='profile_settings_password_new' className='text-xs font-medium text-slate-700'>
              New password
            </label>
            <input
              id='profile_settings_password_new'
              type='password'
              autoComplete='new-password'
              {...passwordForm.register('newPassword')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
            />
            {passwordForm.formState.errors.newPassword?.message ? (
              <div className='mt-1 text-xs text-rose-700'>
                {passwordForm.formState.errors.newPassword.message}
              </div>
            ) : null}
          </div>

          <button
            type='submit'
            disabled={changePassword.isPending}
            className='w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50'
          >
            Change password
          </button>
        </form>
      </div>
    </div>
  )
}
