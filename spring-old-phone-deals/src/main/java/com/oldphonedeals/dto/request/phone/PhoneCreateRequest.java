package com.oldphonedeals.dto.request.phone;

import com.oldphonedeals.enums.PhoneBrand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建商品请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneCreateRequest {
    
    /**
     * 商品标题
     */
    @NotBlank(message = "Title is required")
    private String title;
    
    /**
     * 商品品牌
     */
    @NotNull(message = "Brand is required")
    private PhoneBrand brand;
    
    /**
     * 商品图片 URL
     */
    @NotBlank(message = "Image is required")
    private String image;
    
    /**
     * 库存数量
     */
    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be at least 0")
    private Integer stock;
    
    /**
     * 价格
     */
    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be at least 0")
    private Double price;
    
    /**
     * 卖家ID
     */
    @NotBlank(message = "Seller ID is required")
    private String seller;
}