package com.oldphonedeals.service.impl;

import com.oldphonedeals.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务实现类
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.from}")
    private String fromEmail;
    
    @Value("${frontend.url}")
    private String frontendUrl;
    
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String verifyToken, String userName) {
        log.info("Sending verification email to: {}", toEmail);
        String subject = "Verify Your Email - Old Phone Deals";
        String verifyUrl = frontendUrl + "/verify-email?token=" + verifyToken;
        
        String htmlContent = buildVerificationEmailContent(userName, verifyUrl);
        sendEmail(toEmail, subject, htmlContent);
    }
    
    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken, String userName) {
        log.info("Sending password reset email to: {}", toEmail);
        String subject = "Reset Your Password - Old Phone Deals";
        String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
        
        String htmlContent = buildPasswordResetEmailContent(userName, resetUrl);
        sendEmail(toEmail, subject, htmlContent);
    }
    
    @Override
    @Async
    public void sendPasswordResetCodeEmail(String toEmail, String resetCode, String userName) {
        log.info("Sending password reset code email to: {}", toEmail);
        String subject = "Reset Your Password - Old Phone Deals";
        
        String htmlContent = buildPasswordResetCodeEmailContent(userName, resetCode);
        sendEmail(toEmail, subject, htmlContent);
    }
    
    @Override
    public void sendEmail(String toEmail, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true); // true = HTML
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    // 包访问权限，便于测试
    String buildVerificationEmailContent(String userName, String verifyUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 30px; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #4CAF50; 
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to Old Phone Deals!</h1>
                    </div>
                    <div class="content">
                        <h2>Hi %s,</h2>
                        <p>Thank you for registering with Old Phone Deals. Please verify your email address by clicking the button below:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Verify Email</a>
                        </p>
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; color: #4CAF50;">%s</p>
                        <p>This link will expire in 24 hours.</p>
                        <p>If you didn't create an account, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 Old Phone Deals. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, verifyUrl, verifyUrl);
    }
    
    // 包访问权限，便于测试
    String buildPasswordResetEmailContent(String userName, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #FF9800; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 30px; }
                    .button { display: inline-block; padding: 12px 30px; background-color: #FF9800;
                             color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Password Reset Request</h1>
                    </div>
                    <div class="content">
                        <h2>Hi %s,</h2>
                        <p>We received a request to reset your password. Click the button below to reset it:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Reset Password</a>
                        </p>
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; color: #FF9800;">%s</p>
                        <p>This link will expire in 1 hour.</p>
                        <p>If you didn't request a password reset, please ignore this email or contact support if you have concerns.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 Old Phone Deals. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, resetUrl, resetUrl);
    }
    
    // 包访问权限，便于测试
    String buildPasswordResetCodeEmailContent(String userName, String resetCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #FF9800; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 30px; text-align: center; }
                    .code-box { background-color: #fff; border: 2px solid #FF9800; border-radius: 8px;
                               padding: 20px; margin: 20px 0; font-size: 32px; font-weight: bold;
                               letter-spacing: 8px; color: #FF9800; }
                    .footer { text-align: center; padding: 20px; color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Password Reset Request</h1>
                    </div>
                    <div class="content">
                        <h2>Hi %s,</h2>
                        <p>We received a request to reset your password. Use the code below to reset it:</p>
                        <div class="code-box">%s</div>
                        <p style="color: #666; font-size: 14px;">This code will expire in 1 hour.</p>
                        <p>If you didn't request a password reset, please ignore this email or contact support if you have concerns.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 Old Phone Deals. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, resetCode);
    }
}