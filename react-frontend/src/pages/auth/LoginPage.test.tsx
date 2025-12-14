import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { NotificationProvider } from '../../contexts/NotificationContext'
import { USER_TOKEN_KEY } from '../../auth/tokens'
import { LoginPage } from './LoginPage'
import { login } from '../../api/auth'

vi.mock('../../api/auth', () => ({
  login: vi.fn(),
}))

function createClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
}

describe('LoginPage', () => {
  const loginMock = vi.mocked(login)

  beforeEach(() => {
    localStorage.clear()
    loginMock.mockReset()
  })

  it('stores token and navigates to a safe returnUrl after login', async () => {
    const user = userEvent.setup()

    loginMock.mockResolvedValue({
      success: true,
      data: { token: 't_user', user: { id: 'u1', firstName: 'A', lastName: 'B', email: 'a@b.com' } },
    } as any)

    const router = createMemoryRouter(
      [
        { path: '/login', element: <LoginPage /> },
        { path: '/wishlist', element: <div>Wishlist page</div> },
        { path: '/home', element: <div>Home page</div> },
      ],
      { initialEntries: ['/login?returnUrl=%2Fwishlist'] },
    )

    render(
      <QueryClientProvider client={createClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    await user.type(screen.getByLabelText('Email'), 'user@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByText('Wishlist page')).toBeInTheDocument()
    expect(localStorage.getItem(USER_TOKEN_KEY)).toBe('t_user')
  })

  it('ignores unsafe returnUrl and falls back to /home', async () => {
    const user = userEvent.setup()

    loginMock.mockResolvedValue({
      success: true,
      data: { token: 't_user', user: { id: 'u1', firstName: 'A', lastName: 'B', email: 'a@b.com' } },
    } as any)

    const router = createMemoryRouter(
      [
        { path: '/login', element: <LoginPage /> },
        { path: '/home', element: <div>Home page</div> },
      ],
      { initialEntries: ['/login?returnUrl=https%3A%2F%2Fevil.example%2Fsteal'] },
    )

    render(
      <QueryClientProvider client={createClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    await user.type(screen.getByLabelText('Email'), 'user@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByText('Home page')).toBeInTheDocument()
  })
})

