import { beforeEach, describe, expect, it } from 'vitest'
import {
  ADMIN_TOKEN_KEY,
  USER_TOKEN_KEY,
  clearTokenForPath,
  getTokenForPath,
  safeGetToken,
  safeSetToken,
} from './tokens'

describe('tokens', () => {
  beforeEach(() => {
    localStorage.clear()
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

