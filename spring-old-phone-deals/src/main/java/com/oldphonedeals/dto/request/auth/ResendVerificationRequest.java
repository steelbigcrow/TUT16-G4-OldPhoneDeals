package com.oldphonedeals.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重新发送验证邮件请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendVerificationRequest {
    
    /**
     * 用户邮箱
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
}