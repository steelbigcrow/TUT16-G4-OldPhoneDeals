import React from 'react'

type ErrorBoundaryState = {
  hasError: boolean
  error: unknown
}

export type ErrorBoundaryProps = {
  children: React.ReactNode
  title?: string
}

export class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false, error: null }

  static getDerivedStateFromError(error: unknown): ErrorBoundaryState {
    return { hasError: true, error }
  }

  reset = () => {
    this.setState({ hasError: false, error: null })
  }

  render() {
    if (!this.state.hasError) return this.props.children

    const errorMessage =
      this.state.error instanceof Error ? this.state.error.message : 'Unknown error'

    return (
      <div className='rounded-lg border border-rose-200 bg-rose-50 p-4 text-rose-950'>
        <div className='text-sm font-semibold'>{this.props.title ?? 'Something went wrong'}</div>
        <div className='mt-1 text-sm opacity-90'>{errorMessage}</div>
        <div className='mt-3 flex gap-2'>
          <button
            type='button'
            onClick={this.reset}
            className='rounded-md bg-rose-600 px-3 py-2 text-sm font-medium text-white hover:bg-rose-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-rose-300'
          >
            Try again
          </button>
          <button
            type='button'
            onClick={() => window.location.reload()}
            className='rounded-md border border-rose-300 bg-white px-3 py-2 text-sm font-medium hover:bg-rose-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-rose-200'
          >
            Reload
          </button>
        </div>
      </div>
    )
  }
}

