package com.oldphonedeals.exception;

/**
 * 未授权异常
 * <p>
 * 当用户未登录或认证凭据无效时抛出此异常，例如：
 * - JWT Token 无效或过期
 * - 用户名或密码错误
 * - 缺少认证信息
 * </p>
 * <p>
 * 对应 HTTP 状态码：401 UNAUTHORIZED
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
public class UnauthorizedException extends RuntimeException {

  /**
   * 构造未授权异常
   *
   * @param message 异常消息，描述未授权的原因
   */
  public UnauthorizedException(String message) {
    super(message);
  }
}