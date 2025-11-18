/**
 * 用户资料相关类型定义
 */

/**
 * 用户资料响应
 */
export interface UserProfileResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  isVerified: boolean;
  isDisabled: boolean;
  createdAt: string;
  lastLoginAt?: string;
}

/**
 * 更新用户资料请求
 */
export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
}

/**
 * 修改密码请求
 */
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}