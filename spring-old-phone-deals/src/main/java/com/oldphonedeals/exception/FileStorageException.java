package com.oldphonedeals.exception;

/**
 * 文件存储异常
 * <p>
 * 当文件存储操作失败时抛出此异常，例如：
 * <ul>
 *   <li>无法创建上传目录</li>
 *   <li>文件写入失败</li>
 *   <li>文件删除失败</li>
 *   <li>磁盘空间不足</li>
 * </ul>
 * </p>
 *
 * @author OldPhoneDeals Team
 */
public class FileStorageException extends RuntimeException {

  /**
   * 使用错误消息构造异常
   *
   * @param message 错误消息
   */
  public FileStorageException(String message) {
    super(message);
  }

  /**
   * 使用错误消息和原因构造异常
   *
   * @param message 错误消息
   * @param cause 异常原因
   */
  public FileStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}