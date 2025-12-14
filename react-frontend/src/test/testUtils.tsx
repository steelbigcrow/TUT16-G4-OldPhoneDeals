import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { NotificationProvider } from '../contexts/NotificationContext'

export function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, refetchOnWindowFocus: false },
      mutations: { retry: false },
    },
  })
}

export function renderWithProviders(
  ui: ReactElement,
  options?: {
    route?: string
    queryClient?: QueryClient
  },
) {
  const queryClient = options?.queryClient ?? createTestQueryClient()
  const route = options?.route ?? '/'

  return render(
    <QueryClientProvider client={queryClient}>
      <NotificationProvider>
        <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
      </NotificationProvider>
    </QueryClientProvider>,
  )
}

