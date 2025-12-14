import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { WishlistResponse } from '../types/wishlist'

export type AddToWishlistRequest = {
  phoneId: string
}

export async function getWishlist() {
  const { data } = await apiClient.get<ApiResponse<WishlistResponse>>('/wishlist')
  return data
}

export async function addToWishlist(request: AddToWishlistRequest) {
  const { data } = await apiClient.post<ApiResponse<WishlistResponse>>('/wishlist', request)
  return data
}

export async function removeFromWishlist(phoneId: string) {
  const { data } = await apiClient.delete<ApiResponse<WishlistResponse>>(`/wishlist/${phoneId}`)
  return data
}

