package com.oldphonedeals.exception;

/**
 * 资源重复异常
 * <p>
 * 当尝试创建已存在的资源时抛出此异常，例如：
 * - 邮箱已被注册
 * - 用户名已存在
 * - 商品编号重复
 * </p>
 * <p>
 * 对应 HTTP 状态码：409 CONFLICT
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
public class DuplicateResourceException extends RuntimeException {

  /**
   * 构造资源重复异常
   *
   * @param message 异常消息，描述哪个资源已存在
   */
  public DuplicateResourceException(String message) {
    super(message);
  }
}