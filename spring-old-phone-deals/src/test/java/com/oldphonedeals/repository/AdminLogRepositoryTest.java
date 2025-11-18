package com.oldphonedeals.repository;

import com.oldphonedeals.entity.AdminLog;
import com.oldphonedeals.enums.AdminAction;
import com.oldphonedeals.enums.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AdminLogRepository 集成测试
 * <p>
 * 使用 @DataMongoTest 进行轻量级的 MongoDB 集成测试
 * </p>
 */
@DataMongoTest
@ActiveProfiles("test")
@DisplayName("AdminLogRepository Integration Tests")
class AdminLogRepositoryTest {

  @Autowired
  private AdminLogRepository adminLogRepository;

  @Autowired
  private MongoTemplate mongoTemplate;

  @AfterEach
  void cleanup() {
    adminLogRepository.deleteAll();
  }

  @Test
  @DisplayName("应该保存并找到管理日志 - 通过ID")
  void shouldSaveAndFindAdminLog_byId() {
    // Given
    AdminLog log = createTestLog("admin-123", AdminAction.DISABLE_USER, 
        TargetType.USER, "user-456", "Disabled user for violation");

    // When
    AdminLog savedLog = adminLogRepository.save(log);
    Optional<AdminLog> foundLog = adminLogRepository.findById(savedLog.getId());

    // Then
    assertNotNull(savedLog.getId());
    assertTrue(foundLog.isPresent());
    assertEquals("admin-123", foundLog.get().getAdminUserId());
    assertEquals(AdminAction.DISABLE_USER, foundLog.get().getAction());
  }

  @Test
  @DisplayName("应该通过管理员ID查找日志 - 返回分页")
  void shouldFindLogsByAdminUserId_returnsPage() {
    // Given
    String adminId = "admin-123";
    
    for (int i = 0; i < 5; i++) {
      AdminLog log = createTestLog(adminId, AdminAction.UPDATE_USER, 
          TargetType.USER, "user-" + i, "Updated user " + i);
      adminLogRepository.save(log);
    }

    // 创建另一个管理员的日志
    adminLogRepository.save(createTestLog("admin-456", AdminAction.DELETE_USER, 
        TargetType.USER, "user-999", "Deleted user"));

    Pageable pageable = PageRequest.of(0, 3);

    // When
    Page<AdminLog> logPage = adminLogRepository.findByAdminUserId(adminId, pageable);

    // Then
    assertEquals(3, logPage.getContent().size());
    assertEquals(5, logPage.getTotalElements());
    assertEquals(2, logPage.getTotalPages());
    assertTrue(logPage.getContent().stream()
        .allMatch(log -> log.getAdminUserId().equals(adminId)));
  }

  @Test
  @DisplayName("应该通过操作类型查找日志")
  void shouldFindLogsByAction() {
    // Given
    adminLogRepository.save(createTestLog("admin-1", AdminAction.DISABLE_USER, 
        TargetType.USER, "user-1", "Disabled user 1"));
    adminLogRepository.save(createTestLog("admin-2", AdminAction.DISABLE_USER, 
        TargetType.USER, "user-2", "Disabled user 2"));
    adminLogRepository.save(createTestLog("admin-3", AdminAction.DELETE_USER, 
        TargetType.USER, "user-3", "Deleted user 3"));

    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<AdminLog> disableUserLogs = adminLogRepository.findByAction(AdminAction.DISABLE_USER, pageable);

    // Then
    assertEquals(2, disableUserLogs.getContent().size());
    assertTrue(disableUserLogs.getContent().stream()
        .allMatch(log -> log.getAction() == AdminAction.DISABLE_USER));
  }

  @Test
  @DisplayName("应该通过目标类型查找日志")
  void shouldFindLogsByTargetType() {
    // Given
    adminLogRepository.save(createTestLog("admin-1", AdminAction.DISABLE_USER, 
        TargetType.USER, "user-1", "Disabled user"));
    adminLogRepository.save(createTestLog("admin-2", AdminAction.DISABLE_PHONE, 
        TargetType.PHONE, "phone-1", "Disabled phone"));
    adminLogRepository.save(createTestLog("admin-3", AdminAction.HIDE_REVIEW, 
        TargetType.REVIEW, "review-1", "Hidden review"));

    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<AdminLog> phoneLogs = adminLogRepository.findByTargetType(TargetType.PHONE, pageable);

    // Then
    assertEquals(1, phoneLogs.getContent().size());
    assertEquals(TargetType.PHONE, phoneLogs.getContent().get(0).getTargetType());
  }

