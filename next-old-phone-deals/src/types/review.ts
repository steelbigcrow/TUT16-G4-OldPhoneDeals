/**
 * 评论相关类型定义
 */

/**
 * 卖家收到的评论响应
 */
export interface SellerReviewResponse {
  id: string;
  userId: string;
  userName: string;
  rating: number;
  comment: string;
  isHidden: boolean;
  createdAt: string;
  phoneId: string;
  phoneTitle: string;
}

/**
 * 创建评论请求
 */
export interface ReviewCreateRequest {
  rating: number;
  comment: string;
}

/**
 * 切换评论可见性请求
 */
export interface ToggleReviewVisibilityRequest {
  isHidden: boolean;
}