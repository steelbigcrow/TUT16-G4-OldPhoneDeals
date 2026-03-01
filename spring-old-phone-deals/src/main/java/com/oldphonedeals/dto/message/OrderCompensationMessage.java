package com.oldphonedeals.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 订单补偿消息体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompensationMessage {
  private String sagaId;
  private String orderId;
  private String userId;
  private List<Item> items;
  private Double totalAmount;
  private String reason;
  private Instant timestamp;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Item {
    private String phoneId;
    private Integer quantity;
  }
}
