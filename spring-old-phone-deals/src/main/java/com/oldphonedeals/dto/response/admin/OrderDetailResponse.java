package com.oldphonedeals.dto.response.admin;

import com.oldphonedeals.dto.response.order.OrderItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情响应 DTO（管理员用）
 * 用于管理员查看订单详细信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailResponse {

    /**
     * 订单ID
     */
    private String id;

    /**
     * 用户信息
     */
    private UserInfo user;

    /**
     * 订单商品列表
     */
    private List<OrderItemResponse> items;

    /**
     * 总金额
     */
    private Double totalAmount;

    /**
     * 收货地址
     */
    private AddressInfo address;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 用户基本信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
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
    }

    /**
     * 地址信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressInfo {
        /**
         * 街道地址
         */
        private String street;

        /**
         * 城市
         */
        private String city;

        /**
         * 州/省
         */
        private String state;

        /**
         * 邮编
         */
        private String zip;

        /**
         * 国家
         */
        private String country;
    }
}