/**
 * 订单相关类型定义
 */

/**
 * 订单商品项
 */
export interface OrderItem {
  phoneId: string;
  title: string;
  quantity: number;
  price: number;
}

/**
 * 前端使用的收货地址（表单）
 */
export interface ShippingAddress {
  fullName: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  postalCode: string;
  country: string;
  phone?: string;
}

/**
 * 服务器返回的订单地址
 */
export interface OrderAddress {
  street: string;
  city: string;
  state: string;
  zip: string;
  country: string;
}

/**
 * 订单响应
 */
export interface OrderResponse {
  id: string;
  userId: string;
  items: OrderItem[];
  totalAmount: number;
  address?: OrderAddress;
  createdAt?: string;
}

/**
 * 结账请求（创建订单）
 */
export interface CheckoutRequest {
  address: OrderAddress;
}
