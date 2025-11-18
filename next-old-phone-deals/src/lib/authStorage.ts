/**
 * 认证存储模块
 * 管理用户和管理员的 JWT token 在 localStorage 中的存储
 */

const USER_TOKEN_KEY = 'opd_user_token';
const ADMIN_TOKEN_KEY = 'opd_admin_token';

/**
 * 检查是否在浏览器环境中
 */
function isBrowser(): boolean {
  return typeof window !== 'undefined';
}

// ============================================
// 用户 Token 管理
// ============================================

/**
 * 获取用户 token
 */
export function getUserToken(): string | null {
  if (!isBrowser()) return null;
  return localStorage.getItem(USER_TOKEN_KEY);
}

/**
 * 设置用户 token
 */
export function setUserToken(token: string): void {
  if (!isBrowser()) return;
  localStorage.setItem(USER_TOKEN_KEY, token);
}

/**
 * 清除用户 token
 */
export function clearUserToken(): void {
  if (!isBrowser()) return;
  localStorage.removeItem(USER_TOKEN_KEY);
}

// ============================================
// 管理员 Token 管理
// ============================================

/**
 * 获取管理员 token
 */
export function getAdminToken(): string | null {
  if (!isBrowser()) return null;
  return localStorage.getItem(ADMIN_TOKEN_KEY);
}

/**
 * 设置管理员 token
 */
export function setAdminToken(token: string): void {
  if (!isBrowser()) return;
  localStorage.setItem(ADMIN_TOKEN_KEY, token);
}

/**
 * 清除管理员 token
 */
export function clearAdminToken(): void {
  if (!isBrowser()) return;
  localStorage.removeItem(ADMIN_TOKEN_KEY);
}

// ============================================
// 通用方法
// ============================================

/**
 * 清除所有 token
 */
export function clearAllTokens(): void {
  clearUserToken();
  clearAdminToken();
}

/**
 * 获取当前有效的 token（优先返回管理员 token）
 */
export function getActiveToken(): string | null {
  return getAdminToken() || getUserToken();
}