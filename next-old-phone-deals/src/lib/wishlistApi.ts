/**
 * 心愿单 API 封装
 * 对接 Spring Boot 心愿单端点
 */

import { apiGet, apiPost, apiDelete } from './apiClient';
import type { ApiResponse } from '@/types/auth';
import type { WishlistResponse, AddToWishlistRequest } from '@/types/wishlist';

/**
 * 获取心愿单
 * GET /api/wishlist
 */
export async function getWishlist(token: string): Promise<WishlistResponse> {
  const response = await apiGet<ApiResponse<WishlistResponse>>('/wishlist', {
    authToken: token
  });

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to get wishlist');
  }

  return normalizeWishlistResponse(response.data);
}

/**
 * 添加商品到心愿单
 * POST /api/wishlist
 */
export async function addToWishlist(
  token: string,
  phoneId: string
): Promise<WishlistResponse> {
  const payload: AddToWishlistRequest = { phoneId };
  const response = await apiPost<
    ApiResponse<WishlistResponse>,
    AddToWishlistRequest
  >('/wishlist', payload, { authToken: token });

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to add to wishlist');
  }

  return normalizeWishlistResponse(response.data);
}

/**
 * 从心愿单删除商品
 * DELETE /api/wishlist/{phoneId}
 */
export async function removeFromWishlist(
  token: string,
  phoneId: string
): Promise<WishlistResponse> {
  const response = await apiDelete<ApiResponse<WishlistResponse>>(
    `/wishlist/${phoneId}`,
    { authToken: token }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to remove from wishlist');
  }

  return normalizeWishlistResponse(response.data);
}

function normalizeWishlistResponse(
  data: WishlistResponse
): WishlistResponse {
  return {
    userId: data.userId,
    phones: data.phones ?? [],
    totalItems:
      typeof data.totalItems === 'number'
        ? data.totalItems
        : data.phones?.length ?? 0
  };
}
