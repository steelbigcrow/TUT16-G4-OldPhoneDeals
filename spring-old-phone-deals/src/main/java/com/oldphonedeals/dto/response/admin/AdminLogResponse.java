package com.oldphonedeals.dto.response.admin;

import com.oldphonedeals.enums.AdminAction;
import com.oldphonedeals.enums.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理员日志响应 DTO
 * 用于返回管理员操作日志信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLogResponse {

    /**
     * 日志ID
     */
    private String id;

    /**
     * 管理员用户ID
     */
    private String adminUserId;

    /**
     * 管理员姓名
     */
    private String adminName;

    /**
     * 操作类型
     */
    private AdminAction action;

    /**
     * 目标类型
     */
    private TargetType targetType;

    /**
     * 目标ID
     */
    private String targetId;

    /**
     * 操作详情
     */
    private String details;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}