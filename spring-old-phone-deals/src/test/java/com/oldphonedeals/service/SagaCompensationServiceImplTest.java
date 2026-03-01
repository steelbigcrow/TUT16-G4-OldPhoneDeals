package com.oldphonedeals.service;

import com.oldphonedeals.dto.message.EmailMessage;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.SagaLog;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.OrderCheckoutStatus;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.enums.SagaStatus;
import com.oldphonedeals.enums.StepStatus;
import com.oldphonedeals.producer.EmailMessageProducer;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.SagaLogRepository;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.repository.custom.PhoneStockRepository;
import com.oldphonedeals.service.impl.SagaCompensationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SagaCompensationServiceImplTest {

    @Mock
    private SagaLogRepository sagaLogRepository;

    @Mock
    private PhoneStockRepository phoneStockRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailMessageProducer emailMessageProducer;

    @InjectMocks
    private SagaCompensationServiceImpl service;

    @Captor
    private ArgumentCaptor<SagaLog> sagaLogCaptor;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<EmailMessage> emailCaptor;

    @Test
    void shouldCompleteCompensationFlowWhenAllCriticalStepsSucceed() {
        OrderCompensationMessage message = buildMessage("saga-flow", "order-flow", "user-flow");
        when(sagaLogRepository.findBySagaId("saga-flow")).thenReturn(Optional.empty());
        when(sagaLogRepository.save(any(SagaLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(phoneStockRepository.increaseStockAndDecreaseSales("phone-1", 2)).thenReturn(true);
        when(orderRepository.findById("order-flow")).thenReturn(Optional.of(buildOrder("order-flow")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById("user-flow")).thenReturn(Optional.of(buildUser("user-flow")));

        service.compensate(message);

        verify(phoneStockRepository).increaseStockAndDecreaseSales("phone-1", 2);
        verify(orderRepository).save(orderCaptor.capture());
        Order updatedOrder = orderCaptor.getValue();
        assertEquals(OrderPostProcessStatus.COMPENSATED, updatedOrder.getPostProcessStatus());
        assertEquals(OrderCheckoutStatus.FAILED, updatedOrder.getCheckoutStatus());
        assertEquals("Order compensated: post-process failed", updatedOrder.getCheckoutError());

        verify(emailMessageProducer).publish(emailCaptor.capture());
        assertEquals("buyer@example.com", emailCaptor.getValue().getToEmail());

        verify(sagaLogRepository, atLeastOnce()).save(sagaLogCaptor.capture());
        SagaLog completedLog = lastSavedSagaLog();
        assertEquals(SagaStatus.COMPLETED, completedLog.getStatus());
        assertTrue(hasStep(completedLog, "RESTORE_STOCK", StepStatus.SUCCESS));
        assertTrue(hasStep(completedLog, "UPDATE_ORDER", StepStatus.SUCCESS));
        assertTrue(hasStep(completedLog, "SEND_NOTIFICATION", StepStatus.SUCCESS));
        assertNotNull(completedLog.getCompletedAt());
    }

    @Test
    void shouldThrowAndRecordFailedStepWhenStockRestoreFails() {
        OrderCompensationMessage message = buildMessage("saga-stock-fail", "order-stock-fail", "user-stock-fail");
        when(sagaLogRepository.findBySagaId("saga-stock-fail")).thenReturn(Optional.empty());
        when(sagaLogRepository.save(any(SagaLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(phoneStockRepository.increaseStockAndDecreaseSales("phone-1", 2)).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.compensate(message));

        assertTrue(exception.getMessage().contains("Failed to restore stock"));
        verify(orderRepository, never()).save(any(Order.class));
        verify(emailMessageProducer, never()).publish(any(EmailMessage.class));

        verify(sagaLogRepository, atLeastOnce()).save(sagaLogCaptor.capture());
        SagaLog failedLog = lastSavedSagaLog();
        assertEquals(SagaStatus.STARTED, failedLog.getStatus());
        assertTrue(hasStep(failedLog, "RESTORE_STOCK", StepStatus.FAILED));
    }

    @Test
    void shouldSkipNotificationFailureAndStillCompleteSaga() {
        OrderCompensationMessage message = buildMessage("saga-notification", "order-notification", "user-notification");
        when(sagaLogRepository.findBySagaId("saga-notification")).thenReturn(Optional.empty());
        when(sagaLogRepository.save(any(SagaLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(phoneStockRepository.increaseStockAndDecreaseSales("phone-1", 2)).thenReturn(true);
        when(orderRepository.findById("order-notification")).thenReturn(Optional.of(buildOrder("order-notification")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById("user-notification")).thenReturn(Optional.of(buildUser("user-notification")));
        doThrow(new IllegalStateException("mail down")).when(emailMessageProducer).publish(any(EmailMessage.class));

        service.compensate(message);

        verify(sagaLogRepository, atLeastOnce()).save(sagaLogCaptor.capture());
        SagaLog completedLog = lastSavedSagaLog();
        assertEquals(SagaStatus.COMPLETED, completedLog.getStatus());
        assertTrue(hasStep(completedLog, "SEND_NOTIFICATION", StepStatus.SKIPPED));
    }

    @Test
    void shouldReturnSagaCompletedState() {
        when(sagaLogRepository.findBySagaId("saga-done")).thenReturn(Optional.of(SagaLog.builder()
            .sagaId("saga-done")
            .status(SagaStatus.COMPLETED)
            .build()));
        when(sagaLogRepository.findBySagaId("saga-missing")).thenReturn(Optional.empty());

        assertTrue(service.isSagaCompleted("saga-done"));
        assertFalse(service.isSagaCompleted("saga-missing"));
        assertFalse(service.isSagaCompleted(" "));
    }

    @Test
    void shouldMarkSagaRetryingAndFailed() {
        OrderCompensationMessage message = buildMessage("saga-state", "order-state", "user-state");
        when(sagaLogRepository.findBySagaId("saga-state")).thenReturn(Optional.empty());
        when(sagaLogRepository.save(any(SagaLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.markSagaRetrying(message, "temporary failure");
        service.markSagaFailed(message, "retries exhausted");

        verify(sagaLogRepository, atLeastOnce()).save(sagaLogCaptor.capture());
        List<SagaLog> savedLogs = sagaLogCaptor.getAllValues();
        SagaLog retryingLog = savedLogs.get(savedLogs.size() - 2);
        SagaLog failedLog = savedLogs.get(savedLogs.size() - 1);

        assertEquals(SagaStatus.RETRYING, retryingLog.getStatus());
        assertTrue(hasStep(retryingLog, "ORCHESTRATION", StepStatus.FAILED));

        assertEquals(SagaStatus.FAILED, failedLog.getStatus());
        assertTrue(hasStep(failedLog, "ORCHESTRATION", StepStatus.FAILED));
        assertNotNull(failedLog.getCompletedAt());
    }

    private SagaLog lastSavedSagaLog() {
        List<SagaLog> logs = sagaLogCaptor.getAllValues();
        return logs.get(logs.size() - 1);
    }

    private boolean hasStep(SagaLog log, String stepName, StepStatus status) {
        if (log.getSteps() == null || log.getSteps().isEmpty()) {
            return false;
        }
        return log.getSteps().stream()
            .anyMatch(step -> stepName.equals(step.getStepName()) && status == step.getStatus());
    }

    private OrderCompensationMessage buildMessage(String sagaId, String orderId, String userId) {
        return OrderCompensationMessage.builder()
            .sagaId(sagaId)
            .orderId(orderId)
            .userId(userId)
            .items(List.of(OrderCompensationMessage.Item.builder()
                .phoneId("phone-1")
                .quantity(2)
                .build()))
            .totalAmount(399.0)
            .reason("post-process failed")
            .timestamp(Instant.now())
            .build();
    }

    private Order buildOrder(String orderId) {
        return Order.builder()
            .id(orderId)
            .postProcessStatus(OrderPostProcessStatus.COMPENSATING)
            .checkoutStatus(OrderCheckoutStatus.COMPLETED)
            .build();
    }

    private User buildUser(String userId) {
        return User.builder()
            .id(userId)
            .firstName("Demo")
            .lastName("Buyer")
            .email("buyer@example.com")
            .build();
    }
}
