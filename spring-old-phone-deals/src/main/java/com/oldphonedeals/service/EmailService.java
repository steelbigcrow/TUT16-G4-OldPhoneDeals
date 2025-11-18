package com.oldphonedeals.service;

/**
 * 邮件发送服务接口
 */
public interface EmailService {
    /**
     * 发送邮箱验证邮件
     * 
     * @param toEmail 收件人邮箱
     * @param verifyToken 验证令牌
     * @param userName 用户名
     */
    void sendVerificationEmail(String toEmail, String verifyToken, String userName);

    /**
     * 发送密码重置邮件（使用链接）
     *
     * @param toEmail 收件人邮箱
     * @param resetToken 重置令牌
     * @param userName 用户名
     */
    void sendPasswordResetEmail(String toEmail, String resetToken, String userName);
    
    /**
     * 发送密码重置邮件（使用6位数字重置码）
     *
     * @param toEmail 收件人邮箱
     * @param resetCode 6位数字重置码
     * @param userName 用户名
     */
    void sendPasswordResetCodeEmail(String toEmail, String resetCode, String userName);

    /**
     * 发送通用邮件
     *
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容（HTML）
     */
    void sendEmail(String toEmail, String subject, String content);
}