import { useMemo, useState } from 'react'
import { z } from 'zod'
import { getApiErrorMessage } from '../../api'
import { useAdminOrders } from '../../hooks'

const filterSchema = z.object({
  searchTerm: z.string().optional(),
})

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

export function AdminOrdersPage() {
  const [pageIndex, setPageIndex] = useState(0)
  const [pageSize] = useState(10)

  const [searchDraft, setSearchDraft] = useState('')

  const parsedFilters = useMemo(() => {
    const parsed = filterSchema.safeParse({ searchTerm: searchDraft })
    if (!parsed.success) return { searchTerm: undefined }
    const searchTerm = parsed.data.searchTerm?.trim() ? parsed.data.searchTerm.trim() : undefined
    return { searchTerm }
  }, [searchDraft])

  const query = useAdminOrders({
    pageIndex,
    pageSize,
    searchTerm: parsedFilters.searchTerm,
  })

  const page = query.data?.success ? query.data.data : undefined
  const orders = page?.content ?? []

  return (
    <div className='space-y-4'>
      <div>
        <h2 className='text-base font-semibold'>Orders</h2>
        <p className='mt-1 text-sm text-slate-600'>View order activity across the platform.</p>
      </div>

      <div className='rounded-2xl border border-slate-200 bg-white p-4 shadow-sm'>
        <label className='text-xs font-medium text-slate-700'>Search</label>
        <input
          value={searchDraft}
          onChange={(e) => setSearchDraft(e.target.value)}
          placeholder='orderId, user name, email...'
          className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
        />
        <div className='mt-3 flex items-center justify-between'>
          <button
            type='button'
            onClick={() => setPageIndex(0)}
            className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50'
          >
            Apply
          </button>
          <div className='text-sm text-slate-600'>
            Page <span className='font-medium text-slate-900'>{page?.currentPage ?? 1}</span> /{' '}
            <span className='font-medium text-slate-900'>{page?.totalPages ?? 1}</span>
          </div>
        </div>
      </div>

      {query.isLoading ? <div>Loading…</div> : null}
      {query.isError ? (
        <div className='rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
          {getApiErrorMessage(query.error)}
        </div>
      ) : null}

      {page ? (
        <div className='overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm'>
          <table className='w-full text-left text-sm'>
            <thead className='bg-slate-50 text-xs font-semibold text-slate-700'>
              <tr>
                <th className='px-4 py-3'>Order</th>
                <th className='px-4 py-3'>User</th>
                <th className='px-4 py-3'>Items</th>
                <th className='px-4 py-3'>Total</th>
                <th className='px-4 py-3'>Created</th>
              </tr>
            </thead>
            <tbody className='divide-y divide-slate-200'>
              {orders.map((o) => (
                <tr key={o.id}>
                  <td className='px-4 py-3 font-medium'>{o.id}</td>
                  <td className='px-4 py-3'>
                    <div className='font-medium'>{o.userName}</div>
                    <div className='text-xs text-slate-600'>{o.userEmail}</div>
                  </td>
                  <td className='px-4 py-3'>{o.itemCount ?? '—'}</td>
                  <td className='px-4 py-3 font-medium'>{formatPrice(o.totalAmount)}</td>
                  <td className='px-4 py-3'>{formatDate(o.createdAt)}</td>
                </tr>
              ))}
              {orders.length === 0 ? (
                <tr>
                  <td className='px-4 py-8 text-center text-sm text-slate-600' colSpan={5}>
                    No orders found.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      ) : null}

      <div className='flex items-center justify-between'>
        <button
          type='button'
          disabled={pageIndex <= 0}
          onClick={() => setPageIndex((p) => Math.max(0, p - 1))}
          className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50'
        >
          Previous
        </button>
        <button
          type='button'
          disabled={pageIndex + 1 >= (page?.totalPages ?? 1)}
          onClick={() => setPageIndex((p) => p + 1)}
          className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50'
        >
          Next
        </button>
      </div>
    </div>
  )
}

