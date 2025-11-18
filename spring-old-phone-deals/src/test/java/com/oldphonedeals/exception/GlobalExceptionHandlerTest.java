package com.oldphonedeals.exception;

import com.oldphonedeals.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GlobalExceptionHandler 测试
 */
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
  }

  @Test
  @DisplayName("应该处理 ResourceNotFoundException - 返回 404")
  void shouldHandleResourceNotFoundException_return404() {
    // Given
    String errorMessage = "Resource not found";
    ResourceNotFoundException exception = new ResourceNotFoundException(errorMessage);

    // When
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleResourceNotFoundException(exception);

    // Then
    assertNotNull(response);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertEquals(errorMessage, response.getBody().getMessage());
  }

  @Test
  @DisplayName("应该处理 UnauthorizedException - 返回 401")
  void shouldHandleUnauthorizedException_return401() {
    // Given
    String errorMessage = "Unauthorized access";
    UnauthorizedException exception = new UnauthorizedException(errorMessage);

    // When
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleUnauthorizedException(exception);

    // Then
    assertNotNull(response);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertEquals(errorMessage, response.getBody().getMessage());
  }

  @Test
  @DisplayName("应该处理 BadRequestException - 返回 400")
  void shouldHandleBadRequestException_return400() {
    // Given
    String errorMessage = "Bad request";
    BadRequestException exception = new BadRequestException(errorMessage);

    // When
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBadRequestException(exception);

    // Then
    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertEquals(errorMessage, response.getBody().getMessage());
  }

  @Test
  @DisplayName("应该处理 DuplicateResourceException - 返回 409")
  void shouldHandleDuplicateResourceException_return409() {
    // Given
    String errorMessage = "Resource already exists";
    DuplicateResourceException exception = new DuplicateResourceException(errorMessage);

    // When
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleDuplicateResourceException(exception);

    // Then
    assertNotNull(response);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertEquals(errorMessage, response.getBody().getMessage());
  }

  @Test
  @DisplayName("应该处理 InsufficientStockException - 返回 400")
  void shouldHandleInsufficientStockException_return400() {
    // Given
    String errorMessage = "Insufficient stock";
    InsufficientStockException exception = new InsufficientStockException(errorMessage);

    // When
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleInsufficientStockException(exception);

    // Then
    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertEquals(errorMessage, response.getBody().getMessage());
  }

  @Test
  @DisplayName("应该处理 ForbiddenException - 返回 403")
  void shouldHandleForbiddenException_return403() {
    // Given
    String errorMessage = "Forbidden access";
    ForbiddenException exception = new ForbiddenException(errorMessage);

    // When
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleForbiddenException(exception);

    // Then
    assertNotNull(response);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertEquals(errorMessage, response.getBody().getMessage());
  }

  @Test
  @DisplayName("应该处理 MethodArgumentNotValidException - 返回 400 和所有验证错误")
  void shouldHandleValidationException_return400WithAllErrors() {
    // Given
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError error1 = new FieldError("object", "field1", "must not be null");
    FieldError error2 = new FieldError("object", "field2", "must be valid email");
    List<FieldError> fieldErrors = Arrays.asList(error1, error2);

    when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(error1, error2));

    MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

    // When
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleValidationException(exception);

    // Then
    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertTrue(response.getBody().getMessage().contains("Validation failed"));
    assertTrue(response.getBody().getMessage().contains("field1"));
    assertTrue(response.getBody().getMessage().contains("field2"));
  }

  @Test
  @DisplayName("应该处理通用 Exception - 返回 500")
  void shouldHandleGeneralException_return500() {
    // Given
    String errorMessage = "Unexpected error";
    Exception exception = new Exception(errorMessage);

    // When
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGeneralException(exception);

    // Then
    assertNotNull(response);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertNotNull(response.getBody());
    assertFalse(response.getBody().isSuccess());
    assertTrue(response.getBody().getMessage().contains("unexpected error occurred"));
    // 不应该暴露具体的错误信息
    assertFalse(response.getBody().getMessage().contains(errorMessage));
  }

  @Test
  @DisplayName("应该格式化验证错误消息 - 包含字段名和消息")
  void shouldFormatValidationErrors_withFieldNameAndMessage() {
    // Given
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError error = new FieldError("loginRequest", "email", "Email is required");
    when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(error));

    MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

    // When
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleValidationException(exception);

    // Then
    assertNotNull(response.getBody());
    String message = response.getBody().getMessage();
    assertTrue(message.contains("email"));
    assertTrue(message.contains("Email is required"));
  }

  @Test
  @DisplayName("应该合并多个验证错误 - 用分号分隔")
  void shouldCombineMultipleValidationErrors_withSemicolon() {
    // Given
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError error1 = new FieldError("object", "email", "Email is required");
    FieldError error2 = new FieldError("object", "password", "Password is too short");
    when(bindingResult.getAllErrors()).thenReturn(Arrays.asList(error1, error2));

    MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

    // When
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleValidationException(exception);

    // Then
    String message = response.getBody().getMessage();
    assertTrue(message.contains(";"));
    assertTrue(message.contains("email"));
    assertTrue(message.contains("password"));
  }

  @Test
  @DisplayName("应该返回正确的 HTTP 状态码 - 对于所有异常类型")
  void shouldReturnCorrectStatusCode_forAllExceptionTypes() {
    // Given & When & Then
    assertEquals(HttpStatus.NOT_FOUND,
        exceptionHandler.handleResourceNotFoundException(new ResourceNotFoundException("")).getStatusCode());

    assertEquals(HttpStatus.UNAUTHORIZED,
        exceptionHandler.handleUnauthorizedException(new UnauthorizedException("")).getStatusCode());

    assertEquals(HttpStatus.BAD_REQUEST,
        exceptionHandler.handleBadRequestException(new BadRequestException("")).getStatusCode());

    assertEquals(HttpStatus.CONFLICT,
        exceptionHandler.handleDuplicateResourceException(new DuplicateResourceException("")).getStatusCode());

    assertEquals(HttpStatus.BAD_REQUEST,
        exceptionHandler.handleInsufficientStockException(new InsufficientStockException("")).getStatusCode());

    assertEquals(HttpStatus.FORBIDDEN,
        exceptionHandler.handleForbiddenException(new ForbiddenException("")).getStatusCode());

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
        exceptionHandler.handleGeneralException(new Exception("")).getStatusCode());
  }

  @Test
  @DisplayName("应该始终返回 ApiResponse 格式")
  void shouldAlwaysReturnApiResponseFormat() {
    // Test multiple exception types
    ResponseEntity<ApiResponse<Void>> response1 = 
        exceptionHandler.handleResourceNotFoundException(new ResourceNotFoundException("test"));
    ResponseEntity<ApiResponse<Void>> response2 = 
        exceptionHandler.handleUnauthorizedException(new UnauthorizedException("test"));
    ResponseEntity<ApiResponse<Void>> response3 = 
        exceptionHandler.handleBadRequestException(new BadRequestException("test"));

    // All responses should have ApiResponse body
    assertNotNull(response1.getBody());
    assertNotNull(response2.getBody());
    assertNotNull(response3.getBody());

    // All should be error responses
    assertFalse(response1.getBody().isSuccess());
    assertFalse(response2.getBody().isSuccess());
    assertFalse(response3.getBody().isSuccess());
  }

  @Test
  @DisplayName("应该处理空异常消息")
  void shouldHandleEmptyExceptionMessage() {
    // Given
    ResourceNotFoundException exception = new ResourceNotFoundException("");

    // When
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleResourceNotFoundException(exception);

    // Then
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getMessage());
  }

  @Test
  @DisplayName("应该处理 null 异常消息")
  void shouldHandleNullExceptionMessage() {
    // Given
    ResourceNotFoundException exception = new ResourceNotFoundException(null);

    // When
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleResourceNotFoundException(exception);

    // Then
    assertNotNull(response.getBody());
    // Message might be null, which is acceptable
  }
}