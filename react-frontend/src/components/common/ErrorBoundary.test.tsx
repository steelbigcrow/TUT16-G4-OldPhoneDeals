import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ErrorBoundary } from './ErrorBoundary'

function ProblemChild(props: { shouldThrow: boolean }) {
  if (props.shouldThrow) {
    throw new Error('boom')
  }
  return <div>Safe content</div>
}

describe('ErrorBoundary', () => {
  it('renders children when there is no error', () => {
    render(
      <ErrorBoundary>
        <div>OK</div>
      </ErrorBoundary>,
    )

    expect(screen.getByText('OK')).toBeInTheDocument()
  })

  it('shows fallback UI and can reset after an error', async () => {
    const user = userEvent.setup()

    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    const { rerender } = render(
      <ErrorBoundary>
        <ProblemChild shouldThrow />
      </ErrorBoundary>,
    )

    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
    expect(screen.getByText('boom')).toBeInTheDocument()

    rerender(
      <ErrorBoundary>
        <ProblemChild shouldThrow={false} />
      </ErrorBoundary>,
    )

    await user.click(screen.getByRole('button', { name: 'Try again' }))
    expect(screen.getByText('Safe content')).toBeInTheDocument()

    consoleErrorSpy.mockRestore()
  })
})

