import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import { ProfileHomePage } from './ProfileHomePage'
import { renderWithProviders } from '../../test/testUtils'
import { ordersApi, profileApi } from '../../api'

describe('ProfileHomePage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders profile details and recent orders', async () => {
    vi.spyOn(profileApi, 'getProfile').mockResolvedValue({
      success: true,
      data: {
        id: 'u1',
        firstName: 'A',
        lastName: 'B',
        email: 'a@b.com',
        emailVerified: true,
        createdAt: null,
        updatedAt: null,
      },
    } as any)

    vi.spyOn(ordersApi, 'getOrders').mockResolvedValue({
      success: true,
      data: {
        items: [
          {
            id: 'o1',
            userId: 'u1',
            items: [
              { phoneId: 'p1', title: 'Phone', quantity: 1, price: 10 },
              { phoneId: 'p2', title: 'Phone 2', quantity: 2, price: 5 },
            ],
            totalAmount: 20,
            address: null,
            createdAt: null,
          },
        ],
        pagination: { currentPage: 1, pageSize: 5, totalPages: 1, totalItems: 1 },
      },
    } as any)

    renderWithProviders(<ProfileHomePage />, { route: '/profile' })

    expect(await screen.findByText('A B')).toBeInTheDocument()
    expect(screen.getByText('a@b.com')).toBeInTheDocument()
    expect(screen.getByText('true')).toBeInTheDocument()

    expect(await screen.findByText('Order o1')).toBeInTheDocument()
    expect(screen.getByText('Items:')).toBeInTheDocument()
  })
})

