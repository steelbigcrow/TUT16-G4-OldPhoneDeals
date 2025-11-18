package com.oldphonedeals.dto.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单商品项响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    
    /**
     * 商品ID
     */
    private String phoneId;
    
    /**
     * 商品标题
     */
    private String title;
    
    /**
     * 数量
     */
    private Integer quantity;
    
    /**
     * 价格
     */
    private Double price;
}