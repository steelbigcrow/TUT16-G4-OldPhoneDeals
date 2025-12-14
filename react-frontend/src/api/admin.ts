import { apiClient } from './client'
import type { ApiResponse, PageResponse } from '../types/api'
import type { LoginResponse } from '../types/user'
import type {
  AdminProfileResponse,
  AdminStatsResponse,
  AdminLogResponse,
  OrderManagementResponse,
  PhoneManagementResponse,
  UserManagementResponse,
} from '../types/admin'
import type { PhoneBrand } from '../types/phone'
import type { ReviewManagementResponse } from '../types/review'

export type AdminLoginRequest = {
  email: string
  password: string
}

export async function adminLogin(request: AdminLoginRequest) {
  const { data } = await apiClient.post<ApiResponse<LoginResponse>>('/admin/login', request)
  return data
}

export async function getAdminProfile() {
  const { data } = await apiClient.get<ApiResponse<AdminProfileResponse>>('/admin/profile')
  return data
}

export async function getAdminStats() {
  const { data } = await apiClient.get<ApiResponse<AdminStatsResponse>>('/admin/stats')
  return data
}

export async function getAdminUsers(params: {
  page: number
  pageSize: number
  search?: string
  isDisabled?: boolean
}) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<UserManagementResponse>>>(
    '/admin/users',
    { params },
  )
  return data
}

export async function toggleAdminUserDisabled(userId: string) {
  const { data } = await apiClient.put<ApiResponse<UserManagementResponse>>(
    `/admin/users/${userId}/toggle-disabled`,
  )
  return data
}

export async function getAdminPhones(params: { page: number; pageSize: number }) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<PhoneManagementResponse>>>(
    '/admin/phones',
    { params },
  )
  return data
}

export type UpdateAdminPhoneRequest = {
  title: string
  brand: PhoneBrand
  price: number
  stock: number
  isDisabled: boolean
}

export async function updateAdminPhone(phoneId: string, request: UpdateAdminPhoneRequest) {
  const { data } = await apiClient.put<ApiResponse<PhoneManagementResponse>>(
    `/admin/phones/${phoneId}`,
    request,
  )
  return data
}

export async function toggleAdminPhoneDisabled(phoneId: string) {
  const { data } = await apiClient.put<ApiResponse<PhoneManagementResponse>>(
    `/admin/phones/${phoneId}/toggle-disabled`,
  )
  return data
}

export async function getAdminReviews(params: {
  page: number
  pageSize: number
  visibility?: boolean
  reviewerId?: string
  phoneId?: string
  search?: string
}) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<ReviewManagementResponse>>>(
    '/admin/reviews',
    { params },
  )
  return data
}

export async function toggleAdminReviewVisibility(phoneId: string, reviewId: string) {
  const { data } = await apiClient.put<ApiResponse<ReviewManagementResponse>>(
    `/admin/reviews/${phoneId}/${reviewId}/toggle-visibility`,
  )
  return data
}

export async function deleteAdminReview(phoneId: string, reviewId: string) {
  const { data } = await apiClient.delete<ApiResponse<void>>(`/admin/reviews/${phoneId}/${reviewId}`)
  return data
}

export async function getAdminOrders(params: {
  page: number
  pageSize: number
  userId?: string
  startDate?: string
  endDate?: string
  searchTerm?: string
  brandFilter?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<OrderManagementResponse>>>(
    '/admin/orders',
    { params },
  )
  return data
}

export async function getAdminLogs(params: { page: number; pageSize: number }) {
  const { data } = await apiClient.get<ApiResponse<PageResponse<AdminLogResponse>>>('/admin/logs', {
    params,
  })
  return data
}
