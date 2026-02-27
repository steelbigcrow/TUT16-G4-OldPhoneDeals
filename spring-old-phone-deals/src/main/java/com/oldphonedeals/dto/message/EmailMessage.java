package com.oldphonedeals.dto.message;

import com.oldphonedeals.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 邮件消息体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessage {
  private String messageId;
  private EmailType type;
  private String toEmail;
  private String userName;
  private String token;
  private String subject;
  private String htmlContent;
  private Instant timestamp;
}
