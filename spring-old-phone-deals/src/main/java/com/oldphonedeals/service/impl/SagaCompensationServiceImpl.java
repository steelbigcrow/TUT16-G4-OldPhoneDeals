package com.oldphonedeals.service.impl;

import com.oldphonedeals.dto.message.EmailMessage;
import com.oldphonedeals.dto.message.OrderCompensationMessage;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.SagaLog;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.EmailType;
import com.oldphonedeals.enums.OrderCheckoutStatus;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.enums.SagaStatus;
import com.oldphonedeals.enums.StepStatus;
import com.oldphonedeals.producer.EmailMessageProducer;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.SagaLogRepository;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.repository.custom.PhoneStockRepository;
import com.oldphonedeals.service.SagaCompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaCompensationServiceImpl implements SagaCompensationService {

  private static final String RESTORE_STOCK_STEP = "RESTORE_STOCK";
  private static final String UPDATE_ORDER_STEP = "UPDATE_ORDER";
  private static final String SEND_NOTIFICATION_STEP = "SEND_NOTIFICATION";
  private static final String ORCHESTRATION_STEP = "ORCHESTRATION";

  private final SagaLogRepository sagaLogRepository;
  private final PhoneStockRepository phoneStockRepository;
  private final OrderRepository orderRepository;
  private final UserRepository userRepository;
  private final EmailMessageProducer emailMessageProducer;

  @Override
  public boolean isSagaCompleted(String sagaId) {
    if (sagaId == null || sagaId.isBlank()) {
      return false;
    }
    return sagaLogRepository.findBySagaId(sagaId)
      .map(log -> log.getStatus() == SagaStatus.COMPLETED)
      .orElse(false);
  }

  @Override
  public void compensate(OrderCompensationMessage message) {
    validateCompensationMessage(message);
    SagaLog sagaLog = getOrCreateSagaLog(message);

    if (sagaLog.getStatus() == SagaStatus.COMPLETED) {
      return;
    }

    if (sagaLog.getStatus() == SagaStatus.RETRYING) {
      sagaLog.setStatus(SagaStatus.RETRYING);
    } else {
      sagaLog.setStatus(SagaStatus.STARTED);
    }
    sagaLog.setCompletedAt(null);
    sagaLogRepository.save(sagaLog);

    runCriticalStep(sagaLog, RESTORE_STOCK_STEP, () -> restoreStock(message));
    runCriticalStep(sagaLog, UPDATE_ORDER_STEP, () -> updateOrder(message));
    runBestEffortNotificationStep(sagaLog, message);

    sagaLog.setStatus(SagaStatus.COMPLETED);
    sagaLog.setCompletedAt(Instant.now());
    sagaLogRepository.save(sagaLog);
  }

  @Override
  public void markSagaRetrying(OrderCompensationMessage message, String errorMessage) {
    if (message == null || message.getSagaId() == null || message.getSagaId().isBlank()) {
      return;
    }

    SagaLog sagaLog = getOrCreateSagaLog(message);
    sagaLog.setStatus(SagaStatus.RETRYING);
    sagaLog.setCompletedAt(null);
    appendStep(sagaLog, ORCHESTRATION_STEP, StepStatus.FAILED, errorMessage);
    sagaLogRepository.save(sagaLog);
  }

  @Override
  public void markSagaFailed(OrderCompensationMessage message, String errorMessage) {
    if (message == null || message.getSagaId() == null || message.getSagaId().isBlank()) {
      return;
    }

    SagaLog sagaLog = getOrCreateSagaLog(message);
    sagaLog.setStatus(SagaStatus.FAILED);
    sagaLog.setCompletedAt(Instant.now());
    appendStep(sagaLog, ORCHESTRATION_STEP, StepStatus.FAILED, errorMessage);
    sagaLogRepository.save(sagaLog);
  }

  private void runCriticalStep(SagaLog sagaLog, String stepName, Runnable stepAction) {
    if (hasSuccessfulStep(sagaLog, stepName)) {
      return;
    }

    try {
      stepAction.run();
      appendStep(sagaLog, stepName, StepStatus.SUCCESS, null);
      sagaLogRepository.save(sagaLog);
    } catch (RuntimeException ex) {
      appendStep(sagaLog, stepName, StepStatus.FAILED, ex.getMessage());
      sagaLogRepository.save(sagaLog);
      throw ex;
    }
  }

  private void runBestEffortNotificationStep(SagaLog sagaLog, OrderCompensationMessage message) {
    if (hasSuccessfulStep(sagaLog, SEND_NOTIFICATION_STEP)) {
      return;
    }

    try {
      sendCompensationNotification(message);
      appendStep(sagaLog, SEND_NOTIFICATION_STEP, StepStatus.SUCCESS, null);
    } catch (RuntimeException ex) {
      appendStep(sagaLog, SEND_NOTIFICATION_STEP, StepStatus.SKIPPED, ex.getMessage());
      log.warn("Compensation notification skipped for saga: {}", message.getSagaId(), ex);
    }

    sagaLogRepository.save(sagaLog);
  }

  private void restoreStock(OrderCompensationMessage message) {
    List<OrderCompensationMessage.Item> items = message.getItems();
    if (items == null || items.isEmpty()) {
      throw new IllegalStateException("Compensation items are empty");
    }

    for (OrderCompensationMessage.Item item : items) {
      if (item == null || item.getPhoneId() == null || item.getPhoneId().isBlank() || item.getQuantity() == null || item.getQuantity() <= 0) {
        throw new IllegalStateException("Invalid compensation item");
      }

      boolean restored = phoneStockRepository.increaseStockAndDecreaseSales(item.getPhoneId(), item.getQuantity());
      if (!restored) {
        throw new IllegalStateException("Failed to restore stock for phone: " + item.getPhoneId());
      }
    }
  }

  private void updateOrder(OrderCompensationMessage message) {
    Order order = orderRepository.findById(message.getOrderId())
      .orElseThrow(() -> new IllegalStateException("Order not found: " + message.getOrderId()));

    order.setPostProcessStatus(OrderPostProcessStatus.COMPENSATED);
    order.setPostProcessError(null);
    order.setCheckoutStatus(OrderCheckoutStatus.FAILED);
    order.setCheckoutError(buildCompensatedCheckoutError(message.getReason()));
    orderRepository.save(order);
  }

  private void sendCompensationNotification(OrderCompensationMessage message) {
    User user = userRepository.findById(message.getUserId())
      .orElseThrow(() -> new IllegalStateException("User not found: " + message.getUserId()));

    String userName = buildUserName(user);
    String subject = "Order Compensation Completed - Old Phone Deals";
    String content = buildCompensationEmailContent(userName, message);

    EmailMessage emailMessage = EmailMessage.builder()
      .messageId(UUID.randomUUID().toString())
      .type(EmailType.GENERIC)
      .toEmail(user.getEmail())
      .userName(userName)
      .subject(subject)
      .htmlContent(content)
      .timestamp(Instant.now())
      .build();

    emailMessageProducer.publish(emailMessage);
  }

  private SagaLog getOrCreateSagaLog(OrderCompensationMessage message) {
    SagaLog sagaLog = sagaLogRepository.findBySagaId(message.getSagaId())
      .orElseGet(() -> SagaLog.builder()
        .sagaId(message.getSagaId())
        .orderId(message.getOrderId())
        .userId(message.getUserId())
        .status(SagaStatus.STARTED)
        .reason(message.getReason())
        .startedAt(Instant.now())
        .steps(new ArrayList<>())
        .build());

    if (sagaLog.getOrderId() == null) {
      sagaLog.setOrderId(message.getOrderId());
    }
    if (sagaLog.getUserId() == null) {
      sagaLog.setUserId(message.getUserId());
    }
    if (sagaLog.getReason() == null || sagaLog.getReason().isBlank()) {
      sagaLog.setReason(message.getReason());
    }
    if (sagaLog.getStartedAt() == null) {
      sagaLog.setStartedAt(Instant.now());
    }
    if (sagaLog.getSteps() == null) {
      sagaLog.setSteps(new ArrayList<>());
    }

    return sagaLog;
  }

  private boolean hasSuccessfulStep(SagaLog sagaLog, String stepName) {
    List<SagaLog.SagaStep> steps = sagaLog.getSteps();
    if (steps == null || steps.isEmpty()) {
      return false;
    }
    return steps.stream().anyMatch(step -> stepName.equals(step.getStepName()) && step.getStatus() == StepStatus.SUCCESS);
  }

  private void appendStep(SagaLog sagaLog, String stepName, StepStatus status, String errorMessage) {
    SagaLog.SagaStep step = SagaLog.SagaStep.builder()
      .stepName(stepName)
      .status(status)
      .executedAt(Instant.now())
      .errorMessage(errorMessage)
      .build();

    List<SagaLog.SagaStep> steps = sagaLog.getSteps();
    if (steps == null) {
      steps = new ArrayList<>();
      sagaLog.setSteps(steps);
    }
    steps.add(step);
  }

  private void validateCompensationMessage(OrderCompensationMessage message) {
    if (message == null) {
      throw new IllegalArgumentException("Compensation message is required");
    }
    if (message.getSagaId() == null || message.getSagaId().isBlank()) {
      throw new IllegalArgumentException("Compensation sagaId is required");
    }
    if (message.getOrderId() == null || message.getOrderId().isBlank()) {
      throw new IllegalArgumentException("Compensation orderId is required");
    }
    if (message.getUserId() == null || message.getUserId().isBlank()) {
      throw new IllegalArgumentException("Compensation userId is required");
    }
  }

  private String buildCompensatedCheckoutError(String reason) {
    if (reason == null || reason.isBlank()) {
      return "Order compensated due to post-process failure";
    }
    return "Order compensated: " + reason;
  }

  private String buildUserName(User user) {
    String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
    String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
    String fullName = (firstName + " " + lastName).trim();
    if (!fullName.isBlank()) {
      return fullName;
    }
    return user.getEmail();
  }

  private String buildCompensationEmailContent(String userName, OrderCompensationMessage message) {
    String reason = message.getReason();
    if (reason == null || reason.isBlank()) {
      reason = "post-process failure";
    }

    return """
      <p>Hi %s,</p>
      <p>Your order has been canceled and compensated automatically.</p>
      <p>Order ID: <strong>%s</strong></p>
      <p>Reason: %s</p>
      <p>The reserved stock has been restored. Please place a new order if needed.</p>
      """.formatted(userName, message.getOrderId(), reason);
  }
}
