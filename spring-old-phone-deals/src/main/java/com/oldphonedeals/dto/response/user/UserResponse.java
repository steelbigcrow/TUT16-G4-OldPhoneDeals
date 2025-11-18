package com.oldphonedeals.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息响应 DTO
 * 用于返回用户的公开信息（用于显示卖家信息等场景）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

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
     * 是否已验证邮箱
     */
    private Boolean isVerified;

    /**
     * 是否被禁用
     */
    private Boolean isDisabled;

    /**
     * 是否被封禁
     */
    private Boolean isBan;
}