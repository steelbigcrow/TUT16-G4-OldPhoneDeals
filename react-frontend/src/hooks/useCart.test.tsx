import { describe, expect, it, vi } from 'vitest'
import { QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { cartApi } from '../api'
import { queryKeys } from '../queryKeys'
import { createTestQueryClient } from '../test/testUtils'
import { useAddToCart, useCart, useRemoveFromCart, useUpdateCartItem } from './useCart'

function makeWrapper() {
  const queryClient = createTestQueryClient()

  function Wrapper(props: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{props.children}</QueryClientProvider>
  }

  return { queryClient, Wrapper }
}

describe('useCart()', () => {
  it('fetches cart data', async () => {
    const getCartSpy = vi.spyOn(cartApi, 'getCart').mockResolvedValue({
      success: true,
      data: { id: 'c1', userId: 'u1', items: [], createdAt: null, updatedAt: null },
    } as any)

    const { Wrapper } = makeWrapper()
    const { result } = renderHook(() => useCart(), { wrapper: Wrapper })

    await waitFor(() => expect(result.current.data?.success).toBe(true))
    expect(getCartSpy).toHaveBeenCalled()
  })
})

describe('cart mutations', () => {
  it('useAddToCart() invalidates cart on success', async () => {
    const addSpy = vi.spyOn(cartApi, 'addToCart').mockResolvedValue({ success: true } as any)

    const { queryClient, Wrapper } = makeWrapper()
    const invalidateSpy = vi
      .spyOn(queryClient, 'invalidateQueries')
      .mockResolvedValue(undefined as any)

    const { result } = renderHook(() => useAddToCart(), { wrapper: Wrapper })
    await result.current.mutateAsync({ phoneId: 'p1', quantity: 1 })

    expect(addSpy).toHaveBeenCalledWith({ phoneId: 'p1', quantity: 1 })
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.cart })
  })

  it('useUpdateCartItem() invalidates cart on success', async () => {
    const updateSpy = vi.spyOn(cartApi, 'updateCartItem').mockResolvedValue({ success: true } as any)

    const { queryClient, Wrapper } = makeWrapper()
    const invalidateSpy = vi
      .spyOn(queryClient, 'invalidateQueries')
      .mockResolvedValue(undefined as any)

    const { result } = renderHook(() => useUpdateCartItem(), { wrapper: Wrapper })
    await result.current.mutateAsync({ phoneId: 'p1', quantity: 2 })

    expect(updateSpy).toHaveBeenCalledWith('p1', { quantity: 2 })
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.cart })
  })

  it('useRemoveFromCart() invalidates cart on success', async () => {
    const removeSpy = vi.spyOn(cartApi, 'removeFromCart').mockResolvedValue({ success: true } as any)

    const { queryClient, Wrapper } = makeWrapper()
    const invalidateSpy = vi
      .spyOn(queryClient, 'invalidateQueries')
      .mockResolvedValue(undefined as any)

    const { result } = renderHook(() => useRemoveFromCart(), { wrapper: Wrapper })
    await result.current.mutateAsync('p1')

    expect(removeSpy).toHaveBeenCalledWith('p1')
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.cart })
  })
})

