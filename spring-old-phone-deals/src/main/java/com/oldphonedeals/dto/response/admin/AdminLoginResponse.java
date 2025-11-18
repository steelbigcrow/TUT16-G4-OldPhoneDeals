package com.oldphonedeals.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员登录响应 DTO
 * 返回JWT token和管理员基本信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLoginResponse {

    /**
     * JWT Token
     */
    private String token;

    /**
     * 管理员信息
     */
    private AdminInfo admin;

    /**
     * 管理员基本信息（内部类）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminInfo {
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
    }
}