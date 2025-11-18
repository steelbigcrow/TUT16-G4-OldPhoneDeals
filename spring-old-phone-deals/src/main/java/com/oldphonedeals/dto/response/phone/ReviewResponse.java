package com.oldphonedeals.dto.response.phone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    
    /**
     * 评论ID
     */
    private String id;
    
    /**
     * 评论者ID
     */
    private String reviewerId;
    
    /**
     * 评分 (1-5)
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
     * 评论者姓名
     */
    private String reviewer;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}