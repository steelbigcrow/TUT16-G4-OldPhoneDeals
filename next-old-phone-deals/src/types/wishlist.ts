import type { PhoneBrand } from './phone';

/**
 * 心愿单中的商品信息
 */
export interface WishlistPhone {
  id: string;
  title: string;
  brand: PhoneBrand | string;
  price: number;
  stock: number;
  image?: string;
  isDisabled?: boolean;
  averageRating?: number;
  reviewCount?: number;
  seller?: {
    firstName?: string;
    lastName?: string;
  };
}

/**
 * 心愿单响应
 */
export interface WishlistResponse {
  userId: string;
  phones: WishlistPhone[];
  totalItems: number;
}

/**
 * 添加到心愿单请求
 */
export interface AddToWishlistRequest {
  phoneId: string;
}
