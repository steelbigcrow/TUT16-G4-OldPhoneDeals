package com.oldphonedeals.exception;

/**
 * 资源未找到异常
 * <p>
 * 当请求的资源不存在时抛出此异常，例如：
 * - 用户不存在
 * - 商品不存在
 * - 订单不存在
 * </p>
 * <p>
 * 对应 HTTP 状态码：404 NOT_FOUND
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
public class ResourceNotFoundException extends RuntimeException {

  /**
   * 构造资源未找到异常
   *
   * @param message 异常消息，描述哪个资源未找到
   */
  public ResourceNotFoundException(String message) {
    super(message);
  }
}