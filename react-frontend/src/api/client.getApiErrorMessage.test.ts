import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ADMIN_TOKEN_KEY, USER_TOKEN_KEY } from '../auth/tokens'
import { apiClient, getApiErrorMessage, navigation } from './client'

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

function getRequestInterceptor() {
  const handlers = (apiClient.interceptors.request as any).handlers as Array<any>
  return handlers[0]?.fulfilled as (config: any) => any
}

function getResponseErrorInterceptor() {
  const handlers = (apiClient.interceptors.response as any).handlers as Array<any>
  return handlers[0]?.rejected as (error: any) => Promise<unknown>
}

describe('apiClient interceptors', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('adds Authorization header using correct token for /admin vs user paths', async () => {
    localStorage.setItem(USER_TOKEN_KEY, 'user-token')
    localStorage.setItem(ADMIN_TOKEN_KEY, 'admin-token')

    const request = getRequestInterceptor()

    const userConfig: any = { url: '/phones', headers: {} }
    const outUser = await request(userConfig)
    expect(outUser.headers.Authorization).toBe('Bearer user-token')
    expect(outUser.__normalizedPath).toBe('/phones')

    const adminConfig: any = { url: '/admin/profile?x=1', headers: {} }
    const outAdmin = await request(adminConfig)
    expect(outAdmin.headers.Authorization).toBe('Bearer admin-token')
    expect(outAdmin.__normalizedPath).toBe('/admin/profile')
  })

  it('does not add Authorization header when token is missing', async () => {
    const request = getRequestInterceptor()
    const config: any = { url: '/phones' }
    const out = await request(config)
    expect(out.headers).toBeUndefined()
  })

  it('clears user token and redirects to /login with returnUrl on 401/403', async () => {
    window.history.pushState({}, '', '/wishlist?foo=bar')
    localStorage.setItem(USER_TOKEN_KEY, 'user-token')

    const assignSpy = vi.spyOn(navigation, 'assign').mockImplementation(() => {})
    const errorInterceptor = getResponseErrorInterceptor()

    const error = {
      response: { status: 401 },
      config: { url: '/phones', __normalizedPath: '/phones' },
    }

    await expect(errorInterceptor(error)).rejects.toBe(error)
    expect(localStorage.getItem(USER_TOKEN_KEY)).toBeNull()
    expect(assignSpy).toHaveBeenCalledWith('/login?returnUrl=%2Fwishlist%3Ffoo%3Dbar')
  })

  it('redirects to plain /login when already on /login (avoid login loop)', async () => {
    window.history.pushState({}, '', '/login?returnUrl=%2Fwishlist')
    localStorage.setItem(USER_TOKEN_KEY, 'user-token')

    const assignSpy = vi.spyOn(navigation, 'assign').mockImplementation(() => {})
    const errorInterceptor = getResponseErrorInterceptor()

    const error = {
      response: { status: 403 },
      config: { url: '/phones', __normalizedPath: '/phones' },
    }

    await expect(errorInterceptor(error)).rejects.toBe(error)
    expect(assignSpy).toHaveBeenCalledWith('/login')
  })

  it('clears admin token and redirects to /admin/login for admin requests', async () => {
    window.history.pushState({}, '', '/admin/dashboard')
    localStorage.setItem(ADMIN_TOKEN_KEY, 'admin-token')

    const assignSpy = vi.spyOn(navigation, 'assign').mockImplementation(() => {})
    const errorInterceptor = getResponseErrorInterceptor()

    const error = {
      response: { status: 401 },
      config: { url: '/admin/profile', __normalizedPath: '/admin/profile' },
    }

    await expect(errorInterceptor(error)).rejects.toBe(error)
    expect(localStorage.getItem(ADMIN_TOKEN_KEY)).toBeNull()
    expect(assignSpy).toHaveBeenCalledWith('/admin/login?returnUrl=%2Fadmin%2Fdashboard')
  })
})
