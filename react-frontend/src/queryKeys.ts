import type { PhoneBrand } from './types/phone'

export const queryKeys = {
  auth: {
    me: ['auth', 'me'] as const,
  },
  admin: {
    profile: ['admin', 'profile'] as const,
    stats: ['admin', 'stats'] as const,
    users: (params: {
      pageIndex: number
      pageSize: number
      search?: string
      isDisabled?: boolean
    }) => ['admin', 'users', params] as const,
    phones: (params: { pageIndex: number; pageSize: number }) =>
      ['admin', 'phones', params] as const,
    reviews: (params: {
      pageIndex: number
      pageSize: number
      visibility?: boolean
      reviewerId?: string
      phoneId?: string
      search?: string
    }) => ['admin', 'reviews', params] as const,
    orders: (params: {
      pageIndex: number
      pageSize: number
      userId?: string
      startDate?: string
      endDate?: string
      searchTerm?: string
      brandFilter?: string
      sortBy?: string
      sortOrder?: 'asc' | 'desc'
    }) => ['admin', 'orders', params] as const,
    logs: (params: { pageIndex: number; pageSize: number }) =>
      ['admin', 'logs', params] as const,
  },
  phones: {
    list: (params: {
      search?: string
      brand?: PhoneBrand
      page: number
      limit: number
      sortBy?: string
      sortOrder?: 'asc' | 'desc'
      maxPrice?: number
    }) => ['phones', params] as const,
    special: (special: 'soldOutSoon' | 'bestSellers') =>
      ['phones', 'special', special] as const,
    detail: (phoneId: string) => ['phones', 'detail', phoneId] as const,
    reviews: (phoneId: string, params: { page: number; limit: number }) =>
      ['reviews', phoneId, params] as const,
    bySeller: (sellerId: string) => ['phones', 'seller', sellerId] as const,
  },
  wishlist: ['wishlist'] as const,
  cart: ['cart'] as const,
  profile: ['profile'] as const,
  orders: (params: { page: number; pageSize: number }) => ['orders', params] as const,
} as const
