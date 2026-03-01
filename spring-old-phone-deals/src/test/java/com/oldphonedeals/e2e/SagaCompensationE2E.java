package com.oldphonedeals.e2e;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.SagaLog;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.OrderCheckoutStatus;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.enums.SagaStatus;
import com.oldphonedeals.enums.StepStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("e2e")
class SagaCompensationE2E extends AbstractSagaE2E {

    @Test
    void shouldCompleteCompensationFlowUsingRealMongoAndRabbitMq() {
        User buyer = seedVerifiedUser("buyer-compensation");
        User seller = seedVerifiedUser("seller-compensation");
        Phone phone = seedPhone(seller, "Saga Compensation Phone", 4, 6, 199.0);

        int quantity = 2;
        int stockBeforeCompensation = phone.getStock();
        int salesBeforeCompensation = phone.getSalesCount();

        List<OrderCompensationMessage.Item> items = List.of(
            compensationItem(phone.getId(), quantity)
        );
        Order order = seedCompensatingOrder(buyer, items, 398.0);

        String sagaId = "saga-compensation-e2e-" + UUID.randomUUID();
        publishCompensation(
            buildCompensationMessage(
                sagaId,
                order.getId(),
                buyer.getId(),
                items,
                "post-process failure"
            )
        );

        Awaitility.await()
            .atMost(Duration.ofSeconds(45))
            .untilAsserted(() -> {
                Order updatedOrder = orderRepository.findById(order.getId()).orElse(null);
                assertNotNull(updatedOrder);
                assertEquals(OrderPostProcessStatus.COMPENSATED, updatedOrder.getPostProcessStatus());
                assertEquals(OrderCheckoutStatus.FAILED, updatedOrder.getCheckoutStatus());
                assertTrue(updatedOrder.getCheckoutError().contains("Order compensated"));

                Phone updatedPhone = phoneRepository.findById(phone.getId()).orElse(null);
                assertNotNull(updatedPhone);
                assertEquals(stockBeforeCompensation + quantity, updatedPhone.getStock());
                assertEquals(salesBeforeCompensation - quantity, updatedPhone.getSalesCount());

                SagaLog sagaLog = sagaLogRepository.findBySagaId(sagaId).orElse(null);
                assertNotNull(sagaLog);
                assertEquals(SagaStatus.COMPLETED, sagaLog.getStatus());
                assertTrue(hasStep(sagaLog, "RESTORE_STOCK", StepStatus.SUCCESS));
                assertTrue(hasStep(sagaLog, "UPDATE_ORDER", StepStatus.SUCCESS));
                assertTrue(
                    hasStep(sagaLog, "SEND_NOTIFICATION", StepStatus.SUCCESS)
                        || hasStep(sagaLog, "SEND_NOTIFICATION", StepStatus.SKIPPED)
                );
            });

        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_QUEUE));
        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_DLQ_QUEUE));
    }
}
