package com.oldphonedeals.consumer;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.dto.message.EmailMessage;
import com.oldphonedeals.enums.EmailType;
import com.rabbitmq.client.Channel;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.from}")
  private String fromEmail;

  @Value("${frontend.url}")
  private String frontendUrl;

  @RabbitListener(
    queues = RabbitMQConfig.EMAIL_SEND_QUEUE,
    containerFactory = "emailListenerContainerFactory"
  )
  public void handleEmailMessage(
    EmailMessage message,
    Channel channel,
    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
  ) {
    try {
      sendEmailByType(message);
      channel.basicAck(deliveryTag, false);
      log.debug("Email consumed successfully: {}", message.getMessageId());
    } catch (Exception ex) {
      log.error("Email consumption failed: {}", message.getMessageId(), ex);
      try {
        channel.basicNack(deliveryTag, false, false);
      } catch (Exception channelEx) {
        log.error("Failed to nack email message: {}", message.getMessageId(), channelEx);
      }
    }
  }

  private void sendEmailByType(EmailMessage message) {
    EmailType type = message.getType();
    if (type == null) {
      throw new IllegalArgumentException("Email type is required");
    }

    switch (type) {
      case VERIFICATION -> {
        String verifyUrl =
          frontendUrl
            + "/verify-email?email=" + URLEncoder.encode(message.getToEmail(), StandardCharsets.UTF_8)
            + "&token=" + URLEncoder.encode(message.getToken(), StandardCharsets.UTF_8);
        send(message.getToEmail(), "Verify Your Email - Old Phone Deals",
          buildVerificationEmailContent(message.getUserName(), verifyUrl));
      }
      case PASSWORD_RESET_LINK -> {
        String resetUrl =
          frontendUrl
            + "/reset-password?email=" + URLEncoder.encode(message.getToEmail(), StandardCharsets.UTF_8)
            + "&token=" + URLEncoder.encode(message.getToken(), StandardCharsets.UTF_8);
        send(message.getToEmail(), "Reset Your Password - Old Phone Deals",
          buildPasswordResetEmailContent(message.getUserName(), resetUrl));
      }
      case PASSWORD_RESET_CODE ->
        send(message.getToEmail(), "Reset Your Password - Old Phone Deals",
          buildPasswordResetCodeEmailContent(message.getUserName(), message.getToken()));
      case GENERIC -> send(message.getToEmail(), message.getSubject(), message.getHtmlContent());
      default -> throw new IllegalArgumentException("Unsupported email type: " + type);
    }
  }

  private void send(String toEmail, String subject, String content) {
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject(subject);
      helper.setText(content, true);
      mailSender.send(mimeMessage);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to send mail", ex);
    }
  }

  private String buildVerificationEmailContent(String userName, String verifyUrl) {
    return """
      <p>Hi %s,</p>
      <p>Welcome to Old Phone Deals. Please verify your email:</p>
      <p><a href="%s">Verify Email</a></p>
      <p>%s</p>
      """.formatted(userName, verifyUrl, verifyUrl);
  }

  private String buildPasswordResetEmailContent(String userName, String resetUrl) {
    return """
      <p>Hi %s,</p>
      <p>Click the link below to reset your password:</p>
      <p><a href="%s">Reset Password</a></p>
      <p>%s</p>
      """.formatted(userName, resetUrl, resetUrl);
  }

  private String buildPasswordResetCodeEmailContent(String userName, String resetCode) {
    return """
      <p>Hi %s,</p>
      <p>Your password reset code is:</p>
      <h2>%s</h2>
      """.formatted(userName, resetCode);
  }
}
