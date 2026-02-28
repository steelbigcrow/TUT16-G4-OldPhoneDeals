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
     * 乐观锁版本号（可选）。
     * <p>
     * 当提供该字段时，后端会校验版本是否一致，不一致则返回 409 CONFLICT。
     * </p>
     */
    private Long version;
    
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
