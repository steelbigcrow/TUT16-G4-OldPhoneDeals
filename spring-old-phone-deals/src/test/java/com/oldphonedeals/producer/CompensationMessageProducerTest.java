package com.oldphonedeals.producer;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompensationMessageProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CompensationMessageProducer producer;

    @Test
    void shouldPublishCompensationMessage() {
        OrderCompensationMessage message = OrderCompensationMessage.builder()
            .sagaId("saga-1")
            .orderId("order-1")
            .userId("user-1")
            .build();

        producer.publish(message);

        verify(rabbitTemplate).convertAndSend(
            RabbitMQConfig.COMPENSATION_EXCHANGE,
            RabbitMQConfig.ORDER_COMPENSATION_ROUTING_KEY,
            message
        );
    }

    @Test
    void shouldWrapAmqpExceptionWhenPublishingFails() {
        OrderCompensationMessage message = OrderCompensationMessage.builder()
            .sagaId("saga-2")
            .orderId("order-2")
            .userId("user-2")
            .build();
        AmqpException amqpException = new AmqpException("broker down");
        doThrow(amqpException).when(rabbitTemplate).convertAndSend(
            RabbitMQConfig.COMPENSATION_EXCHANGE,
            RabbitMQConfig.ORDER_COMPENSATION_ROUTING_KEY,
            message
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> producer.publish(message));

        assertSame(amqpException, exception.getCause());
    }
}
