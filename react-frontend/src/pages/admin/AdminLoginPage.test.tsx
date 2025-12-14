import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { NotificationProvider } from '../../contexts/NotificationContext'
import { createTestQueryClient } from '../../test/testUtils'
import { AdminLoginPage } from './AdminLoginPage'
import { adminApi } from '../../api'
import { ADMIN_TOKEN_KEY } from '../../auth/tokens'

describe('AdminLoginPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('stores admin token and navigates to a safe admin returnUrl', async () => {
    const user = userEvent.setup()

    vi.spyOn(adminApi, 'adminLogin').mockResolvedValue({
      success: true,
      data: { token: 't_admin' },
    } as any)

    const router = createMemoryRouter(
      [
        { path: '/admin/login', element: <AdminLoginPage /> },
        { path: '/admin/users', element: <div>Users page</div> },
        { path: '/admin/dashboard', element: <div>Dashboard</div> },
      ],
      { initialEntries: ['/admin/login?returnUrl=%2Fadmin%2Fusers'] },
    )

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    await user.type(screen.getByLabelText('Email'), 'admin@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByText('Users page')).toBeInTheDocument()
    expect(localStorage.getItem(ADMIN_TOKEN_KEY)).toBe('t_admin')
  })

  it('falls back to /admin/dashboard when returnUrl is not an admin path', async () => {
    const user = userEvent.setup()

    vi.spyOn(adminApi, 'adminLogin').mockResolvedValue({
      success: true,
      data: { token: 't_admin' },
    } as any)

    const router = createMemoryRouter(
      [
        { path: '/admin/login', element: <AdminLoginPage /> },
        { path: '/admin/dashboard', element: <div>Dashboard</div> },
      ],
      { initialEntries: ['/admin/login?returnUrl=%2Fwishlist'] },
    )

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    await user.type(screen.getByLabelText('Email'), 'admin@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByText('Dashboard')).toBeInTheDocument()
  })
})

