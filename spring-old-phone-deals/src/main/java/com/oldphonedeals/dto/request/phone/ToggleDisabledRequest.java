package com.oldphonedeals.dto.request.phone;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 启禁用商品请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToggleDisabledRequest {
    
    /**
     * 是否禁用
     * true - 禁用商品
     * false - 启用商品
     */
    @NotNull(message = "isDisabled is required")
    private Boolean isDisabled;
}