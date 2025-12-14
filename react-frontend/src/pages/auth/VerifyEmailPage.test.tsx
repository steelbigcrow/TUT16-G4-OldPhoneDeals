import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { NotificationProvider } from '../../contexts/NotificationContext'
import { createTestQueryClient } from '../../test/testUtils'
import { VerifyEmailPage } from './VerifyEmailPage'
import { resendVerification, verifyEmail } from '../../api/auth'

vi.mock('../../api/auth', () => ({
  verifyEmail: vi.fn(),
  resendVerification: vi.fn(),
}))

describe('VerifyEmailPage', () => {
  const verifyEmailMock = vi.mocked(verifyEmail)
  const resendMock = vi.mocked(resendVerification)

  beforeEach(() => {
    verifyEmailMock.mockReset()
    resendMock.mockReset()
  })

  it('prefills email from query and navigates to /login after successful verification', async () => {
    const user = userEvent.setup()

    verifyEmailMock.mockResolvedValue({ success: true } as any)

    const router = createMemoryRouter(
      [
        { path: '/verify-email', element: <VerifyEmailPage /> },
        { path: '/login', element: <div>Login page</div> },
      ],
      { initialEntries: ['/verify-email?email=user%40example.com'] },
    )

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    await waitFor(() =>
      expect((screen.getByLabelText('Email') as HTMLInputElement).value).toBe('user@example.com'),
    )

    await user.type(screen.getByLabelText('Code'), '123456')
    await user.click(screen.getByRole('button', { name: 'Verify' }))

    expect(verifyEmailMock).toHaveBeenCalledWith({ email: 'user@example.com', code: '123456' })
    expect(await screen.findByText(/Email verified/i)).toBeInTheDocument()
    expect(await screen.findByText('Login page')).toBeInTheDocument()
  })

  it('validates email before resending and shows a toast on success', async () => {
    const user = userEvent.setup()

    resendMock.mockResolvedValue({ success: true } as any)

    const router = createMemoryRouter([{ path: '/verify-email', element: <VerifyEmailPage /> }], {
      initialEntries: ['/verify-email'],
    })

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    await user.type(screen.getByLabelText('Email'), 'not-an-email')
    await user.click(screen.getByRole('button', { name: 'Resend code' }))
    expect(resendMock).not.toHaveBeenCalled()
    expect(await screen.findByText('Enter a valid email')).toBeInTheDocument()

    await user.clear(screen.getByLabelText('Email'))
    await user.type(screen.getByLabelText('Email'), 'user@example.com')
    await user.click(screen.getByRole('button', { name: 'Resend code' }))

    expect(resendMock).toHaveBeenCalledWith({ email: 'user@example.com' })
    expect(await screen.findByText('Verification email resent')).toBeInTheDocument()
  })
})

