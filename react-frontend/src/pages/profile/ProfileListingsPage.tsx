import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { getApiErrorMessage } from '../../api'
import {
  useCreatePhone,
  useProfile,
  useSellerPhones,
  useTogglePhoneDisabled,
  useUploadImage,
} from '../../hooks'
import { useNotifications } from '../../contexts/NotificationContext'
import { setZodFormErrors } from '../../utils/zodFormErrors'
import { resolveImageUrl } from '../../utils/images'

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

const createSchema = z.object({
  title: z.string().trim().min(1, 'Title is required'),
  brand: phoneBrandSchema,
  image: z.string().trim().min(1, 'Image is required'),
  stock: z.coerce.number().int().min(0, 'Stock must be at least 0'),
  price: z.coerce.number().min(0, 'Price must be at least 0'),
})

type CreateValues = z.infer<typeof createSchema>

function formatPrice(price: number | null | undefined) {
  if (price == null) return '—'
  return `$${price.toFixed(2)}`
}

export function ProfileListingsPage() {
  const notifications = useNotifications()

  const profile = useProfile()
  const user = profile.data?.success ? profile.data.data : undefined
  const sellerId = user?.id ?? ''

  const sellerPhones = useSellerPhones(sellerId)
  const createPhone = useCreatePhone()
  const toggleDisabled = useTogglePhoneDisabled(sellerId)
  const uploadImage = useUploadImage()

  const form = useForm<CreateValues>({
    defaultValues: { title: '', brand: 'APPLE', image: '', stock: 1, price: 100 },
  })

  const phones = sellerPhones.data?.success ? sellerPhones.data.data ?? [] : []
  const imageValue = form.watch('image')
  const imagePreview = resolveImageUrl(imageValue)

  return (
    <div className='space-y-6'>
      <div className='flex flex-wrap items-center justify-between gap-3'>
        <div>
          <h2 className='text-base font-semibold'>My listings</h2>
          <p className='mt-1 text-sm text-slate-600'>Create and manage your phone listings.</p>
        </div>
        <Link to='/search' className='text-sm text-slate-700 hover:underline'>
          Browse market
        </Link>
      </div>

      <div className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
        <h3 className='text-sm font-semibold'>Create a listing</h3>

        <form
          className='mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2'
          onSubmit={form.handleSubmit(async (values) => {
            const parsed = createSchema.safeParse(values)
            if (!parsed.success) {
              setZodFormErrors(parsed.error, form.setError)
              return
            }

            if (!sellerId) {
              notifications.error('Missing user id (try reloading)')
              return
            }

            try {
              const res = await createPhone.mutateAsync({
                ...parsed.data,
                seller: sellerId,
              })
              if (res.success) {
                notifications.success('Listing created')
                form.reset({ title: '', brand: 'APPLE', image: '', stock: 1, price: 100 })
              } else {
                notifications.error(res.message ?? 'Failed to create listing')
              }
            } catch (err) {
              notifications.error(getApiErrorMessage(err))
            }
          })}
        >
          <div className='sm:col-span-2'>
            <label className='text-xs font-medium text-slate-700'>Title</label>
            <input
              {...form.register('title')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
              placeholder='e.g. iPhone 6 16GB'
            />
            {form.formState.errors.title?.message ? (
              <div className='mt-1 text-xs text-rose-700'>{form.formState.errors.title.message}</div>
            ) : null}
          </div>

          <div>
            <label className='text-xs font-medium text-slate-700'>Brand</label>
            <select
              {...form.register('brand')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
            >
              {phoneBrandSchema.options.map((b) => (
                <option key={b} value={b}>
                  {b}
                </option>
              ))}
            </select>
          </div>

          <div className='sm:col-span-2 space-y-2'>
            <div>
              <label
                htmlFor='profile_listing_image_upload'
                className='text-xs font-medium text-slate-700'
              >
                Upload image
              </label>
              <input
                id='profile_listing_image_upload'
                aria-label='Upload image'
                type='file'
                accept='image/*'
                disabled={uploadImage.isPending}
                onChange={async (e) => {
                  const file = e.target.files?.[0]
                  if (!file) return

                  try {
                    const res = await uploadImage.mutateAsync(file)
                    if (res.success && res.data?.fileUrl) {
                      form.setValue('image', res.data.fileUrl, {
                        shouldDirty: true,
                        shouldValidate: true,
                      })
                      notifications.success('Image uploaded')
                    } else {
                      notifications.error(res.message ?? 'Image upload failed')
                    }
                  } catch (err) {
                    notifications.error(getApiErrorMessage(err))
                  } finally {
                    e.target.value = ''
                  }
                }}
                className='mt-1 block w-full text-sm'
              />
              <div className='mt-1 text-xs text-slate-500'>
                Upload uses `POST /api/upload/image` and stores a `/uploads/...` URL.
              </div>
            </div>

            <div className='flex flex-wrap items-center gap-3'>
              <div className='h-16 w-24 overflow-hidden rounded-lg border border-slate-200 bg-slate-100'>
                {imagePreview ? (
                  <img
                    src={imagePreview}
                    alt='Preview'
                    className='h-full w-full object-cover'
                  />
                ) : null}
              </div>

              <div className='min-w-0 flex-1'>
                <label
                  htmlFor='profile_listing_image_url'
                  className='text-xs font-medium text-slate-700'
                >
                  Image URL
                </label>
                <input
                  id='profile_listing_image_url'
                  {...form.register('image')}
                  className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
                  placeholder='https://... or /uploads/images/...'
                />
                {form.formState.errors.image?.message ? (
                  <div className='mt-1 text-xs text-rose-700'>
                    {form.formState.errors.image.message}
                  </div>
                ) : null}
              </div>

              {imageValue ? (
                <button
                  type='button'
                  onClick={() => form.setValue('image', '', { shouldDirty: true, shouldValidate: true })}
                  className='rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-medium hover:bg-slate-50'
                >
                  Clear
                </button>
              ) : null}
            </div>
          </div>

          <div>
            <label className='text-xs font-medium text-slate-700'>Stock</label>
            <input
              type='number'
              min={0}
              {...form.register('stock')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
            />
            {form.formState.errors.stock?.message ? (
              <div className='mt-1 text-xs text-rose-700'>{form.formState.errors.stock.message}</div>
            ) : null}
          </div>

          <div>
            <label className='text-xs font-medium text-slate-700'>Price</label>
            <input
              type='number'
              min={0}
              step={0.01}
              {...form.register('price')}
              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
            />
            {form.formState.errors.price?.message ? (
              <div className='mt-1 text-xs text-rose-700'>{form.formState.errors.price.message}</div>
            ) : null}
          </div>

          <div className='sm:col-span-2'>
            <button
              type='submit'
              disabled={createPhone.isPending || uploadImage.isPending}
              className='w-full rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50'
            >
              Create listing
            </button>
          </div>
        </form>
      </div>

      <div className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
        <h3 className='text-sm font-semibold'>Your listings</h3>

        {sellerPhones.isLoading ? <div className='mt-3 text-sm'>Loading…</div> : null}
        {sellerPhones.isError ? (
          <div className='mt-3 rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
            {getApiErrorMessage(sellerPhones.error)}
          </div>
        ) : null}

        {sellerPhones.data?.success ? (
          phones.length === 0 ? (
            <div className='mt-3 text-sm text-slate-700'>No listings yet.</div>
          ) : (
            <ul className='mt-4 space-y-3'>
              {phones.map((p) => (
                <li key={p.id} className='rounded-xl border border-slate-200 p-4'>
                  <div className='flex items-start gap-4'>
                    <div className='h-20 w-28 overflow-hidden rounded-lg bg-slate-100'>
                      {p.image ? (
                        <img
                          src={resolveImageUrl(p.image)}
                          alt={p.title}
                          className='h-full w-full object-cover'
                        />
                      ) : null}
                    </div>
                    <div className='min-w-0 flex-1'>
                      <div className='flex items-start justify-between gap-3'>
                        <div className='min-w-0'>
                          <Link to={`/phone/${p.id}`} className='truncate font-semibold hover:underline'>
                            {p.title}
                          </Link>
                          <div className='mt-1 text-xs text-slate-600'>
                            {p.brand} · stock {p.stock ?? '—'}
                          </div>
                        </div>
                        <div className='text-sm font-semibold'>{formatPrice(p.price)}</div>
                      </div>

                      <div className='mt-3 flex flex-wrap items-center gap-2'>
                        {p.isDisabled ? (
                          <span className='rounded-md bg-rose-50 px-2 py-1 text-xs font-medium text-rose-700'>
                            Disabled
                          </span>
                        ) : (
                          <span className='rounded-md bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700'>
                            Active
                          </span>
                        )}

                        <button
                          type='button'
                          disabled={toggleDisabled.isPending}
                          onClick={async () => {
                            const next = !Boolean(p.isDisabled)
                            try {
                              const res = await toggleDisabled.mutateAsync({
                                phoneId: p.id,
                                isDisabled: next,
                              })
                              if (res.success) notifications.info('Listing status updated')
                              else notifications.error(res.message ?? 'Failed to update listing')
                            } catch (err) {
                              notifications.error(getApiErrorMessage(err))
                            }
                          }}
                          className='rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-medium hover:bg-slate-50 disabled:opacity-50'
                        >
                          Toggle disabled
                        </button>
                      </div>
                    </div>
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
