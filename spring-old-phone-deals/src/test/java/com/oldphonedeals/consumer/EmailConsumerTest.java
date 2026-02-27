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
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
            .toEmail("user+name@example.com")
            .userName("User")
            .token("verify token+1")
            .timestamp(Instant.now())
            .build();
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailConsumer.handleEmailMessage(message, channel, 1L);

        verify(mailSender).send(mimeMessage);
        verify(channel).basicAck(1L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());

        String rawMime = toRawMime(mimeMessage);
        assertTrue(rawMime.contains("email=user%2Bname%40example.com"));
        assertTrue(rawMime.contains("token=verify+token%2B1"));
    }

    @Test
    void shouldSendPasswordResetLinkAndAck() throws Exception {
        EmailMessage message = EmailMessage.builder()
            .messageId("msg-2")
            .type(EmailType.PASSWORD_RESET_LINK)
            .toEmail("user@example.com")
            .userName("User")
            .token("reset-token")
            .timestamp(Instant.now())
            .build();
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailConsumer.handleEmailMessage(message, channel, 2L);

        verify(mailSender, times(1)).send(mimeMessage);
        verify(channel).basicAck(2L, false);
        assertTrue("Reset Your Password - Old Phone Deals".equals(mimeMessage.getSubject()));
    }

    @Test
    void shouldSendPasswordResetCodeAndAck() throws Exception {
        EmailMessage message = EmailMessage.builder()
            .messageId("msg-3")
            .type(EmailType.PASSWORD_RESET_CODE)
            .toEmail("user@example.com")
            .userName("User")
            .token("123456")
            .timestamp(Instant.now())
            .build();
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailConsumer.handleEmailMessage(message, channel, 3L);

        verify(mailSender, times(1)).send(mimeMessage);
        verify(channel).basicAck(3L, false);
    }

    @Test
    void shouldThrowWhenTypeIsNull() throws Exception {
        EmailMessage message = EmailMessage.builder()
            .messageId("msg-4")
            .type(null)
            .toEmail("user@example.com")
            .timestamp(Instant.now())
            .build();

        assertThrows(IllegalStateException.class, () ->
            emailConsumer.handleEmailMessage(message, channel, 4L)
        );
    }

    @Test
    void shouldThrowWhenSendFailsToTriggerRetry() throws Exception {
        EmailMessage message = EmailMessage.builder()
            .messageId("msg-5")
            .type(EmailType.GENERIC)
            .toEmail("user@example.com")
            .subject("subject")
            .htmlContent("<h1>Hello</h1>")
            .timestamp(Instant.now())
            .build();
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new RuntimeException("smtp timeout")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(IllegalStateException.class, () ->
            emailConsumer.handleEmailMessage(message, channel, 5L)
        );
    }

    @Test
    void shouldThrowWhenAckFailsToTriggerRetry() throws Exception {
        EmailMessage message = EmailMessage.builder()
            .messageId("msg-6")
            .type(EmailType.GENERIC)
            .toEmail("user@example.com")
            .subject("subject")
            .htmlContent("<h1>Hello</h1>")
            .timestamp(Instant.now())
            .build();
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new RuntimeException("channel unavailable")).when(channel).basicAck(6L, false);

        assertThrows(IllegalStateException.class, () ->
            emailConsumer.handleEmailMessage(message, channel, 6L)
        );
    }

    @Test
    void shouldNackWithoutRequeueWhenRetryExhausted() throws Exception {
        EmailMessage message = EmailMessage.builder()
            .messageId("msg-7")
            .type(EmailType.GENERIC)
            .toEmail("user@example.com")
            .subject("subject")
            .htmlContent("<h1>Hello</h1>")
            .timestamp(Instant.now())
            .build();
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new RuntimeException("smtp timeout")).when(mailSender).send(any(MimeMessage.class));

        RetryTemplate retryTemplate = RetryTemplate.builder().maxAttempts(3).build();

        retryTemplate.execute(context -> {
            emailConsumer.handleEmailMessage(message, channel, 7L);
            return null;
        });

        verify(mailSender, times(3)).send(any(MimeMessage.class));
        verify(channel).basicNack(7L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    private String toRawMime(MimeMessage mimeMessage) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mimeMessage.writeTo(out);
        return out.toString();
    }
}
