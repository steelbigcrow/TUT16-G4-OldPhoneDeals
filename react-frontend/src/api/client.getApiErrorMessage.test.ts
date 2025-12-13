import { describe, expect, it } from 'vitest'
import { getApiErrorMessage } from './client'

describe('getApiErrorMessage()', () => {
  it('优先返回 response.data.message', () => {
    const error = {
      response: {
        data: { message: '后端错误信息' },
      },
      message: 'fallback message',
    }
    expect(getApiErrorMessage(error)).toBe('后端错误信息')
  })

  it('当没有 response.data.message 时兜底 error.message', () => {
    expect(getApiErrorMessage(new Error('boom'))).toBe('boom')

    const axiosLikeError = { message: 'axios boom' }
    expect(getApiErrorMessage(axiosLikeError)).toBe('axios boom')
  })
})