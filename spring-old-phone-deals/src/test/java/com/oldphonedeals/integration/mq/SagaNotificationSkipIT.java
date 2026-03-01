package com.oldphonedeals.integration.mq;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.SagaLog;
import com.oldphonedeals.enums.SagaStatus;
import com.oldphonedeals.enums.StepStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SagaNotificationSkipIT extends AbstractSagaIT {

    @Test
    void shouldCompleteSagaWhenNotificationFails() {
        String sagaId = "saga-notification-it-" + UUID.randomUUID();
        String orderId = "order-notification-it-" + UUID.randomUUID();
        String userId = "user-notification-it-" + UUID.randomUUID();
        Order order = buildOrder(orderId);

        when(sagaLogRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());
        when(sagaLogRepository.save(any(SagaLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(phoneStockRepository.increaseStockAndDecreaseSales("phone-1", 1)).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId)));
        doThrow(new IllegalStateException("smtp unavailable")).when(emailMessageProducer).publish(any());

        compensationMessageProducer.publish(buildCompensationMessage(sagaId, orderId, userId));

        Awaitility.await()
            .atMost(Duration.ofSeconds(20))
            .untilAsserted(() -> verify(sagaLogRepository, atLeastOnce()).save(argThat(log ->
                log != null
                    && log.getStatus() == SagaStatus.COMPLETED
                    && hasStep(log, "SEND_NOTIFICATION", StepStatus.SKIPPED)
            )));

        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_QUEUE));
        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_DLQ_QUEUE));
    }
}
