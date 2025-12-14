import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../../api'
import { PhoneCard } from '../../components/phone/PhoneCard'
import { useAddToCart, useRemoveFromWishlist, useWishlist } from '../../hooks'
import { useNotifications } from '../../contexts/NotificationContext'

export function WishlistPage() {
  const notifications = useNotifications()
  const wishlist = useWishlist()
  const remove = useRemoveFromWishlist()
  const addToCart = useAddToCart()

  const phones = wishlist.data?.success ? wishlist.data.data?.phones ?? [] : []

  return (
    <div className='space-y-6'>
      <div className='flex flex-wrap items-center justify-between gap-3'>
        <div>
          <h1 className='text-xl font-semibold'>Wishlist</h1>
          <p className='mt-1 text-sm text-slate-600'>Phones you saved for later.</p>
        </div>
        <Link to='/search' className='text-sm text-slate-700 hover:underline'>
          Browse phones
        </Link>
      </div>

      {wishlist.isLoading ? <div>Loading…</div> : null}
      {wishlist.isError ? (
        <div className='rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
          {getApiErrorMessage(wishlist.error)}
        </div>
      ) : null}

      {wishlist.data?.success ? (
        phones.length === 0 ? (
          <div className='rounded-2xl border border-slate-200 bg-white p-6 text-sm text-slate-700 shadow-sm'>
            Wishlist is empty.
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
                      disabled={remove.isPending}
                      onClick={async () => {
                        try {
                          const res = await remove.mutateAsync(phone.id)
                          if (res.success) notifications.info('Removed from wishlist')
                          else notifications.error(res.message ?? 'Failed to remove')
                        } catch (err) {
                          notifications.error(getApiErrorMessage(err))
                        }
                      }}
                      className='rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-medium hover:bg-slate-50 disabled:opacity-50'
                    >
                      Remove
                    </button>
                    <button
                      type='button'
                      disabled={addToCart.isPending}
                      onClick={async () => {
                        try {
                          const res = await addToCart.mutateAsync({ phoneId: phone.id, quantity: 1 })
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
        )
      ) : null}
    </div>
  )
}

