package com.oldphonedeals.dto.response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApiResponse 测试
 */
@DisplayName("ApiResponse Tests")
class ApiResponseTest {

  @Test
  @DisplayName("应该创建成功响应 - 带数据")
  void shouldCreateSuccessResponse_withData() {
    // Given
    TestDataClass testData = new TestDataClass("Test Data", 123);

    // When
    ApiResponse<TestDataClass> response = ApiResponse.success(testData);

    // Then
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertNotNull(response.getData());
    assertEquals("Test Data", response.getData().field1);
    assertEquals(123, response.getData().field2);
    assertNull(response.getMessage());
  }

  @Test
  @DisplayName("应该创建成功响应 - 只带消息")
  void shouldCreateSuccessResponse_withMessageOnly() {
    // Given
    String message = "Operation successful";

    // When
    ApiResponse<Void> response = ApiResponse.success(message);

    // Then
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(message, response.getMessage());
    assertNull(response.getData());
  }

  @Test
  @DisplayName("应该创建成功响应 - 带数据和消息")
  void shouldCreateSuccessResponse_withDataAndMessage() {
    // Given
    String testData = "Test Data";
    String message = "Operation successful";

    // When
    ApiResponse<String> response = ApiResponse.success(testData, message);

    // Then
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals(testData, response.getData());
    assertEquals(message, response.getMessage());
  }

  @Test
  @DisplayName("应该创建错误响应")
  void shouldCreateErrorResponse() {
    // Given
    String errorMessage = "Error occurred";

    // When
    ApiResponse<Void> response = ApiResponse.error(errorMessage);

    // Then
    assertNotNull(response);
    assertFalse(response.isSuccess());
    assertEquals(errorMessage, response.getMessage());
    assertNull(response.getData());
  }

  @Test
  @DisplayName("应该使用 Builder 正确构建")
  void shouldBuildCorrectly_usingBuilder() {
    // Given & When
    ApiResponse<String> response = ApiResponse.<String>builder()
        .success(true)
        .message("Test message")
        .data("Test data")
        .build();

    // Then
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("Test message", response.getMessage());
    assertEquals("Test data", response.getData());
  }

  @Test
  @DisplayName("应该使用无参构造器创建")
  void shouldCreateWithNoArgsConstructor() {
    // Given & When
    ApiResponse<String> response = new ApiResponse<>();
    response.setSuccess(true);
    response.setMessage("Test");
    response.setData("Data");

    // Then
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("Test", response.getMessage());
    assertEquals("Data", response.getData());
  }

  @Test
  @DisplayName("应该使用全参构造器创建")
  void shouldCreateWithAllArgsConstructor() {
    // Given & When
    ApiResponse<String> response = new ApiResponse<>(true, "Message", "Data");

    // Then
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertEquals("Message", response.getMessage());
    assertEquals("Data", response.getData());
  }

  @Test
  @DisplayName("应该处理复杂数据类型")
  void shouldHandleComplexDataTypes() {
    // Given
    TestDataClass testData = new TestDataClass("value1", 42);

    // When
    ApiResponse<TestDataClass> response = ApiResponse.success(testData);

    // Then
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertNotNull(response.getData());
    assertEquals("value1", response.getData().field1);
    assertEquals(42, response.getData().field2);
  }

  @Test
  @DisplayName("应该处理 null 数据")
  void shouldHandleNullData() {
    // When
    ApiResponse<String> response = ApiResponse.success((String) null);

    // Then
    assertNotNull(response);
    assertTrue(response.isSuccess());
    assertNull(response.getData());
  }

  @Test
  @DisplayName("应该正确设置成功标志")
  void shouldCorrectlySetSuccessFlag() {
    // Given & When
    ApiResponse<String> successResponse = ApiResponse.success("data");
    ApiResponse<Void> errorResponse = ApiResponse.error("error");

    // Then
    assertTrue(successResponse.isSuccess());
    assertFalse(errorResponse.isSuccess());
  }

  // Helper class for testing complex data types
  private static class TestDataClass {
    private final String field1;
    private final int field2;

    public TestDataClass(String field1, int field2) {
      this.field1 = field1;
      this.field2 = field2;
    }
  }
}