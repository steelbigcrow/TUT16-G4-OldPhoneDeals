package com.oldphonedeals.consumer;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.dto.message.OrderPostProcessMessage;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.ProcessedMessage;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.producer.EmailMessageProducer;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.ProcessedMessageRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPostProcessConsumer {

  private final ProcessedMessageRepository processedMessageRepository;
  private final OrderRepository orderRepository;
  private final EmailMessageProducer emailMessageProducer;

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
      markOrderFailed(message.getOrderId(), ex.getMessage());
      try {
        channel.basicNack(deliveryTag, false, false);
      } catch (Exception channelEx) {
        log.error("Failed to nack order post-process message: {}", message.getMessageId(), channelEx);
      }
    }
  }

  private void markOrderFailed(String orderId, String errorMessage) {
    try {
      Optional<Order> orderOptional = orderRepository.findById(orderId);
      if (orderOptional != null && orderOptional.isPresent()) {
        Order order = orderOptional.get();
        order.setPostProcessStatus(OrderPostProcessStatus.FAILED);
        order.setPostProcessError(errorMessage);
        orderRepository.save(order);
      }
    } catch (Exception ex) {
      log.error("Failed to mark order post-process status as FAILED: {}", orderId, ex);
    }
  }
}
