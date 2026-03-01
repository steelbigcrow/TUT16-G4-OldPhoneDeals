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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SagaCompensationDlqIT extends AbstractSagaIT {

    @Test
    void shouldRetryAndRouteToCompensationDlqWhenCriticalStepKeepsFailing() {
        String sagaId = "saga-dlq-it-" + UUID.randomUUID();
        String orderId = "order-dlq-it-" + UUID.randomUUID();
        String userId = "user-dlq-it-" + UUID.randomUUID();

        when(sagaLogRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());
        when(sagaLogRepository.save(any(SagaLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(phoneStockRepository.increaseStockAndDecreaseSales("phone-1", 1)).thenReturn(false);

        compensationMessageProducer.publish(buildCompensationMessage(sagaId, orderId, userId));

        Awaitility.await()
            .atMost(Duration.ofSeconds(90))
            .until(() -> queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_DLQ_QUEUE) == 1);

        verify(phoneStockRepository, atLeast(4)).increaseStockAndDecreaseSales("phone-1", 1);
        verify(sagaLogRepository, atLeastOnce()).save(argThat(log ->
            log != null && log.getStatus() == SagaStatus.FAILED
        ));

        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_QUEUE));
    }
}
