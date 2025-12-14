import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { getApiErrorMessage } from '../../api'
import { useCart, useCheckout, useRemoveFromCart, useUpdateCartItem } from '../../hooks'
import { resolveImageUrl } from '../../utils/images'
import { setZodFormErrors } from '../../utils/zodFormErrors'
import { useNotifications } from '../../contexts/NotificationContext'

const addressSchema = z.object({
  street: z.string().trim().min(1, 'Street is required'),
  city: z.string().trim().min(1, 'City is required'),
  state: z.string().trim().min(1, 'State is required'),
  zip: z.string().trim().min(1, 'Zip is required'),
  country: z.string().trim().min(1, 'Country is required'),
})

type AddressValues = z.infer<typeof addressSchema>

function formatPrice(price: number | null | undefined) {
  if (price == null) return '—'
  return `$${price.toFixed(2)}`
}

export function CheckoutPage() {
  const notifications = useNotifications()

  const cart = useCart()
  const removeItem = useRemoveFromCart()
  const updateItem = useUpdateCartItem()
  const checkout = useCheckout()

  const items = cart.data?.success ? cart.data.data?.items ?? [] : []

  const total = useMemo(() => {
    return items.reduce((sum, item) => sum + (item.price ?? 0) * (item.quantity ?? 0), 0)
  }, [items])

  const form = useForm<AddressValues>({
    defaultValues: { street: '', city: '', state: '', zip: '', country: '' },
  })

  const [orderId, setOrderId] = useState<string | null>(null)

  return (
    <div className='space-y-6'>
      <div className='flex flex-wrap items-center justify-between gap-3'>
        <div>
          <h1 className='text-xl font-semibold'>Checkout</h1>
          <p className='mt-1 text-sm text-slate-600'>Review your cart and place the order.</p>
        </div>
        <Link to='/search' className='text-sm text-slate-700 hover:underline'>
          Continue shopping
        </Link>
      </div>

      {cart.isLoading ? <div>Loading…</div> : null}
      {cart.isError ? (
        <div className='rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
          {getApiErrorMessage(cart.error)}
        </div>
      ) : null}

      {cart.data?.success && cart.data.data ? (
        <div className='grid grid-cols-1 gap-6 lg:grid-cols-5'>
          <div className='lg:col-span-3 space-y-3'>
            <div className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
              <h2 className='text-base font-semibold'>Cart</h2>

              {items.length === 0 ? (
                <div className='mt-3 text-sm text-slate-700'>Your cart is empty.</div>
              ) : (
                <ul className='mt-4 space-y-3'>
                  {items.map((item) => (
                    <li key={item.phoneId} className='flex items-start gap-4 rounded-xl border border-slate-200 p-4'>
                      <div className='h-20 w-28 overflow-hidden rounded-lg bg-slate-100'>
                        {item.phone?.image ? (
                          <img
                            src={resolveImageUrl(item.phone.image)}
                            alt={item.title}
                            className='h-full w-full object-cover'
                          />
                        ) : null}
                      </div>

                      <div className='min-w-0 flex-1'>
                        <div className='flex items-start justify-between gap-3'>
                          <div className='min-w-0'>
                            <div className='truncate text-sm font-semibold'>{item.title}</div>
                            <div className='mt-1 text-xs text-slate-600'>
                              {item.seller ? `${item.seller.firstName} ${item.seller.lastName}` : '—'}
                            </div>
                          </div>
                          <div className='text-sm font-semibold'>{formatPrice(item.price)}</div>
                        </div>

                        <div className='mt-3 flex flex-wrap items-center gap-2'>
                          <label className='text-xs text-slate-600'>Qty</label>
                          <input
                            type='number'
                            min={1}
                            value={item.quantity ?? 1}
                            onChange={async (e) => {
                              const nextQty = Math.max(1, Number(e.target.value || 1))
                              try {
                                await updateItem.mutateAsync({ phoneId: item.phoneId, quantity: nextQty })
                              } catch (err) {
                                notifications.error(getApiErrorMessage(err))
                              }
                            }}
                            className='w-20 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
                          />
                          <button
                            type='button'
                            disabled={removeItem.isPending}
                            onClick={async () => {
                              try {
                                await removeItem.mutateAsync(item.phoneId)
                                notifications.info('Removed from cart')
                              } catch (err) {
                                notifications.error(getApiErrorMessage(err))
                              }
                            }}
                            className='rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-medium hover:bg-slate-50 disabled:opacity-50'
                          >
                            Remove
                          </button>
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {orderId ? (
              <div className='rounded-2xl border border-emerald-200 bg-emerald-50 p-6 text-emerald-950 shadow-sm'>
                <div className='text-sm font-semibold'>Order placed</div>
                <div className='mt-1 text-sm'>
                  Order ID: <span className='font-mono'>{orderId}</span>
                </div>
                <div className='mt-3'>
                  <Link to='/profile' className='text-sm font-medium underline'>
                    View orders in profile
                  </Link>
                </div>
              </div>
            ) : null}
          </div>

          <div className='lg:col-span-2 space-y-4'>
            <div className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
              <h2 className='text-base font-semibold'>Summary</h2>
              <div className='mt-3 flex items-center justify-between text-sm'>
                <span className='text-slate-600'>Total</span>
                <span className='font-semibold'>{formatPrice(total)}</span>
              </div>
            </div>

            <div className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
              <h2 className='text-base font-semibold'>Shipping address</h2>

              <form
                className='mt-4 space-y-3'
                onSubmit={form.handleSubmit(async (values) => {
                  const parsed = addressSchema.safeParse(values)
                  if (!parsed.success) {
                    setZodFormErrors(parsed.error, form.setError)
                    return
                  }

                  try {
                    const res = await checkout.mutateAsync({ address: parsed.data })
                    if (res.success && res.data?.id) {
                      notifications.success('Checkout successful')
                      setOrderId(res.data.id)
                    } else {
                      notifications.error(res.message ?? 'Checkout failed')
                    }
                  } catch (err) {
                    notifications.error(getApiErrorMessage(err))
                  }
                })}
              >
                <div>
                  <label htmlFor='checkout_street' className='text-xs font-medium text-slate-700'>
                    Street
                  </label>
                  <input
                    id='checkout_street'
                    {...form.register('street')}
                    className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
                  />
                  {form.formState.errors.street?.message ? (
                    <div className='mt-1 text-xs text-rose-700'>{form.formState.errors.street.message}</div>
                  ) : null}
                </div>

                <div className='grid grid-cols-1 gap-3 sm:grid-cols-2'>
                  <div>
                    <label htmlFor='checkout_city' className='text-xs font-medium text-slate-700'>
                      City
                    </label>
                    <input
                      id='checkout_city'
                      {...form.register('city')}
                      className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
                    />
                    {form.formState.errors.city?.message ? (
                      <div className='mt-1 text-xs text-rose-700'>{form.formState.errors.city.message}</div>
                    ) : null}
                  </div>
                  <div>
                    <label htmlFor='checkout_state' className='text-xs font-medium text-slate-700'>
                      State
                    </label>
                    <input
                      id='checkout_state'
                      {...form.register('state')}
                      className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
                    />
                    {form.formState.errors.state?.message ? (
                      <div className='mt-1 text-xs text-rose-700'>{form.formState.errors.state.message}</div>
                    ) : null}
                  </div>
                </div>

                <div className='grid grid-cols-1 gap-3 sm:grid-cols-2'>
                  <div>
                    <label htmlFor='checkout_zip' className='text-xs font-medium text-slate-700'>
                      Zip
                    </label>
                    <input
                      id='checkout_zip'
                      {...form.register('zip')}
                      className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
                    />
                    {form.formState.errors.zip?.message ? (
                      <div className='mt-1 text-xs text-rose-700'>{form.formState.errors.zip.message}</div>
                    ) : null}
                  </div>
                  <div>
                    <label htmlFor='checkout_country' className='text-xs font-medium text-slate-700'>
                      Country
                    </label>
                    <input
                      id='checkout_country'
                      {...form.register('country')}
                      className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
                    />
                    {form.formState.errors.country?.message ? (
                      <div className='mt-1 text-xs text-rose-700'>
                        {form.formState.errors.country.message}
                      </div>
                    ) : null}
                  </div>
                </div>

                <button
                  type='submit'
                  disabled={checkout.isPending || items.length === 0}
                  className='w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50'
                >
                  Place order
                </button>
              </form>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}
