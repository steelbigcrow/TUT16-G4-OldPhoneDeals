package com.oldphonedeals;

import com.oldphonedeals.entity.User;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.Cart;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.enums.PhoneBrand;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * 测试数据工厂类
 * <p>
 * 提供便捷的方法来创建测试所需的实体对象，避免在每个测试中重复创建数据。
 * </p>
 */
public class TestDataFactory {

  /**
   * 创建默认的测试用户
   */
  public static User createDefaultUser() {
    return User.builder()
        .id("test-user-id-123")
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .password("$2a$10$hashedPassword123") // BCrypt hashed password
        .wishlist(new ArrayList<>())
        .isAdmin(false)
        .isDisabled(false)
        .isBan(false)
        .isVerified(true)
        .verifyToken(null)
        .lastLogin(LocalDateTime.now())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /**
   * 创建管理员用户
   */
  public static User createAdminUser() {
    return User.builder()
        .id("admin-user-id-456")
        .firstName("Admin")
        .lastName("User")
        .email("admin@example.com")
        .password("$2a$10$hashedPassword456")
        .wishlist(new ArrayList<>())
        .isAdmin(true)
        .isDisabled(false)
        .isBan(false)
        .isVerified(true)
        .verifyToken(null)
        .lastLogin(LocalDateTime.now())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /**
   * 创建未验证的用户
   */
  public static User createUnverifiedUser() {
    return User.builder()
        .id("unverified-user-id-789")
        .firstName("Jane")
        .lastName("Smith")
        .email("jane.smith@example.com")
        .password("$2a$10$hashedPassword789")
        .wishlist(new ArrayList<>())
        .isAdmin(false)
        .isDisabled(false)
        .isBan(false)
        .isVerified(false)
        .verifyToken("test-verify-token-abc123")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /**
   * 创建被禁用的用户
   */
  public static User createDisabledUser() {
    return User.builder()
        .id("disabled-user-id-101")
        .firstName("Disabled")
        .lastName("User")
        .email("disabled@example.com")
        .password("$2a$10$hashedPassword101")
        .wishlist(new ArrayList<>())
        .isAdmin(false)
        .isDisabled(true)
        .isBan(false)
        .isVerified(true)
        .verifyToken(null)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /**
   * 创建被封禁的用户
   */
  public static User createBannedUser() {
    return User.builder()
        .id("banned-user-id-102")
        .firstName("Banned")
        .lastName("User")
        .email("banned@example.com")
        .password("$2a$10$hashedPassword102")
        .wishlist(new ArrayList<>())
        .isAdmin(false)
        .isDisabled(false)
        .isBan(true)
        .isVerified(true)
        .verifyToken(null)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /**
   * 创建自定义用户
   */
  public static User createUser(String id, String email, String firstName, String lastName, boolean isAdmin) {
    return User.builder()
        .id(id)
        .firstName(firstName)
        .lastName(lastName)
        .email(email)
        .password("$2a$10$hashedPassword")
        .wishlist(new ArrayList<>())
        .isAdmin(isAdmin)
        .isDisabled(false)
        .isBan(false)
        .isVerified(true)
        .verifyToken(null)
        .lastLogin(LocalDateTime.now())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /**
   * 创建默认的测试手机
   */
  public static Phone createDefaultPhone() {
    return Phone.builder()
        .id("test-phone-id-123")
        .brand(PhoneBrand.APPLE)
        .title("iPhone 12")
        .price(699.99)
        .stock(10)
        .image("/images/iphone12.jpg")
        .seller(null) // Will be set separately if needed
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /**
   * 创建自定义手机
   */
  public static Phone createPhone(String id, PhoneBrand brand, String title, double price, int stock) {
    return Phone.builder()
        .id(id)
        .brand(brand)
        .title(title)
        .price(price)
        .stock(stock)
        .image("/images/test.jpg")
        .seller(null)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /**
   * 创建库存不足的手机
   */
  public static Phone createOutOfStockPhone() {
    return Phone.builder()
        .id("out-of-stock-phone-123")
        .brand(PhoneBrand.SAMSUNG)
        .title("Galaxy S20")
        .price(599.99)
        .stock(0)
        .image("/images/galaxy-s20.jpg")
        .seller(null)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /**
   * 创建默认的购物车
   */
  public static Cart createDefaultCart(String userId) {
    return Cart.builder()
        .id("test-cart-id-123")
        .userId(userId)
        .items(new ArrayList<>())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  /**
   * 创建默认的订单
   */
  public static Order createDefaultOrder(String userId) {
    return Order.builder()
        .id("test-order-id-123")
        .userId(userId)
        .items(new ArrayList<>())
        .totalAmount(0.0)
        .createdAt(LocalDateTime.now())
        .build();
  }

  /**
   * 生成测试 JWT Secret（Base64 编码，至少 256 位）
   */
  public static String generateTestJwtSecret() {
    return "dGVzdC1zZWNyZXQta2V5LXBsZWFzZS1jaGFuZ2UtaW4tcHJvZHVjdGlvbi1hdC1sZWFzdC0yNTYtYml0cy1sb25nLWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHk=";
  }

  /**
   * 生成测试 JWT 过期时间（1小时）
   */
  public static long getTestJwtExpiration() {
    return 3600000L; // 1 hour in milliseconds
  }
}