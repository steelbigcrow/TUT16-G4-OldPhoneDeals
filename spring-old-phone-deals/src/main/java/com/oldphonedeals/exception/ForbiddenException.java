package com.oldphonedeals.exception;

/**
 * 禁止访问异常
 * <p>
 * 当用户已认证但没有权限访问特定资源时抛出此异常，例如：
 * - 普通用户尝试访问管理员功能
 * - 用户尝试修改他人的资源
 * - 权限不足访问特定端点
 * </p>
 * <p>
 * 对应 HTTP 状态码：403 FORBIDDEN
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
public class ForbiddenException extends RuntimeException {

  /**
   * 构造禁止访问异常
   *
   * @param message 异常消息，描述禁止访问的原因
   */
  public ForbiddenException(String message) {
    super(message);
  }
}