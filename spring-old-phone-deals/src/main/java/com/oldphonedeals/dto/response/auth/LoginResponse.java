package com.oldphonedeals.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    
    /**
     * JWT 令牌
     */
    private String token;
    
    /**
     * 用户信息
     */
    private UserInfo user;
    
    /**
     * 用户基本信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        
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
    }
}