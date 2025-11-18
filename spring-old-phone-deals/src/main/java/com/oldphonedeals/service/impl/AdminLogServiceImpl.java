package com.oldphonedeals.service.impl;

import com.oldphonedeals.dto.response.PageResponse;
import com.oldphonedeals.dto.response.admin.AdminLogResponse;
import com.oldphonedeals.entity.AdminLog;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.AdminAction;
import com.oldphonedeals.enums.TargetType;
import com.oldphonedeals.repository.AdminLogRepository;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.service.AdminLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员日志服务实现
 * 负责记录和查询管理员操作日志
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminLogServiceImpl implements AdminLogService {

    private final AdminLogRepository adminLogRepository;
    private final UserRepository userRepository;

    @Override
    public void logAction(String adminId, AdminAction action, TargetType targetType, String targetId, String details) {
        try {
            AdminLog adminLog = AdminLog.builder()
                    .adminUserId(adminId)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .details(details)
                    .build();

            adminLogRepository.save(adminLog);
            
            log.info("Admin action logged: {} performed {} on {} {}", 
                    adminId, action, targetType, targetId);
        } catch (Exception e) {
            log.error("Failed to log admin action: {}", e.getMessage(), e);
            // 不抛出异常，避免影响主业务流程
        }
    }

    @Override
    public PageResponse<AdminLogResponse> getAllLogs(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<AdminLog> logPage = adminLogRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<AdminLogResponse> logs = logPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResponse.<AdminLogResponse>builder()
                .content(logs)
                .currentPage(logPage.getNumber() + 1)
                .totalPages(logPage.getTotalPages())
                .totalItems(logPage.getTotalElements())
                .itemsPerPage(logPage.getSize())
                .hasNext(logPage.hasNext())
                .hasPrevious(logPage.hasPrevious())
                .build();
    }

    @Override
    public PageResponse<AdminLogResponse> getLogsByAdmin(String adminId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<AdminLog> logPage = adminLogRepository.findByAdminUserId(adminId, pageable);

        List<AdminLogResponse> logs = logPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResponse.<AdminLogResponse>builder()
                .content(logs)
                .currentPage(logPage.getNumber() + 1)
                .totalPages(logPage.getTotalPages())
                .totalItems(logPage.getTotalElements())
                .itemsPerPage(logPage.getSize())
                .hasNext(logPage.hasNext())
                .hasPrevious(logPage.hasPrevious())
                .build();
    }

    /**
     * 转换AdminLog实体为AdminLogResponse DTO
     */
    private AdminLogResponse convertToResponse(AdminLog log) {
        // 获取管理员姓名
        String adminName = "";
        try {
            User admin = userRepository.findById(log.getAdminUserId()).orElse(null);
            if (admin != null) {
                adminName = admin.getFirstName() + " " + admin.getLastName();
            }
        } catch (Exception e) {
            AdminLogServiceImpl.log.error("Failed to fetch admin user: {}", e.getMessage());
        }

        return AdminLogResponse.builder()
                .id(log.getId())
                .adminUserId(log.getAdminUserId())
                .adminName(adminName)
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}