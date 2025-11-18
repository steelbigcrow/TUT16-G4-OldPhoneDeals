package com.oldphonedeals.dto.response.cart;

import com.oldphonedeals.enums.PhoneBrand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 购物车商品项响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    
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
    
    /**
     * 平均评分
     */
    private Double averageRating;
    
    /**
     * 评论数量
     */
    private Integer reviewCount;
    
    /**
     * 卖家信息
     */
    private SellerInfo seller;
    
    /**
     * 购物车中商品的完整信息
     */
    private PhoneInfo phone;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 卖家信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SellerInfo {
        
        /**
         * 卖家ID
         */
        private String id;
        
        /**
         * 卖家名字
         */
        private String firstName;
        
        /**
         * 卖家姓氏
         */
        private String lastName;
    }
    
    /**
     * 购物车商品的简化信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PhoneInfo {
        
        /**
         * 商品ID
         */
        private String id;
        
        /**
         * 商品标题
         */
        private String title;
        
        /**
         * 品牌
         */
        private PhoneBrand brand;
        
        /**
         * 图片地址
         */
        private String image;
        
        /**
         * 库存
         */
        private Integer stock;
        
        /**
         * 价格
         */
        private Double price;
        
        /**
         * 是否禁用
         */
        private Boolean isDisabled;
    }
}
