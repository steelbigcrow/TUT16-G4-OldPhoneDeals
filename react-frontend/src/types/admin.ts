import type { IsoDateTimeString, PageResponse } from './api'
import type { PhoneBrand } from './phone'

export type AdminStatsResponse = {
  totalUsers: number | null
  totalListings: number | null
  totalReviews: number | null
  totalSales: number | null
}

export type AdminProfileResponse = {
  id: string
  firstName: string
  lastName: string
  email: string
  role: string
  isVerified: boolean | null
  lastLogin: IsoDateTimeString | null
  createdAt: IsoDateTimeString | null
}

export type UserManagementResponse = {
  id: string
  firstName: string
  lastName: string
  email: string
  role: string
  isVerified: boolean | null
  isDisabled: boolean | null
  isBan: boolean | null
  lastLogin: IsoDateTimeString | null
  createdAt: IsoDateTimeString | null
}

export type UserManagementPageResponse = PageResponse<UserManagementResponse>

export type PhoneManagementSellerInfo = {
  id: string
  firstName: string
  lastName: string
  email: string
}

export type PhoneManagementResponse = {
  id: string
  title: string
  brand: PhoneBrand
  image: string
  price: number | null
  stock: number | null
  isDisabled: boolean | null
  salesCount: number | null
  averageRating: number | null
  reviewCount: number | null
  seller: PhoneManagementSellerInfo | null
  createdAt: IsoDateTimeString | null
  updatedAt: IsoDateTimeString | null
}

export type PhoneManagementPageResponse = PageResponse<PhoneManagementResponse>

export type OrderManagementResponse = {
  id: string
  userId: string
  userName: string
  userEmail: string
  itemCount: number | null
  totalAmount: number | null
  addressSummary: string | null
  createdAt: IsoDateTimeString | null
}

export type OrderManagementPageResponse = PageResponse<OrderManagementResponse>

export type AdminLogResponse = {
  id: string
  adminUserId: string
  adminName: string
  action: string
  targetType: string
  targetId: string
  details: string | null
  createdAt: IsoDateTimeString | null
}

export type AdminLogPageResponse = PageResponse<AdminLogResponse>
