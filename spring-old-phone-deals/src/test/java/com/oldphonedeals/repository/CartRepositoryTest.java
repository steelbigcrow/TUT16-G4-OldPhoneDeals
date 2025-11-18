package com.oldphonedeals.repository;

import com.oldphonedeals.TestDataFactory;
import com.oldphonedeals.entity.Cart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CartRepository 集成测试
 * <p>
 * 使用 @DataMongoTest 进行轻量级的 MongoDB 集成测试
 * </p>
 */
@DataMongoTest
@ActiveProfiles("test")
@DisplayName("CartRepository Integration Tests")
class CartRepositoryTest {

  @Autowired
  private CartRepository cartRepository;

  @AfterEach
  void cleanup() {
    cartRepository.deleteAll();
  }

  @Test
  @DisplayName("应该保存并找到购物车 - 通过用户ID")
  void shouldSaveAndFindCart_byUserId() {
    // Given
    String userId = "user-123";
    Cart cart = createTestCart(userId);

    // When
    Cart savedCart = cartRepository.save(cart);
    Optional<Cart> foundCart = cartRepository.findByUserId(userId);

    // Then
    assertNotNull(savedCart.getId());
    assertTrue(foundCart.isPresent());
    assertEquals(userId, foundCart.get().getUserId());
  }

  @Test
  @DisplayName("应该返回空 - 当用户购物车不存在时")
  void shouldReturnEmpty_whenCartDoesNotExist() {
    // Given
    String nonExistentUserId = "non-existent-user";

    // When
    Optional<Cart> foundCart = cartRepository.findByUserId(nonExistentUserId);

    // Then
    assertFalse(foundCart.isPresent());
  }

  @Test
  @DisplayName("应该检查用户购物车是否存在")
  void shouldCheckIfCartExistsByUserId() {
    // Given
    String userId = "user-123";
    Cart cart = createTestCart(userId);
    cartRepository.save(cart);

    // When
    boolean exists = cartRepository.existsByUserId(userId);
    boolean notExists = cartRepository.existsByUserId("other-user");

    // Then
    assertTrue(exists);
    assertFalse(notExists);
  }

  @Test
  @DisplayName("应该删除用户购物车 - 通过用户ID")
  void shouldDeleteCart_byUserId() {
    // Given
    String userId = "user-123";
    Cart cart = createTestCart(userId);
    cartRepository.save(cart);

    // When
    cartRepository.deleteByUserId(userId);

    // Then
    Optional<Cart> foundCart = cartRepository.findByUserId(userId);
    assertFalse(foundCart.isPresent());
  }

  @Test
  @DisplayName("应该保存带有商品的购物车")
  void shouldSaveCartWithItems() {
    // Given
    String userId = "user-123";
    Cart cart = createTestCart(userId);
    
    Cart.CartItem item1 = createCartItem("phone-1", "iPhone 13", 2, 799.99);
    Cart.CartItem item2 = createCartItem("phone-2", "Galaxy S21", 1, 699.99);
    
    cart.getItems().add(item1);
    cart.getItems().add(item2);

    // When
    Cart savedCart = cartRepository.save(cart);

    // Then
    Optional<Cart> foundCart = cartRepository.findByUserId(userId);
    assertTrue(foundCart.isPresent());
    assertEquals(2, foundCart.get().getItems().size());
    assertEquals("phone-1", foundCart.get().getItems().get(0).getPhoneId());
    assertEquals("iPhone 13", foundCart.get().getItems().get(0).getTitle());
    assertEquals(2, foundCart.get().getItems().get(0).getQuantity());
  }

  @Test
  @DisplayName("应该更新购物车商品")
  void shouldUpdateCartItems() {
    // Given
    String userId = "user-123";
    Cart cart = createTestCart(userId);
    Cart.CartItem item = createCartItem("phone-1", "iPhone 13", 2, 799.99);
    cart.getItems().add(item);
    Cart savedCart = cartRepository.save(cart);

    // When
    savedCart.getItems().get(0).setQuantity(5);
    Cart updatedCart = cartRepository.save(savedCart);

    // Then
    Optional<Cart> foundCart = cartRepository.findByUserId(userId);
    assertTrue(foundCart.isPresent());
    assertEquals(5, foundCart.get().getItems().get(0).getQuantity());
  }

  @Test
  @DisplayName("应该查找包含指定商品的所有购物车")
  void shouldFindCartsContainingPhone() {
    // Given
    String phoneId = "phone-123";
    
    Cart cart1 = createTestCart("user-1");
    cart1.getItems().add(createCartItem(phoneId, "iPhone 13", 2, 799.99));
    cart1.getItems().add(createCartItem("phone-456", "Galaxy S21", 1, 699.99));
    
    Cart cart2 = createTestCart("user-2");
    cart2.getItems().add(createCartItem(phoneId, "iPhone 13", 1, 799.99));
    
    Cart cart3 = createTestCart("user-3");
    cart3.getItems().add(createCartItem("phone-789", "Pixel 6", 1, 599.99));

    cartRepository.save(cart1);
    cartRepository.save(cart2);
    cartRepository.save(cart3);

    // When
    List<Cart> cartsWithPhone = cartRepository.findCartsContainingPhone(phoneId);

    // Then
    assertEquals(2, cartsWithPhone.size());
    assertTrue(cartsWithPhone.stream()
        .allMatch(cart -> cart.getItems().stream()
            .anyMatch(item -> item.getPhoneId().equals(phoneId))));
  }

