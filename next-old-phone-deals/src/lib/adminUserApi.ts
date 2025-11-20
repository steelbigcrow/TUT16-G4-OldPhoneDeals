/**
 * 管理员用户管理 API
 */

import { apiGet, apiPut, apiDelete } from './apiClient';
import type {
  ApiResponse,
  PageResponse,
  UserManagementResponse,
  UserDetailResponse,
  UpdateUserRequest
} from '@/types/admin';

/**
 * 获取用户列表（分页）
 * GET /api/admin/users?page=0&pageSize=10
 */
export async function getAdminUsers(
  token: string,
  params: {
    page?: number;
    pageSize?: number;
  } = {}
): Promise<PageResponse<UserManagementResponse>> {
  const { page = 1, pageSize = 10 } = params;
  const queryParams = new URLSearchParams({
    page: Math.max(0, page - 1).toString(),
    pageSize: pageSize.toString()
  });

  const response = await apiGet<ApiResponse<PageResponse<UserManagementResponse>>>(
    `/admin/users?${queryParams.toString()}`,
    {
      authToken: token
    }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to fetch users');
  }

  return response.data;
}

/**
 * 获取用户详情
 * GET /api/admin/users/{userId}
 */
export async function getUserDetail(
  token: string,
  userId: string
): Promise<UserDetailResponse> {
  const response = await apiGet<ApiResponse<UserDetailResponse>>(
    `/admin/users/${userId}`,
    {
      authToken: token
    }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to fetch user detail');
  }

  return response.data;
}

/**
 * 更新用户信息
 * PUT /api/admin/users/{userId}
 */
export async function updateUser(
  token: string,
  userId: string,
  data: UpdateUserRequest
): Promise<UserManagementResponse> {
  const response = await apiPut<ApiResponse<UserManagementResponse>, UpdateUserRequest>(
    `/admin/users/${userId}`,
    data,
    {
      authToken: token
    }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to update user');
  }

  return response.data;
}

/**
 * 切换用户禁用状态
 * PUT /api/admin/users/{userId}/toggle-disabled
 */
export async function toggleUserDisabled(
  token: string,
  userId: string
): Promise<UserManagementResponse> {
  const response = await apiPut<ApiResponse<UserManagementResponse>, Record<string, never>>(
    `/admin/users/${userId}/toggle-disabled`,
    {},
    {
      authToken: token
    }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to toggle user status');
  }

  return response.data;
}

/**
 * 删除用户
 * DELETE /api/admin/users/{userId}
 */
export async function deleteUser(
  token: string,
  userId: string
): Promise<void> {
  const response = await apiDelete<ApiResponse<null>>(
    `/admin/users/${userId}`,
    {
      authToken: token
    }
  );

  if (!response.success) {
    throw new Error(response.message || 'Failed to delete user');
  }
}
