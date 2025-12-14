import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cartApi } from '../api'
import { queryKeys } from '../queryKeys'

export function useCart(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: queryKeys.cart,
    queryFn: () => cartApi.getCart(),
    enabled: options?.enabled ?? true,
  })
}

export function useAddToCart() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: cartApi.AddToCartRequest) => cartApi.addToCart(request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.cart })
    },
  })
}

export function useUpdateCartItem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (args: { phoneId: string; quantity: number }) =>
      cartApi.updateCartItem(args.phoneId, { quantity: args.quantity }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.cart })
    },
  })
}

export function useRemoveFromCart() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (phoneId: string) => cartApi.removeFromCart(phoneId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.cart })
    },
  })
}

