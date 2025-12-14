import { Link } from 'react-router-dom'
import type { PhoneListItemResponse } from '../../types/phone'
import { resolveImageUrl } from '../../utils/images'

export type PhoneCardProps = {
  phone: PhoneListItemResponse
  actions?: React.ReactNode
}

function formatPrice(price: number | null | undefined) {
  if (price == null) return '—'
  return `$${price.toFixed(2)}`
}

export function PhoneCard({ phone, actions }: PhoneCardProps) {
  const imageSrc = resolveImageUrl(phone.image)

  return (
    <div className='overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm'>
      <div className='aspect-[4/3] w-full bg-slate-100'>
        {imageSrc ? (
          <img
            src={imageSrc}
            alt={phone.title}
            className='h-full w-full object-cover'
            loading='lazy'
          />
        ) : null}
      </div>

      <div className='p-4'>
        <div className='flex items-start justify-between gap-3'>
          <div className='min-w-0'>
            <Link
              to={`/phone/${phone.id}`}
              className='block truncate text-sm font-semibold text-slate-900 hover:underline'
              title={phone.title}
            >
              {phone.title}
            </Link>
            <div className='mt-1 text-xs text-slate-600'>
              <span className='font-medium'>{phone.brand}</span>
              {phone.seller ? (
                <span className='opacity-75'>
                  {' '}
                  · {phone.seller.firstName} {phone.seller.lastName}
                </span>
              ) : null}
            </div>
          </div>
          <div className='shrink-0 text-sm font-semibold text-slate-900'>
            {formatPrice(phone.price)}
          </div>
        </div>

        <div className='mt-2 flex items-center justify-between text-xs text-slate-600'>
          <div>
            <span className='font-medium'>{phone.averageRating ?? '—'}</span>
            <span className='opacity-75'> / 5</span>
            <span className='opacity-75'> · </span>
            <span className='opacity-75'>{phone.reviewCount ?? 0} reviews</span>
          </div>
          <div className='opacity-75'>Stock: {phone.stock ?? '—'}</div>
        </div>

        {actions ? <div className='mt-3 flex flex-wrap gap-2'>{actions}</div> : null}
      </div>
    </div>
  )
}

