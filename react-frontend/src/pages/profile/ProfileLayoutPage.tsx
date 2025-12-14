import { NavLink, Outlet } from 'react-router-dom'
import { cn } from '../../utils/cn'
import { ErrorBoundary } from '../../components/common/ErrorBoundary'

const navLinkBase =
  'text-sm px-3 py-2 rounded-md hover:bg-slate-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-slate-200'

export function ProfileLayoutPage() {
  return (
    <div className='space-y-4'>
      <div className='flex flex-wrap items-center justify-between gap-3'>
        <div>
          <h1 className='text-xl font-semibold'>Profile</h1>
          <p className='mt-1 text-sm text-slate-600'>Manage your account and view orders.</p>
        </div>

        <nav className='flex flex-wrap items-center gap-1 rounded-xl border border-slate-200 bg-white p-1 shadow-sm'>
          <NavLink
            to='/profile'
            end
            className={({ isActive }) => cn(navLinkBase, isActive && 'bg-slate-100 font-medium')}
          >
            Overview
          </NavLink>
          <NavLink
            to='/profile/listings'
            className={({ isActive }) => cn(navLinkBase, isActive && 'bg-slate-100 font-medium')}
          >
            Listings
          </NavLink>
          <NavLink
            to='/profile/settings'
            className={({ isActive }) => cn(navLinkBase, isActive && 'bg-slate-100 font-medium')}
          >
            Settings
          </NavLink>
        </nav>
      </div>

      <ErrorBoundary>
        <Outlet />
      </ErrorBoundary>
    </div>
  )
}
