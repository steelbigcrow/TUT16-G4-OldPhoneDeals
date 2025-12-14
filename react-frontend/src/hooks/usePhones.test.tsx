import { describe, expect, it, vi } from 'vitest'
import { QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { createTestQueryClient } from '../test/testUtils'
import { phonesApi } from '../api'
import { useAddPhoneReview, usePhonesList } from './usePhones'

function makeWrapper() {
  const queryClient = createTestQueryClient()

  function Wrapper(props: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{props.children}</QueryClientProvider>
  }

  return { queryClient, Wrapper }
}

describe('usePhonesList()', () => {
  it('calls phonesApi.getPhones with provided params', async () => {
    const params = { page: 1, limit: 12, search: 'iphone' }

    const getPhonesSpy = vi.spyOn(phonesApi, 'getPhones').mockResolvedValue({
      success: true,
      data: { phones: [], currentPage: 1, totalPages: 1, total: 0 },
    } as any)

    const { Wrapper } = makeWrapper()
    const { result } = renderHook(() => usePhonesList(params as any), { wrapper: Wrapper })

    await waitFor(() => expect(result.current.data?.success).toBe(true))
    expect(getPhonesSpy).toHaveBeenCalledWith(params)
  })
})

describe('useAddPhoneReview()', () => {
  it('invalidates reviews and phone detail queries after a successful mutation', async () => {
    const addReviewSpy = vi.spyOn(phonesApi, 'addPhoneReview').mockResolvedValue({ success: true } as any)

    const { queryClient, Wrapper } = makeWrapper()
    const invalidateSpy = vi
      .spyOn(queryClient, 'invalidateQueries')
      .mockResolvedValue(undefined as any)

    const { result } = renderHook(() => useAddPhoneReview('p1'), { wrapper: Wrapper })

    await result.current.mutateAsync({ rating: 5, comment: 'ok' })

    expect(addReviewSpy).toHaveBeenCalledWith('p1', { rating: 5, comment: 'ok' })
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['reviews'] })
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['phones', 'detail', 'p1'] })
  })
})

