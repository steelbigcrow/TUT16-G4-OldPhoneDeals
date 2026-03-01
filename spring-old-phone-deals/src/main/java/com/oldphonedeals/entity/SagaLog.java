package com.oldphonedeals.entity;

import com.oldphonedeals.enums.SagaStatus;
import com.oldphonedeals.enums.StepStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Saga 补偿审计日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "saga_logs")
public class SagaLog {
  @Id
  private String id;

  @Indexed(unique = true)
  private String sagaId;

  private String orderId;
  private String userId;
  private SagaStatus status;

  @Builder.Default
  private List<SagaStep> steps = new ArrayList<>();

  private String reason;
  private Instant startedAt;
  private Instant completedAt;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SagaStep {
    private String stepName;
    private StepStatus status;
    private Instant executedAt;
    private String errorMessage;
  }
}
