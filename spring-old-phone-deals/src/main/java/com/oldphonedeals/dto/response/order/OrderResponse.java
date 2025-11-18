package com.oldphonedeals.dto.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    
    /**
     * 订单ID
     */
    private String id;
    
    /**
     * 用户ID
     */
    private String userId;
    
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