package com.oldphonedeals.dto.response.phone;

import com.oldphonedeals.enums.PhoneBrand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商品列表项响应 DTO（简化版）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneListItemResponse {
    
    /**
     * 商品ID
     */
    private String id;
    
    /**
     * 商品标题
     */
    private String title;
    
    /**
     * 商品品牌
     */
    private PhoneBrand brand;
    
    /**
     * 商品图片 URL
     */
    private String image;
    
    /**
     * 库存数量
     */
    private Integer stock;
    
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
         * 卖家名字
         */
        private String firstName;
        
        /**
         * 卖家姓氏
         */
        private String lastName;
    }
}