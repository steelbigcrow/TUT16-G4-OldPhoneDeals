export function normalizePath(rawUrl: string | undefined | null): string {
  const trimmed = (rawUrl ?? '').trim()
  if (!trimmed) return '/'

  let path = trimmed

  // Handle absolute URLs (defensive)
  try {
    if (/^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//.test(path)) {
      const parsed = new URL(path)
      path = parsed.pathname
    }
  } catch {
    // ignore parsing errors and fall back to string-based normalization
  }

  // Remove hash and query string
  path = path.split('#')[0].split('?')[0]

  // Ensure leading slash
  if (!path.startsWith('/')) path = `/${path}`

  return path
}