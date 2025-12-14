import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import type { ApiResponse } from '../types/api'
import type { AdminProfileResponse } from '../types/admin'

export type UseAdminAuthOptions = {
  enabled?: boolean
}

export function useAdminAuth(options?: UseAdminAuthOptions) {
  return useQuery<ApiResponse<AdminProfileResponse>, unknown>({
    queryKey: ['admin', 'profile'],
    enabled: options?.enabled ?? true,
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<AdminProfileResponse>>('/admin/profile')
      return data
    },
  })
}
