package com.oldphonedeals.repository;

import com.oldphonedeals.TestDataFactory;
import com.oldphonedeals.entity.Order;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrderRepository 集成测试
 * <p>
 * 使用 @DataMongoTest 进行轻量级的 MongoDB 集成测试
 * </p>
 */
@DataMongoTest
@ActiveProfiles("test")
@DisplayName("OrderRepository Integration Tests")
class OrderRepositoryTest {

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private MongoTemplate mongoTemplate;

  @AfterEach
  void cleanup() {
    orderRepository.deleteAll();
  }

  @Test
  @DisplayName("应该保存并找到订单 - 通过ID")
  void shouldSaveAndFindOrder_byId() {
    // Given
    String userId = "user-123";
    Order order = createTestOrder(userId, 1000.0);

    // When
    Order savedOrder = orderRepository.save(order);
    Optional<Order> foundOrder = orderRepository.findById(savedOrder.getId());

    // Then
    assertNotNull(savedOrder.getId());
    assertTrue(foundOrder.isPresent());
    assertEquals(userId, foundOrder.get().getUserId());
    assertEquals(1000.0, foundOrder.get().getTotalAmount());
  }

  @Test
  @DisplayName("应该通过用户ID查找订单 - 返回分页")
  void shouldFindOrdersByUserId_returnsPage() {
    // Given
    String userId = "user-123";
    for (int i = 0; i < 5; i++) {
      Order order = createTestOrder(userId, 100.0 * (i + 1));
      orderRepository.save(order);
    }

    // 创建另一个用户的订单
    orderRepository.save(createTestOrder("user-456", 500.0));

    Pageable pageable = PageRequest.of(0, 3);

    // When
    Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);

