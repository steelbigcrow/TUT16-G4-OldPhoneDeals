import { describe, expect, it, vi } from 'vitest'
import { QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { ordersApi } from '../api'
import { queryKeys } from '../queryKeys'
import { createTestQueryClient } from '../test/testUtils'
import { useCheckout, useOrders } from './useOrders'

function makeWrapper() {
  const queryClient = createTestQueryClient()

  function Wrapper(props: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{props.children}</QueryClientProvider>
  }

  return { queryClient, Wrapper }
}

describe('useOrders()', () => {
  it('fetches orders with page params', async () => {
    const getSpy = vi.spyOn(ordersApi, 'getOrders').mockResolvedValue({
      success: true,
      data: { items: [], pagination: { currentPage: 1, pageSize: 5, totalPages: 1, totalItems: 0 } },
    } as any)

    const { Wrapper } = makeWrapper()
    const { result } = renderHook(() => useOrders({ page: 1, pageSize: 5 }), { wrapper: Wrapper })

    await waitFor(() => expect(result.current.data?.success).toBe(true))
    expect(getSpy).toHaveBeenCalledWith(1, 5)
  })
})

describe('useCheckout()', () => {
  it('calls checkout and invalidates cart + orders on success', async () => {
    const checkoutSpy = vi.spyOn(ordersApi, 'checkout').mockResolvedValue({
      success: true,
      data: { id: 'o1' },
    } as any)

    const { queryClient, Wrapper } = makeWrapper()
    const invalidateSpy = vi
      .spyOn(queryClient, 'invalidateQueries')
      .mockResolvedValue(undefined as any)

    const { result } = renderHook(() => useCheckout(), { wrapper: Wrapper })

    await result.current.mutateAsync({
      address: { street: '1', city: 'c', state: 's', zip: 'z', country: 'AU' },
    })

    expect(checkoutSpy).toHaveBeenCalledWith({
      address: { street: '1', city: 'c', state: 's', zip: 'z', country: 'AU' },
    })
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.cart })
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['orders'] })
  })
})

