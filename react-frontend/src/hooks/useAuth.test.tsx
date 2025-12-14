import { describe, expect, it, vi } from 'vitest'
import { QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { apiClient } from '../api/client'
import { createTestQueryClient } from '../test/testUtils'
import { useAdminAuth } from './useAdminAuth'
import { useAuth } from './useAuth'

function makeWrapper() {
  const queryClient = createTestQueryClient()

  function Wrapper(props: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{props.children}</QueryClientProvider>
  }

  return { queryClient, Wrapper }
}

describe('useAuth()', () => {
  it('fetches /auth/me when enabled', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValue({
      data: { success: true, data: { id: 'u1', email: 'u1@example.com' } },
    } as any)

    const { Wrapper } = makeWrapper()
    const { result } = renderHook(() => useAuth(), { wrapper: Wrapper })

    await waitFor(() => expect(result.current.data?.success).toBe(true))
    expect(getSpy).toHaveBeenCalledWith('/auth/me')
  })

  it('does not fetch when disabled', async () => {
    const getSpy = vi.spyOn(apiClient, 'get')

    const { Wrapper } = makeWrapper()
    renderHook(() => useAuth({ enabled: false }), { wrapper: Wrapper })

    await Promise.resolve()
    expect(getSpy).not.toHaveBeenCalled()
  })
})

describe('useAdminAuth()', () => {
  it('fetches /admin/profile when enabled', async () => {
    const getSpy = vi.spyOn(apiClient, 'get').mockResolvedValue({
      data: { success: true, data: { id: 'a1', email: 'admin@example.com' } },
    } as any)

    const { Wrapper } = makeWrapper()
    const { result } = renderHook(() => useAdminAuth(), { wrapper: Wrapper })

    await waitFor(() => expect(result.current.data?.success).toBe(true))
    expect(getSpy).toHaveBeenCalledWith('/admin/profile')
  })
})

