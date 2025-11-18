/**
 * 管理员评论管理 API
 */

import { apiGet, apiPut, apiDelete } from './apiClient';
import type {
  ApiResponse,
  PageResponse,
  ReviewManagementResponse
} from '@/types/admin';

/**
 * 获取所有评论（包含隐藏的）
 * GET /api/admin/reviews?page=0&pageSize=10
 */
export async function getAdminReviews(
  token: string,
  params: {
    page?: number;
    pageSize?: number;
  } = {}
): Promise<PageResponse<ReviewManagementResponse>> {
  const { page = 0, pageSize = 10 } = params;
  const queryParams = new URLSearchParams({
    page: page.toString(),
    pageSize: pageSize.toString()
  });

  const response = await apiGet<ApiResponse<PageResponse<ReviewManagementResponse>>>(
    `/admin/reviews?${queryParams.toString()}`,
    {
      authToken: token
    }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to fetch reviews');
  }

  return response.data;
}

/**
 * 切换评论可见性
 * PUT /api/admin/reviews/{phoneId}/{reviewId}/toggle-visibility
 */
export async function toggleReviewVisibility(
  token: string,
  phoneId: string,
  reviewId: string
): Promise<ReviewManagementResponse> {
  const response = await apiPut<ApiResponse<ReviewManagementResponse>, Record<string, never>>(
    `/admin/reviews/${phoneId}/${reviewId}/toggle-visibility`,
    {},
    {
      authToken: token
    }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to toggle review visibility');
  }

  return response.data;
}

/**
 * 删除评论
 * DELETE /api/admin/reviews/{phoneId}/{reviewId}
 */
export async function deleteReview(
  token: string,
  phoneId: string,
  reviewId: string
): Promise<void> {
  const response = await apiDelete<ApiResponse<null>>(
    `/admin/reviews/${phoneId}/${reviewId}`,
    {
      authToken: token
    }
  );

  if (!response.success) {
    throw new Error(response.message || 'Failed to delete review');
  }
}