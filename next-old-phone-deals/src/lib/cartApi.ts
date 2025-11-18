/**
 * 购物车 API 封装
 * 对接 Spring Boot 购物车端点
 */

import { apiGet, apiPost, apiPut, apiDelete } from './apiClient';
import type { ApiResponse } from '@/types/auth';
import type {
  CartResponse,
  AddToCartRequest,
  UpdateCartItemRequest
} from '@/types/cart';

/**
 * 获取购物车
 * GET /api/cart
 */
export async function getCart(token: string): Promise<CartResponse> {
  const response = await apiGet<ApiResponse<CartResponse>>('/cart', {
    authToken: token
  });

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to get cart');
  }

  return response.data;
}

/**
 * 添加商品到购物车
 * POST /api/cart
 */
export async function addToCart(
  token: string,
  payload: AddToCartRequest
): Promise<CartResponse> {
  const response = await apiPost<ApiResponse<CartResponse>, AddToCartRequest>(
    '/cart',
    payload,
    { authToken: token }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to add to cart');
  }

  return response.data;
}

/**
 * 更新购物车商品数量
 * PUT /api/cart/{phoneId}
 */
export async function updateCartItem(
  token: string,
  phoneId: string,
  quantity: number
): Promise<CartResponse> {
  const payload: UpdateCartItemRequest = { quantity };
  const response = await apiPut<
    ApiResponse<CartResponse>,
    UpdateCartItemRequest
  >(`/cart/${phoneId}`, payload, { authToken: token });

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to update cart item');
  }

  return response.data;
}

/**
 * 从购物车删除商品
 * DELETE /api/cart/{phoneId}
 */
export async function removeCartItem(
  token: string,
  phoneId: string
): Promise<CartResponse> {
  const response = await apiDelete<ApiResponse<CartResponse>>(
    `/cart/${phoneId}`,
    { authToken: token }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to remove cart item');
  }

  return response.data;
}