import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { wishlistApi } from '../api'
import { queryKeys } from '../queryKeys'

export function useWishlist(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: queryKeys.wishlist,
    queryFn: () => wishlistApi.getWishlist(),
    enabled: options?.enabled ?? true,
  })
}

export function useAddToWishlist() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (phoneId: string) => wishlistApi.addToWishlist({ phoneId }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.wishlist })
    },
  })
}

export function useRemoveFromWishlist() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (phoneId: string) => wishlistApi.removeFromWishlist(phoneId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.wishlist })
    },
  })
}

