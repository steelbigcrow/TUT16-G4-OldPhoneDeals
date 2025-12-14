import { Fragment, useState } from 'react'
import { getApiErrorMessage } from '../../api'
import { useAdminPhones, useToggleAdminPhoneDisabled, useUpdateAdminPhone } from '../../hooks'
import { useNotifications } from '../../contexts/NotificationContext'
import { resolveImageUrl } from '../../utils/images'
import type { PhoneBrand } from '../../types/phone'

const BRANDS: PhoneBrand[] = [
  'SAMSUNG',
  'APPLE',
  'HTC',
  'HUAWEI',
  'NOKIA',
  'LG',
  'MOTOROLA',
  'SONY',
  'BLACKBERRY',
]

type EditValues = {
  title: string
  brand: PhoneBrand
  price: number
  stock: number
  isDisabled: boolean
}

function formatPrice(price: number | null | undefined) {
  if (price == null) return '—'
  return `$${price.toFixed(2)}`
}

export function AdminPhonesPage() {
  const notifications = useNotifications()

  const [pageIndex, setPageIndex] = useState(0)
  const [pageSize] = useState(10)

  const query = useAdminPhones({ pageIndex, pageSize })
  const toggle = useToggleAdminPhoneDisabled()
  const update = useUpdateAdminPhone()

  const [editingPhoneId, setEditingPhoneId] = useState<string | null>(null)
  const [editValues, setEditValues] = useState<EditValues | null>(null)

  const page = query.data?.success ? query.data.data : undefined
  const phones = page?.content ?? []

  return (
    <div className='space-y-4'>
      <div>
        <h2 className='text-base font-semibold'>Phones</h2>
        <p className='mt-1 text-sm text-slate-600'>
          Enable/disable listings and edit fields (no image updates).
        </p>
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
                <th className='px-4 py-3'>Brand</th>
                <th className='px-4 py-3'>Price</th>
                <th className='px-4 py-3'>Stock</th>
                <th className='px-4 py-3'>Status</th>
                <th className='px-4 py-3'></th>
              </tr>
            </thead>
            <tbody className='divide-y divide-slate-200'>
              {phones.map((p) => (
                <Fragment key={p.id}>
                  <tr>
                    <td className='px-4 py-3'>
                      <div className='flex items-center gap-3'>
                        <div className='h-12 w-16 overflow-hidden rounded-md bg-slate-100'>
                          {p.image ? (
                            <img
                              src={resolveImageUrl(p.image)}
                              alt={p.title}
                              className='h-full w-full object-cover'
                            />
                          ) : null}
                        </div>
                        <div className='min-w-0'>
                          <div className='truncate font-medium'>{p.title}</div>
                          <div className='text-xs text-slate-600'>
                            Seller:{' '}
                            {p.seller ? `${p.seller.firstName} ${p.seller.lastName}` : '—'}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className='px-4 py-3'>{p.brand}</td>
                    <td className='px-4 py-3'>{formatPrice(p.price)}</td>
                    <td className='px-4 py-3'>{p.stock ?? '—'}</td>
                    <td className='px-4 py-3'>
                      {p.isDisabled ? (
                        <span className='rounded-md bg-rose-50 px-2 py-1 text-xs font-medium text-rose-700'>
                          Disabled
                        </span>
                      ) : (
                        <span className='rounded-md bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700'>
                          Active
                        </span>
                      )}
                    </td>
                    <td className='px-4 py-3 text-right'>
                      <div className='flex justify-end gap-2'>
                        <button
                          type='button'
                          disabled={toggle.isPending}
                          onClick={async () => {
                            try {
                              const res = await toggle.mutateAsync(p.id)
                              if (res.success) notifications.success('Phone status updated')
                              else notifications.error(res.message ?? 'Failed to update phone')
                            } catch (err) {
                              notifications.error(getApiErrorMessage(err))
                            }
                          }}
                          className='rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-medium hover:bg-slate-50 disabled:opacity-50'
                        >
                          Toggle disabled
                        </button>
                        <button
                          type='button'
                          onClick={() => {
                            if (editingPhoneId === p.id) {
                              setEditingPhoneId(null)
                              setEditValues(null)
                              return
                            }

                            setEditingPhoneId(p.id)
                            setEditValues({
                              title: p.title,
                              brand: p.brand,
                              price: p.price ?? 0,
                              stock: p.stock ?? 0,
                              isDisabled: Boolean(p.isDisabled),
                            })
                          }}
                          className='rounded-md border border-slate-200 bg-white px-3 py-2 text-xs font-medium hover:bg-slate-50'
                        >
                          {editingPhoneId === p.id ? 'Close' : 'Edit'}
                        </button>
                      </div>
                    </td>
                  </tr>

                  {editingPhoneId === p.id && editValues ? (
                    <tr>
                      <td colSpan={6} className='bg-slate-50 px-4 py-4'>
                        <form
                          className='grid grid-cols-1 gap-3 sm:grid-cols-6'
                          onSubmit={async (e) => {
                            e.preventDefault()
                            try {
                              const res = await update.mutateAsync({
                                phoneId: p.id,
                                request: editValues,
                              })
                              if (res.success) {
                                notifications.success('Phone updated')
                                setEditingPhoneId(null)
                                setEditValues(null)
                              } else {
                                notifications.error(res.message ?? 'Failed to update phone')
                              }
                            } catch (err) {
                              notifications.error(getApiErrorMessage(err))
                            }
                          }}
                        >
                          <div className='sm:col-span-3'>
                            <label className='text-xs font-medium text-slate-700'>Title</label>
                            <input
                              value={editValues.title}
                              onChange={(e) =>
                                setEditValues((prev) =>
                                  prev ? { ...prev, title: e.target.value } : prev,
                                )
                              }
                              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
                            />
                          </div>

                          <div className='sm:col-span-1'>
                            <label className='text-xs font-medium text-slate-700'>Brand</label>
                            <select
                              value={editValues.brand}
                              onChange={(e) =>
                                setEditValues((prev) =>
                                  prev ? { ...prev, brand: e.target.value as PhoneBrand } : prev,
                                )
                              }
                              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
                            >
                              {BRANDS.map((b) => (
                                <option key={b} value={b}>
                                  {b}
                                </option>
                              ))}
                            </select>
                          </div>

                          <div className='sm:col-span-1'>
                            <label className='text-xs font-medium text-slate-700'>Price</label>
                            <input
                              type='number'
                              min={0}
                              step={0.01}
                              value={editValues.price}
                              onChange={(e) =>
                                setEditValues((prev) =>
                                  prev ? { ...prev, price: Number(e.target.value || 0) } : prev,
                                )
                              }
                              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
                            />
                          </div>

                          <div className='sm:col-span-1'>
                            <label className='text-xs font-medium text-slate-700'>Stock</label>
                            <input
                              type='number'
                              min={0}
                              value={editValues.stock}
                              onChange={(e) =>
                                setEditValues((prev) =>
                                  prev ? { ...prev, stock: Number(e.target.value || 0) } : prev,
                                )
                              }
                              className='mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-slate-500 focus:outline-none focus:ring-2 focus:ring-slate-200'
                            />
                          </div>

                          <div className='sm:col-span-5 flex items-center gap-2'>
                            <input
                              id={`disabled_${p.id}`}
                              type='checkbox'
                              checked={editValues.isDisabled}
                              onChange={(e) =>
                                setEditValues((prev) =>
                                  prev ? { ...prev, isDisabled: e.target.checked } : prev,
                                )
                              }
                              className='h-4 w-4 rounded border-slate-300'
                            />
                            <label htmlFor={`disabled_${p.id}`} className='text-sm text-slate-700'>
                              Disabled
                            </label>
                          </div>

                          <div className='sm:col-span-1 flex items-end justify-end gap-2'>
                            <button
                              type='button'
                              onClick={() => {
                                setEditingPhoneId(null)
                                setEditValues(null)
                              }}
                              className='rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50'
                            >
                              Cancel
                            </button>
                            <button
                              type='submit'
                              disabled={update.isPending}
                              className='rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50'
                            >
                              Save
                            </button>
                          </div>
                        </form>
                      </td>
                    </tr>
                  ) : null}
                </Fragment>
              ))}

              {phones.length === 0 ? (
                <tr>
                  <td className='px-4 py-8 text-center text-sm text-slate-600' colSpan={6}>
                    No phones found.
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

