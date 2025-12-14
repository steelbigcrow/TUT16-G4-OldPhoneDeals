import { getApiErrorMessage } from '../../api'
import { useAdminAuth } from '../../hooks'

function formatDate(value: string | null | undefined) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString()
}

export function AdminHomePage() {
  const profile = useAdminAuth()
  const admin = profile.data?.success ? profile.data.data : undefined

  return (
    <div className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
      <h2 className='text-base font-semibold'>Admin profile</h2>

      {profile.isLoading ? <div className='mt-3 text-sm'>Loading…</div> : null}
      {profile.isError ? (
        <div className='mt-3 rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
          {getApiErrorMessage(profile.error)}
        </div>
      ) : null}

      {admin ? (
        <dl className='mt-4 grid grid-cols-1 gap-3 text-sm'>
          <div>
            <dt className='text-xs font-medium text-slate-600'>Name</dt>
            <dd className='font-semibold'>
              {admin.firstName} {admin.lastName}
            </dd>
          </div>
          <div>
            <dt className='text-xs font-medium text-slate-600'>Email</dt>
            <dd className='font-semibold'>{admin.email}</dd>
          </div>
          <div>
            <dt className='text-xs font-medium text-slate-600'>Role</dt>
            <dd className='font-semibold'>{admin.role}</dd>
          </div>
          <div>
            <dt className='text-xs font-medium text-slate-600'>Last login</dt>
            <dd className='font-semibold'>{formatDate(admin.lastLogin)}</dd>
          </div>
        </dl>
      ) : null}
    </div>
  )
}

