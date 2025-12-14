function safeDecode(value: string): string | null {
  try {
    return decodeURIComponent(value)
  } catch {
    return null
  }
}

function isSafeAppPath(path: string): boolean {
  if (!path.startsWith('/')) return false
  if (path.startsWith('//')) return false
  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(path)) return false
  if (path.startsWith('/login') || path.startsWith('/admin/login')) return false
  return true
}

export function getSafeReturnUrl(search: string): string | null {
  const params = new URLSearchParams(search)
  const raw = params.get('returnUrl')
  if (!raw) return null

  const decoded = safeDecode(raw)
  if (!decoded) return null

  if (!isSafeAppPath(decoded)) return null
  return decoded
}

