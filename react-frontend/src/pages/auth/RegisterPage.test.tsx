import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider, createMemoryRouter, useLocation } from 'react-router-dom'
import { NotificationProvider } from '../../contexts/NotificationContext'
import { createTestQueryClient } from '../../test/testUtils'
import { RegisterPage } from './RegisterPage'
import { register } from '../../api/auth'

vi.mock('../../api/auth', () => ({
  register: vi.fn(),
}))

function VerifyEmailProbe() {
  const location = useLocation()
  return (
    <div>
      Verify email <span data-testid='verify_search'>{location.search}</span>
    </div>
  )
}

describe('RegisterPage', () => {
  const registerMock = vi.mocked(register)

  beforeEach(() => {
    registerMock.mockReset()
  })

  it('registers and navigates to /verify-email with prefilled email', async () => {
    const user = userEvent.setup()

    registerMock.mockResolvedValue({ success: true } as any)

    const router = createMemoryRouter(
      [
        { path: '/register', element: <RegisterPage /> },
        { path: '/verify-email', element: <VerifyEmailProbe /> },
      ],
      { initialEntries: ['/register'] },
    )

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    await user.type(screen.getByLabelText('First name'), ' Alice ')
    await user.type(screen.getByLabelText('Last name'), ' Smith ')
    await user.type(screen.getByLabelText('Email'), ' user@example.com ')
    await user.type(screen.getByLabelText('Password'), 'secret1')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(registerMock).toHaveBeenCalledWith({
      firstName: 'Alice',
      lastName: 'Smith',
      email: 'user@example.com',
      password: 'secret1',
    })

    expect(await screen.findByText(/Registered\. Please verify your email\./i)).toBeInTheDocument()
    expect(await screen.findByText(/Verify email/i)).toBeInTheDocument()
    expect(screen.getByTestId('verify_search').textContent).toContain('email=user%40example.com')
  })

  it('shows an error toast when registration fails', async () => {
    const user = userEvent.setup()

    registerMock.mockResolvedValue({ success: false, message: 'Email already exists' } as any)

    const router = createMemoryRouter([{ path: '/register', element: <RegisterPage /> }], {
      initialEntries: ['/register'],
    })

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    await user.type(screen.getByLabelText('First name'), 'Alice')
    await user.type(screen.getByLabelText('Last name'), 'Smith')
    await user.type(screen.getByLabelText('Email'), 'user@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret1')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Email already exists')).toBeInTheDocument()
    expect(screen.getByText('Register')).toBeInTheDocument()
  })
})

