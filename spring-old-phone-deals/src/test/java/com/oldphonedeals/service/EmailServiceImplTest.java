package com.oldphonedeals.service;

import com.oldphonedeals.dto.message.EmailMessage;
import com.oldphonedeals.enums.EmailType;
import com.oldphonedeals.producer.EmailMessageProducer;
import com.oldphonedeals.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * 邮件服务测试类
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {
    
    @Mock
    private EmailMessageProducer emailMessageProducer;
    
    private EmailServiceImpl emailService;
    
    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(emailMessageProducer);
    }
    
    @Test
    void shouldPublishVerificationEmailMessage() {
        // Given
        String toEmail = "user@example.com";
        String verifyToken = "test-verify-token";
        String userName = "Test User";

        // When
        emailService.sendVerificationEmail(toEmail, verifyToken, userName);
        
        // Then
        verify(emailMessageProducer).publish(argThat(message ->
            message.getType() == EmailType.VERIFICATION
                && toEmail.equals(message.getToEmail())
                && verifyToken.equals(message.getToken())
                && userName.equals(message.getUserName())
                && message.getMessageId() != null
                && message.getTimestamp() != null
        ));
    }
    
    @Test
    void shouldPublishPasswordResetLinkMessage() {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token";
        String userName = "Test User";

        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, userName);
        
        // Then
        verify(emailMessageProducer).publish(argThat(message ->
            message.getType() == EmailType.PASSWORD_RESET_LINK
                && toEmail.equals(message.getToEmail())
                && resetToken.equals(message.getToken())
                && userName.equals(message.getUserName())
                && message.getMessageId() != null
                && message.getTimestamp() != null
        ));
    }

    @Test
    void shouldPublishPasswordResetCodeMessage() {
        String toEmail = "user@example.com";
        String resetCode = "123456";
        String userName = "Test User";

        emailService.sendPasswordResetCodeEmail(toEmail, resetCode, userName);

        verify(emailMessageProducer).publish(argThat(message ->
            message.getType() == EmailType.PASSWORD_RESET_CODE
                && toEmail.equals(message.getToEmail())
                && resetCode.equals(message.getToken())
                && userName.equals(message.getUserName())
                && message.getMessageId() != null
                && message.getTimestamp() != null
        ));
    }
    
    @Test
    void shouldPublishGenericEmailMessage() {
        // Given
        String toEmail = "user@example.com";
        String subject = "Test Subject";
        String htmlContent = "<h1>Test HTML Content</h1>";

        // When
        emailService.sendEmail(toEmail, subject, htmlContent);
        
        // Then
        verify(emailMessageProducer).publish(argThat(message ->
            message.getType() == EmailType.GENERIC
                && toEmail.equals(message.getToEmail())
                && subject.equals(message.getSubject())
                && htmlContent.equals(message.getHtmlContent())
                && message.getMessageId() != null
                && message.getTimestamp() != null
        ));
    }
    
    @Test
    void shouldPropagateExceptionWhenPublishFails() {
        // Given
        doThrow(new IllegalStateException("broker unavailable"))
            .when(emailMessageProducer)
            .publish(any(EmailMessage.class));

        // When & Then
        assertThrows(IllegalStateException.class, () ->
            emailService.sendEmail("user@example.com", "subject", "content")
        );
    }
}
