package com.oldphonedeals.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证密码重置码请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyResetCodeRequest {
    
    /**
     * 用户邮箱
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    /**
     * 6位数字重置码
     */
    @NotBlank(message = "Reset code is required")
    @Pattern(regexp = "^\\d{6}$", message = "Reset code must be a 6-digit number")
    private String code;
}