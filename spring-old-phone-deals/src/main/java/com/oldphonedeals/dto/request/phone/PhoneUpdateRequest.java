package com.oldphonedeals.dto.request.phone;

import com.oldphonedeals.enums.PhoneBrand;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新商品请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneUpdateRequest {
    
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
    @Min(value = 0, message = "Stock must be at least 0")
    private Integer stock;
    
    /**
     * 价格
     */
    @Min(value = 0, message = "Price must be at least 0")
    private Double price;
    
    /**
     * 是否禁用
     */
    private Boolean isDisabled;
}