import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, within, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { NotificationProvider } from '../../contexts/NotificationContext'
import { createTestQueryClient } from '../../test/testUtils'
import { ResetPasswordPage } from './ResetPasswordPage'
import { requestPasswordReset, resetPassword } from '../../api/auth'

vi.mock('../../api/auth', () => ({
  requestPasswordReset: vi.fn(),
  resetPassword: vi.fn(),
}))

describe('ResetPasswordPage', () => {
  const requestResetMock = vi.mocked(requestPasswordReset)
  const resetPasswordMock = vi.mocked(resetPassword)

  beforeEach(() => {
    requestResetMock.mockReset()
    resetPasswordMock.mockReset()
  })

  it('requests a reset code and prefills the email for the reset form', async () => {
    const user = userEvent.setup()

    requestResetMock.mockResolvedValue({ success: true } as any)

    const router = createMemoryRouter([{ path: '/reset-password', element: <ResetPasswordPage /> }], {
      initialEntries: ['/reset-password'],
    })

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    const requestSection = screen.getByRole('heading', { level: 2, name: '1) Request reset code' })
      .closest('div')!

    await user.type(within(requestSection).getByLabelText('Email'), 'user@example.com')
    await user.click(within(requestSection).getByRole('button', { name: 'Send code' }))

    expect(requestResetMock).toHaveBeenCalledWith({ email: 'user@example.com' })
    expect(
      await screen.findByText('If the email exists, a reset code has been sent.'),
    ).toBeInTheDocument()

    const resetSection = screen.getByRole('heading', { level: 2, name: '2) Set new password' }).closest('div')!

    await waitFor(() =>
      expect((within(resetSection).getByLabelText('Email') as HTMLInputElement).value).toBe(
        'user@example.com',
      ),
    )
  })

  it('resets password and navigates to /login on success', async () => {
    const user = userEvent.setup()

    resetPasswordMock.mockResolvedValue({ success: true } as any)

    const router = createMemoryRouter(
      [
        { path: '/reset-password', element: <ResetPasswordPage /> },
        { path: '/login', element: <div>Login page</div> },
      ],
      { initialEntries: ['/reset-password'] },
    )

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    const resetSection = screen.getByRole('heading', { level: 2, name: '2) Set new password' }).closest('div')!

    await user.type(within(resetSection).getByLabelText('Email'), 'user@example.com')
    await user.type(within(resetSection).getByLabelText('Code'), '123456')
    await user.type(within(resetSection).getByLabelText('New password'), 'newsecret')
    await user.click(within(resetSection).getByRole('button', { name: 'Reset password' }))

    expect(resetPasswordMock).toHaveBeenCalledWith({
      email: 'user@example.com',
      code: '123456',
      newPassword: 'newsecret',
    })

    expect(await screen.findByText('Password reset. Please log in.')).toBeInTheDocument()
    expect(await screen.findByText('Login page')).toBeInTheDocument()
  })
})

