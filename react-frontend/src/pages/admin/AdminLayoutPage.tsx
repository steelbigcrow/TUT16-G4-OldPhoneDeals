import { NavLink, Outlet } from 'react-router-dom'
import { cn } from '../../utils/cn'
import { ErrorBoundary } from '../../components/common/ErrorBoundary'

const navLinkBase =
  'text-sm px-3 py-2 rounded-md hover:bg-slate-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-slate-200'

export function AdminLayoutPage() {
  return (
    <div className='space-y-4'>
      <div className='flex flex-wrap items-center justify-between gap-3'>
        <div>
          <h1 className='text-xl font-semibold'>Admin</h1>
          <p className='mt-1 text-sm text-slate-600'>Manage users and listings.</p>
        </div>

        <nav className='flex flex-wrap items-center gap-1 rounded-xl border border-slate-200 bg-white p-1 shadow-sm'>
          <NavLink
            to='/admin'
            end
            className={({ isActive }) => cn(navLinkBase, isActive && 'bg-slate-100 font-medium')}
          >
            Home
          </NavLink>
          <NavLink
            to='/admin/dashboard'
            className={({ isActive }) => cn(navLinkBase, isActive && 'bg-slate-100 font-medium')}
          >
            Dashboard
          </NavLink>
          <NavLink
            to='/admin/users'
            className={({ isActive }) => cn(navLinkBase, isActive && 'bg-slate-100 font-medium')}
          >
            Users
          </NavLink>
          <NavLink
            to='/admin/phones'
            className={({ isActive }) => cn(navLinkBase, isActive && 'bg-slate-100 font-medium')}
          >
            Phones
          </NavLink>
          <NavLink
            to='/admin/reviews'
            className={({ isActive }) => cn(navLinkBase, isActive && 'bg-slate-100 font-medium')}
          >
            Reviews
          </NavLink>
          <NavLink
            to='/admin/orders'
            className={({ isActive }) => cn(navLinkBase, isActive && 'bg-slate-100 font-medium')}
          >
            Orders
          </NavLink>
          <NavLink
            to='/admin/logs'
            className={({ isActive }) => cn(navLinkBase, isActive && 'bg-slate-100 font-medium')}
          >
            Logs
          </NavLink>
        </nav>
      </div>

      <ErrorBoundary>
        <Outlet />
      </ErrorBoundary>
    </div>
  )
}
