package com.oldphonedeals.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldphonedeals.config.ControllerTestConfig;
import com.oldphonedeals.dto.request.auth.*;
import com.oldphonedeals.dto.response.auth.AuthUserResponse;
import com.oldphonedeals.dto.response.auth.LoginResponse;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.DuplicateResourceException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.config.FileStorageProperties;
import com.oldphonedeals.security.CustomUserDetailsService;
import com.oldphonedeals.security.JwtTokenProvider;
import com.oldphonedeals.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import com.oldphonedeals.config.CorsConfig;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController集成测试
 * 
 * 测试用户认证相关的REST API端点，包括：
 * - 登录/注册
 * - 邮箱验证
 * - 密码重置
 * - 当前用户信息获取
 * 
 * 使用@WebMvcTest进行Controller层集成测试
 */
@WebMvcTest(value = AuthController.class,
    excludeAutoConfiguration = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        MongoRepositoriesAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = CorsConfig.class
    ))
@Import(ControllerTestConfig.class)
@AutoConfigureMockMvc(addFilters = false) // 禁用Security过滤器以简化测试
@DisplayName("AuthController集成测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private FileStorageProperties fileStorageProperties;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private LoginResponse loginResponse;
    private AuthUserResponse authUserResponse;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        registerRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("test@example.com")
                .password("password123")
                .build();

        loginResponse = LoginResponse.builder()
                .token("jwt-token-12345")
                .user(LoginResponse.UserInfo.builder()
                        .id("user123")
                        .firstName("John")
                        .lastName("Doe")
                        .email("test@example.com")
                        .build())
                .build();

        authUserResponse = AuthUserResponse.builder()
                .id("user123")
                .firstName("John")
                .lastName("Doe")
                .email("test@example.com")
                .isAdmin(false)
                .isDisabled(false)
                .isBan(false)
                .isVerified(true)
                .build();
    }

    // ==================== 登录端点测试 ====================

    @Test
    @DisplayName("testLogin_ValidCredentials_ReturnsOkWithToken")
    void testLogin_ValidCredentials_ReturnsOkWithToken() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").value("jwt-token-12345"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("testLogin_InvalidEmail_ReturnsBadRequest")
    void testLogin_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        LoginRequest invalidRequest = LoginRequest.builder()
                .email("invalid-email")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("testLogin_MissingPassword_ReturnsBadRequest")
    void testLogin_MissingPassword_ReturnsBadRequest() throws Exception {
        // Arrange
        LoginRequest invalidRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("testLogin_WrongCredentials_ReturnsUnauthorized")
    void testLogin_WrongCredentials_ReturnsUnauthorized() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    // ==================== 注册端点测试 ====================

    @Test
    @DisplayName("testRegister_ValidRequest_ReturnsCreated")
    void testRegister_ValidRequest_ReturnsCreated() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class))).thenReturn(authUserResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful! Please check your email to verify your account"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("John"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("testRegister_DuplicateEmail_ReturnsConflict")
    void testRegister_DuplicateEmail_ReturnsConflict() throws Exception {
        // Arrange
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("testRegister_InvalidEmail_ReturnsBadRequest")
    void testRegister_InvalidEmail_ReturnsBadRequest() throws Exception {
        // Arrange
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("invalid-email")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("testRegister_PasswordTooShort_ReturnsBadRequest")
    void testRegister_PasswordTooShort_ReturnsBadRequest() throws Exception {
        // Arrange
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("test@example.com")
                .password("123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("testRegister_MissingFirstName_ReturnsBadRequest")
    void testRegister_MissingFirstName_ReturnsBadRequest() throws Exception {
        // Arrange
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .firstName("")
                .lastName("Doe")
                .email("test@example.com")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    // ==================== 邮箱验证端点测试 ====================

    @Test
    @DisplayName("testVerifyEmail_ValidToken_ReturnsOk")
    void testVerifyEmail_ValidToken_ReturnsOk() throws Exception {
        // Arrange
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .email("test@example.com")
                .token("valid-token-123")
                .build();

        doNothing().when(authService).verifyEmail(any(VerifyEmailRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully! You can now log in"));

        verify(authService, times(1)).verifyEmail(any(VerifyEmailRequest.class));
    }

    @Test
    @DisplayName("testVerifyEmail_InvalidToken_ReturnsBadRequest")
    void testVerifyEmail_InvalidToken_ReturnsBadRequest() throws Exception {
        // Arrange
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .email("test@example.com")
                .token("invalid-token")
                .build();

        doThrow(new BadRequestException("Invalid verification token"))
                .when(authService).verifyEmail(any(VerifyEmailRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, times(1)).verifyEmail(any(VerifyEmailRequest.class));
    }

    // ==================== 密码重置端点测试 ====================

    @Test
    @DisplayName("testRequestPasswordReset_ValidEmail_ReturnsOk")
    void testRequestPasswordReset_ValidEmail_ReturnsOk() throws Exception {
        // Arrange
        SendResetPasswordEmailRequest request = SendResetPasswordEmailRequest.builder()
                .email("test@example.com")
                .build();

        doNothing().when(authService).requestPasswordReset(anyString());

        // Act & Assert
        mockMvc.perform(post("/api/auth/request-password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If the email exists, a password reset code has been sent"));

        verify(authService, times(1)).requestPasswordReset(anyString());
    }

    @Test
    @DisplayName("testVerifyResetCode_ValidCode_ReturnsTrue")
    void testVerifyResetCode_ValidCode_ReturnsTrue() throws Exception {
        // Arrange
        VerifyResetCodeRequest request = VerifyResetCodeRequest.builder()
                .email("test@example.com")
                .code("123456")
                .build();

        when(authService.verifyResetCode(anyString(), anyString())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true))
                .andExpect(jsonPath("$.message").value("Reset code is valid"));

        verify(authService, times(1)).verifyResetCode(anyString(), anyString());
    }

    @Test
    @DisplayName("testVerifyResetCode_InvalidCode_ReturnsFalse")
    void testVerifyResetCode_InvalidCode_ReturnsFalse() throws Exception {
        // Arrange
        VerifyResetCodeRequest request = VerifyResetCodeRequest.builder()
                .email("test@example.com")
                .code("999999")
                .build();

        when(authService.verifyResetCode(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired reset code"));

        verify(authService, times(1)).verifyResetCode(anyString(), anyString());
    }

    @Test
    @DisplayName("testResetPassword_WithResetCode_ReturnsOk")
    void testResetPassword_WithResetCode_ReturnsOk() throws Exception {
        // Arrange
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("test@example.com")
                .code("123456")
                .newPassword("NewPassword123!")
                .build();

        doNothing().when(authService).resetPasswordWithCode(anyString(), anyString(), anyString());

        // Act & Assert
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successfully!"));

        verify(authService, times(1)).resetPasswordWithCode(anyString(), anyString(), anyString());
        verify(authService, never()).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    @DisplayName("testResetPassword_WithoutResetCode_UsesAlternativeMethod")
    void testResetPassword_WithoutResetCode_UsesAlternativeMethod() throws Exception {
        // Arrange
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("test@example.com")
                .currentPassword("oldPassword")
                .newPassword("NewPassword123!")
                .build();

        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successfully!"));

        verify(authService, times(1)).resetPassword(any(ResetPasswordRequest.class));
        verify(authService, never()).resetPasswordWithCode(anyString(), anyString(), anyString());
    }

    // ==================== 重新发送验证邮件端点测试 ====================

    @Test
    @DisplayName("testResendVerification_ValidEmail_ReturnsOk")
    void testResendVerification_ValidEmail_ReturnsOk() throws Exception {
        // Arrange
        ResendVerificationRequest request = ResendVerificationRequest.builder()
                .email("test@example.com")
                .build();

        doNothing().when(authService).resendVerification(anyString());

        // Act & Assert
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verification email has been resent! Please check your email"));

        verify(authService, times(1)).resendVerification(anyString());
    }

    @Test
    @DisplayName("testResendVerification_UserNotFound_ReturnsNotFound")
    void testResendVerification_UserNotFound_ReturnsNotFound() throws Exception {
        // Arrange
        ResendVerificationRequest request = ResendVerificationRequest.builder()
                .email("nonexistent@example.com")
                .build();

        doThrow(new ResourceNotFoundException("User not found"))
                .when(authService).resendVerification(anyString());

        // Act & Assert
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(authService, times(1)).resendVerification(anyString());
    }

    // ==================== 获取当前用户端点测试 ====================

    @Test
    @DisplayName("testGetCurrentUser_Authenticated_ReturnsUserInfo")
    void testGetCurrentUser_Authenticated_ReturnsUserInfo() throws Exception {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(authUserResponse);

        // Act & Assert
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User information retrieved successfully"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.isVerified").value(true));

        verify(authService, times(1)).getCurrentUser();
    }
}