package com.oldphonedeals.dto.request.admin;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员更新用户信息请求 DTO
 * 用于管理员修改用户的基本信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

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
    @Email(message = "Email should be valid")
    private String email;

    /**
     * 是否禁用账户
     */
    private Boolean isDisabled;
}