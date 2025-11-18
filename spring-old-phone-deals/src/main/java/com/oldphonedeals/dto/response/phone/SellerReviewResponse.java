package com.oldphonedeals.dto.response.phone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 卖家查看自己所有商品收到的评论 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerReviewResponse {

  /**
   * 评论 ID
   */
  private String reviewId;

  /**
   * 手机 ID
   */
  private String phoneId;

  /**
   * 手机标题
   */
  private String phoneTitle;

  /**
   * 评论人 ID
   */
  private String reviewerId;

  /**
   * 评论人姓名
   */
  private String reviewerName;

  /**
   * 评分（1-5）
   */
  private Integer rating;

  /**
   * 评论内容
   */
  private String comment;

  /**
   * 是否隐藏
   */
  private Boolean isHidden;

  /**
   * 创建时间
   */
  private LocalDateTime createdAt;
}

