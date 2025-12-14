import { describe, expect, it, vi } from 'vitest'
import { QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { queryKeys } from '../queryKeys'
import { createTestQueryClient } from '../test/testUtils'
import { wishlistApi } from '../api'
import { useAddToWishlist, useRemoveFromWishlist, useWishlist } from './useWishlist'

function makeWrapper() {
  const queryClient = createTestQueryClient()

  function Wrapper(props: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{props.children}</QueryClientProvider>
  }

  return { queryClient, Wrapper }
}

describe('useWishlist()', () => {
  it('fetches wishlist data', async () => {
    const getSpy = vi.spyOn(wishlistApi, 'getWishlist').mockResolvedValue({
      success: true,
      data: { userId: 'u1', phones: [], totalItems: 0 },
    } as any)

    const { Wrapper } = makeWrapper()
    const { result } = renderHook(() => useWishlist(), { wrapper: Wrapper })

    await waitFor(() => expect(result.current.data?.success).toBe(true))
    expect(getSpy).toHaveBeenCalled()
  })
})

describe('wishlist mutations', () => {
  it('useAddToWishlist() invalidates wishlist on success', async () => {
    const addSpy = vi.spyOn(wishlistApi, 'addToWishlist').mockResolvedValue({ success: true } as any)

    const { queryClient, Wrapper } = makeWrapper()
    const invalidateSpy = vi
      .spyOn(queryClient, 'invalidateQueries')
      .mockResolvedValue(undefined as any)

    const { result } = renderHook(() => useAddToWishlist(), { wrapper: Wrapper })
    await result.current.mutateAsync('p1')

    expect(addSpy).toHaveBeenCalledWith({ phoneId: 'p1' })
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.wishlist })
  })

  it('useRemoveFromWishlist() invalidates wishlist on success', async () => {
    const removeSpy = vi
      .spyOn(wishlistApi, 'removeFromWishlist')
      .mockResolvedValue({ success: true } as any)

    const { queryClient, Wrapper } = makeWrapper()
    const invalidateSpy = vi
      .spyOn(queryClient, 'invalidateQueries')
      .mockResolvedValue(undefined as any)

    const { result } = renderHook(() => useRemoveFromWishlist(), { wrapper: Wrapper })
    await result.current.mutateAsync('p1')

    expect(removeSpy).toHaveBeenCalledWith('p1')
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.wishlist })
  })
})

