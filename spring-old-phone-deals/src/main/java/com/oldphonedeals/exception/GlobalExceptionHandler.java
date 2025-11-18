package com.oldphonedeals.exception;

import com.oldphonedeals.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一处理应用程序中的所有异常，确保返回一致的错误响应格式。
 * 所有错误响应都遵循 {@link ApiResponse} 格式，与 Express.js 后端兼容。
 * </p>
 * <p>
 * 异常映射关系：
 * <ul>
 *   <li>ResourceNotFoundException → 404 NOT_FOUND</li>
 *   <li>UnauthorizedException → 401 UNAUTHORIZED</li>
 *   <li>BadRequestException → 400 BAD_REQUEST</li>
 *   <li>DuplicateResourceException → 409 CONFLICT</li>
 *   <li>InsufficientStockException → 400 BAD_REQUEST</li>
 *   <li>ForbiddenException → 403 FORBIDDEN</li>
 *   <li>MethodArgumentNotValidException → 400 BAD_REQUEST（验证错误）</li>
 *   <li>Exception → 500 INTERNAL_SERVER_ERROR（未捕获的异常）</li>
 * </ul>
 * </p>
 *
 * @author OldPhoneDeals Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 处理资源未找到异常
   *
   * @param ex ResourceNotFoundException 异常实例
   * @return 404 错误响应
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
    log.warn("Resource not found: {}", ex.getMessage());
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(ex.getMessage()));
  }

  /**
   * 处理未授权异常
   *
   * @param ex UnauthorizedException 异常实例
   * @return 401 错误响应
   */
  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException ex) {
    log.warn("Unauthorized access: {}", ex.getMessage());
    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error(ex.getMessage()));
  }

  /**
   * 处理错误请求异常
   *
   * @param ex BadRequestException 异常实例
   * @return 400 错误响应
   */
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadRequestException(BadRequestException ex) {
    log.warn("Bad request: {}", ex.getMessage());
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ex.getMessage()));
  }

  /**
   * 处理资源重复异常
   *
   * @param ex DuplicateResourceException 异常实例
   * @return 409 错误响应
   */
  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(DuplicateResourceException ex) {
    log.warn("Duplicate resource: {}", ex.getMessage());
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(ApiResponse.error(ex.getMessage()));
  }

  /**
   * 处理库存不足异常
   *
   * @param ex InsufficientStockException 异常实例
   * @return 400 错误响应
   */
  @ExceptionHandler(InsufficientStockException.class)
  public ResponseEntity<ApiResponse<Void>> handleInsufficientStockException(InsufficientStockException ex) {
    log.warn("Insufficient stock: {}", ex.getMessage());
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ex.getMessage()));
  }

  /**
   * 处理禁止访问异常
   *
   * @param ex ForbiddenException 异常实例
   * @return 403 错误响应
   */
  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ApiResponse<Void>> handleForbiddenException(ForbiddenException ex) {
    log.warn("Forbidden access: {}", ex.getMessage());
    return ResponseEntity
        .status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error(ex.getMessage()));
  }

  /**
   * 处理文件存储异常
   *
   * @param ex FileStorageException 异常实例
   * @return 500 错误响应
   */
  @ExceptionHandler(FileStorageException.class)
  public ResponseEntity<ApiResponse<Void>> handleFileStorageException(FileStorageException ex) {
    log.error("File storage error: {}", ex.getMessage());
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error(ex.getMessage()));
  }

  /**
   * 处理文件验证异常
   *
   * @param ex FileValidationException 异常实例
   * @return 400 错误响应
   */
  @ExceptionHandler(FileValidationException.class)
  public ResponseEntity<ApiResponse<Void>> handleFileValidationException(FileValidationException ex) {
    log.warn("File validation failed: {}", ex.getMessage());
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ex.getMessage()));
  }

  /**
   * 处理文件大小超限异常
   * <p>
   * 当上传文件大小超过Spring配置的最大限制时触发。
   * 此异常由Spring框架在文件上传时自动抛出。
   * </p>
   *
   * @param ex MaxUploadSizeExceededException 异常实例
   * @return 413 错误响应
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeException(MaxUploadSizeExceededException ex) {
    log.warn("File size exceeds maximum allowed size");
    return ResponseEntity
        .status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(ApiResponse.error("File size exceeds maximum allowed size"));
  }

  /**
   * 处理验证错误异常
   * <p>
   * 当请求参数验证失败时（例如 @Valid 注解），提取所有字段错误并组合成清晰的错误消息。
   * </p>
   *
   * @param ex MethodArgumentNotValidException 异常实例
   * @return 400 错误响应，包含所有验证错误信息
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
    String errorMessage = ex.getBindingResult()
        .getAllErrors()
        .stream()
        .map(error -> {
          String fieldName = ((FieldError) error).getField();
          String message = error.getDefaultMessage();
          return fieldName + ": " + message;
        })
        .collect(Collectors.joining("; "));

    log.warn("Validation failed: {}", errorMessage);
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("Validation failed: " + errorMessage));
  }

  /**
   * 处理所有未捕获的异常
   * <p>
   * 作为最后的防线，捕获所有未被其他处理器捕获的异常。
   * 这确保应用程序不会向客户端暴露敏感的堆栈跟踪信息。
   * </p>
   *
   * @param ex Exception 异常实例
   * @return 500 错误响应
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
    log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
  }
}