  @Test
  @DisplayName("应该返回空列表 - 当没有购物车包含指定商品时")
  void shouldReturnEmptyList_whenNoCartsContainPhone() {
    // Given
    String phoneId = "non-existent-phone";
    
    Cart cart = createTestCart("user-1");
    cart.getItems().add(createCartItem("phone-123", "iPhone 13", 2, 799.99));
    cartRepository.save(cart);

    // When
    List<Cart> cartsWithPhone = cartRepository.findCartsContainingPhone(phoneId);

    // Then
    assertTrue(cartsWithPhone.isEmpty());
  }

  @Test
  @DisplayName("应该删除购物车中的商品")
  void shouldRemoveItemFromCart() {
    // Given
    String userId = "user-123";
    Cart cart = createTestCart(userId);
    cart.getItems().add(createCartItem("phone-1", "iPhone 13", 2, 799.99));
    cart.getItems().add(createCartItem("phone-2", "Galaxy S21", 1, 699.99));
    Cart savedCart = cartRepository.save(cart);

    // When
    savedCart.getItems().remove(0);
    Cart updatedCart = cartRepository.save(savedCart);

    // Then
    Optional<Cart> foundCart = cartRepository.findByUserId(userId);
    assertTrue(foundCart.isPresent());
    assertEquals(1, foundCart.get().getItems().size());
    assertEquals("phone-2", foundCart.get().getItems().get(0).getPhoneId());
  }

  @Test
  @DisplayName("应该清空购物车所有商品")
  void shouldClearAllItemsFromCart() {
    // Given
    String userId = "user-123";
    Cart cart = createTestCart(userId);
    cart.getItems().add(createCartItem("phone-1", "iPhone 13", 2, 799.99));
    cart.getItems().add(createCartItem("phone-2", "Galaxy S21", 1, 699.99));
    Cart savedCart = cartRepository.save(cart);

    // When
    savedCart.getItems().clear();
    Cart updatedCart = cartRepository.save(savedCart);

    // Then
    Optional<Cart> foundCart = cartRepository.findByUserId(userId);
    assertTrue(foundCart.isPresent());
    assertTrue(foundCart.get().getItems().isEmpty());
  }

  @Test
  @DisplayName("应该保持用户ID的唯一性")
  void shouldMaintainUserIdUniqueness() {
    // Given
    String userId = "user-123";
    Cart cart1 = createTestCart(userId);
    cartRepository.save(cart1);

    Cart cart2 = createTestCart(userId);

    // When & Then
    // 尝试保存相同用户ID的购物车应该失败或更新现有购物车
    assertThrows(Exception.class, () -> cartRepository.save(cart2));
  }

  @Test
  @DisplayName("应该删除购物车 - 通过ID")
  void shouldDeleteCart_byId() {
    // Given
    String userId = "user-123";
    Cart cart = createTestCart(userId);
    Cart savedCart = cartRepository.save(cart);

    // When
    cartRepository.deleteById(savedCart.getId());

    // Then
    Optional<Cart> foundCart = cartRepository.findById(savedCart.getId());
    assertFalse(foundCart.isPresent());
  }

  @Test
  @DisplayName("应该计数所有购物车")
  void shouldCountAllCarts() {
    // Given
    cartRepository.save(createTestCart("user-1"));
    cartRepository.save(createTestCart("user-2"));
    cartRepository.save(createTestCart("user-3"));

    // When
    long count = cartRepository.count();

    // Then
    assertEquals(3, count);
  }

  @Test
  @DisplayName("应该查找所有购物车")
  void shouldFindAllCarts() {
    // Given
    cartRepository.save(createTestCart("user-1"));
    cartRepository.save(createTestCart("user-2"));

    // When
    List<Cart> allCarts = cartRepository.findAll();

    // Then
    assertEquals(2, allCarts.size());
  }

  @Test
  @DisplayName("应该正确保存时间戳字段")
  void shouldCorrectlySaveTimestampFields() {
    // Given
    String userId = "user-123";
    Cart cart = createTestCart(userId);

    // When
    Cart savedCart = cartRepository.save(cart);

    // Then
    assertNotNull(savedCart.getCreatedAt());
    assertNotNull(savedCart.getUpdatedAt());
  }

  // Helper methods
  private Cart createTestCart(String userId) {
    return Cart.builder()
        .userId(userId)
        .items(new ArrayList<>())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  private Cart.CartItem createCartItem(String phoneId, String title, int quantity, double price) {
    return Cart.CartItem.builder()
        .phoneId(phoneId)
        .title(title)
        .quantity(quantity)
        .price(price)
        .createdAt(LocalDateTime.now())
        .build();
  }
}