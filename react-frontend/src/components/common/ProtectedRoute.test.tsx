import React from 'react'
import { describe, expect, it, beforeEach, vi } from 'vitest'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { render, screen } from '@testing-library/react'
import { ProtectedRoute } from './ProtectedRoute'

vi.mock('../../hooks', () => {
  const query = {
    isLoading: false,
    isFetching: false,
    isError: false,
    error: null,
    refetch: vi.fn(),
  }

  return {
    useAuth: vi.fn(() => query),
    useAdminAuth: vi.fn(() => query),
  }
})

describe('ProtectedRoute', () => {
  beforeEach(() => {
    localStorage.removeItem('user_auth_token')
    localStorage.removeItem('admin_auth_token')
  })

  it('user：无 token 时 Navigate 到 /login 并携带 returnUrl', () => {
    const router = createMemoryRouter(
      [
        {
          path: '/',
          element: <ProtectedRoute mode='user' />,
          children: [{ path: 'protected', element: <div>Protected</div> }],
        },
        { path: '/login', element: <div>Login</div> },
      ],
      { initialEntries: ['/protected?x=1'] },
    )

    render(<RouterProvider router={router} />)

    expect(screen.getByText('Login')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(router.state.location.search).toBe(`?returnUrl=${encodeURIComponent('/protected?x=1')}`)
  })

  it('admin：无 token 时 Navigate 到 /admin/login 并携带 returnUrl', () => {
    const router = createMemoryRouter(
      [
        {
          path: '/',
          element: <ProtectedRoute mode='admin' />,
          children: [{ path: 'admin/dashboard', element: <div>AdminDashboard</div> }],
        },
        { path: '/admin/login', element: <div>AdminLogin</div> },
      ],
      { initialEntries: ['/admin/dashboard?tab=users'] },
    )

    render(<RouterProvider router={router} />)

    expect(screen.getByText('AdminLogin')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/admin/login')
    expect(router.state.location.search).toBe(
      `?returnUrl=${encodeURIComponent('/admin/dashboard?tab=users')}`,
    )
  })

  it('login 页自身不产生循环 returnUrl（会清理现有 returnUrl）', () => {
    const router = createMemoryRouter(
      [
        {
          path: '/',
          element: <ProtectedRoute mode='user' />,
          children: [{ path: 'login', element: <div>Login</div> }],
        },
      ],
      { initialEntries: ['/login?returnUrl=%2Flogin%3FreturnUrl%3D%252Flogin'] },
    )

    render(<RouterProvider router={router} />)

    expect(router.state.location.pathname).toBe('/login')
    expect(router.state.location.search).toBe('')
  })
})