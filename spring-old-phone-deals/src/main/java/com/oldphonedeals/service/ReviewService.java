package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.phone.ReviewCreateRequest;
import com.oldphonedeals.dto.response.phone.ReviewResponse;
import com.oldphonedeals.dto.response.phone.SellerReviewResponse;
import com.oldphonedeals.entity.Phone;

import java.util.List;

/**
 * 评论业务服务接口
 * 处理商品评论的创建、更新、删除和可见性控制
 * 
 * 参考 Express.js 实现：
 * - server/app/controllers/phone.controller.js:236-480
 */
public interface ReviewService {

  /**
   * 添加评论
   * 
   * 业务规则：
   * 1. 用户必须已购买该商品才能评论（查询Order集合验证）
   * 2. 用户不能评论自己的商品
   * 3. 每个用户对每个商品只能评论一次
   * 
   * 参考：server/app/controllers/phone.controller.js:318-398
   * 
   * @param phoneId 商品ID
   * @param request 评论请求
   * @param userId 用户ID（从JWT获取）
   * @return 新创建的评论信息
   * @throws ResourceNotFoundException 商品不存在
   * @throws BadRequestException 违反业务规则（未购买、重复评论等）
   */
  ReviewResponse addReview(String phoneId, ReviewCreateRequest request, String userId);

  /**
   * 切换评论可见性（隐藏/显示）
   * 
   * 权限规则：
   * - 评论作者可以隐藏/显示自己的评论
   * - 商品卖家可以隐藏/显示其商品的任何评论
   * 
   * 参考：server/app/controllers/phone.controller.js:404-480
   * 
   * @param phoneId 商品ID
   * @param reviewId 评论ID
   * @param isHidden 是否隐藏
   * @param userId 用户ID（从JWT获取）
   * @return 更新后的评论信息
   * @throws ResourceNotFoundException 商品或评论不存在
   * @throws UnauthorizedException 用户无权限操作
   */
  ReviewResponse toggleReviewVisibility(String phoneId, String reviewId, Boolean isHidden, String userId);

  /**
   * 删除评论
   * 
   * 权限规则：
   * - 只有评论作者可以删除自己的评论
   * 
   * 参考：Express.js中没有单独的删除评论端点，但逻辑类似toggleReviewVisibility
   * 
   * @param phoneId 商品ID
   * @param reviewId 评论ID
   * @param userId 用户ID（从JWT获取）
   * @throws ResourceNotFoundException 商品或评论不存在
   * @throws UnauthorizedException 用户无权限操作
   */
  void deleteReview(String phoneId, String reviewId, String userId);

  /**
   * 过滤评论可见性
   * 
   * 可见性规则：
   * - 未登录用户：只能看到未隐藏的评论（isHidden = false）
   * - 已登录用户：
   *   1. 所有未隐藏的评论
   *   2. 自己的隐藏评论
   *   3. 作为卖家时，自己商品的所有隐藏评论
   * 
   * 参考：server/app/controllers/phone.controller.js:162-177
   * 
   * @param reviews 原始评论列表
   * @param currentUserId 当前用户ID（可为null表示未登录）
   * @param sellerId 商品卖家ID
   * @return 过滤后的可见评论列表
   */
  List<ReviewResponse> filterVisibleReviews(List<Phone.Review> reviews, String currentUserId, String sellerId);

  /**
   * 获取某个卖家所有商品收到的评论
   *
   * 对应 Express.js 中 seller 查看自己所有手机评论的接口：
   * server/app/controllers/phone.controller.js:getPhonesReviewsByUserID
   *
   * @param sellerId 卖家用户 ID
   * @return 该卖家所有商品的评论列表
   */
  List<SellerReviewResponse> getReviewsBySeller(String sellerId);
}
