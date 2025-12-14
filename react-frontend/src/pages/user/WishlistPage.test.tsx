import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { WishlistPage } from './WishlistPage'
import { renderWithProviders } from '../../test/testUtils'
import { cartApi, wishlistApi } from '../../api'

describe('WishlistPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders wishlist items and allows removing an item', async () => {
    const user = userEvent.setup()

    vi.spyOn(wishlistApi, 'getWishlist').mockResolvedValue({
      success: true,
      data: {
        userId: 'u1',
        totalItems: 1,
        phones: [
          {
            id: 'p1',
            title: 'Saved phone',
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
      },
    } as any)

    const removeSpy = vi
      .spyOn(wishlistApi, 'removeFromWishlist')
      .mockResolvedValue({ success: true } as any)

    vi.spyOn(cartApi, 'addToCart').mockResolvedValue({ success: true } as any)

    renderWithProviders(<WishlistPage />, { route: '/wishlist' })

    expect(await screen.findByText('Saved phone')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Remove' }))

    await waitFor(() => expect(removeSpy).toHaveBeenCalledWith('p1'))
    expect(await screen.findByText('Removed from wishlist')).toBeInTheDocument()
  })

  it('shows empty state when wishlist has no items', async () => {
    vi.spyOn(wishlistApi, 'getWishlist').mockResolvedValue({
      success: true,
      data: { userId: 'u1', totalItems: 0, phones: [] },
    } as any)

    renderWithProviders(<WishlistPage />, { route: '/wishlist' })

    expect(await screen.findByText('Wishlist is empty.')).toBeInTheDocument()
  })
})

