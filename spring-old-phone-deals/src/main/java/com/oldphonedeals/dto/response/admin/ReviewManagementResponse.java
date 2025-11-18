package com.oldphonedeals.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论管理响应 DTO
 * 用于管理员查看评论列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewManagementResponse {

    /**
     * 评论ID
     */
    private String reviewId;

    /**
     * 商品ID
     */
    private String phoneId;

    /**
     * 商品名称
     */
    private String phoneTitle;

    /**
     * 评论者ID
     */
    private String reviewerId;

    /**
     * 评论者姓名
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