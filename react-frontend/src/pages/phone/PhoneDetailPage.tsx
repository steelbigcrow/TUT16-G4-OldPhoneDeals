import { useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { getApiErrorMessage } from '../../api'
import { useAddPhoneReview, useAddToCart, useAddToWishlist, usePhoneDetail, usePhoneReviews } from '../../hooks'
import { USER_TOKEN_KEY, safeGetToken } from '../../auth/tokens'
import { useNotifications } from '../../contexts/NotificationContext'
import { resolveImageUrl } from '../../utils/images'
import { setZodFormErrors } from '../../utils/zodFormErrors'

const reviewSchema = z.object({
  rating: z.coerce.number().int().min(1).max(5),
  comment: z.string().trim().min(1, 'Comment is required').max(1000),
})

type ReviewFormValues = z.infer<typeof reviewSchema>

function formatPrice(price: number | null | undefined) {
  if (price == null) return '—'
  return `$${price.toFixed(2)}`
}

export function PhoneDetailPage() {
  const notifications = useNotifications()
  const location = useLocation()
  const navigate = useNavigate()
  const params = useParams()

  const phoneId = params.id ?? ''
  const detail = usePhoneDetail(phoneId)

  const [reviewsPage, setReviewsPage] = useState(1)
  const reviewsLimit = 10
  const reviewsQuery = usePhoneReviews(phoneId, reviewsPage, reviewsLimit)

  const addToWishlist = useAddToWishlist()
  const addToCart = useAddToCart()
  const addReview = useAddPhoneReview(phoneId)

  const isAuthed = Boolean(safeGetToken(USER_TOKEN_KEY))

  const form = useForm<ReviewFormValues>({
    defaultValues: { rating: 5, comment: '' },
  })

  const phone = detail.data?.success ? detail.data.data : undefined
  const imageSrc = resolveImageUrl(phone?.image)

  const sellerLabel = useMemo(() => {
    if (!phone?.seller) return '—'
    return `${phone.seller.firstName} ${phone.seller.lastName}`
  }, [phone])

  const goToLogin = () => {
    const returnUrl = `${location.pathname}${location.search}`
    navigate(`/login?returnUrl=${encodeURIComponent(returnUrl)}`)
  }

  return (
    <div className='space-y-6'>
      <div className='flex flex-wrap items-center justify-between gap-3'>
        <div className='min-w-0'>
          <h1 className='truncate text-xl font-semibold'>Phone detail</h1>
          <div className='mt-1 text-sm text-slate-600'>
            <Link to='/search' className='hover:underline'>
              Back to search
            </Link>
          </div>
        </div>
      </div>

      {detail.isLoading ? <div>Loading…</div> : null}
      {detail.isError ? (
        <div className='rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
          {getApiErrorMessage(detail.error)}
        </div>
      ) : null}

      {phone ? (
        <div className='grid grid-cols-1 gap-6 lg:grid-cols-5'>
          <div className='lg:col-span-2'>
            <div className='overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm'>
              <div className='aspect-[4/3] w-full bg-slate-100'>
                {imageSrc ? (
                  <img src={imageSrc} alt={phone.title} className='h-full w-full object-cover' />
                ) : null}
              </div>
            </div>
          </div>

          <div className='lg:col-span-3 space-y-4'>
            <div className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
              <div className='flex flex-wrap items-start justify-between gap-4'>
                <div className='min-w-0'>
                  <div className='text-lg font-semibold'>{phone.title}</div>
                  <div className='mt-1 text-sm text-slate-600'>
                    <span className='font-medium'>{phone.brand}</span>
                    <span className='opacity-75'> · seller: {sellerLabel}</span>
                  </div>
                </div>
                <div className='text-lg font-semibold'>{formatPrice(phone.price)}</div>
              </div>

              <div className='mt-3 flex flex-wrap gap-2 text-sm text-slate-700'>
                <div className='rounded-md bg-slate-100 px-3 py-1'>
                  Stock: <span className='font-medium'>{phone.stock ?? '—'}</span>
                </div>
                <div className='rounded-md bg-slate-100 px-3 py-1'>
                  Rating: <span className='font-medium'>{phone.averageRating ?? '—'}</span>
                </div>
              </div>

              <div className='mt-4 flex flex-wrap gap-2'>
                <button
                  type='button'
                  disabled={addToWishlist.isPending}
                  onClick={async () => {
                    if (!isAuthed) return goToLogin()
                    try {
                      const res = await addToWishlist.mutateAsync(phone.id)
                      if (res.success) notifications.success('Added to wishlist')
                      else notifications.error(res.message ?? 'Failed to add')
                    } catch (err) {
                      notifications.error(getApiErrorMessage(err))
                    }
                  }}
                  className='rounded-md border border-slate-200 bg-white px-4 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50'
                >
                  Wishlist
                </button>
                <button
                  type='button'
                  disabled={addToCart.isPending}
                  onClick={async () => {
                    if (!isAuthed) return goToLogin()
                    try {
                      const res = await addToCart.mutateAsync({ phoneId: phone.id, quantity: 1 })
                      if (res.success) notifications.success('Added to cart')
                      else notifications.error(res.message ?? 'Failed to add')
                    } catch (err) {
                      notifications.error(getApiErrorMessage(err))
                    }
                  }}
                  className='rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50'
                >
                  Add to cart
                </button>
              </div>
            </div>

            <div className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
              <h2 className='text-base font-semibold'>Reviews</h2>

              {reviewsQuery.isLoading ? <div className='mt-3 text-sm'>Loading…</div> : null}
              {reviewsQuery.isError ? (
                <div className='mt-3 rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
                  {getApiErrorMessage(reviewsQuery.error)}
                </div>
              ) : null}

              {reviewsQuery.data?.success && reviewsQuery.data.data ? (
                <div className='mt-3 space-y-3'>
                  {reviewsQuery.data.data.reviews.length === 0 ? (
                    <div className='text-sm text-slate-600'>No reviews yet.</div>
                  ) : (
                    <ul className='space-y-3'>
                      {reviewsQuery.data.data.reviews.map((r) => (
                        <li key={r.id} className='rounded-lg border border-slate-200 p-3'>
                          <div className='flex items-center justify-between gap-3'>
                            <div className='text-sm font-semibold'>{r.reviewer}</div>
                            <div className='text-sm font-semibold'>{r.rating ?? '—'}/5</div>
                          </div>
                          <div className='mt-1 text-sm text-slate-700'>{r.comment}</div>
                        </li>
                      ))}
                    </ul>
                  )}

                  <div className='flex items-center justify-between'>
                    <button
                      type='button'
                      disabled={reviewsPage <= 1}
                      onClick={() => setReviewsPage((p) => Math.max(1, p - 1))}
                      className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50'
                    >
                      Previous
                    </button>
                    <div className='text-sm text-slate-600'>
                      Page{' '}
                      <span className='font-medium text-slate-900'>
                        {reviewsQuery.data.data.currentPage}
                      </span>{' '}
                      /{' '}
                      <span className='font-medium text-slate-900'>
                        {reviewsQuery.data.data.totalPages}
                      </span>
                    </div>
                    <button
                      type='button'
                      disabled={reviewsPage >= reviewsQuery.data.data.totalPages}
                      onClick={() => setReviewsPage((p) => p + 1)}
                      className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50'
                    >
                      Next
                    </button>
                  </div>
                </div>
              ) : null}
            </div>

            <div className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
              <h2 className='text-base font-semibold'>Write a review</h2>

              {!isAuthed ? (
                <div className='mt-2 text-sm text-slate-700'>
                  <button type='button' className='text-slate-900 underline' onClick={goToLogin}>
                    Log in
                  </button>{' '}
                  to write a review.
                </div>
              ) : (
                <form
                  className='mt-3 grid grid-cols-1 gap-3'
                  onSubmit={form.handleSubmit(async (values) => {
                    const parsed = reviewSchema.safeParse(values)
                    if (!parsed.success) {
                      setZodFormErrors(parsed.error, form.setError)
                      return
                    }

                    try {
                      const res = await addReview.mutateAsync(parsed.data)
                      if (res.success) {
                        notifications.success('Review submitted')
                        form.reset({ rating: 5, comment: '' })
                      } else {
                        notifications.error(res.message ?? 'Failed to submit review')
                      }
                    } catch (err) {
                      notifications.error(getApiErrorMessage(err))
                    }
                  })}
                >
                  <div>
                    <label className='text-xs font-medium text-slate-700'>Rating</label>
                    <select
                      {...form.register('rating')}
                      className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
                    >
                      {[5, 4, 3, 2, 1].map((v) => (
                        <option key={v} value={v}>
                          {v}
                        </option>
                      ))}
                    </select>
                    {form.formState.errors.rating?.message ? (
                      <div className='mt-1 text-xs text-rose-700'>
                        {form.formState.errors.rating.message}
                      </div>
                    ) : null}
                  </div>

                  <div>
                    <label className='text-xs font-medium text-slate-700'>Comment</label>
                    <textarea
                      {...form.register('comment')}
                      rows={3}
                      className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
                      placeholder='What did you like about it?'
                    />
                    {form.formState.errors.comment?.message ? (
                      <div className='mt-1 text-xs text-rose-700'>
                        {form.formState.errors.comment.message}
                      </div>
                    ) : null}
                  </div>

                  <div className='flex items-center gap-2'>
                    <button
                      type='submit'
                      disabled={addReview.isPending}
                      className='rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50'
                    >
                      Submit
                    </button>
                    {addReview.isError ? (
                      <div className='text-sm text-rose-700'>{getApiErrorMessage(addReview.error)}</div>
                    ) : null}
                  </div>
                </form>
              )}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

