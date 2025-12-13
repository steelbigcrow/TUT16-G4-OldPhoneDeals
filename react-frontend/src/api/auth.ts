import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { AuthUserResponse, LoginResponse } from '../types/user'

// ============================================================================
// Request Types
// ============================================================================

export type LoginRequest = {
  email: string
  password: string
}

export type RegisterRequest = {
  firstName: string
  lastName: string
  email: string
  password: string
}

export type VerifyEmailRequest = {
  email: string
  code: string
}

export type ResendVerificationRequest = {
  email: string
}

export type SendResetPasswordEmailRequest = {
  email: string
}

export type VerifyResetCodeRequest = {
  email: string
  code: string
}

export type ResetPasswordRequest = {
  email: string
  code: string
  newPassword: string
}

// ============================================================================
// API Functions
// ============================================================================

/**
 * User login
 * POST /api/auth/login
 */
export async function login(request: LoginRequest) {
  const { data } = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', request)
  return data
}

/**
 * User registration
 * POST /api/auth/register
 */
export async function register(request: RegisterRequest) {
  const { data } = await apiClient.post<ApiResponse<AuthUserResponse>>(
    '/auth/register',
    request,
  )
  return data
}

/**
 * Verify email with code
 * POST /api/auth/verify-email
 */
export async function verifyEmail(request: VerifyEmailRequest) {
  const { data } = await apiClient.post<ApiResponse<void>>('/auth/verify-email', request)
  return data
}

/**
 * Resend verification email
 * POST /api/auth/resend-verification
 */
export async function resendVerification(request: ResendVerificationRequest) {
  const { data } = await apiClient.post<ApiResponse<void>>('/auth/resend-verification', request)
  return data
}

/**
 * Request password reset (send email)
 * POST /api/auth/request-password-reset
 */
export async function requestPasswordReset(request: SendResetPasswordEmailRequest) {
  const { data } = await apiClient.post<ApiResponse<void>>(
    '/auth/request-password-reset',
    request,
  )
  return data
}

/**
 * Verify reset code
 * POST /api/auth/verify-reset-code
 */
export async function verifyResetCode(request: VerifyResetCodeRequest) {
  const { data } = await apiClient.post<ApiResponse<boolean>>('/auth/verify-reset-code', request)
  return data
}

/**
 * Reset password with code
 * POST /api/auth/reset-password
 */
export async function resetPassword(request: ResetPasswordRequest) {
  const { data } = await apiClient.post<ApiResponse<void>>('/auth/reset-password', request)
  return data
}

/**
 * Get current authenticated user
 * GET /api/auth/me
 */
export async function getMe() {
  const { data } = await apiClient.get<ApiResponse<AuthUserResponse>>('/auth/me')
  return data
}
