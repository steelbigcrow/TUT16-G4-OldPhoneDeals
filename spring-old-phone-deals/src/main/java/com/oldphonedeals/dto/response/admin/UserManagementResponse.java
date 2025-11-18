package com.oldphonedeals.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户管理响应 DTO
 * 用于管理员查看用户列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserManagementResponse {

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
     * 角色
     */
    private String role;

    /**
     * 是否已验证邮箱
     */
    private Boolean isVerified;

    /**
     * 是否禁用
     */
    private Boolean isDisabled;

    /**
     * 是否封禁
     */
    private Boolean isBan;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLogin;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}