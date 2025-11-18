/**
 * 管理员订单管理 API
 */

import { apiGet } from './apiClient';
import type {
  ApiResponse,
  PageResponse,
  OrderManagementResponse,
  OrderDetailResponse
} from '@/types/admin';

/**
 * 获取所有订单（分页）
 * GET /api/admin/orders?page=0&pageSize=10
 */
export async function getAdminOrders(
  token: string,
  params: {
    page?: number;
    pageSize?: number;
  } = {}
): Promise<PageResponse<OrderManagementResponse>> {
  const { page = 0, pageSize = 10 } = params;
  const queryParams = new URLSearchParams({
    page: page.toString(),
    pageSize: pageSize.toString()
  });

  const response = await apiGet<ApiResponse<PageResponse<OrderManagementResponse>>>(
    `/admin/orders?${queryParams.toString()}`,
    {
      authToken: token
    }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to fetch orders');
  }

  return response.data;
}

/**
 * 获取订单详情
 * GET /api/admin/orders/{orderId}
 */
export async function getOrderDetail(
  token: string,
  orderId: string
): Promise<OrderDetailResponse> {
  const response = await apiGet<ApiResponse<OrderDetailResponse>>(
    `/admin/orders/${orderId}`,
    {
      authToken: token
    }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to fetch order detail');
  }

  return response.data;
}