package com.oldphonedeals.service;

import com.oldphonedeals.service.impl.EmailServiceImpl;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 邮件服务测试类
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {
    
    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private MimeMessage mimeMessage;
    
    private EmailServiceImpl emailService;
    
    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender);
        // 使用反射设置私有字段
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:4200");
    }
    
    @Test
    void shouldSendVerificationEmail() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String verifyToken = "test-verify-token";
        String userName = "Test User";
        
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        
        // When
        emailService.sendVerificationEmail(toEmail, verifyToken, userName);
        
        // Then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
    
    @Test
    void shouldSendPasswordResetEmail() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String resetToken = "test-reset-token";
        String userName = "Test User";
        
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        
        // When
        emailService.sendPasswordResetEmail(toEmail, resetToken, userName);
        
        // Then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
    
    @Test
    void shouldSendEmailWithHtmlContent() throws MessagingException {
        // Given
        String toEmail = "user@example.com";
        String subject = "Test Subject";
        String htmlContent = "<h1>Test HTML Content</h1>";
        
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        
        // When
        emailService.sendEmail(toEmail, subject, htmlContent);
        
        // Then
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
    
    @Test
    void shouldThrowMailSendExceptionWhenEmailSendingFails() {
        // Given
        String toEmail = "user@example.com";
        String subject = "Test Subject";
        String htmlContent = "<h1>Test HTML Content</h1>";
        
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("SMTP connection failed")).when(mailSender).send(any(MimeMessage.class));
        
        // When & Then
        MailSendException exception = assertThrows(MailSendException.class, () -> {
            emailService.sendEmail(toEmail, subject, htmlContent);
        });
        
        assertEquals("SMTP connection failed", exception.getMessage());
    }
}