    // Then
    assertEquals(3, orderPage.getContent().size());
    assertEquals(5, orderPage.getTotalElements());
    assertEquals(2, orderPage.getTotalPages());
    assertTrue(orderPage.getContent().stream()
        .allMatch(order -> order.getUserId().equals(userId)));
  }

  @Test
  @DisplayName("应该通过用户ID查找订单 - 返回列表")
  void shouldFindOrdersByUserId_returnsList() {
    // Given
    String userId = "user-123";
    orderRepository.save(createTestOrder(userId, 100.0));
    orderRepository.save(createTestOrder(userId, 200.0));
    orderRepository.save(createTestOrder("user-456", 300.0));

    // When
    List<Order> userOrders = orderRepository.findByUserId(userId);

    // Then
    assertEquals(2, userOrders.size());
    assertTrue(userOrders.stream().allMatch(o -> o.getUserId().equals(userId)));
  }

  @Test
  @DisplayName("应该返回空结果 - 当用户没有订单时")
  void shouldReturnEmpty_whenUserHasNoOrders() {
    // Given
    String userId = "user-with-no-orders";

    // When
    List<Order> orders = orderRepository.findByUserId(userId);

    // Then
    assertTrue(orders.isEmpty());
  }

  @Test
  @DisplayName("应该查找指定时间范围内的订单")
  void shouldFindOrdersByDateRange() {
    // Given
    LocalDateTime searchStart = LocalDateTime.of(2025, 1, 1, 0, 0);
    LocalDateTime searchEnd = LocalDateTime.of(2025, 1, 10, 23, 59);

    Order order1 = createTestOrder("user-1", 100.0);
    Order order2 = createTestOrder("user-2", 200.0);
    Order order3 = createTestOrder("user-3", 300.0);

    // 先保存，然后手动更新createdAt（绕过@CreatedDate自动设置）
    Order saved1 = orderRepository.save(order1);
    Order saved2 = orderRepository.save(order2);
    Order saved3 = orderRepository.save(order3);
    
    // 使用MongoTemplate直接更新createdAt字段
    updateCreatedAt(saved1.getId(), LocalDateTime.of(2025, 1, 5, 10, 0));
    updateCreatedAt(saved2.getId(), LocalDateTime.of(2025, 1, 8, 15, 30));
    updateCreatedAt(saved3.getId(), LocalDateTime.of(2024, 12, 25, 10, 0));

    // When
    List<Order> ordersInRange = orderRepository.findByCreatedAtBetween(searchStart, searchEnd);

    // Then
    assertEquals(2, ordersInRange.size());
    assertTrue(ordersInRange.stream()
        .allMatch(o -> !o.getCreatedAt().isBefore(searchStart) && !o.getCreatedAt().isAfter(searchEnd)));
  }

  @Test
  @DisplayName("应该删除用户的所有订单")
  void shouldDeleteOrdersByUserId() {
    // Given
    String userId = "user-123";
    orderRepository.save(createTestOrder(userId, 100.0));
    orderRepository.save(createTestOrder(userId, 200.0));
    orderRepository.save(createTestOrder("user-456", 300.0));

    // When
    orderRepository.deleteByUserId(userId);

    // Then
    List<Order> remainingOrders = orderRepository.findAll();
    assertEquals(1, remainingOrders.size());
    assertEquals("user-456", remainingOrders.get(0).getUserId());
  }

  @Test
  @DisplayName("应该保存带有商品的订单")
  void shouldSaveOrderWithItems() {
    // Given
    String userId = "user-123";
    Order order = createTestOrder(userId, 1499.98);
    
    Order.OrderItem item1 = createOrderItem("phone-1", "iPhone 13", 2, 799.99);
    Order.OrderItem item2 = createOrderItem("phone-2", "Galaxy S21", 1, 699.99);
    
    order.setItems(List.of(item1, item2));

    // When
    Order savedOrder = orderRepository.save(order);

    // Then
    Optional<Order> foundOrder = orderRepository.findById(savedOrder.getId());
    assertTrue(foundOrder.isPresent());
    assertEquals(2, foundOrder.get().getItems().size());
    assertEquals("phone-1", foundOrder.get().getItems().get(0).getPhoneId());
    assertEquals("iPhone 13", foundOrder.get().getItems().get(0).getTitle());
  }

  @Test
  @DisplayName("应该保存带有地址的订单")
  void shouldSaveOrderWithAddress() {
    // Given
    String userId = "user-123";
    Order order = createTestOrder(userId, 1000.0);
    
    Order.Address address = Order.Address.builder()
        .street("123 Main St")
        .city("Sydney")
        .state("NSW")
        .zip("2000")
        .country("Australia")
        .build();
    
    order.setAddress(address);

    // When
    Order savedOrder = orderRepository.save(order);

    // Then
    Optional<Order> foundOrder = orderRepository.findById(savedOrder.getId());
    assertTrue(foundOrder.isPresent());
    assertNotNull(foundOrder.get().getAddress());
    assertEquals("123 Main St", foundOrder.get().getAddress().getStreet());
    assertEquals("Sydney", foundOrder.get().getAddress().getCity());
  }

  @Test
  @DisplayName("应该查找用户购买过指定商品的订单")
  void shouldFindOrdersByUserIdAndPhoneId() {
    // Given
    String userId = "user-123";
    String phoneId = "phone-123";
    
    Order order1 = createTestOrder(userId, 799.99);
    order1.setItems(List.of(
        createOrderItem(phoneId, "iPhone 13", 1, 799.99)
    ));
    
    Order order2 = createTestOrder(userId, 699.99);
    order2.setItems(List.of(
        createOrderItem("phone-456", "Galaxy S21", 1, 699.99)
    ));
    
    Order order3 = createTestOrder(userId, 1499.98);
    order3.setItems(List.of(
        createOrderItem(phoneId, "iPhone 13", 1, 799.99),
        createOrderItem("phone-789", "Pixel 6", 1, 699.99)
    ));

    orderRepository.save(order1);
    orderRepository.save(order2);
    orderRepository.save(order3);

    // When
    List<Order> ordersWithPhone = orderRepository.findByUserIdAndPhoneId(userId, phoneId);

    // Then
    assertEquals(2, ordersWithPhone.size());
    assertTrue(ordersWithPhone.stream()
        .allMatch(order -> order.getItems().stream()
            .anyMatch(item -> item.getPhoneId().equals(phoneId))));
  }

  @Test
  @DisplayName("应该正确处理分页和排序")
  void shouldHandlePaginationAndSorting() {
    // Given
    for (int i = 0; i < 5; i++) {
      Order order = createTestOrder("user-123", 100.0 * (i + 1));
      order.setCreatedAt(LocalDateTime.now().minusDays(i));
      orderRepository.save(order);
    }

    Pageable pageable = PageRequest.of(0, 3, Sort.by("createdAt").descending());

    // When
    Page<Order> orderPage = orderRepository.findByUserId("user-123", pageable);

    // Then
    assertEquals(3, orderPage.getContent().size());
    assertEquals(5, orderPage.getTotalElements());
    
    // 验证排序 - 最新的订单应该在前面
    LocalDateTime firstDate = orderPage.getContent().get(0).getCreatedAt();
    LocalDateTime secondDate = orderPage.getContent().get(1).getCreatedAt();
    assertTrue(firstDate.isAfter(secondDate) || firstDate.isEqual(secondDate));
  }

  @Test
  @DisplayName("应该查询所有订单用于统计")
  void shouldFindAllOrdersForStats() {
    // Given
    orderRepository.save(createTestOrder("user-1", 100.0));
    orderRepository.save(createTestOrder("user-2", 200.0));
    orderRepository.save(createTestOrder("user-3", 300.0));

    // When
    List<Order> allOrders = orderRepository.findAllOrdersForStats();

    // Then
    assertEquals(3, allOrders.size());
    // 验证所有订单都有totalAmount字段
    assertTrue(allOrders.stream().allMatch(order -> order.getTotalAmount() != null));
  }

  @Test
  @DisplayName("应该更新订单信息")
  void shouldUpdateOrderInformation() {
    // Given
    Order order = createTestOrder("user-123", 1000.0);
    Order savedOrder = orderRepository.save(order);

    // When
    savedOrder.setTotalAmount(1500.0);
    Order updatedOrder = orderRepository.save(savedOrder);

    // Then
    Optional<Order> foundOrder = orderRepository.findById(updatedOrder.getId());
    assertTrue(foundOrder.isPresent());
    assertEquals(1500.0, foundOrder.get().getTotalAmount());
  }

  @Test
  @DisplayName("应该删除订单 - 通过ID")
  void shouldDeleteOrder_byId() {
    // Given
    Order order = createTestOrder("user-123", 1000.0);
    Order savedOrder = orderRepository.save(order);

    // When
    orderRepository.deleteById(savedOrder.getId());

    // Then
    Optional<Order> foundOrder = orderRepository.findById(savedOrder.getId());
    assertFalse(foundOrder.isPresent());
  }

  @Test
  @DisplayName("应该计数所有订单")
  void shouldCountAllOrders() {
    // Given
    orderRepository.save(createTestOrder("user-1", 100.0));
    orderRepository.save(createTestOrder("user-2", 200.0));
    orderRepository.save(createTestOrder("user-3", 300.0));

    // When
    long count = orderRepository.count();

    // Then
    assertEquals(3, count);
  }

  @Test
  @DisplayName("应该正确保存时间戳字段")
  void shouldCorrectlySaveTimestampFields() {
    // Given
    Order order = createTestOrder("user-123", 1000.0);

    // When
    Order savedOrder = orderRepository.save(order);

    // Then
    assertNotNull(savedOrder.getCreatedAt());
  }

  // Helper methods
  private Order createTestOrder(String userId, double totalAmount) {
    return Order.builder()
        .userId(userId)
        .items(new ArrayList<>())
        .totalAmount(totalAmount)
        .createdAt(LocalDateTime.now())
        .build();
  }

  private Order.OrderItem createOrderItem(String phoneId, String title, int quantity, double price) {
    return Order.OrderItem.builder()
        .phoneId(phoneId)
        .title(title)
        .quantity(quantity)
        .price(price)
        .build();
  }

  /**
   * 辅助方法：直接更新MongoDB中的createdAt字段（绕过@CreatedDate）
   */
  private void updateCreatedAt(String orderId, LocalDateTime newCreatedAt) {
    Query query = new Query(Criteria.where("id").is(orderId));
    Update update = new Update().set("createdAt", newCreatedAt);
    mongoTemplate.updateFirst(query, update, Order.class);
  }
}