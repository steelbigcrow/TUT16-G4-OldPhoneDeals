package com.oldphonedeals.dto.response.phone;

import com.oldphonedeals.enums.PhoneBrand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneResponse {
    
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
     * 是否禁用
     */
    private Boolean isDisabled;
    
    /**
     * 销售数量
     */
    private Integer salesCount;
    
    /**
     * 平均评分
     */
    private Double averageRating;
    
    /**
     * 卖家信息
     */
    private SellerInfo seller;
    
    /**
     * 评论列表
     */
    private List<ReviewResponse> reviews;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
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
}