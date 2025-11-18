package com.oldphonedeals.dto.request.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoginRequest DTO 验证测试
 */
@DisplayName("LoginRequest Validation Tests")
class LoginRequestTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("应该通过验证 - 当所有字段都有效时")
  void shouldPassValidation_whenAllFieldsAreValid() {
    // Given
    LoginRequest request = LoginRequest.builder()
        .email("test@example.com")
        .password("password123")
        .build();

    // When
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // Then
    assertTrue(violations.isEmpty(), "No validation errors should occur with valid data");
  }

  @Test
  @DisplayName("应该失败验证 - 当邮箱为 null 时")
  void shouldFailValidation_whenEmailIsNull() {
    // Given
    LoginRequest request = LoginRequest.builder()
        .email(null)
        .password("password123")
        .build();

    // When
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty(), "Should have validation errors");
    assertEquals(1, violations.size());
    ConstraintViolation<LoginRequest> violation = violations.iterator().next();
    assertEquals("email", violation.getPropertyPath().toString());
    assertTrue(violation.getMessage().contains("Email is required"));
  }

  @Test
  @DisplayName("应该失败验证 - 当邮箱为空字符串时")
  void shouldFailValidation_whenEmailIsEmpty() {
    // Given
    LoginRequest request = LoginRequest.builder()
        .email("")
        .password("password123")
        .build();

    // When
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty(), "Should have validation errors");
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
  }

  @Test
  @DisplayName("应该失败验证 - 当邮箱为空白字符时")
  void shouldFailValidation_whenEmailIsBlank() {
    // Given
    LoginRequest request = LoginRequest.builder()
        .email("   ")
        .password("password123")
        .build();

    // When
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty(), "Should have validation errors");
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
  }

  @Test
  @DisplayName("应该失败验证 - 当邮箱格式无效时")
  void shouldFailValidation_whenEmailIsInvalid() {
    // Given
    LoginRequest request = LoginRequest.builder()
        .email("invalid-email")
        .password("password123")
        .build();

    // When
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty(), "Should have validation errors");
    assertEquals(1, violations.size());
    ConstraintViolation<LoginRequest> violation = violations.iterator().next();
    assertEquals("email", violation.getPropertyPath().toString());
    assertTrue(violation.getMessage().toLowerCase().contains("valid"));
  }

  @Test
  @DisplayName("应该失败验证 - 当密码为 null 时")
  void shouldFailValidation_whenPasswordIsNull() {
    // Given
    LoginRequest request = LoginRequest.builder()
        .email("test@example.com")
        .password(null)
        .build();

    // When
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty(), "Should have validation errors");
    assertEquals(1, violations.size());
    ConstraintViolation<LoginRequest> violation = violations.iterator().next();
    assertEquals("password", violation.getPropertyPath().toString());
    assertTrue(violation.getMessage().contains("Password is required"));
  }

  @Test
  @DisplayName("应该失败验证 - 当密码为空字符串时")
  void shouldFailValidation_whenPasswordIsEmpty() {
    // Given
    LoginRequest request = LoginRequest.builder()
        .email("test@example.com")
        .password("")
        .build();

    // When
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty(), "Should have validation errors");
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
  }

  @Test
  @DisplayName("应该失败验证 - 当密码为空白字符时")
  void shouldFailValidation_whenPasswordIsBlank() {
    // Given
    LoginRequest request = LoginRequest.builder()
        .email("test@example.com")
        .password("   ")
        .build();

    // When
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty(), "Should have validation errors");
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
  }

  @Test
  @DisplayName("应该失败验证 - 当邮箱和密码都无效时")
  void shouldFailValidation_whenBothFieldsAreInvalid() {
    // Given
    LoginRequest request = LoginRequest.builder()
        .email("invalid-email")
        .password("")
        .build();

    // When
    Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty(), "Should have validation errors");
    assertTrue(violations.size() >= 2, "Should have at least 2 violations");
  }

  @Test
  @DisplayName("应该正确构建 - 使用 Builder 模式")
  void shouldBuildCorrectly_usingBuilder() {
    // Given & When
    LoginRequest request = LoginRequest.builder()
        .email("test@example.com")
        .password("password123")
        .build();

    // Then
    assertNotNull(request);
    assertEquals("test@example.com", request.getEmail());
    assertEquals("password123", request.getPassword());
  }

  @Test
  @DisplayName("应该正确创建 - 使用无参构造器")
  void shouldCreateCorrectly_usingNoArgsConstructor() {
    // Given & When
    LoginRequest request = new LoginRequest();
    request.setEmail("test@example.com");
    request.setPassword("password123");

    // Then
    assertNotNull(request);
    assertEquals("test@example.com", request.getEmail());
    assertEquals("password123", request.getPassword());
  }

  @Test
  @DisplayName("应该接受各种有效的邮箱格式")
  void shouldAcceptVariousValidEmailFormats() {
    String[] validEmails = {
        "user@example.com",
        "user.name@example.com",
        "user+tag@example.co.uk",
        "user_123@example-domain.com"
    };

    for (String email : validEmails) {
      LoginRequest request = LoginRequest.builder()
          .email(email)
          .password("password123")
          .build();

      Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
      assertTrue(violations.isEmpty(),
          "Email " + email + " should be valid but got violations: " + violations);
    }
  }
}