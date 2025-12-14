import { describe, expect, it, vi } from 'vitest'
import { QueryClientProvider } from '@tanstack/react-query'
import { renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { uploadApi } from '../api'
import { createTestQueryClient } from '../test/testUtils'
import { useUploadImage } from './useUploadImage'

function makeWrapper() {
  const queryClient = createTestQueryClient()

  function Wrapper(props: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{props.children}</QueryClientProvider>
  }

  return { Wrapper }
}

describe('useUploadImage()', () => {
  it('calls uploadApi.uploadImage with the selected File', async () => {
    const file = new File(['hello'], 'hello.png', { type: 'image/png' })

    const uploadSpy = vi.spyOn(uploadApi, 'uploadImage').mockResolvedValue({
      success: true,
      data: { fileUrl: '/uploads/images/hello.png' },
    } as any)

    const { Wrapper } = makeWrapper()
    const { result } = renderHook(() => useUploadImage(), { wrapper: Wrapper })

    await result.current.mutateAsync(file)

    expect(uploadSpy).toHaveBeenCalledWith(file)
  })
})

