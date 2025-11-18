/**
 * 管理员相关类型定义
 * 对应 Spring Boot Admin API 的响应结构
 */

import { PhoneBrand } from './phone';

/**
 * 分页响应基础结构
 */
export interface PageResponse<T> {
  content: T[];
  currentPage: number;
  totalPages: number;
  total: number;
  pageSize: number;
}

/**
 * 管理员资料响应
 */
export interface AdminProfileResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  createdAt: string;
  lastLoginAt?: string;
}

/**
 * 仪表盘统计响应
 */
export interface DashboardStatsResponse {
  totalUsers: number;
  totalListings: number;
  totalReviews: number;
  totalOrders: number;
  totalRevenue: number;
  recentSales?: SalesSummary[];
}

/**
 * 销售摘要
 */
export interface SalesSummary {
  date: string;
  count: number;
  revenue: number;
}

/**
 * 用户管理响应
 */
export interface UserManagementResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  isEmailVerified: boolean;
  isDisabled: boolean;
  createdAt: string;
  lastLoginAt?: string;
}

/**
 * 用户详情响应（包含统计信息）
 */
export interface UserDetailResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  isEmailVerified: boolean;
  isDisabled: boolean;
  createdAt: string;
  lastLoginAt?: string;
  listingsCount: number;
  reviewsCount: number;
  ordersCount: number;
}

/**
 * 更新用户请求
 */
export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  isDisabled?: boolean;
}

/**
 * 商品管理响应
 */
export interface PhoneManagementResponse {
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
  sellerEmail: string;
  sellerName: string;
  createdAt: string;
  updatedAt?: string;
  reviewCount: number;
  averageRating: number;
}

/**
 * 更新商品请求（管理员）
 */
export interface UpdatePhoneRequest {
  title?: string;
  brand?: PhoneBrand;
  price?: number;
  stock?: number;
  description?: string;
  imageUrl?: string;
  isDisabled?: boolean;
}

/**
 * 评论管理响应
 */
export interface ReviewManagementResponse {
  id: string;
  phoneId: string;
  phoneTitle: string;
  userId: string;
  userName: string;
  userEmail: string;
  rating: number;
  comment: string;
  isHidden: boolean;
  createdAt: string;
}

/**
 * 订单管理响应（列表项）
 */
export interface OrderManagementResponse {
  id: string;
  orderNumber: string;
  userId: string;
  userEmail: string;
  userName: string;
  totalAmount: number;
  itemCount: number;
  createdAt: string;
}

/**
 * 订单详情响应
 */
export interface OrderDetailResponse {
  id: string;
  orderNumber: string;
  userId: string;
  userEmail: string;
  userName: string;
  totalAmount: number;
  createdAt: string;
  items: OrderItemResponse[];
}

/**
 * 订单项响应
 */
export interface OrderItemResponse {
  phoneId: string;
  phoneTitle: string;
  quantity: number;
  priceAtPurchase: number;
  subtotal: number;
}

/**
 * 管理日志响应
 */
export interface AdminLogResponse {
  id: string;
  adminId: string;
  adminEmail: string;
  action: AdminAction;
  targetType: TargetType;
  targetId: string;
  details?: string;
  createdAt: string;
}

/**
 * 管理员操作类型枚举
 */
export enum AdminAction {
  CREATE_USER = 'CREATE_USER',
  UPDATE_USER = 'UPDATE_USER',
  DELETE_USER = 'DELETE_USER',
  TOGGLE_USER_STATUS = 'TOGGLE_USER_STATUS',
  CREATE_PHONE = 'CREATE_PHONE',
  UPDATE_PHONE = 'UPDATE_PHONE',
  DELETE_PHONE = 'DELETE_PHONE',
  TOGGLE_PHONE_STATUS = 'TOGGLE_PHONE_STATUS',
  HIDE_REVIEW = 'HIDE_REVIEW',
  DELETE_REVIEW = 'DELETE_REVIEW',
  VIEW_ORDER = 'VIEW_ORDER'
}

/**
 * 目标类型枚举
 */
export enum TargetType {
  USER = 'USER',
  PHONE = 'PHONE',
  REVIEW = 'REVIEW',
  ORDER = 'ORDER'
}

/**
 * API 通用响应结构
 */
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}