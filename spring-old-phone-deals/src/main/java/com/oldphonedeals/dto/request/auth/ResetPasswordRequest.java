package com.oldphonedeals.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 重置密码请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {
    
    /**
     * 用户邮箱
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    /**
     * 当前密码（可选，用于需要验证当前密码的场景）
     */
    private String currentPassword;
    
    /**
     * 6位数字重置码（可选，用于忘记密码场景）
     */
    @Pattern(regexp = "^\\d{6}$", message = "Reset code must be a 6-digit number")
    private String code;
    
    /**
     * 新密码
     */
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,}$",
        message = "Password must include uppercase, lowercase, number, and special character"
    )
    private String newPassword;
}