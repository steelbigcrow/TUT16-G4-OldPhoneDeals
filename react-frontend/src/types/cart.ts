import type { IsoDateTimeString } from './api'
import type { PhoneBrand } from './phone'

export type CartItemSellerInfo = {
  id: string
  firstName: string
  lastName: string
}

export type CartItemPhoneInfo = {
  id: string
  title: string
  brand: PhoneBrand
  image: string
  stock: number | null
  price: number | null
  isDisabled: boolean | null
}

export type CartItemResponse = {
  phoneId: string
  title: string
  quantity: number | null
  price: number | null
  averageRating: number | null
  reviewCount: number | null
  seller: CartItemSellerInfo | null
  phone: CartItemPhoneInfo | null
  createdAt: IsoDateTimeString | null
}

export type CartResponse = {
  id: string
  userId: string
  items: CartItemResponse[]
  createdAt: IsoDateTimeString | null
  updatedAt: IsoDateTimeString | null
}

