package com.oldphonedeals.dto.request.admin;

import com.oldphonedeals.enums.PhoneBrand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员更新商品请求 DTO
 * 用于管理员修改商品信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePhoneRequest {

    /**
     * 商品标题
     */
    @NotBlank(message = "Title is required")
    private String title;

    /**
     * 品牌
     */
    @NotNull(message = "Brand is required")
    private PhoneBrand brand;

    /**
     * 价格
     */
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private Double price;

    /**
     * 库存
     */
    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;

    /**
     * 是否禁用
     */
    private Boolean isDisabled;
}