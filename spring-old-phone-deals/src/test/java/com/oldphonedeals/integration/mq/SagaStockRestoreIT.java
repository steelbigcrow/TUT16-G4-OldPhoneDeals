package com.oldphonedeals.integration.mq;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.SagaLog;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SagaStockRestoreIT extends AbstractSagaIT {

    @Test
    void shouldRestoreStockForEachCompensationItem() {
        String sagaId = "saga-stock-it-" + UUID.randomUUID();
        String orderId = "order-stock-it-" + UUID.randomUUID();
        String userId = "user-stock-it-" + UUID.randomUUID();
        Order order = buildOrder(orderId);
        List<OrderCompensationMessage.Item> items = List.of(
            OrderCompensationMessage.Item.builder().phoneId("phone-1").quantity(1).build(),
            OrderCompensationMessage.Item.builder().phoneId("phone-2").quantity(2).build()
        );

        when(sagaLogRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());
        when(sagaLogRepository.save(any(SagaLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(phoneStockRepository.increaseStockAndDecreaseSales("phone-1", 1)).thenReturn(true);
        when(phoneStockRepository.increaseStockAndDecreaseSales("phone-2", 2)).thenReturn(true);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId)));

        compensationMessageProducer.publish(buildCompensationMessage(sagaId, orderId, userId, items, "manual test"));

        Awaitility.await()
            .atMost(Duration.ofSeconds(20))
            .untilAsserted(() -> {
                verify(phoneStockRepository).increaseStockAndDecreaseSales("phone-1", 1);
                verify(phoneStockRepository).increaseStockAndDecreaseSales("phone-2", 2);
            });

        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_QUEUE));
        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_DLQ_QUEUE));
    }
}
