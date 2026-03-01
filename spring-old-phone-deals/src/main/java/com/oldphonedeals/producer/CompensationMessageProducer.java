package com.oldphonedeals.producer;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompensationMessageProducer {

  private final RabbitTemplate rabbitTemplate;

  public void publish(OrderCompensationMessage message) {
    try {
      rabbitTemplate.convertAndSend(
        RabbitMQConfig.COMPENSATION_EXCHANGE,
        RabbitMQConfig.ORDER_COMPENSATION_ROUTING_KEY,
        message
      );
      log.debug("Published compensation message for saga: {}", message.getSagaId());
    } catch (AmqpException ex) {
      log.error("Failed to publish compensation message for saga: {}", message.getSagaId(), ex);
      throw new IllegalStateException("Failed to publish compensation message", ex);
    }
  }
}
