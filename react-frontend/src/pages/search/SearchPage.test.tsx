import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider, createMemoryRouter, useLocation } from 'react-router-dom'
import { NotificationProvider } from '../../contexts/NotificationContext'
import { createTestQueryClient } from '../../test/testUtils'
import { SearchPage } from './SearchPage'
import { cartApi, phonesApi, wishlistApi } from '../../api'
import { USER_TOKEN_KEY } from '../../auth/tokens'

function LoginProbe() {
  const location = useLocation()
  return (
    <div>
      Login page <span data-testid='search'>{location.search}</span>
    </div>
  )
}

describe('SearchPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('redirects unauthenticated users to /login when clicking Wishlist', async () => {
    const user = userEvent.setup()

    vi.spyOn(phonesApi, 'getPhones').mockResolvedValue({
      success: true,
      data: {
        phones: [
          {
            id: 'p1',
            title: 'Test phone',
            brand: 'APPLE',
            image: '',
            stock: 1,
            price: 10,
            averageRating: null,
            reviewCount: null,
            seller: null,
            createdAt: null,
          },
        ],
        currentPage: 1,
        totalPages: 1,
        total: 1,
      },
    } as any)

    const addWishlistSpy = vi.spyOn(wishlistApi, 'addToWishlist')

    const router = createMemoryRouter(
      [
        { path: '/search', element: <SearchPage /> },
        { path: '/login', element: <LoginProbe /> },
      ],
      { initialEntries: ['/search?search=iphone'] },
    )

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    expect(await screen.findByText('Test phone')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Wishlist' }))

    expect(addWishlistSpy).not.toHaveBeenCalled()
    expect(await screen.findByText('Please log in to use wishlist')).toBeInTheDocument()
    expect(await screen.findByText(/Login page/i)).toBeInTheDocument()
    expect(screen.getByTestId('search').textContent).toContain(
      'returnUrl=%2Fsearch%3Fsearch%3Diphone',
    )
  })

  it('adds to cart when authenticated and shows a success toast', async () => {
    const user = userEvent.setup()

    localStorage.setItem(USER_TOKEN_KEY, 't_user')

    vi.spyOn(phonesApi, 'getPhones').mockResolvedValue({
      success: true,
      data: {
        phones: [
          {
            id: 'p1',
            title: 'Test phone',
            brand: 'APPLE',
            image: '',
            stock: 1,
            price: 10,
            averageRating: null,
            reviewCount: null,
            seller: null,
            createdAt: null,
          },
        ],
        currentPage: 1,
        totalPages: 1,
        total: 1,
      },
    } as any)

    const addToCartSpy = vi.spyOn(cartApi, 'addToCart').mockResolvedValue({ success: true } as any)

    const router = createMemoryRouter([{ path: '/search', element: <SearchPage /> }], {
      initialEntries: ['/search'],
    })

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <NotificationProvider>
          <RouterProvider router={router} />
        </NotificationProvider>
      </QueryClientProvider>,
    )

    expect(await screen.findByText('Test phone')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Add to cart' }))

    await waitFor(() =>
      expect(addToCartSpy).toHaveBeenCalledWith({
        phoneId: 'p1',
        quantity: 1,
      }),
    )
    expect(await screen.findByText('Added to cart')).toBeInTheDocument()
  })
})

