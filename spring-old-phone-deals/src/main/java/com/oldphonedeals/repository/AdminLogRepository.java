package com.oldphonedeals.repository;

import com.oldphonedeals.entity.AdminLog;
import com.oldphonedeals.enums.AdminAction;
import com.oldphonedeals.enums.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminLogRepository extends MongoRepository<AdminLog, String> {
    
    // 根据管理员查询日志
    Page<AdminLog> findByAdminUserId(String adminUserId, Pageable pageable);
    
    // 根据操作类型查询
    Page<AdminLog> findByAction(AdminAction action, Pageable pageable);
    
    // 根据目标类型查询
    Page<AdminLog> findByTargetType(TargetType targetType, Pageable pageable);
    
    // 根据目标ID查询
    List<AdminLog> findByTargetId(String targetId);
    
    // 查询指定时间范围内的日志
    List<AdminLog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 按创建时间倒序查询最近的日志
    @Query(value = "{}", sort = "{ \"createdAt\": -1 }")
    Page<AdminLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}