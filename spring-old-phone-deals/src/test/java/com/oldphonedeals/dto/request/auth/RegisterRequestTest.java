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
 * RegisterRequest DTO 验证测试
 */
@DisplayName("RegisterRequest Validation Tests")
class RegisterRequestTest {

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
    RegisterRequest request = RegisterRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .password("password123")
        .build();

    // When
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

    // Then
    assertTrue(violations.isEmpty(), "No validation errors should occur with valid data");
  }

  @Test
  @DisplayName("应该失败验证 - 当名字为 null 时")
  void shouldFailValidation_whenFirstNameIsNull() {
    // Given
    RegisterRequest request = RegisterRequest.builder()
        .firstName(null)
        .lastName("Doe")
        .email("john.doe@example.com")
        .password("password123")
        .build();

    // When
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("firstName")));
  }

  @Test
  @DisplayName("应该失败验证 - 当名字为空字符串时")
  void shouldFailValidation_whenFirstNameIsEmpty() {
    // Given
    RegisterRequest request = RegisterRequest.builder()
        .firstName("")
        .lastName("Doe")
        .email("john.doe@example.com")
        .password("password123")
        .build();

    // When
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("firstName")));
  }

  @Test
  @DisplayName("应该失败验证 - 当姓氏为 null 时")
  void shouldFailValidation_whenLastNameIsNull() {
    // Given
    RegisterRequest request = RegisterRequest.builder()
        .firstName("John")
        .lastName(null)
        .email("john.doe@example.com")
        .password("password123")
        .build();

    // When
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("lastName")));
  }

  @Test
  @DisplayName("应该失败验证 - 当姓氏为空字符串时")
  void shouldFailValidation_whenLastNameIsEmpty() {
    // Given
    RegisterRequest request = RegisterRequest.builder()
        .firstName("John")
        .lastName("")
        .email("john.doe@example.com")
        .password("password123")
        .build();

    // When
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("lastName")));
  }

  @Test
  @DisplayName("应该失败验证 - 当邮箱为 null 时")
  void shouldFailValidation_whenEmailIsNull() {
    // Given
    RegisterRequest request = RegisterRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email(null)
        .password("password123")
        .build();

    // When
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
  }

  @Test
  @DisplayName("应该失败验证 - 当邮箱格式无效时")
  void shouldFailValidation_whenEmailIsInvalid() {
    // Given
    RegisterRequest request = RegisterRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email("invalid-email")
        .password("password123")
        .build();

    // When
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("email") &&
            v.getMessage().toLowerCase().contains("valid")));
  }

  @Test
  @DisplayName("应该失败验证 - 当密码为 null 时")
  void shouldFailValidation_whenPasswordIsNull() {
    // Given
    RegisterRequest request = RegisterRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .password(null)
        .build();

    // When
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
  }

  @Test
  @DisplayName("应该失败验证 - 当密码太短时（少于6个字符）")
  void shouldFailValidation_whenPasswordIsTooShort() {
    // Given
    RegisterRequest request = RegisterRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .password("12345")
        .build();

    // When
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream()
        .anyMatch(v -> v.getPropertyPath().toString().equals("password") &&
            (v.getMessage().contains("at least 6 characters") ||
                v.getMessage().contains("size must be"))));
  }

  @Test
  @DisplayName("应该通过验证 - 当密码正好6个字符时")
  void shouldPassValidation_whenPasswordIsExactly6Characters() {
    // Given
    RegisterRequest request = RegisterRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .password("123456")
        .build();

    // When
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

    // Then
    assertTrue(violations.isEmpty(), "Password with exactly 6 characters should be valid");
  }

  @Test
  @DisplayName("应该通过验证 - 当密码很长时")
  void shouldPassValidation_whenPasswordIsLong() {
    // Given
    RegisterRequest request = RegisterRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .password("thisIsAVeryLongPasswordWithMoreThan6Characters")
        .build();

    // When
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

    // Then
    assertTrue(violations.isEmpty(), "Long password should be valid");
  }

  @Test
  @DisplayName("应该失败验证 - 当多个字段无效时")
  void shouldFailValidation_whenMultipleFieldsAreInvalid() {
    // Given
    RegisterRequest request = RegisterRequest.builder()
        .firstName("")
        .lastName("")
        .email("invalid-email")
        .password("123")
        .build();

    // When
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

    // Then
    assertFalse(violations.isEmpty());
    assertTrue(violations.size() >= 4, "Should have at least 4 violations");
  }

  @Test
  @DisplayName("应该正确构建 - 使用 Builder 模式")
  void shouldBuildCorrectly_usingBuilder() {
    // Given & When
    RegisterRequest request = RegisterRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .password("password123")
        .build();

    // Then
    assertNotNull(request);
    assertEquals("John", request.getFirstName());
    assertEquals("Doe", request.getLastName());
    assertEquals("john.doe@example.com", request.getEmail());
    assertEquals("password123", request.getPassword());
  }

  @Test
  @DisplayName("应该正确创建 - 使用无参构造器和 setter")
  void shouldCreateCorrectly_usingNoArgsConstructorAndSetters() {
    // Given & When
    RegisterRequest request = new RegisterRequest();
    request.setFirstName("John");
    request.setLastName("Doe");
    request.setEmail("john.doe@example.com");
    request.setPassword("password123");

    // Then
    assertNotNull(request);
    assertEquals("John", request.getFirstName());
    assertEquals("Doe", request.getLastName());
    assertEquals("john.doe@example.com", request.getEmail());
    assertEquals("password123", request.getPassword());
  }

  @Test
  @DisplayName("应该正确创建 - 使用全参构造器")
  void shouldCreateCorrectly_usingAllArgsConstructor() {
    // Given & When
    RegisterRequest request = new RegisterRequest("John", "Doe", "john.doe@example.com", "password123");

    // Then
    assertNotNull(request);
    assertEquals("John", request.getFirstName());
    assertEquals("Doe", request.getLastName());
    assertEquals("john.doe@example.com", request.getEmail());
    assertEquals("password123", request.getPassword());
  }

  @Test
  @DisplayName("应该接受包含特殊字符的名字")
  void shouldAcceptNamesWithSpecialCharacters() {
    // Given
    RegisterRequest request = RegisterRequest.builder()
        .firstName("Jean-Pierre")
        .lastName("O'Connor")
        .email("jean@example.com")
        .password("password123")
        .build();

    // When
    Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

    // Then
    assertTrue(violations.isEmpty(), "Names with hyphens and apostrophes should be valid");
  }

  @Test
  @DisplayName("应该接受包含数字和特殊字符的密码")
  void shouldAcceptPasswordsWithNumbersAndSpecialCharacters() {
    String[] validPasswords = {
        "Pass123!",
        "MyP@ssw0rd",
        "Secur3#Pass",
        "C0mpl3x$Pass!"
    };

    for (String password : validPasswords) {
      RegisterRequest request = RegisterRequest.builder()
          .firstName("John")
          .lastName("Doe")
          .email("john@example.com")
          .password(password)
          .build();

      Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
      assertTrue(violations.isEmpty(),
          "Password " + password + " should be valid but got violations: " + violations);
    }
  }
}