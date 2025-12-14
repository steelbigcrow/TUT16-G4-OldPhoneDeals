import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider, createMemoryRouter, useLocation } from 'react-router-dom'
import { NotificationProvider } from '../../contexts/NotificationContext'
import { createTestQueryClient } from '../../test/testUtils'
import { PhoneDetailPage } from './PhoneDetailPage'
import { phonesApi } from '../../api'
import { USER_TOKEN_KEY } from '../../auth/tokens'

function LoginProbe() {
  const location = useLocation()
  return (
    <div>
      Login page <span data-testid='search'>{location.search}</span>
    </div>
  )
}

describe('PhoneDetailPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()

    vi.spyOn(phonesApi, 'getPhoneById').mockResolvedValue({
      success: true,
      data: {
        id: 'p1',
        title: 'Phone one',
        brand: 'APPLE',
        image: '',
        stock: 1,
        price: 10,
        isDisabled: null,
        salesCount: null,
        averageRating: 4.2,
        seller: null,
        reviews: [],
        createdAt: null,
        updatedAt: null,
      },
    } as any)

    vi.spyOn(phonesApi, 'getPhoneReviews').mockResolvedValue({
      success: true,
      data: {
        reviews: [],
        totalReviews: 0,
        currentPage: 1,
        totalPages: 1,
      },
    } as any)
  })

  it('redirects to /login with returnUrl when clicking Wishlist while unauthenticated', async () => {
    const user = userEvent.setup()

    const router = createMemoryRouter(
      [
        { path: '/phone/:id', element: <PhoneDetailPage /> },
        { path: '/login', element: <LoginProbe /> },
      ],
      { initialEntries: ['/phone/p1?from=search'] },
    )

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    expect(await screen.findByText('Phone one')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Wishlist' }))

    expect(await screen.findByText(/Login page/i)).toBeInTheDocument()
    expect(screen.getByTestId('search').textContent).toContain(
      'returnUrl=%2Fphone%2Fp1%3Ffrom%3Dsearch',
    )
  })

  it('shows review form validation errors when authenticated', async () => {
    const user = userEvent.setup()

    localStorage.setItem(USER_TOKEN_KEY, 't_user')

    const router = createMemoryRouter([{ path: '/phone/:id', element: <PhoneDetailPage /> }], {
      initialEntries: ['/phone/p1'],
    })

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    expect(await screen.findByText('Phone one')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Submit' }))

    expect(await screen.findByText('Comment is required')).toBeInTheDocument()
  })
})

