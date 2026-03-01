package com.oldphonedeals.e2e;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.SagaLog;
import com.oldphonedeals.entity.User;
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
class SagaIdempotentE2E extends AbstractSagaE2E {

    @Test
    void shouldProcessSameSagaIdOnlyOnce() {
        User buyer = seedVerifiedUser("buyer-idempotent");
        User seller = seedVerifiedUser("seller-idempotent");
        Phone phone = seedPhone(seller, "Saga Idempotent Phone", 7, 8, 249.0);

        int quantity = 3;
        int stockBeforeCompensation = phone.getStock();
        int salesBeforeCompensation = phone.getSalesCount();

        List<OrderCompensationMessage.Item> items = List.of(compensationItem(phone.getId(), quantity));
        Order order = seedCompensatingOrder(buyer, items, 747.0);
        String sagaId = "saga-idempotent-e2e-" + UUID.randomUUID();

        OrderCompensationMessage message = buildCompensationMessage(
            sagaId,
            order.getId(),
            buyer.getId(),
            items,
            "idempotent verification"
        );

        publishCompensation(message);
        publishCompensation(message);

        Awaitility.await()
            .atMost(Duration.ofSeconds(45))
            .untilAsserted(() -> {
                SagaLog sagaLog = sagaLogRepository.findBySagaId(sagaId).orElse(null);
                assertNotNull(sagaLog);
                assertEquals(SagaStatus.COMPLETED, sagaLog.getStatus());

                Phone updatedPhone = phoneRepository.findById(phone.getId()).orElse(null);
                assertNotNull(updatedPhone);
                assertEquals(stockBeforeCompensation + quantity, updatedPhone.getStock());
                assertEquals(salesBeforeCompensation - quantity, updatedPhone.getSalesCount());
            });

        long sagaLogCount = sagaLogRepository.findAll().stream()
            .filter(log -> sagaId.equals(log.getSagaId()))
            .count();
        assertEquals(1, sagaLogCount);

        SagaLog savedLog = sagaLogRepository.findBySagaId(sagaId).orElse(null);
        assertNotNull(savedLog);
        assertTrue(hasStep(savedLog, "RESTORE_STOCK", StepStatus.SUCCESS));
        long restoreSuccessTimes = savedLog.getSteps().stream()
            .filter(step -> "RESTORE_STOCK".equals(step.getStepName()) && step.getStatus() == StepStatus.SUCCESS)
            .count();
        assertEquals(1, restoreSuccessTimes);

        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_QUEUE));
        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_DLQ_QUEUE));
    }
}
