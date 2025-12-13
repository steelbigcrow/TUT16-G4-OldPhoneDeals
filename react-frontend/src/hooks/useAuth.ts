import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../api/client'
import type { ApiResponse } from '../types/api'

export type AuthMe = {
  id?: string
  _id?: string
  email?: string
  username?: string
  name?: string
}

export type UseAuthOptions = {
  enabled?: boolean
}

export function useAuth(options?: UseAuthOptions) {
  return useQuery<ApiResponse<AuthMe>, unknown>({
    queryKey: ['auth', 'me'],
    enabled: options?.enabled ?? true,
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<AuthMe>>('/auth/me')
      return data
    },
  })
}