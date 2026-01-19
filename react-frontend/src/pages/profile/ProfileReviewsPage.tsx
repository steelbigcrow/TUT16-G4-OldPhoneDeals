import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../../api'
import { useNotifications } from '../../contexts/NotificationContext'
import { useSellerReviews, useToggleSellerReviewVisibility } from '../../hooks'
import { USER_TOKEN_KEY, safeGetToken } from '../../auth/tokens'
import type { SellerReviewResponse } from '../../types/review'

function formatDate(value: string | null | undefined) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString()
}

function formatRating(value: number | null | undefined) {
  if (value == null) return '—'
  return `${value}/5`
}

export function ProfileReviewsPage() {
  const notifications = useNotifications()

  const isAuthed = Boolean(safeGetToken(USER_TOKEN_KEY))
  const query = useSellerReviews({ enabled: isAuthed })
  const toggle = useToggleSellerReviewVisibility()

  const [page, setPage] = useState(1)
  const pageSize = 10

  const allReviews = query.data?.success ? query.data.data ?? [] : []

  const totalPages = Math.max(1, Math.ceil(allReviews.length / pageSize))
  const safePage = Math.min(Math.max(1, page), totalPages)

  const pageItems = useMemo(() => {
    const start = (safePage - 1) * pageSize
    return allReviews.slice(start, start + pageSize)
  }, [allReviews, safePage])

  const emptyState =
    query.data?.success && allReviews.length === 0 ? (
      <div className='rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-700 shadow-sm'>
        No reviews yet.
      </div>
    ) : null

  const onToggle = async (review: SellerReviewResponse) => {
    const nextHidden = !Boolean(review.isHidden)
    try {
      const res = await toggle.mutateAsync({
        phoneId: review.phoneId,
        reviewId: review.reviewId,
        isHidden: nextHidden,
      })
      if (res.success) notifications.success(`Review is now ${nextHidden ? 'hidden' : 'visible'}`)
      else notifications.error(res.message ?? 'Failed to update review')
    } catch (err) {
      notifications.error(getApiErrorMessage(err))
    }
  }

  return (
    <div className='space-y-4'>
      <div>
        <h2 className='text-base font-semibold'>Seller reviews</h2>
        <p className='mt-1 text-sm text-slate-600'>
          Reviews left on your listings. You can hide or show them.
        </p>
      </div>

      {query.isLoading ? <div className='text-sm'>Loading…</div> : null}
      {query.isError ? (
        <div className='rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
          {getApiErrorMessage(query.error)}
        </div>
      ) : null}

      {emptyState}

      {query.data?.success && allReviews.length > 0 ? (
        <div className='overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm'>
          <table className='w-full text-left text-sm'>
            <thead className='bg-slate-50 text-xs font-semibold text-slate-700'>
              <tr>
                <th className='px-4 py-3'>Phone</th>
                <th className='px-4 py-3'>Reviewer</th>
                <th className='px-4 py-3'>Rating</th>
                <th className='px-4 py-3'>Comment</th>
                <th className='px-4 py-3'>Status</th>
                <th className='px-4 py-3'>Created</th>
                <th className='px-4 py-3'></th>
              </tr>
            </thead>
            <tbody className='divide-y divide-slate-200'>
              {pageItems.map((r) => (
                <tr key={r.reviewId}>
                  <td className='px-4 py-3'>
                    <Link to={`/phone/${r.phoneId}`} className='font-medium hover:underline'>
                      {r.phoneTitle}
                    </Link>
                    <div className='text-xs text-slate-600'>{r.phoneId}</div>
                  </td>
                  <td className='px-4 py-3'>
                    <div className='font-medium'>{r.reviewerName}</div>
                    <div className='text-xs text-slate-600'>{r.reviewerId}</div>
                  </td>
                  <td className='px-4 py-3'>{formatRating(r.rating)}</td>
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
                  <td className='px-4 py-3'>{formatDate(r.createdAt)}</td>
                  <td className='px-4 py-3 text-right'>
                    <button
                      type='button'
                      disabled={toggle.isPending}
                      onClick={() => onToggle(r)}
                      className='rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-medium hover:bg-slate-50 disabled:opacity-50'
                    >
                      Toggle visibility
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {query.data?.success && allReviews.length > 0 ? (
        <div className='flex items-center justify-between'>
          <button
            type='button'
            disabled={safePage <= 1}
            onClick={() => setPage((p) => Math.max(1, p - 1))}
            className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50'
          >
            Previous
          </button>
          <div className='text-sm text-slate-600'>
            Page <span className='font-medium text-slate-900'>{safePage}</span> /{' '}
            <span className='font-medium text-slate-900'>{totalPages}</span>
          </div>
          <button
            type='button'
            disabled={safePage >= totalPages}
            onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
            className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50'
          >
            Next
          </button>
        </div>
      ) : null}
    </div>
  )
}

