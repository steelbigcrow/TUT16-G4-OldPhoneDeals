import { Link } from 'react-router-dom'
import { getApiErrorMessage } from '../../api'
import { useSpecialPhones } from '../../hooks'
import { PhoneCard } from '../../components/phone/PhoneCard'

export function HomePage() {
  const bestSellers = useSpecialPhones('bestSellers')
  const soldOutSoon = useSpecialPhones('soldOutSoon')

  return (
    <div className='space-y-10'>
      <section className='rounded-2xl border border-slate-200 bg-white p-6 shadow-sm'>
        <h1 className='text-xl font-semibold'>OldPhoneDeals</h1>
        <p className='mt-1 text-sm text-slate-600'>Find great deals on classic phones.</p>
        <div className='mt-4 flex flex-wrap gap-2'>
          <Link
            to='/search'
            className='rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800'
          >
            Browse phones
          </Link>
        </div>
      </section>

      <section className='space-y-3'>
        <div className='flex items-center justify-between'>
          <h2 className='text-lg font-semibold'>Best sellers</h2>
          <Link to='/search' className='text-sm text-slate-700 hover:underline'>
            View all
          </Link>
        </div>

        {bestSellers.isLoading ? <div>Loading…</div> : null}
        {bestSellers.isError ? (
          <div className='rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
            {getApiErrorMessage(bestSellers.error)}
          </div>
        ) : null}

        {bestSellers.data?.success && bestSellers.data.data ? (
          <div className='grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3'>
            {bestSellers.data.data.map((phone) => (
              <PhoneCard key={phone.id} phone={phone} />
            ))}
          </div>
        ) : null}
      </section>

      <section className='space-y-3'>
        <div className='flex items-center justify-between'>
          <h2 className='text-lg font-semibold'>Sold out soon</h2>
          <Link to='/search' className='text-sm text-slate-700 hover:underline'>
            View all
          </Link>
        </div>

        {soldOutSoon.isLoading ? <div>Loading…</div> : null}
        {soldOutSoon.isError ? (
          <div className='rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950'>
            {getApiErrorMessage(soldOutSoon.error)}
          </div>
        ) : null}

        {soldOutSoon.data?.success && soldOutSoon.data.data ? (
          <div className='grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3'>
            {soldOutSoon.data.data.map((phone) => (
              <PhoneCard key={phone.id} phone={phone} />
            ))}
          </div>
        ) : null}
      </section>
    </div>
  )
}

