import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { CartResponse } from '../types/cart'

export type AddToCartRequest = {
  phoneId: string
  quantity: number
}

export type UpdateCartItemRequest = {
  quantity: number
}

export async function getCart() {
  const { data } = await apiClient.get<ApiResponse<CartResponse>>('/cart')
  return data
}

export async function addToCart(request: AddToCartRequest) {
  const { data } = await apiClient.post<ApiResponse<CartResponse>>('/cart', request)
  return data
}

export async function updateCartItem(phoneId: string, request: UpdateCartItemRequest) {
  const { data } = await apiClient.put<ApiResponse<CartResponse>>(`/cart/${phoneId}`, request)
  return data
}

export async function removeFromCart(phoneId: string) {
  const { data } = await apiClient.delete<ApiResponse<CartResponse>>(`/cart/${phoneId}`)
  return data
}

