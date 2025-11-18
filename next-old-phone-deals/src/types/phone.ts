/**
 * 手机商品相关类型定义
 */

/**
 * 手机品牌枚举
 */
export enum PhoneBrand {
  APPLE = 'APPLE',
  SAMSUNG = 'SAMSUNG',
  HUAWEI = 'HUAWEI',
  NOKIA = 'NOKIA',
  SONY = 'SONY',
  LG = 'LG',
  HTC = 'HTC',
  MOTOROLA = 'MOTOROLA',
  BLACKBERRY = 'BLACKBERRY'
}

/**
 * 商品列表项响应
 */
export interface PhoneListItemResponse {
  id: string;
  title: string;
  brand: PhoneBrand;
  price: number;
  stock: number;
  imageUrl?: string;
  salesCount: number;
  isDisabled: boolean;
  createdAt: string;
  averageRating: number;
  reviewCount: number;
}

/**
 * 评论响应
 */
export interface ReviewResponse {
  id: string;
  userId: string;
  userName: string;
  rating: number;
  comment: string;
  isHidden: boolean;
  createdAt: string;
}

/**
 * 商品详情响应
 */
export interface PhoneResponse {
  id: string;
  title: string;
  brand: PhoneBrand;
  price: number;
  stock: number;
  description?: string;
  imageUrl?: string;
  salesCount: number;
  isDisabled: boolean;
  sellerId: string;
  sellerName?: string;
  createdAt: string;
  updatedAt?: string;
  averageRating: number;
  reviewCount: number;
  reviews: ReviewResponse[];
}

/**
 * 创建商品请求
 */
export interface PhoneCreateRequest {
  title: string;
  brand: PhoneBrand;
  price: number;
  stock: number;
  description?: string;
  imageUrl?: string;
}

/**
 * 更新商品请求
 */
export interface PhoneUpdateRequest {
  title?: string;
  brand?: PhoneBrand;
  price?: number;
  stock?: number;
  description?: string;
  imageUrl?: string;
  isDisabled?: boolean;
}

/**
 * 商品列表响应（带分页）
 */
export interface PhoneListResponse {
  phones: PhoneListItemResponse[];
  currentPage: number;
  totalPages: number;
  total: number;
}

/**
 * ??? (Node/Express API ??)
 */
export interface CatalogPhone {
  id: string;
  title: string;
  brand: string;
  price: number;
  stock: number;
  imageUrl?: string;
  averageRating?: number;
  reviewCount?: number;
  sellerName?: string;
  salesCount?: number;
  isDisabled?: boolean;
}

export interface CatalogSearchResponse {
  phones: CatalogPhone[];
  totalPages: number;
  currentPage: number;
  total: number;
}

export type SpecialPhoneCategory = 'soldOutSoon' | 'bestSellers';
