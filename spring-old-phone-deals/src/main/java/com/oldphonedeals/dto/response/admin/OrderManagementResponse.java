package com.oldphonedeals.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单管理响应 DTO
 * 用于管理员查看订单列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderManagementResponse {

    /**
     * 订单ID
     */
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 用户邮箱
     */
    private String userEmail;

    /**
     * 订单商品数量
     */
    private Integer itemCount;

    /**
     * 总金额
     */
    private Double totalAmount;

    /**
     * 收货地址信息（简化）
     */
    private String addressSummary;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}