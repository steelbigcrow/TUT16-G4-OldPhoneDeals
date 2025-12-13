import { NavLink, Outlet, useLocation } from 'react-router-dom'

function classNames(...values: Array<string | false | null | undefined>) {
  return values.filter(Boolean).join(' ')
}

const navLinkBase =
  'text-sm px-3 py-1 rounded-md hover:bg-slate-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-slate-400'

export function AppLayout() {
  const location = useLocation()

  return (
    <div className='min-h-screen bg-slate-50 text-slate-900'>
      <header className='sticky top-0 z-10 border-b border-slate-200 bg-slate-900 text-white'>
        <div className='mx-auto flex max-w-6xl flex-wrap items-center gap-2 px-4 py-3'>
          <div className='mr-2 font-semibold'>OldPhoneDeals</div>

          <nav className='flex flex-wrap items-center gap-1'>
            <NavLink
              to='/home'
              className={({ isActive }) =>
                classNames(navLinkBase, isActive && 'bg-slate-800')
              }
            >
              /home
            </NavLink>
            <NavLink
              to='/search'
              className={({ isActive }) =>
                classNames(navLinkBase, isActive && 'bg-slate-800')
              }
            >
              /search
            </NavLink>
            <NavLink
              to='/phone/1'
              className={({ isActive }) =>
                classNames(navLinkBase, isActive && 'bg-slate-800')
              }
            >
              /phone/1
            </NavLink>
            <NavLink
              to='/login'
              className={({ isActive }) =>
                classNames(navLinkBase, isActive && 'bg-slate-800')
              }
            >
              /login
            </NavLink>
            <NavLink
              to='/admin/login'
              className={({ isActive }) =>
                classNames(navLinkBase, isActive && 'bg-slate-800')
              }
            >
              /admin/login
            </NavLink>

            <span className='mx-1 hidden h-5 w-px bg-slate-700 sm:inline-block' />

            <NavLink
              to='/wishlist'
              className={({ isActive }) =>
                classNames(navLinkBase, isActive && 'bg-slate-800')
              }
            >
              /wishlist (protected)
            </NavLink>
            <NavLink
              to='/profile'
              className={({ isActive }) =>
                classNames(navLinkBase, isActive && 'bg-slate-800')
              }
            >
              /profile (protected)
            </NavLink>
            <NavLink
              to='/admin/dashboard'
              className={({ isActive }) =>
                classNames(navLinkBase, isActive && 'bg-slate-800')
              }
            >
              /admin/dashboard (protected)
            </NavLink>
          </nav>

          <div className='ml-auto hidden text-xs text-slate-300 sm:block'>
            <span className='mr-2 opacity-75'>Path</span>
            <span className='font-mono'>{location.pathname}</span>
          </div>
        </div>
      </header>

      <main className='mx-auto max-w-6xl px-4 py-6'>
        <Outlet />
      </main>
    </div>
  )
}