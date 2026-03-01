package com.oldphonedeals.integration.mq;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.SagaLog;
import com.oldphonedeals.enums.OrderCheckoutStatus;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.enums.SagaStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SagaCompensationFlowIT extends AbstractSagaIT {

    @Test
    void shouldConsumeCompensationMessageAndCompleteSagaFlow() {
        String sagaId = "saga-flow-it-" + UUID.randomUUID();
        String orderId = "order-flow-it-" + UUID.randomUUID();
        String userId = "user-flow-it-" + UUID.randomUUID();
        Order order = buildOrder(orderId);

        when(sagaLogRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());
        when(sagaLogRepository.save(any(SagaLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(phoneStockRepository.increaseStockAndDecreaseSales("phone-1", 1)).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId)));

        compensationMessageProducer.publish(buildCompensationMessage(sagaId, orderId, userId));

        Awaitility.await()
            .atMost(Duration.ofSeconds(20))
            .untilAsserted(() -> {
                verify(phoneStockRepository).increaseStockAndDecreaseSales("phone-1", 1);
                verify(orderRepository).save(argThat(saved ->
                    saved != null
                        && saved.getPostProcessStatus() == OrderPostProcessStatus.COMPENSATED
                        && saved.getCheckoutStatus() == OrderCheckoutStatus.FAILED
                ));
                verify(sagaLogRepository, atLeastOnce()).save(argThat(log ->
                    log != null && log.getStatus() == SagaStatus.COMPLETED
                ));
                verify(emailMessageProducer).publish(any());
            });

        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_QUEUE));
        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_DLQ_QUEUE));
    }
}
