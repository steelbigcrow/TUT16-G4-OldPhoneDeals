import { describe, expect, it } from 'vitest'
import { resolveImageUrl } from './images'

describe('resolveImageUrl()', () => {
  it('returns empty string for empty input', () => {
    expect(resolveImageUrl(undefined)).toBe('')
    expect(resolveImageUrl(null)).toBe('')
    expect(resolveImageUrl('')).toBe('')
    expect(resolveImageUrl('   ')).toBe('')
  })

  it('keeps absolute urls unchanged', () => {
    expect(resolveImageUrl('https://example.com/a.png')).toBe('https://example.com/a.png')
    expect(resolveImageUrl('http://example.com/a.png')).toBe('http://example.com/a.png')
  })

  it('ensures a leading slash for app-relative paths', () => {
    expect(resolveImageUrl('/uploads/images/a.png')).toBe('/uploads/images/a.png')
    expect(resolveImageUrl('uploads/images/a.png')).toBe('/uploads/images/a.png')
  })
})

