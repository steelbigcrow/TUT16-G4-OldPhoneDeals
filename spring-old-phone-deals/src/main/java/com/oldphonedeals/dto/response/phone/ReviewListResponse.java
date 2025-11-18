package com.oldphonedeals.dto.response.phone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评论列表响应 DTO
 * 用于分页返回评论列表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListResponse {
    
    /**
     * 评论列表
     */
    private List<ReviewResponse> reviews;
    
    /**
     * 总评论数
     */
    private Integer totalReviews;
    
    /**
     * 当前页码
     */
    private Integer currentPage;
    
    /**
     * 总页数
     */
    private Integer totalPages;
}