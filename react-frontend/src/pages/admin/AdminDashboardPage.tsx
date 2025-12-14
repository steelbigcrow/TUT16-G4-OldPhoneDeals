import { getApiErrorMessage } from '../../api'
import { useAdminStats } from '../../hooks'

export function AdminDashboardPage() {
  const stats = useAdminStats()
  const data = stats.data?.success ? stats.data.data : undefined

  return (
    <div className='space-y-4'>
      <div>
        <h2 className='text-base font-semibold'>Dashboard</h2>
        <p className='mt-1 text-sm text-slate-600'>High-level platform metrics.</p>
      </div>

      {stats.isLoading ? <div>Loading…</div> : null}
      {stats.isError ? (
        <div className='rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
          {getApiErrorMessage(stats.error)}
        </div>
      ) : null}

      {data ? (
        <div className='grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4'>
          <div className='rounded-2xl border border-slate-200 bg-white p-5 shadow-sm'>
            <div className='text-xs font-medium text-slate-600'>Users</div>
            <div className='mt-1 text-2xl font-semibold'>{data.totalUsers ?? 0}</div>
          </div>
          <div className='rounded-2xl border border-slate-200 bg-white p-5 shadow-sm'>
            <div className='text-xs font-medium text-slate-600'>Listings</div>
            <div className='mt-1 text-2xl font-semibold'>{data.totalListings ?? 0}</div>
          </div>
          <div className='rounded-2xl border border-slate-200 bg-white p-5 shadow-sm'>
            <div className='text-xs font-medium text-slate-600'>Reviews</div>
            <div className='mt-1 text-2xl font-semibold'>{data.totalReviews ?? 0}</div>
          </div>
          <div className='rounded-2xl border border-slate-200 bg-white p-5 shadow-sm'>
            <div className='text-xs font-medium text-slate-600'>Sales</div>
            <div className='mt-1 text-2xl font-semibold'>{data.totalSales ?? 0}</div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

