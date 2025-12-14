import { describe, expect, it, beforeEach, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { RouterProvider, createMemoryRouter, useLocation } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'
import { USER_TOKEN_KEY } from '../../auth/tokens'
import * as hooks from '../../hooks'

vi.mock('../../hooks', () => ({
  useAuth: vi.fn(),
  useAdminAuth: vi.fn(),
}))

function LoginProbe() {
  const location = useLocation()
  return (
    <div>
      Login page <span data-testid='search'>{location.search}</span>
    </div>
  )
}

describe('ProtectedRoute', () => {
  const useAuthMock = vi.mocked(hooks.useAuth)

  beforeEach(() => {
    localStorage.clear()
    useAuthMock.mockReset()
    useAuthMock.mockReturnValue({
      isLoading: false,
      isFetching: false,
      isError: false,
    } as any)
  })

  it('redirects to /login with returnUrl when no user token is present', async () => {
    const router = createMemoryRouter(
      [
        { path: '/login', element: <LoginProbe /> },
        {
          element: <ProtectedRoute mode='user' />,
          children: [{ path: '/wishlist', element: <div>Wishlist</div> }],
        },
      ],
      { initialEntries: ['/wishlist'] },
    )

    render(<RouterProvider router={router} />)

    expect(await screen.findByText(/Login page/i)).toBeInTheDocument()
    expect(screen.getByTestId('search').textContent).toContain('returnUrl=')
  })

  it('shows loadingFallback while auth query is loading', () => {
    localStorage.setItem(USER_TOKEN_KEY, 'token')
    useAuthMock.mockReturnValue({
      isLoading: true,
      isFetching: false,
      isError: false,
    } as any)

    const router = createMemoryRouter(
      [
        { path: '/login', element: <LoginProbe /> },
        {
          element: <ProtectedRoute mode='user' loadingFallback={<div>Auth loading</div>} />,
          children: [{ path: '/wishlist', element: <div>Wishlist</div> }],
        },
      ],
      { initialEntries: ['/wishlist'] },
    )

    render(<RouterProvider router={router} />)
    expect(screen.getByText('Auth loading')).toBeInTheDocument()
    expect(screen.queryByText('Wishlist')).not.toBeInTheDocument()
  })

  it('renders child routes when token exists and auth query succeeds', async () => {
    localStorage.setItem(USER_TOKEN_KEY, 'token')
    useAuthMock.mockReturnValue({
      isLoading: false,
      isFetching: false,
      isError: false,
    } as any)

    const router = createMemoryRouter(
      [
        {
          element: <ProtectedRoute mode='user' />,
          children: [{ path: '/wishlist', element: <div>Wishlist</div> }],
        },
      ],
      { initialEntries: ['/wishlist'] },
    )

    render(<RouterProvider router={router} />)
    expect(await screen.findByText('Wishlist')).toBeInTheDocument()
  })
})

