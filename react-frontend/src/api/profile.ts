import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { UserProfileResponse } from '../types/user'

export type UpdateProfileRequest = {
  firstName: string
  lastName: string
  email: string
  currentPassword?: string
}

export type ChangePasswordRequest = {
  currentPassword: string
  newPassword: string
}

export async function getProfile() {
  const { data } = await apiClient.get<ApiResponse<UserProfileResponse>>('/profile')
  return data
}

export async function updateProfile(request: UpdateProfileRequest) {
  const { data } = await apiClient.put<ApiResponse<UserProfileResponse>>('/profile', request)
  return data
}

export async function changePassword(request: ChangePasswordRequest) {
  const { data } = await apiClient.put<ApiResponse<void>>('/profile/change-password', request)
  return data
}

