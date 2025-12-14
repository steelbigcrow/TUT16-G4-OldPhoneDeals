import { getApiErrorMessage } from '../../api'
import { useOrders, useProfile } from '../../hooks'

function formatDate(value: string | null | undefined) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString()
}

function formatPrice(price: number | null | undefined) {
  if (price == null) return '—'
  return `$${price.toFixed(2)}`
}

export function ProfileHomePage() {
  const profile = useProfile()
  const orders = useOrders({ page: 1, pageSize: 5 })

  const user = profile.data?.success ? profile.data.data : undefined

  return (
    <div className='grid grid-cols-1 gap-6 lg:grid-cols-2'>
      <div className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
        <h2 className='text-base font-semibold'>Account</h2>

        {profile.isLoading ? <div className='mt-3 text-sm'>Loading…</div> : null}
        {profile.isError ? (
          <div className='mt-3 rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
            {getApiErrorMessage(profile.error)}
          </div>
        ) : null}

        {user ? (
          <dl className='mt-4 grid grid-cols-1 gap-3 text-sm'>
            <div>
              <dt className='text-xs font-medium text-slate-600'>Name</dt>
              <dd className='font-semibold'>
                {user.firstName} {user.lastName}
              </dd>
            </div>
            <div>
              <dt className='text-xs font-medium text-slate-600'>Email</dt>
              <dd className='font-semibold'>{user.email}</dd>
            </div>
            <div>
              <dt className='text-xs font-medium text-slate-600'>Email verified</dt>
              <dd className='font-semibold'>{String(Boolean(user.emailVerified))}</dd>
            </div>
            <div>
              <dt className='text-xs font-medium text-slate-600'>Created</dt>
              <dd className='font-semibold'>{formatDate(user.createdAt)}</dd>
            </div>
          </dl>
        ) : null}
      </div>

      <div className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
        <h2 className='text-base font-semibold'>Recent orders</h2>

        {orders.isLoading ? <div className='mt-3 text-sm'>Loading…</div> : null}
        {orders.isError ? (
          <div className='mt-3 rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
            {getApiErrorMessage(orders.error)}
          </div>
        ) : null}

        {orders.data?.success && orders.data.data ? (
          orders.data.data.items.length === 0 ? (
            <div className='mt-3 text-sm text-slate-700'>No orders yet.</div>
          ) : (
            <ul className='mt-4 space-y-3'>
              {orders.data.data.items.map((o) => (
                <li key={o.id} className='rounded-xl border border-slate-200 p-4'>
                  <div className='flex items-start justify-between gap-3'>
                    <div className='min-w-0'>
                      <div className='text-sm font-semibold'>Order {o.id}</div>
                      <div className='mt-1 text-xs text-slate-600'>{formatDate(o.createdAt)}</div>
                    </div>
                    <div className='text-sm font-semibold'>{formatPrice(o.totalAmount)}</div>
                  </div>
                  <div className='mt-3 text-xs text-slate-600'>
                    Items: <span className='font-medium'>{o.items.length}</span>
                  </div>
                </li>
              ))}
            </ul>
          )
        ) : null}
      </div>
    </div>
  )
}

