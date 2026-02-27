package com.oldphonedeals.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 消息幂等记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "processed_messages")
public class ProcessedMessage {
  @Id
  private String id;

  @Indexed(unique = true)
  private String messageId;

  private String messageType;

  @CreatedDate
  private LocalDateTime processedAt;
}
