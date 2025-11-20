/**
 * 用户资料 API
 */

import { apiGet, apiPut } from './apiClient';
import type { ApiResponse } from '@/types/auth';
import type {
  UserProfileResponse,
  UpdateProfileRequest,
  ChangePasswordRequest
} from '@/types/profile';

/**
 * 获取用户资料
 * GET /api/profile/{userId}
 */
export async function getProfile(
  userId: string,
  token: string
): Promise<UserProfileResponse> {
  const response = await apiGet<ApiResponse<UserProfileResponse>>(
    `/profile/${userId}`,
    { authToken: token }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to get profile');
  }

  return response.data;
}

/**
 * 更新用户资料
 * PUT /api/profile/{userId}
 */
export async function updateProfile(
  userId: string,
  data: UpdateProfileRequest,
  token: string
): Promise<UserProfileResponse> {
  const response = await apiPut<ApiResponse<UserProfileResponse>, UpdateProfileRequest>(
    `/profile/${userId}`,
    data,
    { authToken: token }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to update profile');
  }

  return response.data;
}

/**
 * 修改密码
 * PUT /api/profile/{userId}/change-password
 */
export async function changePassword(
  userId: string,
  data: ChangePasswordRequest,
  token: string
): Promise<void> {
  const response = await apiPut<ApiResponse<void>, ChangePasswordRequest>(
    `/profile/${userId}/change-password`,
    data,
    { authToken: token }
  );

  if (!response.success) {
    throw new Error(response.message || 'Failed to change password');
  }
}
