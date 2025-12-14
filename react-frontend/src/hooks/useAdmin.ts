import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi } from '../api'
import { queryKeys } from '../queryKeys'

export function useAdminStats(options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: queryKeys.admin.stats,
    queryFn: () => adminApi.getAdminStats(),
    enabled: options?.enabled ?? true,
  })
}

export function useAdminUsers(params: {
  pageIndex: number
  pageSize: number
  search?: string
  isDisabled?: boolean
}) {
  return useQuery({
    queryKey: queryKeys.admin.users(params),
    queryFn: () =>
      adminApi.getAdminUsers({
        page: params.pageIndex,
        pageSize: params.pageSize,
        search: params.search,
        isDisabled: params.isDisabled,
      }),
  })
}

export function useToggleAdminUserDisabled() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (userId: string) => adminApi.toggleAdminUserDisabled(userId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
    },
  })
}

export function useAdminPhones(params: { pageIndex: number; pageSize: number }) {
  return useQuery({
    queryKey: queryKeys.admin.phones(params),
    queryFn: () =>
      adminApi.getAdminPhones({
        page: params.pageIndex,
        pageSize: params.pageSize,
      }),
  })
}

export function useToggleAdminPhoneDisabled() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (phoneId: string) => adminApi.toggleAdminPhoneDisabled(phoneId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'phones'] })
    },
  })
}

export function useUpdateAdminPhone() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (args: { phoneId: string; request: adminApi.UpdateAdminPhoneRequest }) =>
      adminApi.updateAdminPhone(args.phoneId, args.request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'phones'] })
    },
  })
}

export function useAdminReviews(params: {
  pageIndex: number
  pageSize: number
  visibility?: boolean
  reviewerId?: string
  phoneId?: string
  search?: string
}) {
  return useQuery({
    queryKey: queryKeys.admin.reviews(params),
    queryFn: () =>
      adminApi.getAdminReviews({
        page: params.pageIndex,
        pageSize: params.pageSize,
        visibility: params.visibility,
        reviewerId: params.reviewerId,
        phoneId: params.phoneId,
        search: params.search,
      }),
  })
}

export function useToggleAdminReviewVisibility() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (args: { phoneId: string; reviewId: string }) =>
      adminApi.toggleAdminReviewVisibility(args.phoneId, args.reviewId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'reviews'] })
    },
  })
}

export function useDeleteAdminReview() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (args: { phoneId: string; reviewId: string }) =>
      adminApi.deleteAdminReview(args.phoneId, args.reviewId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin', 'reviews'] })
    },
  })
}

export function useAdminOrders(params: {
  pageIndex: number
  pageSize: number
  userId?: string
  startDate?: string
  endDate?: string
  searchTerm?: string
  brandFilter?: string
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}) {
  return useQuery({
    queryKey: queryKeys.admin.orders(params),
    queryFn: () =>
      adminApi.getAdminOrders({
        page: params.pageIndex,
        pageSize: params.pageSize,
        userId: params.userId,
        startDate: params.startDate,
        endDate: params.endDate,
        searchTerm: params.searchTerm,
        brandFilter: params.brandFilter,
        sortBy: params.sortBy,
        sortOrder: params.sortOrder,
      }),
  })
}

export function useAdminLogs(params: { pageIndex: number; pageSize: number }) {
  return useQuery({
    queryKey: queryKeys.admin.logs(params),
    queryFn: () =>
      adminApi.getAdminLogs({
        page: params.pageIndex,
        pageSize: params.pageSize,
      }),
  })
}
