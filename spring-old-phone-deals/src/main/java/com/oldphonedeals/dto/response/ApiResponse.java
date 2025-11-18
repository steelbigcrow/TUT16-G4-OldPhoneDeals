package com.oldphonedeals.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一的 API 响应包装类
 * <p>
 * 该类用于封装所有 API 响应，确保前后端响应格式的一致性。
 * 支持成功和错误两种响应类型，可携带数据和消息。
 * </p>
 *
 * @param <T> 响应数据的类型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

  /**
   * 请求是否成功
   */
  private boolean success;

  /**
   * 响应消息（可选）
   */
  private String message;

  /**
   * 响应数据（可选）
   */
  private T data;

  /**
   * 创建成功响应，包含数据
   *
   * @param data 响应数据
   * @param <T> 数据类型
   * @return ApiResponse 实例
   */
  public static <T> ApiResponse<T> success(T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .data(data)
        .build();
  }

  /**
   * 创建成功响应，只包含消息
   *
   * @param message 响应消息
   * @param <T> 数据类型
   * @return ApiResponse 实例
   */
  public static <T> ApiResponse<T> success(String message) {
    return ApiResponse.<T>builder()
        .success(true)
        .message(message)
        .build();
  }

  /**
   * 创建成功响应，包含数据和消息
   *
   * @param data 响应数据
   * @param message 响应消息
   * @param <T> 数据类型
   * @return ApiResponse 实例
   */
  public static <T> ApiResponse<T> success(T data, String message) {
    return ApiResponse.<T>builder()
        .success(true)
        .data(data)
        .message(message)
        .build();
  }

  /**
   * 创建错误响应
   *
   * @param message 错误消息
   * @param <T> 数据类型
   * @return ApiResponse 实例
   */
  public static <T> ApiResponse<T> error(String message) {
    return ApiResponse.<T>builder()
        .success(false)
        .message(message)
        .build();
  }
}