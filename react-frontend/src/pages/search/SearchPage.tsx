import { useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { z } from 'zod'
import { getApiErrorMessage } from '../../api'
import { PhoneCard } from '../../components/phone/PhoneCard'
import { useAddToCart, useAddToWishlist, usePhonesList } from '../../hooks'
import type { PhoneBrand } from '../../types/phone'
import { USER_TOKEN_KEY, safeGetToken } from '../../auth/tokens'
import { useNotifications } from '../../contexts/NotificationContext'

const phoneBrandSchema = z.enum([
  'SAMSUNG',
  'APPLE',
  'HTC',
  'HUAWEI',
  'NOKIA',
  'LG',
  'MOTOROLA',
  'SONY',
  'BLACKBERRY',
])

function parseBrand(value: string | null): PhoneBrand | undefined {
  const parsed = phoneBrandSchema.safeParse(value)
  return parsed.success ? parsed.data : undefined
}

function parseNumber(value: string | null): number | undefined {
  if (!value) return undefined
  const n = Number(value)
  return Number.isFinite(n) ? n : undefined
}

export function SearchPage() {
  const notifications = useNotifications()
  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const [searchDraft, setSearchDraft] = useState(searchParams.get('search') ?? '')

  const page = Math.max(1, parseNumber(searchParams.get('page')) ?? 1)
  const limit = Math.min(24, Math.max(1, parseNumber(searchParams.get('limit')) ?? 12))

  const params = useMemo(
    () => ({
      search: searchParams.get('search') ?? undefined,
      brand: parseBrand(searchParams.get('brand')),
      maxPrice: parseNumber(searchParams.get('maxPrice')),
      sortBy: searchParams.get('sortBy') ?? undefined,
      sortOrder: (searchParams.get('sortOrder') as 'asc' | 'desc' | null) ?? undefined,
      page,
      limit,
    }),
    [searchParams, page, limit],
  )

  const query = usePhonesList(params)

  const addToWishlist = useAddToWishlist()
  const addToCart = useAddToCart()

  const canMutate = Boolean(safeGetToken(USER_TOKEN_KEY))

  const goToLogin = () => {
    const returnUrl = `${location.pathname}${location.search}`
    navigate(`/login?returnUrl=${encodeURIComponent(returnUrl)}`)
  }

  const listData = query.data?.success ? query.data.data : undefined
  const phones = listData?.phones ?? []

  const totalPages = listData?.totalPages ?? 1

  return (
    <div className='space-y-6'>
      <div className='flex flex-wrap items-end justify-between gap-3'>
        <div>
          <h1 className='text-xl font-semibold'>Search</h1>
          <p className='mt-1 text-sm text-slate-600'>Browse listings with filters.</p>
        </div>
        <Link to='/home' className='text-sm text-slate-700 hover:underline'>
          Back to home
        </Link>
      </div>

      <form
        className='grid grid-cols-1 gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-6'
        onSubmit={(e) => {
          e.preventDefault()
          const next = new URLSearchParams(searchParams)
          if (searchDraft.trim()) next.set('search', searchDraft.trim())
          else next.delete('search')
          next.set('page', '1')
          setSearchParams(next)
        }}
      >
        <div className='md:col-span-3'>
          <label className='text-xs font-medium text-slate-700'>Search</label>
          <input
            value={searchDraft}
            onChange={(e) => setSearchDraft(e.target.value)}
            placeholder='iPhone, Nokia...'
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          />
        </div>

        <div className='md:col-span-1'>
          <label className='text-xs font-medium text-slate-700'>Brand</label>
          <select
            value={searchParams.get('brand') ?? ''}
            onChange={(e) => {
              const next = new URLSearchParams(searchParams)
              if (e.target.value) next.set('brand', e.target.value)
              else next.delete('brand')
              next.set('page', '1')
              setSearchParams(next)
            }}
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          >
            <option value=''>All</option>
            {phoneBrandSchema.options.map((b) => (
              <option key={b} value={b}>
                {b}
              </option>
            ))}
          </select>
        </div>

        <div className='md:col-span-1'>
          <label className='text-xs font-medium text-slate-700'>Max price</label>
          <input
            inputMode='numeric'
            value={searchParams.get('maxPrice') ?? ''}
            onChange={(e) => {
              const next = new URLSearchParams(searchParams)
              if (e.target.value) next.set('maxPrice', e.target.value)
              else next.delete('maxPrice')
              next.set('page', '1')
              setSearchParams(next)
            }}
            placeholder='500'
            className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
          />
        </div>

        <div className='md:col-span-1 flex items-end'>
          <button
            type='submit'
            className='w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800'
          >
            Apply
          </button>
        </div>
      </form>

      {query.isLoading ? <div>Loading…</div> : null}
      {query.isError ? (
        <div className='rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
          {getApiErrorMessage(query.error)}
        </div>
      ) : null}

      {query.data?.success && listData ? (
        <div className='space-y-4'>
          <div className='flex items-center justify-between text-sm text-slate-600'>
            <div>
              Total: <span className='font-medium text-slate-900'>{listData.total}</span>
            </div>
            <div>
              Page <span className='font-medium text-slate-900'>{listData.currentPage}</span> /{' '}
              <span className='font-medium text-slate-900'>{listData.totalPages}</span>
            </div>
          </div>

          {phones.length === 0 ? (
            <div className='rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-700 shadow-sm'>
              No results.
            </div>
          ) : (
            <div className='grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3'>
              {phones.map((phone) => (
                <PhoneCard
                  key={phone.id}
                  phone={phone}
                  actions={
                    <>
                      <button
                        type='button'
                        disabled={addToWishlist.isPending}
                        onClick={async () => {
                          if (!canMutate) {
                            notifications.info('Please log in to use wishlist')
                            return goToLogin()
                          }
                          try {
                            const res = await addToWishlist.mutateAsync(phone.id)
                            if (res.success) notifications.success('Added to wishlist')
                            else notifications.error(res.message ?? 'Failed to add')
                          } catch (err) {
                            notifications.error(getApiErrorMessage(err))
                          }
                        }}
                        className='rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-medium hover:bg-slate-50 disabled:opacity-50'
                      >
                        Wishlist
                      </button>
                      <button
                        type='button'
                        disabled={addToCart.isPending}
                        onClick={async () => {
                          if (!canMutate) {
                            notifications.info('Please log in to add to cart')
                            return goToLogin()
                          }
                          try {
                            const res = await addToCart.mutateAsync({
                              phoneId: phone.id,
                              quantity: 1,
                            })
                            if (res.success) notifications.success('Added to cart')
                            else notifications.error(res.message ?? 'Failed to add')
                          } catch (err) {
                            notifications.error(getApiErrorMessage(err))
                          }
                        }}
                        className='rounded-md bg-slate-900 px-3 py-2 text-xs font-medium text-white hover:bg-slate-800 disabled:opacity-50'
                      >
                        Add to cart
                      </button>
                    </>
                  }
                />
              ))}
            </div>
          )}

          <div className='flex items-center justify-between'>
            <button
              type='button'
              disabled={page <= 1}
              onClick={() => {
                const next = new URLSearchParams(searchParams)
                next.set('page', String(page - 1))
                setSearchParams(next)
              }}
              className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50'
            >
              Previous
            </button>
            <button
              type='button'
              disabled={page >= totalPages}
              onClick={() => {
                const next = new URLSearchParams(searchParams)
                next.set('page', String(page + 1))
                setSearchParams(next)
              }}
              className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:opacity-50'
            >
              Next
            </button>
          </div>
        </div>
      ) : null}
    </div>
  )
}
