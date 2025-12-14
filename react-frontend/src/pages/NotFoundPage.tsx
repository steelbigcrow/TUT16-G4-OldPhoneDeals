import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className='rounded-2xl border border-slate-200 bg-white p-6 text-slate-900 shadow-sm'>
      <h1 className='text-xl font-semibold'>404</h1>
      <p className='mt-1 text-sm text-slate-600'>Page not found.</p>
      <div className='mt-4'>
        <Link to='/home' className='text-sm font-medium text-slate-900 underline'>
          Go to home
        </Link>
      </div>
    </div>
  )
}

