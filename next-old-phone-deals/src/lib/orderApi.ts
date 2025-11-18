/**
 * 订单 API 封装
 * 对接 Spring Boot 订单端点
 */

import { apiGet, apiPost } from './apiClient';
import type { ApiResponse } from '@/types/auth';
import type { OrderResponse, CheckoutRequest } from '@/types/order';

/**
 * 创建订单（结账）
 * POST /api/orders/checkout
 */
export async function createOrder(
  token: string,
  payload: CheckoutRequest
): Promise<OrderResponse> {
  const response = await apiPost<ApiResponse<OrderResponse>, CheckoutRequest>(
    '/orders/checkout',
    payload,
    { authToken: token }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to create order');
  }

  return response.data;
}

/**
 * 获取用户订单列表
 * GET /api/orders/user/{userId}
 */
export async function getUserOrders(
  token: string,
  userId: string
): Promise<OrderResponse[]> {
  const response = await apiGet<ApiResponse<OrderResponse[]>>(
    `/orders/user/${userId}`,
    { authToken: token }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to get orders');
  }

  return response.data;
}

/**
 * 获取订单详情
 * GET /api/orders/{orderId}
 */
export async function getOrderById(
  token: string,
  orderId: string
): Promise<OrderResponse> {
  const response = await apiGet<ApiResponse<OrderResponse>>(
    `/orders/${orderId}`,
    { authToken: token }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to get order');
  }

  return response.data;
}