package com.oldphonedeals.repository;

import com.oldphonedeals.TestDataFactory;
import com.oldphonedeals.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserRepository 集成测试
 * <p>
 * 使用 @DataMongoTest 进行轻量级的 MongoDB 集成测试
 * </p>
 */
@DataMongoTest
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @AfterEach
  void cleanup() {
    // 清理测试数据
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("应该保存并找到用户 - 通过邮箱")
  void shouldSaveAndFindUser_byEmail() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    user.setId(null); // 让 MongoDB 自动生成 ID

    // When
    User savedUser = userRepository.save(user);
    Optional<User> foundUser = userRepository.findByEmail(user.getEmail());

    // Then
    assertNotNull(savedUser.getId());
    assertTrue(foundUser.isPresent());
    assertEquals(savedUser.getId(), foundUser.get().getId());
    assertEquals(user.getEmail(), foundUser.get().getEmail());
    assertEquals(user.getFirstName(), foundUser.get().getFirstName());
    assertEquals(user.getLastName(), foundUser.get().getLastName());
  }

  @Test
  @DisplayName("应该返回空 - 当邮箱不存在时")
  void shouldReturnEmpty_whenEmailDoesNotExist() {
    // Given
    String nonExistentEmail = "nonexistent@example.com";

    // When
    Optional<User> foundUser = userRepository.findByEmail(nonExistentEmail);

    // Then
    assertFalse(foundUser.isPresent());
  }

  @Test
  @DisplayName("应该检查邮箱是否存在")
  void shouldCheckIfEmailExists() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    user.setId(null);
    userRepository.save(user);

    // When
    boolean exists = userRepository.existsByEmail(user.getEmail());
    boolean notExists = userRepository.existsByEmail("other@example.com");

    // Then
    assertTrue(exists);
    assertFalse(notExists);
  }

  @Test
  @DisplayName("应该通过验证令牌找到用户")
  void shouldFindUser_byVerifyToken() {
    // Given
    User user = TestDataFactory.createUnverifiedUser();
    user.setId(null);
    String verifyToken = "test-token-12345";
    user.setVerifyToken(verifyToken);
    userRepository.save(user);

    // When
    Optional<User> foundUser = userRepository.findByVerifyToken(verifyToken);

    // Then
    assertTrue(foundUser.isPresent());
    assertEquals(user.getEmail(), foundUser.get().getEmail());
    assertEquals(verifyToken, foundUser.get().getVerifyToken());
  }

  @Test
  @DisplayName("应该返回空 - 当验证令牌不存在时")
  void shouldReturnEmpty_whenVerifyTokenDoesNotExist() {
    // Given
    String nonExistentToken = "nonexistent-token";

    // When
    Optional<User> foundUser = userRepository.findByVerifyToken(nonExistentToken);

    // Then
    assertFalse(foundUser.isPresent());
  }

  @Test
  @DisplayName("应该更新用户信息")
  void shouldUpdateUserInformation() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    user.setId(null);
    User savedUser = userRepository.save(user);

    // When
    savedUser.setFirstName("Updated");
    savedUser.setLastName("Name");
    User updatedUser = userRepository.save(savedUser);

    // Then
    Optional<User> foundUser = userRepository.findById(updatedUser.getId());
    assertTrue(foundUser.isPresent());
    assertEquals("Updated", foundUser.get().getFirstName());
    assertEquals("Name", foundUser.get().getLastName());
  }

  @Test
  @DisplayName("应该删除用户")
  void shouldDeleteUser() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    user.setId(null);
    User savedUser = userRepository.save(user);

    // When
    userRepository.deleteById(savedUser.getId());

    // Then
    Optional<User> foundUser = userRepository.findById(savedUser.getId());
    assertFalse(foundUser.isPresent());
  }

  @Test
  @DisplayName("应该保存管理员用户")
  void shouldSaveAdminUser() {
    // Given
    User adminUser = TestDataFactory.createAdminUser();
    adminUser.setId(null);

    // When
    User savedUser = userRepository.save(adminUser);

    // Then
    assertNotNull(savedUser.getId());
    assertTrue(savedUser.getIsAdmin());
    Optional<User> foundUser = userRepository.findByEmail(adminUser.getEmail());
    assertTrue(foundUser.isPresent());
    assertTrue(foundUser.get().getIsAdmin());
  }

  @Test
  @DisplayName("应该保存被禁用的用户")
  void shouldSaveDisabledUser() {
    // Given
    User disabledUser = TestDataFactory.createDisabledUser();
    disabledUser.setId(null);

    // When
    User savedUser = userRepository.save(disabledUser);

    // Then
    assertNotNull(savedUser.getId());
    assertTrue(savedUser.getIsDisabled());
    Optional<User> foundUser = userRepository.findById(savedUser.getId());
    assertTrue(foundUser.isPresent());
    assertTrue(foundUser.get().getIsDisabled());
  }

  @Test
  @DisplayName("应该保存被封禁的用户")
  void shouldSaveBannedUser() {
    // Given
    User bannedUser = TestDataFactory.createBannedUser();
    bannedUser.setId(null);

    // When
    User savedUser = userRepository.save(bannedUser);

    // Then
    assertNotNull(savedUser.getId());
    assertTrue(savedUser.getIsBan());
    Optional<User> foundUser = userRepository.findById(savedUser.getId());
    assertTrue(foundUser.isPresent());
    assertTrue(foundUser.get().getIsBan());
  }

  @Test
  @DisplayName("应该处理愿望清单")
  void shouldHandleWishlist() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    user.setId(null);
    user.getWishlist().add("phone-id-1");
    user.getWishlist().add("phone-id-2");

    // When
    User savedUser = userRepository.save(user);

    // Then
    Optional<User> foundUser = userRepository.findById(savedUser.getId());
    assertTrue(foundUser.isPresent());
    assertEquals(2, foundUser.get().getWishlist().size());
    assertTrue(foundUser.get().getWishlist().contains("phone-id-1"));
    assertTrue(foundUser.get().getWishlist().contains("phone-id-2"));
  }

  @Test
  @DisplayName("应该保持邮箱唯一性")
  void shouldMaintainEmailUniqueness() {
    // Given
    User user1 = TestDataFactory.createDefaultUser();
    user1.setId(null);
    userRepository.save(user1);

    User user2 = TestDataFactory.createDefaultUser();
    user2.setId(null);

    // When & Then
    // 尝试保存相同邮箱应该失败
    assertThrows(Exception.class, () -> userRepository.save(user2));
  }

  @Test
  @DisplayName("应该计数所有用户")
  void shouldCountAllUsers() {
    // Given
    User user1 = TestDataFactory.createDefaultUser();
    user1.setId(null);
    user1.setEmail("user1@example.com");

    User user2 = TestDataFactory.createDefaultUser();
    user2.setId(null);
    user2.setEmail("user2@example.com");

    // When
    userRepository.save(user1);
    userRepository.save(user2);
    long count = userRepository.count();

    // Then
    assertEquals(2, count);
  }

  @Test
  @DisplayName("应该查找所有用户")
  void shouldFindAllUsers() {
    // Given
    User user1 = TestDataFactory.createDefaultUser();
    user1.setId(null);
    user1.setEmail("user1@example.com");

    User user2 = TestDataFactory.createDefaultUser();
    user2.setId(null);
    user2.setEmail("user2@example.com");

    // When
    userRepository.save(user1);
    userRepository.save(user2);
    var allUsers = userRepository.findAll();

    // Then
    assertEquals(2, allUsers.size());
  }

  @Test
  @DisplayName("应该正确保存时间戳字段")
  void shouldCorrectlySaveTimestampFields() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    user.setId(null);

    // When
    User savedUser = userRepository.save(user);

    // Then
    assertNotNull(savedUser.getCreatedAt());
    assertNotNull(savedUser.getUpdatedAt());
  }
}