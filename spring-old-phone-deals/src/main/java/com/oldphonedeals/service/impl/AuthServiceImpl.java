package com.oldphonedeals.service.impl;

import com.oldphonedeals.dto.request.auth.*;
import com.oldphonedeals.dto.response.auth.*;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.exception.*;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.security.JwtTokenProvider;
import com.oldphonedeals.security.SecurityContextHelper;
import com.oldphonedeals.service.AuthService;
import com.oldphonedeals.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 用户认证服务实现类
 * <p>
 * 实现用户认证相关的核心业务逻辑，包括登录、注册、邮箱验证和密码重置等功能。
 * 该实现参考了Express.js后端的用户控制器逻辑，保持功能一致性。
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    
    /**
     * 密码强度正则表达式
     * 要求：至少8个字符，包含大小写字母、数字和特殊字符
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,}$"
    );
    
    /**
     * 用户登录
     * <p>
     * 登录流程：
     * 1. 检查用户是否存在
     * 2. 验证密码是否正确
     * 3. 检查邮箱是否已验证
     * 4. 检查账户是否被禁用
     * 5. 生成JWT令牌
     * 6. 更新最后登录时间
     * </p>
     * 
     * @param request 登录请求
     * @return 登录响应，包含JWT令牌和用户信息
     * @throws UnauthorizedException 如果邮箱或密码错误
     * @throws ForbiddenException 如果邮箱未验证或账户被禁用
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getEmail());
        
        // 1. 查找用户
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        
        // 2. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }
        
        // 3. 检查邮箱是否已验证
        if (!user.getIsVerified()) {
            log.warn("Email not verified for user: {}", request.getEmail());
            throw new ForbiddenException("Email not verified. Please check your email to verify your account");
        }
        
        // 4. 检查账户是否被禁用
        if (user.getIsDisabled()) {
            log.warn("Disabled account login attempt: {}", request.getEmail());
            throw new ForbiddenException("Account has been disabled. Please contact support");
        }
        
        // 5. 生成JWT令牌
        String token = jwtTokenProvider.generateToken(user);
        
        // 6. 更新最后登录时间
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("User logged in successfully: {}", user.getEmail());
        
        // 7. 构建并返回响应
        return LoginResponse.builder()
            .token(token)
            .user(buildLoginUserInfo(user))
            .build();
    }
    
    /**
     * 用户注册
     * <p>
     * 注册流程：
     * 1. 检查邮箱是否已被注册
     * 2. 生成唯一的验证令牌
     * 3. 创建新用户（密码自动加密）
     * 4. 发送验证邮件
     * </p>
     * 
     * @param request 注册请求
     * @return 用户响应
     * @throws DuplicateResourceException 如果邮箱已被注册
     */
    @Override
    @Transactional
    public AuthUserResponse register(RegisterRequest request) {
        log.info("User registration attempt: {}", request.getEmail());
        
        // 1. 检查邮箱是否已被注册
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new DuplicateResourceException("Account with that email already exists");
        }
        
        // 2. 生成唯一的验证令牌
        String verifyToken = UUID.randomUUID().toString();
        
        // 3. 创建新用户
        User user = User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .isAdmin(false)
            .isDisabled(false)
            .isBan(false)
            .isVerified(false)
            .verifyToken(verifyToken)
            .createdAt(LocalDateTime.now())
            .build();
        
        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());
        
        // 4. 发送验证邮件（异步）
        try {
            emailService.sendVerificationEmail(
                user.getEmail(),
                verifyToken,
                user.getFirstName()
            );
            log.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
            // 不抛出异常，因为用户已创建成功，只是邮件发送失败
        }
        
        return buildAuthUserResponse(user);
    }
    
    /**
     * 验证邮箱
     * <p>
     * 通过验证令牌和邮箱激活用户账户。
     * </p>
     *
     * @param request 验证请求，包含token和email
     * @throws BadRequestException 如果令牌无效或邮箱不匹配
     */
    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        log.info("Email verification attempt for: {}", request.getEmail());
        
        // 根据email和token查找用户
        User user = userRepository.findByEmail(request.getEmail().toLowerCase()).orElse(null);
        
        if (user == null || user.getVerifyToken() == null || !user.getVerifyToken().equals(request.getToken())) {
            log.warn("Email verification failed - invalid token or email: {}", request.getEmail());
            throw new BadRequestException("Invalid or expired verification token");
        }
        
        // 设置为已验证
        user.setIsVerified(true);
        user.setVerifyToken(null);
        userRepository.save(user);
        
        log.info("Email verified successfully for user: {}", user.getEmail());
    }
    
    /**
     * 发送密码重置邮件
     * <p>
     * 检查邮箱是否存在，如果存在则发送密码重置邮件。
     * </p>
     * 
     * @param request 密码重置邮件请求
     * @throws ResourceNotFoundException 如果邮箱不存在
     */
    @Override
    public void sendPasswordResetEmail(SendResetPasswordEmailRequest request) {
        log.info("Password reset email request for: {}", request.getEmail());
        
        // 查找用户
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));
        
        // 生成重置token（这里简化处理，实际应该存储token和过期时间）
        String resetToken = UUID.randomUUID().toString();
        
        // 发送密码重置邮件
        try {
            emailService.sendPasswordResetEmail(
                user.getEmail(),
                resetToken,
                user.getFirstName()
            );
            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    /**
     * 重置密码
     * <p>
     * 重置用户密码的流程：
     * 1. 查找用户
     * 2. 如果提供了当前密码，则验证当前密码
     * 3. 验证新密码强度
     * 4. 更新密码
     * 5. 发送邮件通知
     * </p>
     * 
     * @param request 密码重置请求
     * @throws ResourceNotFoundException 如果用户不存在
     * @throws UnauthorizedException 如果当前密码错误
     * @throws BadRequestException 如果新密码不符合要求
     */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Password reset attempt for: {}", request.getEmail());
        
        // 1. 查找用户
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // 2. 如果提供了当前密码，则验证
        if (request.getCurrentPassword() != null && !request.getCurrentPassword().isEmpty()) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                log.warn("Current password incorrect for user: {}", request.getEmail());
                throw new UnauthorizedException("Current password is incorrect");
            }
            
            // 检查新密码是否与当前密码相同
            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                throw new BadRequestException("The new password cannot be the same as the current password");
            }
        }
        
        // 3. 验证新密码强度
        if (!PASSWORD_PATTERN.matcher(request.getNewPassword()).matches()) {
            throw new BadRequestException(
                "Password must be at least 8 characters and include uppercase, lowercase, number, and special character"
            );
        }
        
        // 4. 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Password reset successfully for user: {}", user.getEmail());
        
        // 5. 发送邮件通知（异步）
        try {
            String subject = "Password Changed Successfully - Old Phone Deals";
            String content = String.format(
                "<p>Hi %s,</p>" +
                "<p>We wanted to let you know that your password has been successfully changed.</p>" +
                "<p>If you did not make this change, please contact our support team immediately to secure your account.</p>" +
                "<p>Thanks,<br/>The Old Phone Deals Team</p>",
                user.getFirstName()
            );
            emailService.sendEmail(user.getEmail(), subject, content);
            log.info("Password change notification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password change notification to: {}", user.getEmail(), e);
            // 不抛出异常，因为密码已成功重置
        }
    }
    
    /**
     * 获取当前登录用户信息
     * <p>
     * 从安全上下文中获取当前已认证用户的详细信息。
     * </p>
     * 
     * @return 用户响应
     * @throws UnauthorizedException 如果用户未登录
     * @throws ResourceNotFoundException 如果用户不存在
     */
    @Override
    public AuthUserResponse getCurrentUser() {
        // 从安全上下文获取当前用户ID
        String userId = SecurityContextHelper.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("User not authenticated");
        }
        
        log.info("Getting current user info for ID: {}", userId);
        
        // 查找用户
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return buildAuthUserResponse(user);
    }
    
    /**
     * 请求密码重置（生成6位数字重置码）
     * <p>
     * 为用户生成6位数字重置码，设置1小时有效期，并发送包含重置码的邮件。
     * 即使用户不存在也返回成功，以防止邮箱枚举攻击。
     * </p>
     *
     * @param email 用户邮箱（不区分大小写）
     */
    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("Password reset request for: {}", email);
        
        // 查找用户（不区分大小写）
        User user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
        
        // 即使用户不存在也返回成功，防止邮箱枚举攻击
        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", email);
            // 不抛出异常，让调用者以为发送成功
            return;
        }
        
        // 生成6位数字重置码
        Random random = new Random();
        String resetCode = String.format("%06d", random.nextInt(1000000));
        
        // 设置1小时后过期
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        
        // 保存重置码和过期时间
        user.setPasswordResetCode(resetCode);
        user.setPasswordResetExpires(expiresAt);
        userRepository.save(user);
        
        log.info("Password reset code generated for user: {}", user.getEmail());
        
        // 发送包含重置码的邮件
        try {
            emailService.sendPasswordResetCodeEmail(
                user.getEmail(),
                resetCode,
                user.getFirstName()
            );
            log.info("Password reset code email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset code email to: {}", user.getEmail(), e);
            // 不抛出异常，因为重置码已保存，用户可以稍后重试
        }
    }
    
    /**
     * 验证密码重置码
     * <p>
     * 验证用户提供的6位数字重置码是否有效且未过期。
     * </p>
     *
     * @param email 用户邮箱
     * @param code 6位数字重置码
     * @return 如果重置码有效返回true，否则返回false
     */
    @Override
    public boolean verifyResetCode(String email, String code) {
        log.info("Verifying reset code for email: {}", email);
        
        // 查找用户（不区分大小写）
        User user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
        if (user == null) {
            log.warn("Reset code verification failed - user not found: {}", email);
            return false;
        }
        
        // 检查重置码是否匹配
        if (user.getPasswordResetCode() == null || !user.getPasswordResetCode().equals(code)) {
            log.warn("Reset code verification failed - code mismatch for user: {}", email);
            return false;
        }
        
        // 检查是否过期
        if (user.getPasswordResetExpires() == null || LocalDateTime.now().isAfter(user.getPasswordResetExpires())) {
            log.warn("Reset code verification failed - code expired for user: {}", email);
            return false;
        }
        
        log.info("Reset code verified successfully for user: {}", email);
        return true;
    }
    
    /**
     * 使用重置码重置密码
     * <p>
     * 验证重置码后，重置用户密码并清除重置码。
     * </p>
     *
     * @param email 用户邮箱
     * @param code 6位数字重置码
     * @param newPassword 新密码
     * @throws BadRequestException 如果重置码无效或已过期
     */
    @Override
    @Transactional
    public void resetPasswordWithCode(String email, String code, String newPassword) {
        log.info("Resetting password with code for: {}", email);
        
        // 验证重置码
        if (!verifyResetCode(email, code)) {
            throw new BadRequestException("Invalid or expired reset code");
        }
        
        // 查找用户
        User user = userRepository.findByEmail(email.toLowerCase())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // 验证新密码强度
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BadRequestException(
                "Password must be at least 8 characters and include uppercase, lowercase, number, and special character"
            );
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        
        // 清除重置码和过期时间
        user.setPasswordResetCode(null);
        user.setPasswordResetExpires(null);
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        log.info("Password reset successfully with code for user: {}", user.getEmail());
        
        // 发送密码更改通知邮件
        try {
            String subject = "Password Changed Successfully - Old Phone Deals";
            String content = String.format(
                "<p>Hi %s,</p>" +
                "<p>We wanted to let you know that your password has been successfully changed.</p>" +
                "<p>If you did not make this change, please contact our support team immediately to secure your account.</p>" +
                "<p>Thanks,<br/>The Old Phone Deals Team</p>",
                user.getFirstName()
            );
            emailService.sendEmail(user.getEmail(), subject, content);
            log.info("Password change notification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password change notification to: {}", user.getEmail(), e);
            // 不抛出异常，因为密码已成功重置
        }
    }
    
    /**
     * 重新发送验证邮件
     * <p>
     * 为未验证的用户重新生成验证token并发送验证邮件。
     * </p>
     *
     * @param email 用户邮箱
     * @throws ResourceNotFoundException 如果用户不存在
     * @throws BadRequestException 如果用户已验证
     */
    @Override
    @Transactional
    public void resendVerification(String email) {
        log.info("Resending verification email to: {}", email);
        
        // 查找用户（不区分大小写）
        User user = userRepository.findByEmail(email.toLowerCase())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        // 检查是否已验证
        if (user.getIsVerified()) {
            log.warn("Verification email resend failed - user already verified: {}", email);
            throw new BadRequestException("Email is already verified");
        }
        
        // 生成新的验证token
        String verifyToken = UUID.randomUUID().toString();
        user.setVerifyToken(verifyToken);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("New verification token generated for user: {}", user.getEmail());
        
        // 发送验证邮件
        try {
            emailService.sendVerificationEmail(
                user.getEmail(),
                verifyToken,
                user.getFirstName()
            );
            log.info("Verification email resent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to resend verification email to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to resend verification email", e);
        }
    }
    
    /**
     * 构建登录响应的用户信息对象
     *
     * @param user 用户实体
     * @return 登录用户信息
     */
    private LoginResponse.UserInfo buildLoginUserInfo(User user) {
        return LoginResponse.UserInfo.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .build();
    }
    
    /**
     * 构建用户响应对象
     *
     * @param user 用户实体
     * @return 用户响应DTO
     */
    private AuthUserResponse buildAuthUserResponse(User user) {
        return AuthUserResponse.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .isAdmin(user.getIsAdmin())
            .isDisabled(user.getIsDisabled())
            .isVerified(user.getIsVerified())
            .build();
    }
}