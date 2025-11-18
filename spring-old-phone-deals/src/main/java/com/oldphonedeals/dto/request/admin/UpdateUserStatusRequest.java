package com.oldphonedeals.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员更新用户状态请求 DTO
 * 用于管理员启用或禁用用户账户
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserStatusRequest {

    /**
     * 是否禁用账户
     */
    @NotNull(message = "Status is required")
    private Boolean isDisabled;
}