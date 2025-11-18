package com.oldphonedeals.exception;

/**
 * 库存不足异常
 * <p>
 * 当商品库存不足以满足购买数量时抛出此异常，例如：
 * - 购物车中商品数量超过库存
 * - 下单时库存不足
 * - 并发购买导致库存不足
 * </p>
 * <p>
 * 对应 HTTP 状态码：400 BAD_REQUEST
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
public class InsufficientStockException extends RuntimeException {

  /**
   * 构造库存不足异常
   *
   * @param message 异常消息，描述库存不足的详细信息
   */
  public InsufficientStockException(String message) {
    super(message);
  }
}