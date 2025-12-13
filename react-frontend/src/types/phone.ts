import type { IsoDateTimeString } from './api';
import type { ReviewResponse } from './review';

export type PhoneBrand =
  | 'SAMSUNG'
  | 'APPLE'
  | 'HTC'
  | 'HUAWEI'
  | 'NOKIA'
  | 'LG'
  | 'MOTOROLA'
  | 'SONY'
  | 'BLACKBERRY';

export type PhoneListItemSellerInfo = {
  firstName: string;
  lastName: string;
};

export type PhoneListItemResponse = {
  id: string;
  title: string;
  brand: PhoneBrand;
  image: string;
  stock: number | null;
  price: number | null;
  averageRating: number | null;
  reviewCount: number | null;
  seller: PhoneListItemSellerInfo | null;
  createdAt: IsoDateTimeString | null;
};

export type PhoneSellerInfo = {
  id: string;
  firstName: string;
  lastName: string;
};

export type PhoneResponse = {
  id: string;
  title: string;
  brand: PhoneBrand;
  image: string;
  stock: number | null;
  price: number | null;
  isDisabled: boolean | null;
  salesCount: number | null;
  averageRating: number | null;
  seller: PhoneSellerInfo | null;
  reviews: ReviewResponse[];
  createdAt: IsoDateTimeString | null;
  updatedAt: IsoDateTimeString | null;
};