package com.oldphonedeals.service;

import com.oldphonedeals.dto.response.PageResponse;
import com.oldphonedeals.dto.response.admin.AdminLogResponse;
import com.oldphonedeals.entity.AdminLog;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.AdminAction;
import com.oldphonedeals.enums.TargetType;
import com.oldphonedeals.repository.AdminLogRepository;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.service.AdminLogService;
import com.oldphonedeals.service.impl.AdminLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdminLogService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AdminLogServiceTest {

    @Mock
    private AdminLogRepository adminLogRepository;

    @Mock
    private UserRepository userRepository;

    private AdminLogService adminLogService;

    @BeforeEach
    void setUp() {
        adminLogService = new AdminLogServiceImpl(adminLogRepository, userRepository);
    }

    @Test
    void logAction_shouldSaveLogAndNotThrow() {
        when(adminLogRepository.save(any(AdminLog.class))).thenAnswer(invocation -> {
            AdminLog log = invocation.getArgument(0);
            log.setId("log-1");
            return log;
        });

        assertDoesNotThrow(() -> adminLogService.logAction(
            "admin-1", AdminAction.CREATE_USER, TargetType.USER, "user-1", "details"));

        verify(adminLogRepository).save(any(AdminLog.class));
    }

    @Test
    void getAllLogs_shouldConvertToPageResponse() {
        AdminLog log = AdminLog.builder()
            .id("log-1")
            .adminUserId("admin-1")
            .action(AdminAction.UPDATE_USER)
            .targetType(TargetType.USER)
            .targetId("user-1")
            .details("details")
            .createdAt(LocalDateTime.now())
            .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<AdminLog> page = new PageImpl<>(List.of(log), pageable, 1);
        when(adminLogRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

        User admin = User.builder().id("admin-1").firstName("Admin").lastName("User").build();
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));

        PageResponse<AdminLogResponse> response = adminLogService.getAllLogs(0, 10);

        assertEquals(1, response.getTotalItems());
        assertEquals(1, response.getContent().size());
        AdminLogResponse item = response.getContent().get(0);
        assertEquals("Admin User", item.getAdminName());
        assertEquals(AdminAction.UPDATE_USER, item.getAction());
    }

    @Test
    void getLogsByAdmin_shouldFilterLogsByAdminId() {
        AdminLog log = AdminLog.builder()
            .id("log-1")
            .adminUserId("admin-1")
            .action(AdminAction.UPDATE_USER)
            .targetType(TargetType.USER)
            .targetId("user-1")
            .details("details")
            .createdAt(LocalDateTime.now())
            .build();

        Pageable pageable = PageRequest.of(0, 5);
        Page<AdminLog> page = new PageImpl<>(List.of(log), pageable, 1);
        when(adminLogRepository.findByAdminUserId("admin-1", pageable)).thenReturn(page);

        when(userRepository.findById("admin-1")).thenReturn(Optional.empty());

        PageResponse<AdminLogResponse> response = adminLogService.getLogsByAdmin("admin-1", 0, 5);

        assertEquals(1, response.getTotalItems());
        assertEquals("admin-1", response.getContent().get(0).getAdminUserId());
        assertEquals(AdminAction.UPDATE_USER, response.getContent().get(0).getAction());
        // adminName should be empty string when user not found
        assertEquals("", response.getContent().get(0).getAdminName());
    }
}
