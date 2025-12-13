import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import type { ApiResponse } from '../types/api'

export type AdminProfile = {
  id?: string
  _id?: string
  email?: string
  username?: string
  name?: string
  role?: string
}

export type UseAdminAuthOptions = {
  enabled?: boolean
}

export function useAdminAuth(options?: UseAdminAuthOptions) {
  return useQuery<ApiResponse<AdminProfile>, unknown>({
    queryKey: ['admin', 'profile'],
    enabled: options?.enabled ?? true,
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<AdminProfile>>('/admin/profile')
      return data
    },
  })
}