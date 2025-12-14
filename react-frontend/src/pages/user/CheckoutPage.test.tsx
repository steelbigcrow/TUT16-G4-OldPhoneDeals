import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { CheckoutPage } from './CheckoutPage'
import { renderWithProviders } from '../../test/testUtils'
import { cartApi, ordersApi } from '../../api'

describe('CheckoutPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('submits checkout with address and shows a success toast', async () => {
    const user = userEvent.setup()

    vi.spyOn(cartApi, 'getCart').mockResolvedValue({
      success: true,
      data: {
        id: 'c1',
        userId: 'u1',
        createdAt: null,
        updatedAt: null,
        items: [
          {
            phoneId: 'p1',
            title: 'Cart phone',
            quantity: 2,
            price: 10,
            averageRating: null,
            reviewCount: null,
            seller: null,
            phone: { id: 'p1', title: 'Cart phone', brand: 'APPLE', image: '', stock: 3, price: 10, isDisabled: null },
            createdAt: null,
          },
        ],
      },
    } as any)

    const checkoutSpy = vi.spyOn(ordersApi, 'checkout').mockResolvedValue({
      success: true,
      data: {
        id: 'o1',
      },
    } as any)

    renderWithProviders(<CheckoutPage />, { route: '/checkout' })

    expect(await screen.findByText('Cart phone')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Street'), ' 1 Main St ')
    await user.type(screen.getByLabelText('City'), ' Sydney ')
    await user.type(screen.getByLabelText('State'), ' NSW ')
    await user.type(screen.getByLabelText('Zip'), ' 2000 ')
    await user.type(screen.getByLabelText('Country'), ' AU ')

    await user.click(screen.getByRole('button', { name: 'Place order' }))

    await waitFor(() =>
      expect(checkoutSpy).toHaveBeenCalledWith({
        address: {
          street: '1 Main St',
          city: 'Sydney',
          state: 'NSW',
          zip: '2000',
          country: 'AU',
        },
      }),
    )

    expect(await screen.findByText('Checkout successful')).toBeInTheDocument()
  })
})

