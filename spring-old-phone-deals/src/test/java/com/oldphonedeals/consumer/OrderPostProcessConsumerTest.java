package com.oldphonedeals.consumer;

import com.oldphonedeals.dto.message.OrderPostProcessMessage;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.ProcessedMessage;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.producer.CompensationMessageProducer;
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
import org.springframework.retry.support.RetryTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private CompensationMessageProducer compensationMessageProducer;

    @Mock
    private Channel channel;

    private OrderPostProcessConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderPostProcessConsumer(
            processedMessageRepository,
            orderRepository,
            emailMessageProducer,
            compensationMessageProducer
        );
    }

    @Test
    void shouldAckAndSkipWhenMessageAlreadyProcessed() throws Exception {
        OrderPostProcessMessage message = buildMessage("msg-1", "order-1");
        when(processedMessageRepository.existsByMessageId("msg-1")).thenReturn(true);

        consumer.handleOrderPostProcessMessage(message, channel, 7L);

        verify(channel).basicAck(7L, false);
        verify(orderRepository, never()).save(any(Order.class));
        verify(processedMessageRepository, never()).save(any(ProcessedMessage.class));
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
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        consumer.handleOrderPostProcessMessage(message, channel, 8L);

        verify(processedMessageRepository).save(any(ProcessedMessage.class));
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(OrderPostProcessStatus.SUCCESS, orderCaptor.getValue().getPostProcessStatus());
        verify(channel).basicAck(8L, false);
    }

    @Test
    void shouldThrowWhenOrderNotFoundToTriggerRetry() {
        OrderPostProcessMessage message = buildMessage("msg-3", "order-404");
        when(processedMessageRepository.existsByMessageId("msg-3")).thenReturn(false);
        when(orderRepository.findById("order-404")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
            consumer.handleOrderPostProcessMessage(message, channel, 9L)
        );
    }

    @Test
    void shouldThrowAndMarkOrderFailedWhenSaveFails() {
        OrderPostProcessMessage message = buildMessage("msg-4", "order-4");
        Order order = Order.builder()
            .id("order-4")
            .postProcessStatus(OrderPostProcessStatus.PENDING)
            .build();

        when(processedMessageRepository.existsByMessageId("msg-4")).thenReturn(false);
        when(orderRepository.findById("order-4")).thenReturn(Optional.of(order));
        doThrow(new IllegalStateException("db down"))
            .doAnswer(invocation -> invocation.getArgument(0))
            .when(orderRepository)
            .save(any(Order.class));

        assertThrows(IllegalStateException.class, () ->
            consumer.handleOrderPostProcessMessage(message, channel, 10L)
        );

        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderRepository, atLeastOnce()).save(argThat(saved ->
            saved != null && saved.getPostProcessStatus() == OrderPostProcessStatus.FAILED
        ));
    }

    @Test
    void shouldThrowWhenProcessingFailsToTriggerRetry() {
        OrderPostProcessMessage message = buildMessage("msg-5", "order-5");
        when(processedMessageRepository.existsByMessageId("msg-5")).thenReturn(false);
        doThrow(new IllegalStateException("db error"))
            .when(processedMessageRepository)
            .save(any(ProcessedMessage.class));

        assertThrows(IllegalStateException.class, () ->
            consumer.handleOrderPostProcessMessage(message, channel, 11L)
        );
    }

    @Test
    void shouldNackWithoutRequeueWhenRetryExhausted() throws Exception {
        OrderPostProcessMessage message = buildMessage("msg-6", "order-6");
        Order order = Order.builder()
            .id("order-6")
            .postProcessStatus(OrderPostProcessStatus.PENDING)
            .build();
        when(processedMessageRepository.existsByMessageId("msg-6")).thenReturn(false);
        doThrow(new IllegalStateException("db error"))
            .when(processedMessageRepository)
            .save(any(ProcessedMessage.class));
        when(orderRepository.findById("order-6")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(3).build();

        retryTemplate.execute(context -> {
            consumer.handleOrderPostProcessMessage(message, channel, 12L);
            return null;
        });

        verify(processedMessageRepository, times(3)).save(any(ProcessedMessage.class));
        verify(compensationMessageProducer).publish(any(OrderCompensationMessage.class));
        verify(channel).basicNack(12L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void shouldPublishCompensationAndMarkOrderCompensatingWhenRetryExhausted() throws Exception {
        OrderPostProcessMessage message = buildMessage("msg-7", "order-7");
        Order order = Order.builder()
            .id("order-7")
            .postProcessStatus(OrderPostProcessStatus.PENDING)
            .build();

        when(processedMessageRepository.existsByMessageId("msg-7")).thenReturn(false);
        doThrow(new IllegalStateException("db error"))
            .when(processedMessageRepository)
            .save(any(ProcessedMessage.class));
        when(orderRepository.findById("order-7")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(3).build();
        retryTemplate.execute(context -> {
            consumer.handleOrderPostProcessMessage(message, channel, 13L);
            return null;
        });

        verify(compensationMessageProducer).publish(any(OrderCompensationMessage.class));
        verify(orderRepository, atLeastOnce()).save(argThat(saved ->
            saved != null && saved.getPostProcessStatus() == OrderPostProcessStatus.COMPENSATING
        ));
        verify(channel).basicNack(13L, false, false);
    }

    @Test
    void shouldNackAndFallbackToFailedWhenCompensationPublishFails() throws Exception {
        OrderPostProcessMessage message = buildMessage("msg-8", "order-8");
        Order order = Order.builder()
            .id("order-8")
            .postProcessStatus(OrderPostProcessStatus.PENDING)
            .build();

        when(processedMessageRepository.existsByMessageId("msg-8")).thenReturn(false);
        doThrow(new IllegalStateException("db error"))
            .when(processedMessageRepository)
            .save(any(ProcessedMessage.class));
        when(orderRepository.findById("order-8")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("compensation broker down"))
            .when(compensationMessageProducer)
            .publish(any(OrderCompensationMessage.class));

        RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(3).build();
        retryTemplate.execute(context -> {
            consumer.handleOrderPostProcessMessage(message, channel, 14L);
            return null;
        });

        verify(compensationMessageProducer).publish(any(OrderCompensationMessage.class));
        verify(orderRepository, atLeastOnce()).save(argThat(saved ->
            saved != null && saved.getPostProcessStatus() == OrderPostProcessStatus.FAILED
        ));
        verify(channel).basicNack(14L, false, false);
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
