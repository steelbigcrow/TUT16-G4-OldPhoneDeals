package com.oldphonedeals.integration.mq;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.entity.SagaLog;
import com.oldphonedeals.enums.SagaStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SagaCompensationIdempotentIT extends AbstractSagaIT {

    @Test
    void shouldAckAndSkipWhenSagaAlreadyCompleted() {
        String sagaId = "saga-idempotent-it-" + UUID.randomUUID();
        String orderId = "order-idempotent-it-" + UUID.randomUUID();
        String userId = "user-idempotent-it-" + UUID.randomUUID();

        when(sagaLogRepository.findBySagaId(sagaId)).thenReturn(Optional.of(SagaLog.builder()
            .sagaId(sagaId)
            .orderId(orderId)
            .userId(userId)
            .status(SagaStatus.COMPLETED)
            .build()));

        compensationMessageProducer.publish(buildCompensationMessage(sagaId, orderId, userId));

        Awaitility.await()
            .atMost(Duration.ofSeconds(15))
            .until(() -> queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_QUEUE) == 0);

        verify(phoneStockRepository, never()).increaseStockAndDecreaseSales("phone-1", 1);
        verify(orderRepository, never()).findById(orderId);
        verify(emailMessageProducer, never()).publish(any());
        verify(sagaLogRepository, never()).save(any());

        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_DLQ_QUEUE));
    }
}
