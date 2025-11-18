/**
 * 认证相关类型定义
 */

// Spring Boot API 响应包装
export type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

// 用户信息
export type User = {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  isVerified: boolean;
  isDisabled: boolean;
  createdAt: string;
  lastLoginAt?: string;
};

// 管理员信息
export type Admin = {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  createdAt: string;
  lastLoginAt?: string;
};

// 登录响应
export type LoginResponse = {
  token: string;
  user?: User;
  admin?: Admin;
};

// 登录请求
export type LoginRequest = {
  email: string;
  password: string;
};

// 注册请求
export type RegisterRequest = {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
};

// 邮箱验证请求
export type VerifyEmailRequest = {
  email: string;
  token: string;
};

// 重置密码请求
export type ResetPasswordRequest = {
  email: string;
  code?: string;
  newPassword: string;
};