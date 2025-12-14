import { apiClient } from './client'
import type { ApiResponse, OrderPageResponse } from '../types/api'
import type { OrderAddressInfo, OrderResponse } from '../types/order'

export type CheckoutRequest = {
  address: OrderAddressInfo
}

export async function checkout(request: CheckoutRequest) {
  const { data } = await apiClient.post<ApiResponse<OrderResponse>>('/orders/checkout', request)
  return data
}

export async function getOrders(page: number, pageSize: number) {
  const { data } = await apiClient.get<ApiResponse<OrderPageResponse<OrderResponse>>>('/orders', {
    params: { page, pageSize },
  })
  return data
}

