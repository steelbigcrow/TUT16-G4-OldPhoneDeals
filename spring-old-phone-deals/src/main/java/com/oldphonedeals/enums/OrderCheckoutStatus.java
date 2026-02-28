package com.oldphonedeals.enums;

/**
 * Checkout request processing status used for idempotent checkout replay.
 */
public enum OrderCheckoutStatus {
  PROCESSING,
  COMPLETED,
  FAILED
}
