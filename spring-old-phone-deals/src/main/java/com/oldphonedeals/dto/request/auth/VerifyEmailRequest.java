package com.oldphonedeals.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证邮箱请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyEmailRequest {
    
    /**
     * 验证令牌
     */
    @NotBlank(message = "Token is required")
    private String token;
    
    /**
     * 用户邮箱
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
}