package com.oldphonedeals.exception;

/**
 * 文件验证异常
 * <p>
 * 当文件验证失败时抛出此异常，例如：
 * <ul>
 *   <li>文件为空</li>
 *   <li>文件扩展名不允许</li>
 *   <li>MIME类型不匹配</li>
 *   <li>文件大小超过限制</li>
 *   <li>文件名包含非法字符</li>
 * </ul>
 * </p>
 *
 * @author OldPhoneDeals Team
 */
public class FileValidationException extends RuntimeException {

  /**
   * 使用错误消息构造异常
   *
   * @param message 错误消息
   */
  public FileValidationException(String message) {
    super(message);
  }

  /**
   * 使用错误消息和原因构造异常
   *
   * @param message 错误消息
   * @param cause 异常原因
   */
  public FileValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}