package com.oldphonedeals.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证用户信息响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUserResponse {
    
    /**
     * 用户ID
     */
    private String id;
    
    /**
     * 名字
     */
    private String firstName;
    
    /**
     * 姓氏
     */
    private String lastName;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 是否管理员
     */
    private Boolean isAdmin;
    
    /**
     * 是否已禁用
     */
    private Boolean isDisabled;
    
    /**
     * 是否已封禁
     */
    private Boolean isBan;
    
    /**
     * 是否已验证邮箱
     */
    private Boolean isVerified;
}