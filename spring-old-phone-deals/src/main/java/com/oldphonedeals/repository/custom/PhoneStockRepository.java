package com.oldphonedeals.repository.custom;

/**
 * 商品库存原子扣减仓储
 */
public interface PhoneStockRepository {
  /**
   * 原子扣减库存并增加销量
   *
   * @param phoneId   商品 ID
   * @param quantity  扣减数量
   * @return true 表示成功扣减，false 表示库存不足或商品不存在
   */
  boolean decreaseStockAndIncreaseSales(String phoneId, int quantity);
}
