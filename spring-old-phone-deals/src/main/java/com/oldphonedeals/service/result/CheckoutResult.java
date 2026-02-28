package com.oldphonedeals.service.result;

import com.oldphonedeals.dto.response.order.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Wraps checkout outcome so controller can distinguish created vs replayed response.
 */
@Getter
@AllArgsConstructor(staticName = "of")
public class CheckoutResult {
  private final OrderResponse order;
  private final boolean replayed;

  public static CheckoutResult created(OrderResponse order) {
    return CheckoutResult.of(order, false);
  }

  public static CheckoutResult replayed(OrderResponse order) {
    return CheckoutResult.of(order, true);
  }
}
