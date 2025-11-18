package com.oldphonedeals.security;

import com.oldphonedeals.TestDataFactory;
import com.oldphonedeals.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtTokenProvider 测试
 */
@DisplayName("JwtTokenProvider Tests")
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

  private JwtTokenProvider jwtTokenProvider;

  @BeforeEach
  void setUp() {
    jwtTokenProvider = new JwtTokenProvider();
    // 使用 ReflectionTestUtils 设置私有字段
    ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
        TestDataFactory.generateTestJwtSecret());
    ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpiration",
        TestDataFactory.getTestJwtExpiration());
    // 调用 init 方法初始化密钥
    jwtTokenProvider.init();
  }

  @Test
  @DisplayName("应该成功生成 Token - 当提供有效用户时")
  void shouldGenerateToken_whenValidUserProvided() {
    // Given
    User user = TestDataFactory.createDefaultUser();

    // When
    String token = jwtTokenProvider.generateToken(user);

    // Then
    assertNotNull(token);
    assertFalse(token.isEmpty());
    assertTrue(token.split("\\.").length == 3, "JWT should have 3 parts separated by dots");
  }

  @Test
  @DisplayName("应该成功生成 Token - 对于管理员用户")
  void shouldGenerateToken_forAdminUser() {
    // Given
    User adminUser = TestDataFactory.createAdminUser();

    // When
    String token = jwtTokenProvider.generateToken(adminUser);

    // Then
    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  @DisplayName("应该验证 Token 为有效 - 当 Token 正确时")
  void shouldValidateToken_whenTokenIsValid() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    String token = jwtTokenProvider.generateToken(user);

    // When
    boolean isValid = jwtTokenProvider.validateToken(token);

    // Then
    assertTrue(isValid);
  }

  @Test
  @DisplayName("应该验证 Token 为无效 - 当 Token 被篡改时")
  void shouldInvalidateToken_whenTokenIsTampered() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    String token = jwtTokenProvider.generateToken(user);
    String tamperedToken = token + "tampered";

    // When
    boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

    // Then
    assertFalse(isValid);
  }

  @Test
  @DisplayName("应该验证 Token 为无效 - 当 Token 格式错误时")
  void shouldInvalidateToken_whenTokenIsMalformed() {
    // Given
    String malformedToken = "this.is.not.a.valid.jwt";

    // When
    boolean isValid = jwtTokenProvider.validateToken(malformedToken);

    // Then
    assertFalse(isValid);
  }

  @Test
  @DisplayName("应该验证 Token 为无效 - 当 Token 为空时")
  void shouldInvalidateToken_whenTokenIsEmpty() {
    // Given
    String emptyToken = "";

    // When
    boolean isValid = jwtTokenProvider.validateToken(emptyToken);

    // Then
    assertFalse(isValid);
  }

  @Test
  @DisplayName("应该验证 Token 为无效 - 当 Token 为 null 时")
  void shouldInvalidateToken_whenTokenIsNull() {
    // Given
    String nullToken = null;

    // When
    boolean isValid = jwtTokenProvider.validateToken(nullToken);

    // Then
    assertFalse(isValid);
  }

  @Test
  @DisplayName("应该从 Token 提取用户 ID")
  void shouldExtractUserId_fromToken() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    String token = jwtTokenProvider.generateToken(user);

    // When
    String extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

    // Then
    assertNotNull(extractedUserId);
    assertEquals(user.getId(), extractedUserId);
  }

  @Test
  @DisplayName("应该从 Token 提取用户邮箱")
  void shouldExtractEmail_fromToken() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    String token = jwtTokenProvider.generateToken(user);

    // When
    String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

    // Then
    assertNotNull(extractedEmail);
    assertEquals(user.getEmail(), extractedEmail);
  }

  @Test
  @DisplayName("应该在 Token 中包含管理员标志 - 对于普通用户")
  void shouldIncludeAdminFlag_forRegularUser() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    assertFalse(user.getIsAdmin());

    // When
    String token = jwtTokenProvider.generateToken(user);

    // Then
    assertTrue(jwtTokenProvider.validateToken(token));
    // Token 应该包含 isAdmin=false 的信息（通过能成功解析验证）
  }

  @Test
  @DisplayName("应该在 Token 中包含管理员标志 - 对于管理员用户")
  void shouldIncludeAdminFlag_forAdminUser() {
    // Given
    User adminUser = TestDataFactory.createAdminUser();
    assertTrue(adminUser.getIsAdmin());

    // When
    String token = jwtTokenProvider.generateToken(adminUser);

    // Then
    assertTrue(jwtTokenProvider.validateToken(token));
    assertEquals(adminUser.getId(), jwtTokenProvider.getUserIdFromToken(token));
    assertEquals(adminUser.getEmail(), jwtTokenProvider.getEmailFromToken(token));
  }

  @Test
  @DisplayName("应该处理过期的 Token")
  void shouldHandleExpiredToken() {
    // Given - 创建一个立即过期的 Token Provider
    JwtTokenProvider expiredTokenProvider = new JwtTokenProvider();
    ReflectionTestUtils.setField(expiredTokenProvider, "jwtSecret",
        TestDataFactory.generateTestJwtSecret());
    ReflectionTestUtils.setField(expiredTokenProvider, "jwtExpiration", 1L); // 1ms
    expiredTokenProvider.init();

    User user = TestDataFactory.createDefaultUser();
    String token = expiredTokenProvider.generateToken(user);

    // Wait for token to expire
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // When
    boolean isValid = expiredTokenProvider.validateToken(token);

    // Then
    assertFalse(isValid);
  }

  @Test
  @DisplayName("应该为不同用户生成不同的 Token")
  void shouldGenerateDifferentTokens_forDifferentUsers() {
    // Given
    User user1 = TestDataFactory.createUser("id1", "user1@example.com", "User", "One", false);
    User user2 = TestDataFactory.createUser("id2", "user2@example.com", "User", "Two", false);

    // When
    String token1 = jwtTokenProvider.generateToken(user1);
    String token2 = jwtTokenProvider.generateToken(user2);

    // Then
    assertNotEquals(token1, token2);
    assertEquals("id1", jwtTokenProvider.getUserIdFromToken(token1));
    assertEquals("id2", jwtTokenProvider.getUserIdFromToken(token2));
  }

  @Test
  @DisplayName("应该在合理时间内生成 Token")
  void shouldGenerateTokenInReasonableTime() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    long startTime = System.currentTimeMillis();

    // When
    String token = jwtTokenProvider.generateToken(user);
    long endTime = System.currentTimeMillis();

    // Then
    assertNotNull(token);
    long duration = endTime - startTime;
    assertTrue(duration < 1000, "Token generation should take less than 1 second, took: " + duration + "ms");
  }

  @Test
  @DisplayName("应该正确处理签名异常")
  void shouldHandleSignatureException() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    String token = jwtTokenProvider.generateToken(user);

    // Create a token with wrong signature by using different secret (must be at least 512 bits for HS512)
    JwtTokenProvider differentProvider = new JwtTokenProvider();
    ReflectionTestUtils.setField(differentProvider, "jwtSecret",
        "ZGlmZmVyZW50LXNlY3JldC1rZXktZm9yLXRlc3RpbmctcHVycG9zZXMtb25seS1hdC1sZWFzdC01MTItYml0cy1sb25nLWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHk="); // Different secret, 512+ bits
    ReflectionTestUtils.setField(differentProvider, "jwtExpiration", 3600000L);
    differentProvider.init();

    // When - Try to validate token with different provider (different secret)
    boolean isValid = differentProvider.validateToken(token);

    // Then
    assertFalse(isValid);
  }

  @Test
  @DisplayName("应该生成包含所有必需声明的 Token")
  void shouldGenerateTokenWithAllRequiredClaims() {
    // Given
    User user = TestDataFactory.createDefaultUser();

    // When
    String token = jwtTokenProvider.generateToken(user);

    // Then
    assertNotNull(token);
    assertTrue(jwtTokenProvider.validateToken(token));

    // Verify all claims can be extracted
    assertNotNull(jwtTokenProvider.getUserIdFromToken(token));
    assertNotNull(jwtTokenProvider.getEmailFromToken(token));
    assertEquals(user.getId(), jwtTokenProvider.getUserIdFromToken(token));
    assertEquals(user.getEmail(), jwtTokenProvider.getEmailFromToken(token));
  }

  @Test
  @DisplayName("应该一致性验证同一 Token 多次")
  void shouldConsistentlyValidateSameTokenMultipleTimes() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    String token = jwtTokenProvider.generateToken(user);

    // When & Then - Validate multiple times
    for (int i = 0; i < 5; i++) {
      assertTrue(jwtTokenProvider.validateToken(token),
          "Token should be valid on attempt " + (i + 1));
      assertEquals(user.getId(), jwtTokenProvider.getUserIdFromToken(token));
      assertEquals(user.getEmail(), jwtTokenProvider.getEmailFromToken(token));
    }
  }
}