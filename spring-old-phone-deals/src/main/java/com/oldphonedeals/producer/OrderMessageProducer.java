package com.oldphonedeals.producer;

import com.oldphonedeals.dto.message.OrderPostProcessMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMessageProducer {

  public static final String ORDER_EXCHANGE = "order.exchange";
  public static final String ORDER_POST_PROCESS_ROUTING_KEY = "order.post.process";

  private final RabbitTemplate rabbitTemplate;

  public void publishOrderPostProcessMessage(OrderPostProcessMessage message) {
    try {
      rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_POST_PROCESS_ROUTING_KEY, message);
      log.debug("Published order post-process message: {}", message.getMessageId());
    } catch (AmqpException ex) {
      log.error("Failed to publish order post-process message: {}", message.getMessageId(), ex);
      throw new IllegalStateException("Failed to publish order post-process message", ex);
    }
  }
}
