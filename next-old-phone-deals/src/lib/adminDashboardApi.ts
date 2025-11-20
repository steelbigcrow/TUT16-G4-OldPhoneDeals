/**
 * 管理员仪表盘 API
 */

import { apiGet } from './apiClient';
import type { ApiResponse, DashboardStatsResponse } from '@/types/admin';

/**
 * 获取仪表盘统计数据
 * GET /api/admin/stats
 */
export async function getDashboardStats(
  token: string
): Promise<DashboardStatsResponse> {
  const response = await apiGet<ApiResponse<DashboardStatsResponse>>(
    '/admin/stats',
    {
      authToken: token
    }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to fetch dashboard stats');
  }

  return response.data;
}
