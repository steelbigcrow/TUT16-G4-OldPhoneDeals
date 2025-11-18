package com.oldphonedeals.dto.response.phone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评论分页响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewPageResponse {

  /**
   * 当前页的评论列表
   */
  private List<ReviewResponse> reviews;

  /**
   * 评论总数
   */
  private long totalReviews;

  /**
   * 当前页码（从 1 开始）
   */
  private int currentPage;

  /**
   * 总页数
   */
  private int totalPages;
}

