package com.oldphonedeals.producer;

import com.oldphonedeals.dto.message.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailMessageProducer {

  public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
  public static final String EMAIL_SEND_ROUTING_KEY = "email.send";

  private final RabbitTemplate rabbitTemplate;

  public void publish(EmailMessage message) {
    try {
      rabbitTemplate.convertAndSend(NOTIFICATION_EXCHANGE, EMAIL_SEND_ROUTING_KEY, message);
      log.debug("Published email message: {}", message.getMessageId());
    } catch (AmqpException ex) {
      log.error("Failed to publish email message: {}", message.getMessageId(), ex);
      throw new IllegalStateException("Failed to publish email message", ex);
    }
  }
}
