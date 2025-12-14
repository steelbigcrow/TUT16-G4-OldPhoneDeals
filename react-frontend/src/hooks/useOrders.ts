import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ordersApi } from '../api'
import { queryKeys } from '../queryKeys'

export function useOrders(params: { page: number; pageSize: number }, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: queryKeys.orders(params),
    queryFn: () => ordersApi.getOrders(params.page, params.pageSize),
    enabled: options?.enabled ?? true,
  })
}

export function useCheckout() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: ordersApi.CheckoutRequest) => ordersApi.checkout(request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.cart })
      await queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
  })
}