  @Test
  @DisplayName("应该通过目标ID查找所有相关日志")
  void shouldFindLogsByTargetId() {
    // Given
    String targetId = "user-123";
    
    adminLogRepository.save(createTestLog("admin-1", AdminAction.UPDATE_USER, 
        TargetType.USER, targetId, "Updated user"));
    adminLogRepository.save(createTestLog("admin-2", AdminAction.DISABLE_USER, 
        TargetType.USER, targetId, "Disabled user"));
    adminLogRepository.save(createTestLog("admin-3", AdminAction.DELETE_USER, 
        TargetType.USER, "user-456", "Deleted another user"));

    // When
    List<AdminLog> targetLogs = adminLogRepository.findByTargetId(targetId);

    // Then
    assertEquals(2, targetLogs.size());
    assertTrue(targetLogs.stream().allMatch(log -> log.getTargetId().equals(targetId)));
  }

  @Test
  @DisplayName("应该查找指定时间范围内的日志")
  void shouldFindLogsByDateRange() {
    // Given
    LocalDateTime searchStart = LocalDateTime.of(2025, 1, 1, 0, 0);
    LocalDateTime searchEnd = LocalDateTime.of(2025, 1, 10, 23, 59);

    AdminLog log1 = createTestLog("admin-1", AdminAction.UPDATE_USER,
        TargetType.USER, "user-1", "Log 1");
    AdminLog log2 = createTestLog("admin-2", AdminAction.UPDATE_USER,
        TargetType.USER, "user-2", "Log 2");
    AdminLog log3 = createTestLog("admin-3", AdminAction.UPDATE_USER,
        TargetType.USER, "user-3", "Log 3");

    // 先保存，然后手动更新createdAt（绕过@CreatedDate自动设置）
    AdminLog saved1 = adminLogRepository.save(log1);
    AdminLog saved2 = adminLogRepository.save(log2);
    AdminLog saved3 = adminLogRepository.save(log3);
    
    // 使用MongoTemplate直接更新createdAt字段
    updateCreatedAt(saved1.getId(), LocalDateTime.of(2025, 1, 5, 10, 0));
    updateCreatedAt(saved2.getId(), LocalDateTime.of(2025, 1, 8, 15, 30));
    updateCreatedAt(saved3.getId(), LocalDateTime.of(2024, 12, 25, 10, 0));

    // When
    List<AdminLog> logsInRange = adminLogRepository.findByCreatedAtBetween(searchStart, searchEnd);

    // Then
    assertEquals(2, logsInRange.size());
    assertTrue(logsInRange.stream()
        .allMatch(log -> !log.getCreatedAt().isBefore(searchStart) && !log.getCreatedAt().isAfter(searchEnd)));
  }

  @Test
  @DisplayName("应该按创建时间倒序查询所有日志")
  void shouldFindAllOrderedByCreatedAtDesc() {
    // Given
    LocalDateTime now = LocalDateTime.now();
    
    AdminLog log1 = createTestLog("admin-1", AdminAction.UPDATE_USER, 
        TargetType.USER, "user-1", "Oldest");
    log1.setCreatedAt(now.minusDays(3));
    
    AdminLog log2 = createTestLog("admin-2", AdminAction.UPDATE_USER, 
        TargetType.USER, "user-2", "Middle");
    log2.setCreatedAt(now.minusDays(2));
    
    AdminLog log3 = createTestLog("admin-3", AdminAction.UPDATE_USER, 
        TargetType.USER, "user-3", "Newest");
    log3.setCreatedAt(now.minusDays(1));

    adminLogRepository.save(log1);
    adminLogRepository.save(log2);
    adminLogRepository.save(log3);

    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<AdminLog> orderedLogs = adminLogRepository.findAllByOrderByCreatedAtDesc(pageable);

    // Then
    assertEquals(3, orderedLogs.getContent().size());
    assertEquals("Newest", orderedLogs.getContent().get(0).getDetails());
    assertEquals("Middle", orderedLogs.getContent().get(1).getDetails());
    assertEquals("Oldest", orderedLogs.getContent().get(2).getDetails());
  }

