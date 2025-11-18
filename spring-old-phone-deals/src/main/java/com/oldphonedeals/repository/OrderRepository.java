package com.oldphonedeals.repository;

import com.oldphonedeals.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    
    // 根据用户查询订单
    Page<Order> findByUserId(String userId, Pageable pageable);
    
    List<Order> findByUserId(String userId);
    
    // 查询指定时间范围内的订单
    @Query("{'createdAt': {$gte: ?0, $lte: ?1}}")
    List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 删除用户的所有订单
    void deleteByUserId(String userId);
    
    // 统计总销售额
    @Query(value = "{}", fields = "{'totalAmount': 1}")
    List<Order> findAllOrdersForStats();
    
    // 验证用户是否购买过指定商品
    // 查询条件：订单中的items数组包含指定phoneId的商品
    @Query("{'userId': ?0, 'items.phoneId': ?1}")
    List<Order> findByUserIdAndPhoneId(String userId, String phoneId);
}