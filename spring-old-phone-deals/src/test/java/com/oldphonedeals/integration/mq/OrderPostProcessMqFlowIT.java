package com.oldphonedeals.integration.mq;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.consumer.OrderPostProcessConsumer;
import com.oldphonedeals.dto.message.OrderPostProcessMessage;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.ProcessedMessage;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.producer.EmailMessageProducer;
import com.oldphonedeals.producer.OrderMessageProducer;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.ProcessedMessageRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = OrderPostProcessMqFlowIT.OrderMqTestApplication.class)
class OrderPostProcessMqFlowIT extends AbstractRabbitMqIT {

    @Autowired
    private OrderMessageProducer orderMessageProducer;

    @MockBean
    private ProcessedMessageRepository processedMessageRepository;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private EmailMessageProducer emailMessageProducer;

    @Test
    void shouldConsumeOrderPostProcessMessageAndMarkSuccess() {
        String orderId = "order-it-" + UUID.randomUUID();
        String messageId = "order-msg-it-" + UUID.randomUUID();
        Order order = Order.builder()
            .id(orderId)
            .postProcessStatus(OrderPostProcessStatus.PENDING)
            .build();

        when(processedMessageRepository.existsByMessageId(messageId)).thenReturn(false);
        when(processedMessageRepository.save(any(ProcessedMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderMessageProducer.publishOrderPostProcessMessage(buildMessage(messageId, orderId));

        Awaitility.await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> verify(orderRepository).save(argThat(saved ->
                OrderPostProcessStatus.SUCCESS == saved.getPostProcessStatus()
            )));

        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_POST_PROCESS_QUEUE));
        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_POST_PROCESS_DLQ_QUEUE));
    }

    @Test
    void shouldRetryAndRouteToDlqWhenPostProcessFails() {
        String orderId = "order-dlq-it-" + UUID.randomUUID();
        String messageId = "order-dlq-msg-it-" + UUID.randomUUID();
        Order order = Order.builder()
            .id(orderId)
            .postProcessStatus(OrderPostProcessStatus.PENDING)
            .build();

        when(processedMessageRepository.existsByMessageId(messageId)).thenReturn(false);
        doThrow(new IllegalStateException("db unavailable"))
            .when(processedMessageRepository)
            .save(any(ProcessedMessage.class));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderMessageProducer.publishOrderPostProcessMessage(buildMessage(messageId, orderId));

        Awaitility.await()
            .atMost(Duration.ofSeconds(60))
            .until(() -> queueMessageCount(RabbitMQConfig.ORDER_POST_PROCESS_DLQ_QUEUE) == 1);

        verify(processedMessageRepository, atLeast(3)).save(any(ProcessedMessage.class));
        verify(orderRepository, atLeast(3)).save(argThat(saved ->
            OrderPostProcessStatus.FAILED == saved.getPostProcessStatus()
        ));
        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_POST_PROCESS_QUEUE));
    }

    private OrderPostProcessMessage buildMessage(String messageId, String orderId) {
        return OrderPostProcessMessage.builder()
            .messageId(messageId)
            .orderId(orderId)
            .userId("user-it")
            .items(List.of(OrderPostProcessMessage.Item.builder()
                .phoneId("phone-1")
                .quantity(1)
                .build()))
            .totalAmount(199.0)
            .timestamp(Instant.now())
            .build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        MongoRepositoriesAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
    })
    @Import({RabbitMQConfig.class, OrderMessageProducer.class, OrderPostProcessConsumer.class})
    static class OrderMqTestApplication {
    }
}
