import { useMemo, useState } from 'react'
import { z } from 'zod'
import { getApiErrorMessage } from '../../api'
import { useAdminReviews, useDeleteAdminReview, useToggleAdminReviewVisibility } from '../../hooks'
import { useNotifications } from '../../contexts/NotificationContext'

const filterSchema = z.object({
  search: z.string().optional(),
  visibility: z.enum(['all', 'visible', 'hidden']).default('all'),
})

function formatDate(value: string | null | undefined) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString()
}

export function AdminReviewsPage() {
  const notifications = useNotifications()

  const [pageIndex, setPageIndex] = useState(0)
  const [pageSize] = useState(10)

  const [searchDraft, setSearchDraft] = useState('')
  const [visibility, setVisibility] = useState<'all' | 'visible' | 'hidden'>('all')

  const parsedFilters = useMemo(() => {
    const parsed = filterSchema.safeParse({ search: searchDraft, visibility })
    if (!parsed.success) return { search: undefined, visibility: undefined as boolean | undefined }
    const search = parsed.data.search?.trim() ? parsed.data.search.trim() : undefined
    const visibilityParam =
      parsed.data.visibility === 'all'
        ? undefined
        : parsed.data.visibility === 'visible'
          ? true
          : false
    return { search, visibility: visibilityParam }
  }, [searchDraft, visibility])

  const query = useAdminReviews({
    pageIndex,
    pageSize,
    search: parsedFilters.search,
    visibility: parsedFilters.visibility,
  })

  const toggleVisibility = useToggleAdminReviewVisibility()
  const deleteReview = useDeleteAdminReview()

  const page = query.data?.success ? query.data.data : undefined
  const reviews = page?.content ?? []

  return (
    <div className='space-y-4'>
      <div>
        <h2 className='text-base font-semibold'>Reviews</h2>
        <p className='mt-1 text-sm text-slate-600'>Moderate reviews: hide/show or delete.</p>
      </div>

      <div className='grid grid-cols-1 gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:grid-cols-6'>
        <div className='sm:col-span-4'>
          <label className='text-xs font-medium text-slate-700'>Search (comment)</label>
          <input
            value={searchDraft}
            onChange={(e) => setSearchDraft(e.target.value)}
            placeholder='keyword'
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          />
        </div>
        <div className='sm:col-span-2'>
          <label className='text-xs font-medium text-slate-700'>Visibility</label>
          <select
            value={visibility}
            onChange={(e) => setVisibility(e.target.value as any)}
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          >
            <option value='all'>All</option>
            <option value='visible'>Visible</option>
            <option value='hidden'>Hidden</option>
          </select>
        </div>
        <div className='sm:col-span-6 flex items-center justify-between'>
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
                <th className='px-4 py-3'>Phone</th>
                <th className='px-4 py-3'>Reviewer</th>
                <th className='px-4 py-3'>Rating</th>
                <th className='px-4 py-3'>Comment</th>
                <th className='px-4 py-3'>Status</th>
                <th className='px-4 py-3'></th>
              </tr>
            </thead>
            <tbody className='divide-y divide-slate-200'>
              {reviews.map((r) => (
                <tr key={r.reviewId}>
                  <td className='px-4 py-3'>
                    <div className='font-medium'>{r.phoneTitle}</div>
                    <div className='text-xs text-slate-600'>{formatDate(r.createdAt)}</div>
                  </td>
                  <td className='px-4 py-3'>
                    <div className='font-medium'>{r.reviewerName}</div>
                    <div className='text-xs text-slate-600'>{r.reviewerId}</div>
                  </td>
                  <td className='px-4 py-3'>{r.rating ?? '—'}</td>
                  <td className='px-4 py-3'>
                    <div className='max-w-[32rem] truncate' title={r.comment}>
                      {r.comment}
                    </div>
                  </td>
                  <td className='px-4 py-3'>
                    {r.isHidden ? (
                      <span className='rounded-md bg-rose-50 px-2 py-1 text-xs font-medium text-rose-700'>
                        Hidden
                      </span>
                    ) : (
                      <span className='rounded-md bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700'>
                        Visible
                      </span>
                    )}
                  </td>
                  <td className='px-4 py-3 text-right'>
                    <div className='flex justify-end gap-2'>
                      <button
                        type='button'
                        disabled={toggleVisibility.isPending}
                        onClick={async () => {
                          try {
                            const res = await toggleVisibility.mutateAsync({
                              phoneId: r.phoneId,
                              reviewId: r.reviewId,
                            })
                            if (res.success) notifications.success('Review updated')
                            else notifications.error(res.message ?? 'Failed to update')
                          } catch (err) {
                            notifications.error(getApiErrorMessage(err))
                          }
                        }}
                        className='rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-medium hover:bg-slate-50 disabled:opacity-50'
                      >
                        Toggle visibility
                      </button>
                      <button
                        type='button'
                        disabled={deleteReview.isPending}
                        onClick={async () => {
                          if (!confirm('Delete this review?')) return
                          try {
                            const res = await deleteReview.mutateAsync({
                              phoneId: r.phoneId,
                              reviewId: r.reviewId,
                            })
                            if (res.success) notifications.info('Review deleted')
                            else notifications.error(res.message ?? 'Failed to delete')
                          } catch (err) {
                            notifications.error(getApiErrorMessage(err))
                          }
                        }}
                        className='rounded-md border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-medium text-rose-700 hover:bg-rose-100 disabled:opacity-50'
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {reviews.length === 0 ? (
                <tr>
                  <td className='px-4 py-8 text-center text-sm text-slate-600' colSpan={6}>
                    No reviews found.
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

