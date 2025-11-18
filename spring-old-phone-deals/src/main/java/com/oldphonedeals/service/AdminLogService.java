package com.oldphonedeals.service;

import com.oldphonedeals.dto.response.PageResponse;
import com.oldphonedeals.dto.response.admin.AdminLogResponse;
import com.oldphonedeals.enums.AdminAction;
import com.oldphonedeals.enums.TargetType;

/**
 * 管理员日志服务接口
 * 用于记录和查询管理员操作日志
 */
public interface AdminLogService {

    /**
     * 记录管理员操作
     *
     * @param adminId    管理员ID
     * @param action     操作类型
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @param details    操作详情（可选）
     */
    void logAction(String adminId, AdminAction action, TargetType targetType, String targetId, String details);

    /**
     * 获取所有操作日志（分页）
     *
     * @param page     页码（从0开始）
     * @param pageSize 每页大小
     * @return 日志分页响应
     */
    PageResponse<AdminLogResponse> getAllLogs(int page, int pageSize);

    /**
     * 获取特定管理员的操作日志
     *
     * @param adminId  管理员ID
     * @param page     页码（从0开始）
     * @param pageSize 每页大小
     * @return 日志分页响应
     */
    PageResponse<AdminLogResponse> getLogsByAdmin(String adminId, int page, int pageSize);
}