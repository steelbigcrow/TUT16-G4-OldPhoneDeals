package com.oldphonedeals.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员统计数据响应 DTO
 * 用于显示管理员仪表板统计信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsResponse {

    /**
     * 用户总数（不包括管理员）
     */
    private Long totalUsers;

    /**
     * 商品总数
     */
    private Long totalListings;

    /**
     * 评论总数
     */
    private Long totalReviews;

    /**
     * 订单总数
     */
    private Long totalSales;
}