package com.oldphonedeals.consumer;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
import com.oldphonedeals.dto.message.OrderPostProcessMessage;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.ProcessedMessage;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.producer.CompensationMessageProducer;
import com.oldphonedeals.producer.EmailMessageProducer;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.ProcessedMessageRepository;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class OrderPostProcessConsumer {

  private static final int ORDER_POST_PROCESS_MAX_ATTEMPTS = 3;

  private final ProcessedMessageRepository processedMessageRepository;
  private final OrderRepository orderRepository;
  private final EmailMessageProducer emailMessageProducer;
  private final CompensationMessageProducer compensationMessageProducer;

  @Autowired
  public OrderPostProcessConsumer(
    ProcessedMessageRepository processedMessageRepository,
    OrderRepository orderRepository,
    EmailMessageProducer emailMessageProducer,
    CompensationMessageProducer compensationMessageProducer
  ) {
    this.processedMessageRepository = processedMessageRepository;
    this.orderRepository = orderRepository;
    this.emailMessageProducer = emailMessageProducer;
    this.compensationMessageProducer = compensationMessageProducer;
  }

  public OrderPostProcessConsumer(
    ProcessedMessageRepository processedMessageRepository,
    OrderRepository orderRepository,
    EmailMessageProducer emailMessageProducer
  ) {
    this(processedMessageRepository, orderRepository, emailMessageProducer, null);
  }

  @RabbitListener(
    queues = RabbitMQConfig.ORDER_POST_PROCESS_QUEUE,
    containerFactory = "orderPostProcessListenerContainerFactory"
  )
  public void handleOrderPostProcessMessage(
    OrderPostProcessMessage message,
    Channel channel,
    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
  ) {
    try {
      if (processedMessageRepository.existsByMessageId(message.getMessageId())) {
        channel.basicAck(deliveryTag, false);
        return;
      }

      processedMessageRepository.save(ProcessedMessage.builder()
        .messageId(message.getMessageId())
        .messageType("ORDER_POST_PROCESS")
        .processedAt(LocalDateTime.now())
        .build());

      Order order = orderRepository.findById(message.getOrderId())
        .orElseThrow(() -> new IllegalStateException("Order not found: " + message.getOrderId()));

      // 预留给后续通知扩展（例如订单确认邮件/卖家通知）
      log.debug("Order post-process notifications placeholder for order: {}", message.getOrderId());
      if (emailMessageProducer == null) {
        log.debug("Email producer unavailable for order: {}", message.getOrderId());
      }

      order.setPostProcessStatus(OrderPostProcessStatus.SUCCESS);
      order.setPostProcessError(null);
      orderRepository.save(order);

      channel.basicAck(deliveryTag, false);
      log.debug("Order post-process consumed: {}", message.getMessageId());
    } catch (Exception ex) {
      log.error("Order post-process failed: {}", message.getMessageId(), ex);
      if (isRetryExhausted()) {
        triggerCompensation(message, ex.getMessage());
        try {
          channel.basicNack(deliveryTag, false, false);
          log.warn("Order post-process moved to DLQ after retries exhausted: {}", message.getMessageId());
          return;
        } catch (Exception nackEx) {
          nackEx.addSuppressed(ex);
          throw new IllegalStateException("Order post-process failed and could not nack: " + message.getMessageId(), nackEx);
        }
      }
      markOrderFailed(message.getOrderId(), ex.getMessage());
      throw new IllegalStateException("Order post-process failed: " + message.getMessageId(), ex);
    }
  }

  private void markOrderFailed(String orderId, String errorMessage) {
    try {
      Optional<Order> orderOptional = orderRepository.findById(orderId);
      if (orderOptional.isPresent()) {
        Order order = orderOptional.get();
        order.setPostProcessStatus(OrderPostProcessStatus.FAILED);
        order.setPostProcessError(errorMessage);
        orderRepository.save(order);
      }
    } catch (Exception ex) {
      log.error("Failed to mark order post-process status as FAILED: {}", orderId, ex);
    }
  }

  private void markOrderCompensating(String orderId, String errorMessage) {
    try {
      orderRepository.findById(orderId).ifPresent(order -> {
        order.setPostProcessStatus(OrderPostProcessStatus.COMPENSATING);
        order.setPostProcessError(errorMessage);
        orderRepository.save(order);
      });
    } catch (Exception ex) {
      log.error("Failed to mark order post-process status as COMPENSATING: {}", orderId, ex);
    }
  }

  private void triggerCompensation(OrderPostProcessMessage message, String errorMessage) {
    markOrderCompensating(message.getOrderId(), errorMessage);

    if (compensationMessageProducer == null) {
      markOrderFailed(message.getOrderId(), "Compensation producer unavailable");
      log.error("Compensation producer unavailable for order: {}", message.getOrderId());
      return;
    }

    try {
      compensationMessageProducer.publish(buildCompensationMessage(message, errorMessage));
      log.warn("Compensation triggered for order: {}", message.getOrderId());
    } catch (Exception compensationEx) {
      markOrderFailed(message.getOrderId(), compensationEx.getMessage());
      log.error("Failed to publish compensation message for order: {}", message.getOrderId(), compensationEx);
    }
  }

  private OrderCompensationMessage buildCompensationMessage(OrderPostProcessMessage message, String errorMessage) {
    var items = message.getItems() == null ? Collections.<OrderPostProcessMessage.Item>emptyList() : message.getItems();

    return OrderCompensationMessage.builder()
      .sagaId(UUID.randomUUID().toString())
      .orderId(message.getOrderId())
      .userId(message.getUserId())
      .items(items.stream()
        .map(item -> OrderCompensationMessage.Item.builder()
          .phoneId(item.getPhoneId())
          .quantity(item.getQuantity())
          .build())
        .collect(Collectors.toList()))
      .totalAmount(message.getTotalAmount())
      .reason(errorMessage)
      .timestamp(Instant.now())
      .build();
  }

  private boolean isRetryExhausted() {
    var retryContext = RetrySynchronizationManager.getContext();
    return retryContext != null && retryContext.getRetryCount() >= ORDER_POST_PROCESS_MAX_ATTEMPTS - 1;
  }
}
