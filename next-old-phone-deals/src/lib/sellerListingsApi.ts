/**
 * 卖家挂牌管理 API
 */

import { apiGet, apiPost, apiPut, apiDelete, apiPatch } from './apiClient';
import type { ApiResponse } from '@/types/auth';
import type {
  PhoneResponse,
  PhoneCreateRequest,
  PhoneUpdateRequest
} from '@/types/phone';

/**
 * 获取卖家的所有商品
 * GET /api/phones/by-seller/{sellerId}
 */
export async function getSellerListings(
  sellerId: string,
  token: string
): Promise<PhoneResponse[]> {
  const response = await apiGet<ApiResponse<PhoneResponse[]>>(
    `/phones/by-seller/${sellerId}`,
    { authToken: token }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to load seller listings');
  }

  return response.data;
}

/**
 * 创建新商品
 * POST /api/phones
 */
export async function createPhone(
  data: PhoneCreateRequest,
  token: string
): Promise<PhoneResponse> {
  const response = await apiPost<ApiResponse<PhoneResponse>, PhoneCreateRequest>(
    '/phones',
    data,
    { authToken: token }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to create phone');
  }

  return response.data;
}

/**
 * 更新商品信息
 * PUT /api/phones/{phoneId}
 */
export async function updatePhone(
  phoneId: string,
  data: PhoneUpdateRequest,
  token: string
): Promise<PhoneResponse> {
  const response = await apiPut<ApiResponse<PhoneResponse>, PhoneUpdateRequest>(
    `/phones/${phoneId}`,
    data,
    { authToken: token }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to update phone');
  }

  return response.data;
}

/**
 * 删除商品
 * DELETE /api/phones/{phoneId}
 */
export async function deletePhone(
  phoneId: string,
  token: string
): Promise<void> {
  const response = await apiDelete<ApiResponse<string>>(
    `/phones/${phoneId}`,
    { authToken: token }
  );

  if (!response.success) {
    throw new Error(response.message || 'Failed to delete phone');
  }
}

/**
 * 启用/禁用商品
 * PUT /api/phones/{phoneId}/disable
 * 
 * @param phoneId 商品ID
 * @param isDisabled 是否禁用
 * @param token 认证令牌
 */
export async function togglePhoneDisabled(
  phoneId: string,
  isDisabled: boolean,
  token: string
): Promise<void> {
  const response = await apiPut<ApiResponse<string>, { isDisabled: boolean }>(
    `/phones/${phoneId}/disable`,
    { isDisabled },
    { authToken: token }
  );

  if (!response.success) {
    throw new Error(response.message || 'Failed to update phone status');
  }
}
