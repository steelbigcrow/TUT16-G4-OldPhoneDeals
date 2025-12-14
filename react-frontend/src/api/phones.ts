import { apiClient } from './client'
import type { ApiResponse, PhonesListData } from '../types/api'
import type { PhoneBrand, PhoneListItemResponse, PhoneResponse } from '../types/phone'
import type { ReviewPageResponse, ReviewResponse } from '../types/review'

export type PhoneListQuery = {
  search?: string
  brand?: PhoneBrand
  maxPrice?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
  page?: number
  limit?: number
}

export type PhoneSpecialQuery = {
  special: 'soldOutSoon' | 'bestSellers'
}

export async function getPhones(query: PhoneListQuery) {
  const { data } = await apiClient.get<ApiResponse<PhonesListData<PhoneListItemResponse>>>(
    '/phones',
    {
      params: query,
    },
  )
  return data
}

export async function getSpecialPhones(query: PhoneSpecialQuery) {
  const { data } = await apiClient.get<ApiResponse<PhoneListItemResponse[]>>('/phones', {
    params: query,
  })
  return data
}

export async function getPhoneById(phoneId: string) {
  const { data } = await apiClient.get<ApiResponse<PhoneResponse>>(`/phones/${phoneId}`)
  return data
}

export async function getPhonesBySeller(sellerId: string) {
  const { data } = await apiClient.get<ApiResponse<PhoneResponse[]>>(`/phones/by-seller/${sellerId}`)
  return data
}

export async function getPhoneReviews(phoneId: string, page: number, limit: number) {
  const { data } = await apiClient.get<ApiResponse<ReviewPageResponse>>(
    `/phones/${phoneId}/reviews`,
    {
      params: { page, limit },
    },
  )
  return data
}

export type CreateReviewRequest = {
  rating: number
  comment: string
}

export async function addPhoneReview(phoneId: string, request: CreateReviewRequest) {
  const { data } = await apiClient.post<ApiResponse<ReviewResponse>>(
    `/phones/${phoneId}/reviews`,
    request,
  )
  return data
}

export type CreatePhoneRequest = {
  title: string
  brand: PhoneBrand
  image: string
  stock: number
  price: number
  seller: string
}

export async function createPhone(request: CreatePhoneRequest) {
  const { data } = await apiClient.post<ApiResponse<PhoneResponse>>('/phones', request)
  return data
}

export async function togglePhoneDisabled(phoneId: string, isDisabled: boolean) {
  const { data } = await apiClient.put<ApiResponse<string>>(`/phones/${phoneId}/disable`, {
    isDisabled,
  })
  return data
}
