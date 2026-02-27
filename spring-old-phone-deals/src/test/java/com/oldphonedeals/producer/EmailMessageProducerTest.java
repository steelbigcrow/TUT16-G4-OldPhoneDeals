package com.oldphonedeals.producer;

import com.oldphonedeals.dto.message.EmailMessage;
import com.oldphonedeals.enums.EmailType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailMessageProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private EmailMessageProducer producer;

    @BeforeEach
    void setUp() {
        producer = new EmailMessageProducer(rabbitTemplate);
    }

    @Test
    void shouldPublishEmailMessageToNotificationExchange() {
        EmailMessage message = EmailMessage.builder()
            .messageId("msg-1")
            .type(EmailType.VERIFICATION)
            .toEmail("user@example.com")
            .userName("Test User")
            .token("verify-token")
            .timestamp(Instant.now())
            .build();

        producer.publish(message);

        verify(rabbitTemplate).convertAndSend("notification.exchange", "email.send", message);
    }

    @Test
    void shouldThrowWhenRabbitTemplateFails() {
        EmailMessage message = EmailMessage.builder()
            .messageId("msg-1")
            .type(EmailType.GENERIC)
            .toEmail("user@example.com")
            .subject("subject")
            .htmlContent("<h1>content</h1>")
            .timestamp(Instant.now())
            .build();

        doThrow(new AmqpException("broker down") {})
            .when(rabbitTemplate)
            .convertAndSend("notification.exchange", "email.send", message);

        assertThrows(IllegalStateException.class, () -> producer.publish(message));
    }
}
