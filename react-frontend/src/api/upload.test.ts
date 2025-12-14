import { describe, expect, it, vi } from 'vitest'
import { apiClient } from './client'
import { uploadImage } from './upload'

describe('uploadImage()', () => {
  it('POSTs FormData to /upload/image with the file under "file"', async () => {
    const file = new File(['hello'], 'hello.png', { type: 'image/png' })

    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValue({
      data: {
        success: true,
        data: {
          fileName: 'images/hello.png',
          fileUrl: '/uploads/images/hello.png',
          originalName: 'hello.png',
          size: 5,
          contentType: 'image/png',
        },
      },
    } as any)

    await uploadImage(file)

    expect(postSpy).toHaveBeenCalledTimes(1)
    const [url, body] = postSpy.mock.calls[0] as unknown as [string, unknown]
    expect(url).toBe('/upload/image')
    expect(body).toBeInstanceOf(FormData)
    expect((body as FormData).get('file')).toBe(file)
  })
})

