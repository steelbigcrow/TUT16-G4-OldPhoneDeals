import type { PhoneBrand } from './phone';

/**
 * 购物车商品中的卖家信息
 */
export interface CartItemSeller {
  id?: string;
  firstName?: string;
  lastName?: string;
}

/**
 * 购物车商品的简化信息
 */
export interface CartItemPhone {
  id: string;
  title: string;
  brand: PhoneBrand | string;
  price: number;
  stock: number;
  image?: string;
  isDisabled?: boolean;
}

/**
 * 购物车商品项
 */
export interface CartItem {
  phoneId: string;
  title: string;
  quantity: number;
  price: number;
  averageRating?: number;
  reviewCount?: number;
  seller?: CartItemSeller;
  phone?: CartItemPhone;
  createdAt?: string;
}

/**
 * 购物车响应
 */
export interface CartResponse {
  id: string;
  userId: string;
  items: CartItem[];
  createdAt?: string;
  updatedAt?: string;
}

/**
 * 添加到购物车请求
 */
export interface AddToCartRequest {
  phoneId: string;
  quantity: number;
}

/**
 * 更新购物车商品数量请求
 */
export interface UpdateCartItemRequest {
  quantity: number;
}
