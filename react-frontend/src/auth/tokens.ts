export const USER_TOKEN_KEY = 'user_auth_token'
export const ADMIN_TOKEN_KEY = 'admin_auth_token'

export function safeGetToken(tokenKey: string): string | null {
  try {
    return localStorage.getItem(tokenKey)
  } catch {
    return null
  }
}

export function safeSetToken(tokenKey: string, token: string) {
  try {
    localStorage.setItem(tokenKey, token)
  } catch {
    // ignore
  }
}

export function safeClearToken(tokenKey: string) {
  try {
    localStorage.removeItem(tokenKey)
  } catch {
    // ignore
  }
}

export function clearUserToken() {
  safeClearToken(USER_TOKEN_KEY)
}

export function clearAdminToken() {
  safeClearToken(ADMIN_TOKEN_KEY)
}

export function clearAllTokens() {
  clearUserToken()
  clearAdminToken()
}

export function getTokenForPath(normalizedPath: string): string | null {
  const isAdminRequest = normalizedPath.startsWith('/admin')
  return safeGetToken(isAdminRequest ? ADMIN_TOKEN_KEY : USER_TOKEN_KEY)
}

export function clearTokenForPath(normalizedPath: string) {
  const isAdminRequest = normalizedPath.startsWith('/admin')
  safeClearToken(isAdminRequest ? ADMIN_TOKEN_KEY : USER_TOKEN_KEY)
}

