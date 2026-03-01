package com.oldphonedeals.e2e;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
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
class SagaDlqFallbackE2E extends AbstractSagaE2E {

    @Test
    void shouldRouteCompensationMessageToDlqWhenCriticalStepAlwaysFails() {
        User buyer = seedVerifiedUser("buyer-dlq");
        User seller = seedVerifiedUser("seller-dlq");
        Phone phone = seedPhone(seller, "Saga DLQ Phone", 9, 11, 333.0);

        int quantity = 2;
        int stockBeforeCompensation = phone.getStock();
        int salesBeforeCompensation = phone.getSalesCount();

        String missingOrderId = "missing-order-" + UUID.randomUUID();
        String sagaId = "saga-dlq-e2e-" + UUID.randomUUID();
        List<OrderCompensationMessage.Item> items = List.of(compensationItem(phone.getId(), quantity));

        publishCompensation(buildCompensationMessage(
            sagaId,
            missingOrderId,
            buyer.getId(),
            items,
            "forced failure"
        ));

        Awaitility.await()
            .atMost(Duration.ofSeconds(120))
            .untilAsserted(() -> {
                assertEquals(1, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_DLQ_QUEUE));

                SagaLog sagaLog = sagaLogRepository.findBySagaId(sagaId).orElse(null);
                assertNotNull(sagaLog);
                assertEquals(SagaStatus.FAILED, sagaLog.getStatus());
                assertTrue(hasStep(sagaLog, "RESTORE_STOCK", StepStatus.SUCCESS));
                assertTrue(hasStep(sagaLog, "UPDATE_ORDER", StepStatus.FAILED));
                assertTrue(hasStep(sagaLog, "ORCHESTRATION", StepStatus.FAILED));
            });

        Phone updatedPhone = phoneRepository.findById(phone.getId()).orElse(null);
        assertNotNull(updatedPhone);
        assertEquals(stockBeforeCompensation + quantity, updatedPhone.getStock());
        assertEquals(salesBeforeCompensation - quantity, updatedPhone.getSalesCount());
        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_QUEUE));
    }
}
