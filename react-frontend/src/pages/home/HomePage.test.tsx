import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { HomePage } from './HomePage'
import { renderWithProviders } from '../../test/testUtils'
import { phonesApi } from '../../api'

describe('HomePage', () => {
  it('renders special phone sections from API', async () => {
    const best = {
      id: 'p1',
      title: 'Best phone',
      brand: 'APPLE',
      image: '',
      stock: 3,
      price: 199,
      averageRating: 4.5,
      reviewCount: 12,
      seller: null,
      createdAt: null,
    } as any

    const soon = {
      id: 'p2',
      title: 'Almost sold out',
      brand: 'NOKIA',
      image: '',
      stock: 1,
      price: 49,
      averageRating: 4.2,
      reviewCount: 8,
      seller: null,
      createdAt: null,
    } as any

    const spy = vi.spyOn(phonesApi, 'getSpecialPhones').mockImplementation(async ({ special }) => {
      return {
        success: true,
        data: special === 'bestSellers' ? [best] : [soon],
      } as any
    })

    renderWithProviders(<HomePage />, { route: '/home' })

    await waitFor(() =>
      expect(spy).toHaveBeenCalledWith({
        special: 'bestSellers',
      }),
    )
    await waitFor(() => expect(spy).toHaveBeenCalledWith({ special: 'soldOutSoon' }))

    expect(await screen.findByText('Best phone')).toBeInTheDocument()
    expect(await screen.findByText('Almost sold out')).toBeInTheDocument()
  })
})

