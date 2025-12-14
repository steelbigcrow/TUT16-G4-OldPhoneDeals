import { describe, expect, it } from 'vitest'
import { getSafeReturnUrl } from './returnUrl'

describe('getSafeReturnUrl()', () => {
  it('returns decoded in-app path', () => {
    expect(getSafeReturnUrl(`?returnUrl=${encodeURIComponent('/profile?x=1')}`)).toBe('/profile?x=1')
  })

  it('blocks login loop redirects', () => {
    expect(getSafeReturnUrl(`?returnUrl=${encodeURIComponent('/login')}`)).toBe(null)
    expect(getSafeReturnUrl(`?returnUrl=${encodeURIComponent('/admin/login')}`)).toBe(null)
  })

  it('blocks absolute and protocol-relative URLs', () => {
    expect(getSafeReturnUrl(`?returnUrl=${encodeURIComponent('https://evil.example/')}`)).toBe(null)
    expect(getSafeReturnUrl(`?returnUrl=${encodeURIComponent('//evil.example/')}`)).toBe(null)
  })

  it('returns null for invalid encoding', () => {
    expect(getSafeReturnUrl('?returnUrl=%E0%A4%A')).toBe(null)
  })
})

