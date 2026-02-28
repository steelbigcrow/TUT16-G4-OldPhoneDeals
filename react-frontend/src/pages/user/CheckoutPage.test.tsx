import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { CheckoutPage } from './CheckoutPage'
import { renderWithProviders } from '../../test/testUtils'
import { cartApi, ordersApi } from '../../api'

describe('CheckoutPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    sessionStorage.clear()
  })

  function mockCart() {
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
  }

  async function fillAddress(user: ReturnType<typeof userEvent.setup>) {
    await user.type(screen.getByLabelText('Street'), ' 1 Main St ')
    await user.type(screen.getByLabelText('City'), ' Sydney ')
    await user.type(screen.getByLabelText('State'), ' NSW ')
    await user.type(screen.getByLabelText('Zip'), ' 2000 ')
    await user.type(screen.getByLabelText('Country'), ' AU ')
  }

  it('submits checkout with address and shows a success toast', async () => {
    const user = userEvent.setup()
    mockCart()

    const checkoutSpy = vi.spyOn(ordersApi, 'checkout').mockResolvedValue({
      success: true,
      data: {
        id: 'o1',
      },
    } as any)

    renderWithProviders(<CheckoutPage />, { route: '/checkout' })

    expect(await screen.findByText('Cart phone')).toBeInTheDocument()

    await fillAddress(user)

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
      }, expect.stringMatching(/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i)),
    )

    expect(await screen.findByText('Checkout successful')).toBeInTheDocument()
    expect(sessionStorage.getItem('checkout_pending_idempotency_key')).toBeNull()
  })

  it('disables submit immediately and prevents duplicate checkout calls on rapid clicks', async () => {
    const user = userEvent.setup()
    mockCart()

    let resolveCheckout: ((value: any) => void) | null = null
    const checkoutPromise = new Promise<any>((resolve) => {
      resolveCheckout = resolve
    })
    const checkoutSpy = vi.spyOn(ordersApi, 'checkout').mockReturnValue(checkoutPromise as any)

    renderWithProviders(<CheckoutPage />, { route: '/checkout' })
    expect(await screen.findByText('Cart phone')).toBeInTheDocument()

    await fillAddress(user)

    const submitButton = screen.getByRole('button', { name: 'Place order' })
    await user.click(submitButton)
    expect(submitButton).toBeDisabled()
    expect(sessionStorage.getItem('checkout_pending_idempotency_key')).toBeTruthy()

    await user.click(submitButton)
    expect(checkoutSpy).toHaveBeenCalledTimes(1)

    resolveCheckout?.({
      success: true,
      data: { id: 'o2' },
    })

    expect(await screen.findByText('Checkout successful')).toBeInTheDocument()
  })

  it('keeps pending idempotency key when checkout fails with unknown outcome', async () => {
    const user = userEvent.setup()
    mockCart()

    const checkoutSpy = vi.spyOn(ordersApi, 'checkout').mockRejectedValue(new Error('Network Error'))

    renderWithProviders(<CheckoutPage />, { route: '/checkout' })
    expect(await screen.findByText('Cart phone')).toBeInTheDocument()

    await fillAddress(user)
    await user.click(screen.getByRole('button', { name: 'Place order' }))

    await waitFor(() => expect(checkoutSpy).toHaveBeenCalledTimes(1))
    const sentIdempotencyKey = checkoutSpy.mock.calls[0]?.[1]
    expect(typeof sentIdempotencyKey).toBe('string')
    expect(sentIdempotencyKey).toBeTruthy()
    expect(await screen.findByText('Network Error')).toBeInTheDocument()
    expect(sessionStorage.getItem('checkout_pending_idempotency_key')).toBe(sentIdempotencyKey)
  })
})
