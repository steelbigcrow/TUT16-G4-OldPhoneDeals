/**
 * 管理员挂牌管理 API
 */

import { apiGet, apiPut, apiDelete } from './apiClient';
import type {
  ApiResponse,
  PageResponse,
  PhoneManagementResponse,
  UpdatePhoneRequest
} from '@/types/admin';

/**
 * 获取所有商品（包含禁用的）
 * GET /api/admin/phones?page=0&pageSize=10
 */
export async function getAdminPhones(
  token: string,
  params: {
    page?: number;
    pageSize?: number;
  } = {}
): Promise<PageResponse<PhoneManagementResponse>> {
  const { page = 0, pageSize = 10 } = params;
  const queryParams = new URLSearchParams({
    page: page.toString(),
    pageSize: pageSize.toString()
  });

  const response = await apiGet<ApiResponse<PageResponse<PhoneManagementResponse>>>(
    `/admin/phones?${queryParams.toString()}`,
    {
      authToken: token
    }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to fetch phones');
  }

  return response.data;
}

/**
 * 更新商品信息
 * PUT /api/admin/phones/{phoneId}
 */
export async function updateAdminPhone(
  token: string,
  phoneId: string,
  data: UpdatePhoneRequest
): Promise<PhoneManagementResponse> {
  const response = await apiPut<ApiResponse<PhoneManagementResponse>, UpdatePhoneRequest>(
    `/admin/phones/${phoneId}`,
    data,
    {
      authToken: token
    }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to update phone');
  }

  return response.data;
}

/**
 * 切换商品禁用状态
 * PUT /api/admin/phones/{phoneId}/toggle-disabled
 */
export async function toggleAdminPhoneDisabled(
  token: string,
  phoneId: string
): Promise<PhoneManagementResponse> {
  const response = await apiPut<ApiResponse<PhoneManagementResponse>, Record<string, never>>(
    `/admin/phones/${phoneId}/toggle-disabled`,
    {},
    {
      authToken: token
    }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to toggle phone status');
  }

  return response.data;
}

/**
 * 删除商品
 * DELETE /api/admin/phones/{phoneId}
 */
export async function deleteAdminPhone(
  token: string,
  phoneId: string
): Promise<void> {
  const response = await apiDelete<ApiResponse<null>>(
    `/admin/phones/${phoneId}`,
    {
      authToken: token
    }
  );

  if (!response.success) {
    throw new Error(response.message || 'Failed to delete phone');
  }
}