package com.oldphonedeals.e2e;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.SagaLog;
import com.oldphonedeals.entity.User;
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
class SagaMultiItemStockE2E extends AbstractSagaE2E {

    @Test
    void shouldRestoreStockPreciselyForMultipleItems() {
        User buyer = seedVerifiedUser("buyer-multi");
        User seller = seedVerifiedUser("seller-multi");

        Phone phoneA = seedPhone(seller, "Saga Multi A", 3, 10, 180.0);
        Phone phoneB = seedPhone(seller, "Saga Multi B", 5, 9, 220.0);
        Phone phoneC = seedPhone(seller, "Saga Multi C", 2, 12, 300.0);

        int qtyA = 1;
        int qtyB = 2;
        int qtyC = 3;

        int stockABefore = phoneA.getStock();
        int stockBBefore = phoneB.getStock();
        int stockCBefore = phoneC.getStock();
        int salesABefore = phoneA.getSalesCount();
        int salesBBefore = phoneB.getSalesCount();
        int salesCBefore = phoneC.getSalesCount();

        List<OrderCompensationMessage.Item> items = List.of(
            compensationItem(phoneA.getId(), qtyA),
            compensationItem(phoneB.getId(), qtyB),
            compensationItem(phoneC.getId(), qtyC)
        );
        Order order = seedCompensatingOrder(buyer, items, 1520.0);
        String sagaId = "saga-multi-item-e2e-" + UUID.randomUUID();

        publishCompensation(buildCompensationMessage(
            sagaId,
            order.getId(),
            buyer.getId(),
            items,
            "multi-item restore"
        ));

        Awaitility.await()
            .atMost(Duration.ofSeconds(45))
            .untilAsserted(() -> {
                Order updatedOrder = orderRepository.findById(order.getId()).orElse(null);
                assertNotNull(updatedOrder);
                assertEquals(OrderPostProcessStatus.COMPENSATED, updatedOrder.getPostProcessStatus());

                Phone updatedPhoneA = phoneRepository.findById(phoneA.getId()).orElse(null);
                Phone updatedPhoneB = phoneRepository.findById(phoneB.getId()).orElse(null);
                Phone updatedPhoneC = phoneRepository.findById(phoneC.getId()).orElse(null);
                assertNotNull(updatedPhoneA);
                assertNotNull(updatedPhoneB);
                assertNotNull(updatedPhoneC);

                assertEquals(stockABefore + qtyA, updatedPhoneA.getStock());
                assertEquals(salesABefore - qtyA, updatedPhoneA.getSalesCount());
                assertEquals(stockBBefore + qtyB, updatedPhoneB.getStock());
                assertEquals(salesBBefore - qtyB, updatedPhoneB.getSalesCount());
                assertEquals(stockCBefore + qtyC, updatedPhoneC.getStock());
                assertEquals(salesCBefore - qtyC, updatedPhoneC.getSalesCount());

                SagaLog sagaLog = sagaLogRepository.findBySagaId(sagaId).orElse(null);
                assertNotNull(sagaLog);
                assertEquals(SagaStatus.COMPLETED, sagaLog.getStatus());
                assertTrue(hasStep(sagaLog, "RESTORE_STOCK", StepStatus.SUCCESS));
            });

        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_QUEUE));
        assertEquals(0, queueMessageCount(RabbitMQConfig.ORDER_COMPENSATION_DLQ_QUEUE));
    }
}
