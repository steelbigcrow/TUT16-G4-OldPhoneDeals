package com.oldphonedeals.producer;

import com.oldphonedeals.dto.message.OrderPostProcessMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderMessageProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OrderMessageProducer producer;

    @BeforeEach
    void setUp() {
        producer = new OrderMessageProducer(rabbitTemplate);
    }

    @Test
    void shouldPublishOrderPostProcessMessage() {
        OrderPostProcessMessage message = OrderPostProcessMessage.builder()
            .messageId("msg-1")
            .orderId("order-1")
            .userId("user-1")
            .items(List.of(OrderPostProcessMessage.Item.builder()
                .phoneId("phone-1")
                .quantity(2)
                .build()))
            .totalAmount(1999.98)
            .timestamp(Instant.now())
            .build();

        producer.publishOrderPostProcessMessage(message);

        verify(rabbitTemplate).convertAndSend("order.exchange", "order.post.process", message);
    }

    @Test
    void shouldThrowWhenPublishFails() {
        OrderPostProcessMessage message = OrderPostProcessMessage.builder()
            .messageId("msg-1")
            .orderId("order-1")
            .userId("user-1")
            .items(List.of())
            .totalAmount(100.0)
            .timestamp(Instant.now())
            .build();

        doThrow(new AmqpException("nack") {})
            .when(rabbitTemplate)
            .convertAndSend("order.exchange", "order.post.process", message);

        assertThrows(IllegalStateException.class, () -> producer.publishOrderPostProcessMessage(message));
    }
}
