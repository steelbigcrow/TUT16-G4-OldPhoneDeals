package com.oldphonedeals.controller;

import com.oldphonedeals.dto.request.auth.LoginRequest;
import com.oldphonedeals.dto.request.auth.RegisterRequest;
import com.oldphonedeals.dto.request.auth.VerifyEmailRequest;
import com.oldphonedeals.dto.request.auth.SendResetPasswordEmailRequest;
import com.oldphonedeals.dto.request.auth.VerifyResetCodeRequest;
import com.oldphonedeals.dto.request.auth.ResetPasswordRequest;
import com.oldphonedeals.dto.request.auth.ResendVerificationRequest;
import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.auth.LoginResponse;
import com.oldphonedeals.dto.response.auth.AuthUserResponse;
import com.oldphonedeals.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 用户认证控制器
 * <p>
 * 提供用户认证相关的REST API端点，包括登录、注册、邮箱验证、密码重置等功能。
 * 所有响应都使用统一的 {@link ApiResponse} 格式进行包装。
 * </p>
 * 
 * <h3>API端点列表：</h3>
 * <ul>
 *   <li>POST /api/auth/login - 用户登录</li>
 *   <li>POST /api/auth/register - 用户注册</li>
 *   <li>POST /api/auth/verify-email - 验证邮箱</li>
 *   <li>POST /api/auth/send-reset-password-email - 发送密码重置邮件</li>
 *   <li>POST /api/auth/reset-password - 重置密码</li>
 *   <li>GET /api/auth/me - 获取当前登录用户信息</li>
 * </ul>
 * 
 * @author OldPhoneDeals Team
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 用户登录
     * <p>
     * 验证用户凭证并返回JWT令牌。登录成功后，客户端应将令牌存储在本地，
     * 并在后续请求中通过 Authorization 头部发送：{@code Authorization: Bearer <token>}
     * </p>
     * 
     * @param request 登录请求，包含邮箱和密码
     * @return 登录响应，包含JWT令牌和用户基本信息
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request
    ) {
        log.info("Login request received for email: {}", request.getEmail());
        
        LoginResponse response = authService.login(request);
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "Login successful")
        );
    }
    
    /**
     * 用户注册
     * <p>
     * 创建新用户账户并发送验证邮件。用户需要点击邮件中的验证链接
     * 来激活账户后才能登录。
     * </p>
     * 
     * @param request 注册请求，包含firstName、lastName、email、password
     * @return 用户响应，包含新创建的用户信息
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthUserResponse>> register(
        @Valid @RequestBody RegisterRequest request
    ) {
        log.info("Registration request received for email: {}", request.getEmail());
        
        AuthUserResponse response = authService.register(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(
                response,
                "Registration successful! Please check your email to verify your account"
            )
        );
    }
    
    /**
     * 验证邮箱
     * <p>
     * 通过验证令牌激活用户账户。客户端应该从邮件中的链接提取令牌参数，
     * 然后调用此API来完成邮箱验证。
     * </p>
     *
     * @param request 验证请求，包含验证令牌和邮箱
     * @return 消息响应，确认验证成功
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
        @Valid @RequestBody VerifyEmailRequest request
    ) {
        log.info("Email verification request received for: {}", request.getEmail());
        
        authService.verifyEmail(request);
        
        return ResponseEntity.ok(
            ApiResponse.success(null, "Email verified successfully! You can now log in")
        );
    }
    
    /**
     * 请求密码重置
     * <p>
     * 向指定邮箱发送包含6位数字重置码的邮件。
     * 出于安全考虑，无论邮箱是否存在，都返回成功消息。
     * </p>
     *
     * @param request 密码重置请求，包含邮箱地址
     * @return 消息响应，确认邮件已发送
     */
    @PostMapping("/request-password-reset")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
        @Valid @RequestBody SendResetPasswordEmailRequest request
    ) {
        log.info("Password reset request for: {}", request.getEmail());
        
        authService.requestPasswordReset(request.getEmail());
        
        return ResponseEntity.ok(
            ApiResponse.success(null, "If the email exists, a password reset code has been sent")
        );
    }
    
    /**
     * 验证密码重置码
     * <p>
     * 验证用户提供的6位数字重置码是否有效且未过期。
     * </p>
     *
     * @param request 验证请求，包含邮箱和重置码
     * @return 验证结果
     */
    @PostMapping("/verify-reset-code")
    public ResponseEntity<ApiResponse<Boolean>> verifyResetCode(
        @Valid @RequestBody VerifyResetCodeRequest request
    ) {
        log.info("Verifying reset code for: {}", request.getEmail());
        
        boolean isValid = authService.verifyResetCode(request.getEmail(), request.getCode());
        
        return ResponseEntity.ok(
            ApiResponse.success(isValid, isValid ? "Reset code is valid" : "Invalid or expired reset code")
        );
    }
    
    /**
     * 重置密码
     * <p>
     * 使用重置码重置用户密码。支持两种场景：
     * <ol>
     *   <li>忘记密码：提供邮箱、重置码和新密码</li>
     *   <li>修改密码：提供邮箱、当前密码和新密码（不需要重置码）</li>
     * </ol>
     * </p>
     *
     * @param request 密码重置请求
     * @return 消息响应，确认密码已重置
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        log.info("Password reset request for: {}", request.getEmail());
        
        // 如果提供了重置码，使用重置码重置密码
        if (request.getCode() != null && !request.getCode().isEmpty()) {
            authService.resetPasswordWithCode(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword()
            );
        } else {
            // 否则使用当前密码验证方式（需要登录）
            authService.resetPassword(request);
        }
        
        return ResponseEntity.ok(
            ApiResponse.success(null, "Password reset successfully!")
        );
    }
    
    /**
     * 重新发送验证邮件
     * <p>
     * 为未验证的用户重新发送邮箱验证邮件。
     * </p>
     *
     * @param request 重新发送验证邮件请求，包含邮箱地址
     * @return 消息响应，确认邮件已发送
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
        @Valid @RequestBody ResendVerificationRequest request
    ) {
        log.info("Resend verification email request for: {}", request.getEmail());
        
        authService.resendVerification(request.getEmail());
        
        return ResponseEntity.ok(
            ApiResponse.success(null, "Verification email has been resent! Please check your email")
        );
    }
    
    /**
     * 获取当前登录用户信息
     * <p>
     * 从JWT令牌中获取当前已认证用户的详细信息。
     * 此端点需要在请求头中包含有效的JWT令牌。
     * </p>
     * 
     * @return 用户响应，包含当前用户的详细信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserResponse>> getCurrentUser() {
        log.info("Get current user request received");
        
        AuthUserResponse response = authService.getCurrentUser();
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "User information retrieved successfully")
        );
    }
}