  @Test
  @DisplayName("应该正确处理分页")
  void shouldHandlePaginationCorrectly() {
    // Given
    for (int i = 0; i < 10; i++) {
      AdminLog log = createTestLog("admin-1", AdminAction.UPDATE_USER, 
          TargetType.USER, "user-" + i, "Log " + i);
      log.setCreatedAt(LocalDateTime.now().minusMinutes(i));
      adminLogRepository.save(log);
    }

    // When
    Page<AdminLog> page1 = adminLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 3));
    Page<AdminLog> page2 = adminLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(1, 3));

    // Then
    assertEquals(3, page1.getContent().size());
    assertEquals(3, page2.getContent().size());
    assertEquals(10, page1.getTotalElements());
    assertEquals(4, page1.getTotalPages());
    assertNotEquals(page1.getContent().get(0).getId(), page2.getContent().get(0).getId());
  }

  @Test
  @DisplayName("应该返回空结果 - 当管理员没有日志时")
  void shouldReturnEmpty_whenAdminHasNoLogs() {
    // Given
    String nonExistentAdminId = "admin-nonexistent";

    Pageable pageable = PageRequest.of(0, 10);

    // When
    Page<AdminLog> logs = adminLogRepository.findByAdminUserId(nonExistentAdminId, pageable);

    // Then
    assertTrue(logs.getContent().isEmpty());
  }

  @Test
  @DisplayName("应该删除日志 - 通过ID")
  void shouldDeleteLog_byId() {
    // Given
    AdminLog log = createTestLog("admin-123", AdminAction.DELETE_USER, 
        TargetType.USER, "user-456", "Deleted user");
    AdminLog savedLog = adminLogRepository.save(log);

    // When
    adminLogRepository.deleteById(savedLog.getId());

    // Then
    Optional<AdminLog> foundLog = adminLogRepository.findById(savedLog.getId());
    assertFalse(foundLog.isPresent());
  }

  @Test
  @DisplayName("应该计数所有日志")
  void shouldCountAllLogs() {
    // Given
    adminLogRepository.save(createTestLog("admin-1", AdminAction.UPDATE_USER, 
        TargetType.USER, "user-1", "Log 1"));
    adminLogRepository.save(createTestLog("admin-2", AdminAction.DELETE_USER, 
        TargetType.USER, "user-2", "Log 2"));
    adminLogRepository.save(createTestLog("admin-3", AdminAction.DISABLE_PHONE, 
        TargetType.PHONE, "phone-1", "Log 3"));

    // When
    long count = adminLogRepository.count();

    // Then
    assertEquals(3, count);
  }

  @Test
  @DisplayName("应该正确保存时间戳字段")
  void shouldCorrectlySaveTimestampFields() {
    // Given
    AdminLog log = createTestLog("admin-123", AdminAction.UPDATE_USER, 
        TargetType.USER, "user-456", "Updated user");

    // When
    AdminLog savedLog = adminLogRepository.save(log);

    // Then
    assertNotNull(savedLog.getCreatedAt());
  }

  @Test
  @DisplayName("应该正确处理排序和分页组合")
  void shouldHandleSortingAndPagination() {
    // Given
    for (int i = 0; i < 5; i++) {
      AdminLog log = createTestLog("admin-" + i, AdminAction.UPDATE_USER, 
          TargetType.USER, "user-" + i, "Log " + i);
      log.setCreatedAt(LocalDateTime.now().minusMinutes(i));
      adminLogRepository.save(log);
    }

    Pageable pageable = PageRequest.of(0, 3, Sort.by("createdAt").descending());

    // When
    Page<AdminLog> sortedPage = adminLogRepository.findByAction(AdminAction.UPDATE_USER, pageable);

    // Then
    assertEquals(3, sortedPage.getContent().size());
    
    // 验证排序 - 最新的日志应该在前面
    LocalDateTime firstDate = sortedPage.getContent().get(0).getCreatedAt();
    LocalDateTime secondDate = sortedPage.getContent().get(1).getCreatedAt();
    assertTrue(firstDate.isAfter(secondDate) || firstDate.isEqual(secondDate));
  }

  // Helper method
  private AdminLog createTestLog(String adminUserId, AdminAction action,
                                  TargetType targetType, String targetId, String details) {
    return AdminLog.builder()
        .adminUserId(adminUserId)
        .action(action)
        .targetType(targetType)
        .targetId(targetId)
        .details(details)
        .createdAt(LocalDateTime.now())
        .build();
  }

  /**
   * 辅助方法：直接更新MongoDB中的createdAt字段（绕过@CreatedDate）
   */
  private void updateCreatedAt(String logId, LocalDateTime newCreatedAt) {
    Query query = new Query(Criteria.where("id").is(logId));
    Update update = new Update().set("createdAt", newCreatedAt);
    mongoTemplate.updateFirst(query, update, AdminLog.class);
  }
}