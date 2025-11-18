/**
 * 评论相关 API
 */

import { apiGet } from './apiClient';
import type { ApiResponse } from '@/types/auth';
import type { SellerReviewResponse } from '@/types/review';

/**
 * 获取卖家收到的所有评论
 * GET /api/phones/reviews/by-seller
 * 
 * sellerId 从 JWT token 中自动解析
 */
export async function getReviewsBySeller(
  token: string
): Promise<SellerReviewResponse[]> {
  const response = await apiGet<ApiResponse<SellerReviewResponse[]>>(
    '/phones/reviews/by-seller',
    { authToken: token }
  );
  return response.data;
}