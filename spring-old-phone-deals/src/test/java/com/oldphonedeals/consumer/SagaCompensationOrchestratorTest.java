package com.oldphonedeals.consumer;

import com.oldphonedeals.dto.message.OrderCompensationMessage;
import com.oldphonedeals.service.SagaCompensationService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SagaCompensationOrchestratorTest {

    @Mock
    private SagaCompensationService sagaCompensationService;

    @Mock
    private Channel channel;

    @InjectMocks
    private SagaCompensationOrchestrator orchestrator;

    @Test
    void shouldAckAndSkipWhenSagaAlreadyCompleted() throws Exception {
        OrderCompensationMessage message = buildMessage("saga-completed");
        when(sagaCompensationService.isSagaCompleted("saga-completed")).thenReturn(true);

        orchestrator.handleCompensationMessage(message, channel, 100L);

        verify(channel).basicAck(100L, false);
        verify(sagaCompensationService, never()).compensate(message);
    }

    @Test
    void shouldCompensateAndAckWhenSagaNotCompleted() throws Exception {
        OrderCompensationMessage message = buildMessage("saga-active");
        when(sagaCompensationService.isSagaCompleted("saga-active")).thenReturn(false);

        orchestrator.handleCompensationMessage(message, channel, 101L);

        verify(sagaCompensationService).compensate(message);
        verify(channel).basicAck(101L, false);
    }

    @Test
    void shouldThrowAndMarkRetryingWhenCompensationFailsBeforeRetryExhausted() {
        OrderCompensationMessage message = buildMessage("saga-retry");
        when(sagaCompensationService.isSagaCompleted("saga-retry")).thenReturn(false);
        doThrow(new IllegalStateException("stock restore failed"))
            .when(sagaCompensationService)
            .compensate(message);

        assertThrows(IllegalStateException.class, () ->
            orchestrator.handleCompensationMessage(message, channel, 102L)
        );

        verify(sagaCompensationService).markSagaRetrying(message, "stock restore failed");
        verify(sagaCompensationService, never()).markSagaFailed(message, "stock restore failed");
    }

    @Test
    void shouldNackAndMarkFailedWhenRetryExhausted() throws Exception {
        OrderCompensationMessage message = buildMessage("saga-dlq");
        when(sagaCompensationService.isSagaCompleted("saga-dlq")).thenReturn(false);
        doThrow(new IllegalStateException("order update failed"))
            .when(sagaCompensationService)
            .compensate(message);

        RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(4).build();
        retryTemplate.execute(context -> {
            orchestrator.handleCompensationMessage(message, channel, 103L);
            return null;
        });

        verify(sagaCompensationService, times(4)).compensate(message);
        verify(sagaCompensationService, times(3)).markSagaRetrying(message, "order update failed");
        verify(sagaCompensationService).markSagaFailed(message, "order update failed");
        verify(channel).basicNack(103L, false, false);
    }

    private OrderCompensationMessage buildMessage(String sagaId) {
        return OrderCompensationMessage.builder()
            .sagaId(sagaId)
            .orderId("order-1")
            .userId("user-1")
            .items(List.of(OrderCompensationMessage.Item.builder()
                .phoneId("phone-1")
                .quantity(1)
                .build()))
            .totalAmount(199.0)
            .reason("post-process failed")
            .timestamp(Instant.now())
            .build();
    }
}
