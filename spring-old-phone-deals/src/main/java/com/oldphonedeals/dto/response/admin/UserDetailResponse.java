package com.oldphonedeals.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详情响应 DTO
 * 用于管理员查看用户详细信息和统计数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponse {

    /**
     * 用户ID
     */
    private String id;

    /**
     * 名字
     */
    private String firstName;

    /**
     * 姓氏
     */
    private String lastName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 角色
     */
    private String role;

    /**
     * 是否已验证邮箱
     */
    private Boolean isVerified;

    /**
     * 是否禁用
     */
    private Boolean isDisabled;

    /**
     * 是否封禁
     */
    private Boolean isBan;

    /**
     * 心愿单商品ID列表
     */
    private List<String> wishlist;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLogin;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 统计信息
     */
    private UserStats stats;

    /**
     * 用户统计信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserStats {
        /**
         * 发布的商品数量
         */
        private Long listedPhonesCount;

        /**
         * 已购买订单数量
         */
        private Long ordersCount;

        /**
         * 评论数量
         */
        private Long reviewsCount;

        /**
         * 总消费金额
         */
        private Double totalSpent;
    }
}