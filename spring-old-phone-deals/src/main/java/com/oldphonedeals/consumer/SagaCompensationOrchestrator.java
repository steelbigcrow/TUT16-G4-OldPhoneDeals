package com.oldphonedeals.consumer;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
import com.oldphonedeals.service.SagaCompensationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaCompensationOrchestrator {

  private static final int COMPENSATION_MAX_ATTEMPTS = 4;

  private final SagaCompensationService sagaCompensationService;

  @RabbitListener(
    queues = RabbitMQConfig.ORDER_COMPENSATION_QUEUE,
    containerFactory = "compensationListenerContainerFactory"
  )
  public void handleCompensationMessage(
    OrderCompensationMessage message,
    Channel channel,
    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
  ) {
    try {
      if (message == null || message.getSagaId() == null || message.getSagaId().isBlank()) {
        throw new IllegalArgumentException("Compensation message and sagaId are required");
      }

      if (sagaCompensationService.isSagaCompleted(message.getSagaId())) {
        channel.basicAck(deliveryTag, false);
        return;
      }

      sagaCompensationService.compensate(message);
      channel.basicAck(deliveryTag, false);
      log.debug("Compensation consumed successfully for saga: {}", message.getSagaId());
    } catch (Exception ex) {
      log.error("Compensation failed for saga: {}", message != null ? message.getSagaId() : null, ex);

      if (isRetryExhausted()) {
        try {
          sagaCompensationService.markSagaFailed(message, ex.getMessage());
          channel.basicNack(deliveryTag, false, false);
          log.warn("Compensation moved to DLQ after retries exhausted for saga: {}", message != null ? message.getSagaId() : null);
          return;
        } catch (Exception nackEx) {
          nackEx.addSuppressed(ex);
          throw new IllegalStateException(
            "Compensation failed and could not nack for saga: " + (message != null ? message.getSagaId() : null),
            nackEx
          );
        }
      }

      sagaCompensationService.markSagaRetrying(message, ex.getMessage());
      throw new IllegalStateException(
        "Compensation failed for saga: " + (message != null ? message.getSagaId() : null),
        ex
      );
    }
  }

  private boolean isRetryExhausted() {
    var retryContext = RetrySynchronizationManager.getContext();
    return retryContext != null && retryContext.getRetryCount() >= COMPENSATION_MAX_ATTEMPTS - 1;
  }
}
