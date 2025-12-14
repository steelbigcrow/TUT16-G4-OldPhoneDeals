import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { profileApi } from '../api'
import { queryKeys } from '../queryKeys'

export function useProfile(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: queryKeys.profile,
    queryFn: () => profileApi.getProfile(),
    enabled: options?.enabled ?? true,
  })
}

export function useUpdateProfile() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: profileApi.UpdateProfileRequest) => profileApi.updateProfile(request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.profile })
      await queryClient.invalidateQueries({ queryKey: queryKeys.auth.me })
    },
  })
}

export function useChangePassword() {
  return useMutation({
    mutationFn: (request: profileApi.ChangePasswordRequest) => profileApi.changePassword(request),
  })
}

