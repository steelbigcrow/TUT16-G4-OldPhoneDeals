import { useMemo, useState } from 'react'
import { z } from 'zod'
import { apiClient, getApiErrorMessage } from '../../api'
import { useAdminOrders, useAdminSalesStats } from '../../hooks'
import { useNotifications } from '../../contexts/NotificationContext'

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
  const notifications = useNotifications()
  const [pageIndex, setPageIndex] = useState(0)
  const [pageSize] = useState(10)

  const [searchDraft, setSearchDraft] = useState('')
  const [brandFilter, setBrandFilter] = useState('All Brands')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [sortOption, setSortOption] = useState('createdAt:desc')

  const filterSchema = useMemo(
    () =>
      z.object({
        searchTerm: z.string().optional(),
        brandFilter: z.string().optional(),
        startDate: z.string().optional(),
        endDate: z.string().optional(),
        sortOption: z.string().optional(),
      }),
    [],
  )

  const parsedFilters = useMemo(() => {
    const parsed = filterSchema.safeParse({
      searchTerm: searchDraft,
      brandFilter,
      startDate,
      endDate,
      sortOption,
    })
    if (!parsed.success) {
      return {
        searchTerm: undefined,
        brandFilter: undefined,
        startDate: undefined,
        endDate: undefined,
        sortBy: undefined,
        sortOrder: undefined as 'asc' | 'desc' | undefined,
      }
    }

    const searchTerm = parsed.data.searchTerm?.trim() ? parsed.data.searchTerm.trim() : undefined

    const normalizedBrand =
      parsed.data.brandFilter && parsed.data.brandFilter !== 'All Brands'
        ? parsed.data.brandFilter
        : undefined

    const startDateParam = parsed.data.startDate ? `${parsed.data.startDate}T00:00:00` : undefined
    const endDateParam = parsed.data.endDate ? `${parsed.data.endDate}T23:59:59` : undefined

    const [sortByRaw, sortOrderRaw] = (parsed.data.sortOption ?? '').split(':')
    const sortBy = sortByRaw ? sortByRaw : undefined
    const sortOrder: 'asc' | 'desc' | undefined =
      sortOrderRaw === 'asc' ? 'asc' : sortOrderRaw === 'desc' ? 'desc' : undefined

    return {
      searchTerm,
      brandFilter: normalizedBrand,
      startDate: startDateParam,
      endDate: endDateParam,
      sortBy,
      sortOrder,
    }
  }, [filterSchema, searchDraft, brandFilter, startDate, endDate, sortOption])

  const query = useAdminOrders({
    pageIndex,
    pageSize,
    searchTerm: parsedFilters.searchTerm,
    brandFilter: parsedFilters.brandFilter,
    startDate: parsedFilters.startDate,
    endDate: parsedFilters.endDate,
    sortBy: parsedFilters.sortBy,
    sortOrder: parsedFilters.sortOrder,
  })

  const stats = useAdminSalesStats()

  const page = query.data?.success ? query.data.data : undefined
  const orders = page?.content ?? []

  return (
    <div className='space-y-4'>
      <div>
        <h2 className='text-base font-semibold'>Orders</h2>
        <p className='mt-1 text-sm text-slate-600'>
          View and export order activity across the platform.
        </p>
      </div>

      <div className='grid grid-cols-1 gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm lg:grid-cols-12'>
        <div className='lg:col-span-4'>
          <label className='text-xs font-medium text-slate-700'>Search</label>
          <input
            value={searchDraft}
            onChange={(e) => setSearchDraft(e.target.value)}
            placeholder='orderId, user name, email, item title...'
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          />
        </div>

        <div className='lg:col-span-2'>
          <label className='text-xs font-medium text-slate-700'>Brand</label>
          <select
            value={brandFilter}
            onChange={(e) => setBrandFilter(e.target.value)}
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          >
            {[
              'All Brands',
              'Samsung',
              'Apple',
              'HTC',
              'Huawei',
              'Nokia',
              'LG',
              'Motorola',
              'Sony',
              'BlackBerry',
            ].map((b) => (
              <option key={b} value={b}>
                {b}
              </option>
            ))}
          </select>
        </div>

        <div className='lg:col-span-2'>
          <label className='text-xs font-medium text-slate-700'>Start date</label>
          <input
            type='date'
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          />
        </div>

        <div className='lg:col-span-2'>
          <label className='text-xs font-medium text-slate-700'>End date</label>
          <input
            type='date'
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          />
        </div>

        <div className='lg:col-span-2'>
          <label className='text-xs font-medium text-slate-700'>Sort</label>
          <select
            value={sortOption}
            onChange={(e) => setSortOption(e.target.value)}
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          >
            <option value='createdAt:desc'>Newest</option>
            <option value='createdAt:asc'>Oldest</option>
            <option value='totalAmount:desc'>Total: high to low</option>
            <option value='totalAmount:asc'>Total: low to high</option>
          </select>
        </div>

        <div className='lg:col-span-12 flex flex-wrap items-center justify-between gap-2'>
          <div className='flex flex-wrap items-center gap-2'>
            <button
              type='button'
              onClick={() => setPageIndex(0)}
              className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50'
            >
              Apply
            </button>

            <button
              type='button'
              onClick={() => {
                setSearchDraft('')
                setBrandFilter('All Brands')
                setStartDate('')
                setEndDate('')
                setSortOption('createdAt:desc')
                setPageIndex(0)
              }}
              className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50'
            >
              Reset
            </button>

            <button
              type='button'
              disabled={query.isLoading}
              onClick={async () => {
                try {
                  const res = await apiClient.get('/admin/orders/export', {
                    params: {
                      format: 'csv',
                      searchTerm: parsedFilters.searchTerm,
                      brandFilter: parsedFilters.brandFilter,
                      startDate: parsedFilters.startDate,
                      endDate: parsedFilters.endDate,
                      sortBy: parsedFilters.sortBy,
                      sortOrder: parsedFilters.sortOrder,
                    },
                    responseType: 'blob',
                  })

                  const blob = res.data as Blob
                  const url = window.URL.createObjectURL(blob)
                  const a = document.createElement('a')
                  a.href = url
                  a.download = 'orders_export.csv'
                  a.click()
                  window.URL.revokeObjectURL(url)

                  notifications.success('Exported CSV')
                } catch (err) {
                  notifications.error(getApiErrorMessage(err))
                }
              }}
              className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50'
            >
              Export CSV
            </button>

            <button
              type='button'
              disabled={query.isLoading}
              onClick={async () => {
                try {
                  const res = await apiClient.get('/admin/orders/export', {
                    params: {
                      format: 'json',
                      searchTerm: parsedFilters.searchTerm,
                      brandFilter: parsedFilters.brandFilter,
                      startDate: parsedFilters.startDate,
                      endDate: parsedFilters.endDate,
                      sortBy: parsedFilters.sortBy,
                      sortOrder: parsedFilters.sortOrder,
                    },
                    responseType: 'blob',
                  })

                  const blob = res.data as Blob
                  const url = window.URL.createObjectURL(blob)
                  const a = document.createElement('a')
                  a.href = url
                  a.download = 'orders_export.json'
                  a.click()
                  window.URL.revokeObjectURL(url)

                  notifications.success('Exported JSON')
                } catch (err) {
                  notifications.error(getApiErrorMessage(err))
                }
              }}
              className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50'
            >
              Export JSON
            </button>
          </div>

          <div className='text-sm text-slate-600'>
            Page <span className='font-medium text-slate-900'>{page?.currentPage ?? 1}</span> /{' '}
            <span className='font-medium text-slate-900'>{page?.totalPages ?? 1}</span>
          </div>
        </div>
      </div>

      <div className='grid grid-cols-1 gap-3 lg:grid-cols-2'>
        <div className='rounded-2xl border border-slate-200 bg-white p-4 shadow-sm'>
          <div className='text-sm font-semibold'>Sales stats</div>
          {stats.isLoading ? <div className='mt-1 text-sm text-slate-600'>Loading…</div> : null}
          {stats.isError ? (
            <div className='mt-2 text-sm text-rose-700'>{getApiErrorMessage(stats.error)}</div>
          ) : null}
          {stats.data?.success && stats.data.data ? (
            <dl className='mt-3 grid grid-cols-2 gap-3 text-sm'>
              <div>
                <dt className='text-xs font-medium text-slate-600'>Total sales</dt>
                <dd className='font-semibold'>{formatPrice(stats.data.data.totalSales)}</dd>
              </div>
              <div>
                <dt className='text-xs font-medium text-slate-600'>Transactions</dt>
                <dd className='font-semibold'>{stats.data.data.totalTransactions ?? '—'}</dd>
              </div>
            </dl>
          ) : null}
        </div>

        <div className='rounded-2xl border border-slate-200 bg-white p-4 shadow-sm'>
          <div className='text-sm font-semibold'>Current results</div>
          <dl className='mt-3 grid grid-cols-2 gap-3 text-sm'>
            <div>
              <dt className='text-xs font-medium text-slate-600'>Items</dt>
              <dd className='font-semibold'>{orders.length}</dd>
            </div>
            <div>
              <dt className='text-xs font-medium text-slate-600'>Total (all pages)</dt>
              <dd className='font-semibold'>{page?.totalItems ?? '—'}</dd>
            </div>
          </dl>
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
