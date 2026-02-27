package com.oldphonedeals.consumer;

import com.oldphonedeals.dto.message.OrderPostProcessMessage;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.ProcessedMessage;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.producer.EmailMessageProducer;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.ProcessedMessageRepository;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPostProcessConsumerTest {

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EmailMessageProducer emailMessageProducer;

    @Mock
    private Channel channel;

    private OrderPostProcessConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderPostProcessConsumer(processedMessageRepository, orderRepository, emailMessageProducer);
    }

    @Test
    void shouldAckAndSkipWhenMessageAlreadyProcessed() throws Exception {
        OrderPostProcessMessage message = buildMessage("msg-1", "order-1");
        when(processedMessageRepository.existsByMessageId("msg-1")).thenReturn(true);

        consumer.handleOrderPostProcessMessage(message, channel, 7L);

        verify(channel).basicAck(7L, false);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldMarkOrderSuccessAndAckForNewMessage() throws Exception {
        OrderPostProcessMessage message = buildMessage("msg-2", "order-2");
        Order order = Order.builder()
            .id("order-2")
            .userId("user-1")
            .postProcessStatus(OrderPostProcessStatus.PENDING)
            .build();

        when(processedMessageRepository.existsByMessageId("msg-2")).thenReturn(false);
        when(orderRepository.findById("order-2")).thenReturn(Optional.of(order));

        consumer.handleOrderPostProcessMessage(message, channel, 8L);

        verify(processedMessageRepository).save(any(ProcessedMessage.class));
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(OrderPostProcessStatus.SUCCESS, orderCaptor.getValue().getPostProcessStatus());
        verify(channel).basicAck(8L, false);
    }

    @Test
    void shouldNackWhenProcessingFails() throws Exception {
        OrderPostProcessMessage message = buildMessage("msg-3", "order-3");
        when(processedMessageRepository.existsByMessageId("msg-3")).thenReturn(false);
        doThrow(new IllegalStateException("db error"))
            .when(processedMessageRepository)
            .save(any(ProcessedMessage.class));

        consumer.handleOrderPostProcessMessage(message, channel, 9L);

        verify(channel).basicNack(9L, false, false);
    }

    private OrderPostProcessMessage buildMessage(String messageId, String orderId) {
        return OrderPostProcessMessage.builder()
            .messageId(messageId)
            .orderId(orderId)
            .userId("user-1")
            .items(List.of(OrderPostProcessMessage.Item.builder()
                .phoneId("phone-1")
                .quantity(1)
                .build()))
            .totalAmount(99.0)
            .timestamp(Instant.now())
            .build();
    }
}
