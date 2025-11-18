package com.oldphonedeals.dto.response.admin;

import com.oldphonedeals.enums.PhoneBrand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商品管理响应 DTO
 * 用于管理员查看商品列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneManagementResponse {

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
     * 图片URL
     */
    private String image;

    /**
     * 价格
     */
    private Double price;

    /**
     * 库存
     */
    private Integer stock;

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
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 卖家基本信息
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

        /**
         * 卖家邮箱
         */
        private String email;
    }
}