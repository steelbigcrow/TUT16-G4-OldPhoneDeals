import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ADMIN_TOKEN_KEY,
  USER_TOKEN_KEY,
  clearTokenForPath,
  clearAllTokens,
  clearAdminToken,
  clearUserToken,
  getTokenForPath,
  safeGetToken,
  safeSetToken,
} from './tokens'

describe('tokens', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('safeGetToken() returns null when localStorage throws', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('no storage')
    })

    expect(safeGetToken(USER_TOKEN_KEY)).toBeNull()
  })

  it('safeSetToken() does not throw when localStorage throws', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota exceeded')
    })

    expect(() => safeSetToken(USER_TOKEN_KEY, 't')).not.toThrow()
  })

  it('clearUserToken()/clearAdminToken()/clearAllTokens() clear expected keys', () => {
    safeSetToken(USER_TOKEN_KEY, 'user-token')
    safeSetToken(ADMIN_TOKEN_KEY, 'admin-token')

    clearUserToken()
    expect(safeGetToken(USER_TOKEN_KEY)).toBeNull()
    expect(safeGetToken(ADMIN_TOKEN_KEY)).toBe('admin-token')

    clearAdminToken()
    expect(safeGetToken(ADMIN_TOKEN_KEY)).toBeNull()

    safeSetToken(USER_TOKEN_KEY, 'user-token-2')
    safeSetToken(ADMIN_TOKEN_KEY, 'admin-token-2')
    clearAllTokens()
    expect(safeGetToken(USER_TOKEN_KEY)).toBeNull()
    expect(safeGetToken(ADMIN_TOKEN_KEY)).toBeNull()
  })

  it('selects the correct token based on /admin prefix', () => {
    safeSetToken(USER_TOKEN_KEY, 'user-token')
    safeSetToken(ADMIN_TOKEN_KEY, 'admin-token')

    expect(getTokenForPath('/phones')).toBe('user-token')
    expect(getTokenForPath('/admin/users')).toBe('admin-token')
  })

  it('clears the correct token based on /admin prefix', () => {
    safeSetToken(USER_TOKEN_KEY, 'user-token')
    safeSetToken(ADMIN_TOKEN_KEY, 'admin-token')

    clearTokenForPath('/admin/anything')
    expect(safeGetToken(ADMIN_TOKEN_KEY)).toBeNull()
    expect(safeGetToken(USER_TOKEN_KEY)).toBe('user-token')

    clearTokenForPath('/phones')
    expect(safeGetToken(USER_TOKEN_KEY)).toBeNull()
  })
})
