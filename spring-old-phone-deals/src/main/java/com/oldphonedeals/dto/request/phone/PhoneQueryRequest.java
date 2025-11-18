package com.oldphonedeals.dto.request.phone;

import com.oldphonedeals.enums.PhoneBrand;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品查询请求 DTO
 * 用于商品列表查询，支持搜索、筛选、分页和排序
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneQueryRequest {
    
    /**
     * 搜索关键词（模糊匹配商品标题）
     */
    private String search;
    
    /**
     * 品牌过滤
     */
    private PhoneBrand brand;
    
    /**
     * 最高价格过滤
     */
    private Double maxPrice;
    
    /**
     * 排序字段（默认：createdAt）
     */
    @Builder.Default
    private String sortBy = "createdAt";
    
    /**
     * 排序方向（asc/desc，默认：desc）
     */
    @Builder.Default
    private String sortOrder = "desc";
    
    /**
     * 页码（从 1 开始）
     */
    @Min(value = 1, message = "Page must be at least 1")
    @Builder.Default
    private Integer page = 1;
    
    /**
     * 每页数量
     */
    @Min(value = 1, message = "Limit must be at least 1")
    @Builder.Default
    private Integer limit = 10;
    
    /**
     * 特殊查询类型
     * - soldOutSoon: 低库存商品
     * - bestSellers: 畅销商品
     */
    private String special;
}