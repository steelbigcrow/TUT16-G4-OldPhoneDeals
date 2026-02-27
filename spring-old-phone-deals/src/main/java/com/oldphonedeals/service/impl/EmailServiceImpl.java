package com.oldphonedeals.service.impl;

import com.oldphonedeals.dto.message.EmailMessage;
import com.oldphonedeals.enums.EmailType;
import com.oldphonedeals.producer.EmailMessageProducer;
import com.oldphonedeals.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * 邮件发送服务实现：仅负责发布 MQ 消息
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

  private final EmailMessageProducer emailMessageProducer;

  @Override
  public void sendVerificationEmail(String toEmail, String verifyToken, String userName) {
    publish(EmailMessage.builder()
      .messageId(UUID.randomUUID().toString())
      .type(EmailType.VERIFICATION)
      .toEmail(toEmail)
      .userName(userName)
      .token(verifyToken)
      .timestamp(Instant.now())
      .build());
  }

  @Override
  public void sendPasswordResetEmail(String toEmail, String resetToken, String userName) {
    publish(EmailMessage.builder()
      .messageId(UUID.randomUUID().toString())
      .type(EmailType.PASSWORD_RESET_LINK)
      .toEmail(toEmail)
      .userName(userName)
      .token(resetToken)
      .timestamp(Instant.now())
      .build());
  }

  @Override
  public void sendPasswordResetCodeEmail(String toEmail, String resetCode, String userName) {
    publish(EmailMessage.builder()
      .messageId(UUID.randomUUID().toString())
      .type(EmailType.PASSWORD_RESET_CODE)
      .toEmail(toEmail)
      .userName(userName)
      .token(resetCode)
      .timestamp(Instant.now())
      .build());
  }

  @Override
  public void sendEmail(String toEmail, String subject, String content) {
    publish(EmailMessage.builder()
      .messageId(UUID.randomUUID().toString())
      .type(EmailType.GENERIC)
      .toEmail(toEmail)
      .subject(subject)
      .htmlContent(content)
      .timestamp(Instant.now())
      .build());
  }

  private void publish(EmailMessage message) {
    log.debug("Publishing email message: {} type={}", message.getMessageId(), message.getType());
    emailMessageProducer.publish(message);
  }
}
