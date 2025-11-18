package com.oldphonedeals.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员资料响应 DTO
 * 用于返回管理员个人信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProfileResponse {

    /**
     * 管理员ID
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
     * 角色
     */
    private String role;

    /**
     * 是否已验证邮箱
     */
    private Boolean isVerified;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLogin;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}