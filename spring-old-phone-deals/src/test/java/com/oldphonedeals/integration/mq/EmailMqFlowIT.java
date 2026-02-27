package com.oldphonedeals.integration.mq;

import com.oldphonedeals.config.RabbitMQConfig;
import com.oldphonedeals.consumer.EmailConsumer;
import com.oldphonedeals.dto.message.EmailMessage;
import com.oldphonedeals.enums.EmailType;
import com.oldphonedeals.producer.EmailMessageProducer;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = EmailMqFlowIT.EmailMqTestApplication.class)
class EmailMqFlowIT extends AbstractRabbitMqIT {

    @Autowired
    private EmailMessageProducer emailMessageProducer;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void shouldConsumeEmailMessageAndAck() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        EmailMessage message = EmailMessage.builder()
            .messageId("email-it-" + UUID.randomUUID())
            .type(EmailType.VERIFICATION)
            .toEmail("integration@example.com")
            .userName("Integration User")
            .token("verify-token")
            .timestamp(Instant.now())
            .build();

        emailMessageProducer.publish(message);

        Awaitility.await()
            .atMost(Duration.ofSeconds(15))
            .untilAsserted(() -> verify(mailSender, times(1)).send(any(MimeMessage.class)));

        Awaitility.await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> queueMessageCount(RabbitMQConfig.EMAIL_SEND_QUEUE) == 0);

        assertEquals(0, queueMessageCount(RabbitMQConfig.EMAIL_DLQ_QUEUE));
    }

    @Test
    void shouldRetryAndRouteToDlqWhenEmailSendFails() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new IllegalStateException("smtp down"))
            .when(mailSender)
            .send(any(MimeMessage.class));

        EmailMessage message = EmailMessage.builder()
            .messageId("email-dlq-it-" + UUID.randomUUID())
            .type(EmailType.GENERIC)
            .toEmail("integration@example.com")
            .subject("DLQ test")
            .htmlContent("<p>DLQ</p>")
            .timestamp(Instant.now())
            .build();

        emailMessageProducer.publish(message);

        Awaitility.await()
            .atMost(Duration.ofSeconds(90))
            .until(() -> queueMessageCount(RabbitMQConfig.EMAIL_DLQ_QUEUE) == 1);

        verify(mailSender, atLeast(3)).send(any(MimeMessage.class));
        assertEquals(0, queueMessageCount(RabbitMQConfig.EMAIL_SEND_QUEUE));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        MongoRepositoriesAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
    })
    @Import({RabbitMQConfig.class, EmailMessageProducer.class, EmailConsumer.class})
    static class EmailMqTestApplication {
    }
}
