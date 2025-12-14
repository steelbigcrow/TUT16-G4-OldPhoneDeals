import { describe, expect, it } from 'vitest'
import { getSafeReturnUrl } from './returnUrl'

describe('getSafeReturnUrl()', () => {
  it('returns null when no returnUrl is provided', () => {
    expect(getSafeReturnUrl('')).toBeNull()
    expect(getSafeReturnUrl('?foo=bar')).toBeNull()
  })

  it('accepts safe in-app paths', () => {
    expect(getSafeReturnUrl('?returnUrl=%2Fprofile')).toBe('/profile')
    expect(getSafeReturnUrl('?returnUrl=%2Fadmin%2Fdashboard')).toBe('/admin/dashboard')
    expect(getSafeReturnUrl('?returnUrl=%2Fsearch%3Fq%3Diphone')).toBe('/search?q=iphone')
  })

  it('rejects malformed encoding and non-app paths', () => {
    // URLSearchParams already decodes once; keep a % sequence by encoding the percent sign.
    expect(getSafeReturnUrl('?returnUrl=%25E0%25A4%25A')).toBeNull()
    expect(getSafeReturnUrl('?returnUrl=profile')).toBeNull()
  })

  it('rejects unsafe or login-loop return URLs', () => {
    expect(getSafeReturnUrl('?returnUrl=https%3A%2F%2Fevil.com')).toBeNull()
    expect(getSafeReturnUrl('?returnUrl=%2F%2Fevil.com')).toBeNull()
    expect(getSafeReturnUrl('?returnUrl=%2Flogin')).toBeNull()
    expect(getSafeReturnUrl('?returnUrl=%2Fadmin%2Flogin')).toBeNull()
  })
})
