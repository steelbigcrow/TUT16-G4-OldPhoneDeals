package com.oldphonedeals.service;

import com.oldphonedeals.dto.message.OrderCompensationMessage;

public interface SagaCompensationService {
  boolean isSagaCompleted(String sagaId);

  void compensate(OrderCompensationMessage message);

  void markSagaRetrying(OrderCompensationMessage message, String errorMessage);

  void markSagaFailed(OrderCompensationMessage message, String errorMessage);
}
