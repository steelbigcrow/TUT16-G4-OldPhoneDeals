/**
 * 管理员日志 API
 */

import { apiGet } from './apiClient';
import type {
  ApiResponse,
  PageResponse,
  AdminLogResponse
} from '@/types/admin';

/**
 * 获取所有操作日志（分页）
 * GET /api/admin/logs?page=0&pageSize=10
 */
export async function getAdminLogs(
  token: string,
  params: {
    page?: number;
    pageSize?: number;
  } = {}
): Promise<PageResponse<AdminLogResponse>> {
  const { page = 0, pageSize = 10 } = params;
  const queryParams = new URLSearchParams({
    page: page.toString(),
    pageSize: pageSize.toString()
  });

  const response = await apiGet<ApiResponse<PageResponse<AdminLogResponse>>>(
    `/admin/logs?${queryParams.toString()}`,
    {
      authToken: token
    }
  );

  if (!response.success || !response.data) {
    throw new Error(response.message || 'Failed to fetch admin logs');
  }

  return response.data;
}