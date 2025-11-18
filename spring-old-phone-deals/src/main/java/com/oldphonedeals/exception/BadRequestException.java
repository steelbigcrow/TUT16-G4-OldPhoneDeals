package com.oldphonedeals.exception;

/**
 * 错误请求异常
 * <p>
 * 当客户端请求参数无效或不符合要求时抛出此异常，例如：
 * - 邮箱格式不正确
 * - 必填字段缺失
 * - 参数值超出范围
 * - 输入数据格式错误
 * </p>
 * <p>
 * 对应 HTTP 状态码：400 BAD_REQUEST
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
public class BadRequestException extends RuntimeException {

  /**
   * 构造错误请求异常
   *
   * @param message 异常消息，描述请求错误的原因
   */
  public BadRequestException(String message) {
    super(message);
  }
}