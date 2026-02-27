package com.oldphonedeals.consumer;

import com.oldphonedeals.dto.message.EmailMessage;
import com.oldphonedeals.enums.EmailType;
import com.rabbitmq.client.Channel;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailConsumerTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private Channel channel;

    private EmailConsumer emailConsumer;

    @BeforeEach
    void setUp() {
        emailConsumer = new EmailConsumer(mailSender);
        ReflectionTestUtils.setField(emailConsumer, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(emailConsumer, "frontendUrl", "http://localhost:5173");
    }

    @Test
    void shouldSendVerificationEmailAndAck() throws Exception {
        EmailMessage message = EmailMessage.builder()
            .messageId("msg-1")
            .type(EmailType.VERIFICATION)
            .toEmail("user@example.com")
            .userName("User")
            .token("verify-token")
            .timestamp(Instant.now())
            .build();
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        emailConsumer.handleEmailMessage(message, channel, 1L);

        verify(mailSender).send(any(MimeMessage.class));
        verify(channel).basicAck(1L, false);
    }

    @Test
    void shouldNackWhenSendFails() throws Exception {
        EmailMessage message = EmailMessage.builder()
            .messageId("msg-2")
            .type(EmailType.GENERIC)
            .toEmail("user@example.com")
            .subject("subject")
            .htmlContent("<h1>Hello</h1>")
            .timestamp(Instant.now())
            .build();
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new RuntimeException("smtp timeout")).when(mailSender).send(any(MimeMessage.class));

        emailConsumer.handleEmailMessage(message, channel, 2L);

        verify(channel).basicNack(2L, false, false);
    }
}
