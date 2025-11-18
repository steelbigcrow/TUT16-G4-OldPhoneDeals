package com.oldphonedeals.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用消息响应 DTO
 * 用于返回简单的操作成功或失败消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 便捷方法：创建成功响应
     */
    public static MessageResponse success(String message) {
        return MessageResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * 便捷方法：创建失败响应
     */
    public static MessageResponse error(String message) {
        return MessageResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}