import { describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { NotificationProvider, useNotifications } from './NotificationContext'

function TestHarness() {
  const notifications = useNotifications()
  return (
    <div>
      <button type='button' onClick={() => notifications.success('Hello')}>
        Success
      </button>
      <button type='button' onClick={() => notifications.error('Boom')}>
        Error
      </button>
    </div>
  )
}

describe('NotificationProvider', () => {
  it('renders toasts and allows dismissing them', async () => {
    const user = userEvent.setup()

    render(
      <NotificationProvider>
        <TestHarness />
      </NotificationProvider>,
    )

    await user.click(screen.getByRole('button', { name: 'Success' }))
    expect(screen.getByText('Hello')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '×' }))
    expect(screen.queryByText('Hello')).not.toBeInTheDocument()
  })

  it('auto-dismisses toasts after a timeout', async () => {
    vi.useFakeTimers()

    render(
      <NotificationProvider>
        <TestHarness />
      </NotificationProvider>,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Error' }))
    expect(screen.getByText('Boom')).toBeInTheDocument()

    act(() => {
      vi.advanceTimersByTime(4600)
    })
    expect(screen.queryByText('Boom')).not.toBeInTheDocument()

    vi.useRealTimers()
  })
})
