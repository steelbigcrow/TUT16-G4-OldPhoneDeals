import { describe, expect, it } from 'vitest'
import { normalizePath } from './normalizePath'

describe('normalizePath()', () => {
  it('当不以 / 开头时会补上 /', () => {
    expect(normalizePath('home')).toBe('/home')
    expect(normalizePath('  home  ')).toBe('/home')
  })

  it('会去掉 querystring 和 hash', () => {
    expect(normalizePath('/home?foo=bar')).toBe('/home')
    expect(normalizePath('/home#section')).toBe('/home')
    expect(normalizePath('/home?foo=bar#section')).toBe('/home')
  })

  it('包含 /admin 时输出应保持 /admin 前缀（便于 startsWith(/admin) 判断）', () => {
    const out = normalizePath('admin/dashboard?foo=bar#hash')
    expect(out).toBe('/admin/dashboard')
    expect(out.startsWith('/admin')).toBe(true)
  })

  it('对绝对 URL 也能提取 pathname（防御性）', () => {
    expect(normalizePath('https://example.com/admin/users?x=1')).toBe('/admin/users')
  })
})