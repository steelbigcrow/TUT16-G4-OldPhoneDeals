import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { PhoneBrand } from '../types/phone'
import { phonesApi } from '../api'
import { queryKeys } from '../queryKeys'

export type PhonesListParams = {
  search?: string
  brand?: PhoneBrand
  maxPrice?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
  page: number
  limit: number
}

export function usePhonesList(params: PhonesListParams) {
  return useQuery({
    queryKey: queryKeys.phones.list(params),
    queryFn: () => phonesApi.getPhones(params),
  })
}

export function useSpecialPhones(special: 'soldOutSoon' | 'bestSellers') {
  return useQuery({
    queryKey: queryKeys.phones.special(special),
    queryFn: () => phonesApi.getSpecialPhones({ special }),
  })
}

export function usePhoneDetail(phoneId: string) {
  return useQuery({
    queryKey: queryKeys.phones.detail(phoneId),
    queryFn: () => phonesApi.getPhoneById(phoneId),
    enabled: Boolean(phoneId),
  })
}

export function usePhoneReviews(phoneId: string, page: number, limit: number) {
  return useQuery({
    queryKey: queryKeys.phones.reviews(phoneId, { page, limit }),
    queryFn: () => phonesApi.getPhoneReviews(phoneId, page, limit),
    enabled: Boolean(phoneId),
  })
}

export function useAddPhoneReview(phoneId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: phonesApi.CreateReviewRequest) =>
      phonesApi.addPhoneReview(phoneId, request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['reviews'] })
      await queryClient.invalidateQueries({ queryKey: ['phones', 'detail', phoneId] })
    },
  })
}

export function useSellerPhones(sellerId: string) {
  return useQuery({
    queryKey: queryKeys.phones.bySeller(sellerId),
    queryFn: () => phonesApi.getPhonesBySeller(sellerId),
    enabled: Boolean(sellerId),
  })
}

export function useCreatePhone() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: phonesApi.CreatePhoneRequest) => phonesApi.createPhone(request),
    onSuccess: async (_res, variables) => {
      await queryClient.invalidateQueries({ queryKey: ['phones'] })
      await queryClient.invalidateQueries({
        queryKey: queryKeys.phones.bySeller(variables.seller),
      })
    },
  })
}

export function useTogglePhoneDisabled(sellerId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (args: { phoneId: string; isDisabled: boolean }) =>
      phonesApi.togglePhoneDisabled(args.phoneId, args.isDisabled),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['phones'] })
      await queryClient.invalidateQueries({ queryKey: queryKeys.phones.bySeller(sellerId) })
    },
  })
}
