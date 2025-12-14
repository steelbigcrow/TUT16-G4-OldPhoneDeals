import type { PhoneListItemResponse } from './phone'

export type WishlistResponse = {
  userId: string
  phones: PhoneListItemResponse[]
  totalItems: number | null
}

