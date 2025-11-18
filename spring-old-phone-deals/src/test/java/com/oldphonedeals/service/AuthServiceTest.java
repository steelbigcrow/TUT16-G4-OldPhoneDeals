package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.auth.*;
import com.oldphonedeals.dto.response.auth.*;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.exception.*;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.security.JwtTokenProvider;
import com.oldphonedeals.security.SecurityContextHelper;
import com.oldphonedeals.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试
 * <p>
 * 测试用户认证服务的所有核心功能，包括登录、注册、邮箱验证、密码重置等。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private EmailService emailService;
    
    private AuthService authService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
            userRepository,
            passwordEncoder,
            jwtTokenProvider,
            emailService
        );
        
        // 创建测试用户
        testUser = User.builder()
            .id("user123")
            .firstName("John")
            .lastName("Doe")
            .email("john@example.com")
            .password("$2a$10$encodedPassword")
            .isAdmin(false)
            .isDisabled(false)
            .isVerified(true)
            .build();
    }
    
    // ========== 登录测试 ==========
    
    @Test
    void shouldLoginSuccessfully() {
        // Given
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getPassword(), testUser.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(testUser)).thenReturn("jwt-token");
        
        // When
        LoginResponse response = authService.login(request);
        
        // Then
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertNotNull(response.getUser());
        assertEquals(testUser.getId(), response.getUser().getId());
        assertEquals(testUser.getEmail(), response.getUser().getEmail());
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        LoginRequest request = new LoginRequest("notfound@example.com", "password123");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        
        // When & Then
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.login(request)
        );
        assertEquals("Invalid email or password", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenPasswordIncorrect() {
        // Given
        LoginRequest request = new LoginRequest("john@example.com", "wrongpassword");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getPassword(), testUser.getPassword())).thenReturn(false);
        
        // When & Then
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.login(request)
        );
        assertEquals("Invalid email or password", exception.getMessage());
    }
    
    @Test
    void shouldThrowExceptionWhenEmailNotVerified() {
        // Given
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        testUser.setIsVerified(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getPassword(), testUser.getPassword())).thenReturn(true);
        
        // When & Then
        ForbiddenException exception = assertThrows(
            ForbiddenException.class,
            () -> authService.login(request)
        );
        assertTrue(exception.getMessage().contains("Email not verified"));
    }
    
    @Test
    void shouldThrowExceptionWhenAccountDisabled() {
        // Given
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        testUser.setIsDisabled(true);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getPassword(), testUser.getPassword())).thenReturn(true);
        
        // When & Then
        ForbiddenException exception = assertThrows(
            ForbiddenException.class,
            () -> authService.login(request)
        );
        assertTrue(exception.getMessage().contains("disabled"));
    }
    
    // ========== 注册测试 ==========
    
    @Test
    void shouldRegisterSuccessfully() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
            .firstName("Jane")
            .lastName("Smith")
            .email("jane@example.com")
            .password("Password123!")
            .build();
        
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("newUser123");
            return user;
        });
        
        // When
        AuthUserResponse response = authService.register(request);
        
        // Then
        assertNotNull(response);
        assertEquals("Jane", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        assertEquals("jane@example.com", response.getEmail());
        assertFalse(response.getIsVerified());
        verify(emailService).sendVerificationEmail(
            eq(request.getEmail()),
            anyString(),
            eq(request.getFirstName())
        );
    }
    
    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
            .firstName("Jane")
            .lastName("Smith")
            .email("existing@example.com")
            .password("Password123!")
            .build();
        
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
        
        // When & Then
        DuplicateResourceException exception = assertThrows(
            DuplicateResourceException.class,
            () -> authService.register(request)
        );
        assertTrue(exception.getMessage().contains("email already exists"));
    }
    
    // ========== 邮箱验证测试 ==========
    
    @Test
    void shouldVerifyEmailSuccessfully() {
        // Given
        String token = "verify-token-123";
        VerifyEmailRequest request = VerifyEmailRequest.builder()
            .token(token)
            .email("john@example.com")
            .build();
        testUser.setIsVerified(false);
        testUser.setVerifyToken(token);
        testUser.setEmail("john@example.com");
        
        // AuthServiceImpl使用findByEmail而不是findByVerifyToken
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        authService.verifyEmail(request);
        
        // Then
        verify(userRepository).save(argThat(user ->
            user.getIsVerified() && user.getVerifyToken() == null
        ));
    }
    
    @Test
    void shouldThrowExceptionWhenVerifyTokenInvalid() {
        // Given
        String token = "invalid-token";
        VerifyEmailRequest request = VerifyEmailRequest.builder()
            .token(token)
            .email("john@example.com")
            .build();
        // 模拟找到用户但token不匹配的情况
        testUser.setVerifyToken("different-token");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        
        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> authService.verifyEmail(request)
        );
        assertTrue(exception.getMessage().contains("Invalid or expired"));
    }
    
    // ========== 发送密码重置邮件测试 ==========
    
    @Test
    void shouldSendPasswordResetEmailSuccessfully() {
        // Given
        SendResetPasswordEmailRequest request = SendResetPasswordEmailRequest.builder()
            .email("john@example.com")
            .build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        
        // When
        authService.sendPasswordResetEmail(request);
        
        // Then
        verify(emailService).sendPasswordResetEmail(
            eq(request.getEmail()),
            anyString(),
            eq(testUser.getFirstName())
        );
    }
    
    @Test
    void shouldThrowExceptionWhenEmailNotFoundForReset() {
        // Given
        SendResetPasswordEmailRequest request = SendResetPasswordEmailRequest.builder()
            .email("notfound@example.com")
            .build();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> authService.sendPasswordResetEmail(request)
        );
        assertTrue(exception.getMessage().contains("User not found"));
    }
    
    // ========== 重置密码测试 ==========
    
    @Test
    void shouldResetPasswordWithoutCurrentPassword() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
            .email("john@example.com")
            .newPassword("NewPassword123!")
            .build();
        
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("$2a$10$newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        authService.resetPassword(request);
        
        // Then
        verify(userRepository).save(argThat(user -> 
            "$2a$10$newEncodedPassword".equals(user.getPassword())
        ));
        verify(emailService).sendEmail(
            eq(testUser.getEmail()),
            anyString(),
            anyString()
        );
    }
    
    @Test
    void shouldResetPasswordWithCurrentPassword() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
            .email("john@example.com")
            .currentPassword("oldPassword123")
            .newPassword("NewPassword123!")
            .build();
        
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword123", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("NewPassword123!", testUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("$2a$10$newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // When
        authService.resetPassword(request);
        
        // Then
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void shouldThrowExceptionWhenCurrentPasswordIncorrect() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
            .email("john@example.com")
            .currentPassword("wrongPassword")
            .newPassword("NewPassword123!")
            .build();
        
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);
        
        // When & Then
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.resetPassword(request)
        );
        assertTrue(exception.getMessage().contains("Current password is incorrect"));
    }
    
    @Test
    void shouldThrowExceptionWhenNewPasswordSameAsCurrent() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
            .email("john@example.com")
            .currentPassword("password123")
            .newPassword("password123")
            .build();
        
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", testUser.getPassword())).thenReturn(true);
        
        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> authService.resetPassword(request)
        );
        assertTrue(exception.getMessage().contains("new password cannot be the same"));
    }
    
    @Test
    void shouldThrowExceptionWhenNewPasswordWeak() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
            .email("john@example.com")
            .newPassword("weak")
            .build();
        
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        
        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> authService.resetPassword(request)
        );
        assertTrue(exception.getMessage().contains("at least 8 characters"));
    }
    
    // ========== 获取当前用户测试 ==========
    
    @Test
    void shouldGetCurrentUserSuccessfully() {
        // Given
        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("user123");
            when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
            
            // When
            AuthUserResponse response = authService.getCurrentUser();
            
            // Then
            assertNotNull(response);
            assertEquals(testUser.getId(), response.getId());
            assertEquals(testUser.getEmail(), response.getEmail());
            assertEquals(testUser.getFirstName(), response.getFirstName());
            assertEquals(testUser.getLastName(), response.getLastName());
        }
    }
    
    @Test
    void shouldThrowExceptionWhenUserNotAuthenticated() {
        // Given
        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn(null);
            
            // When & Then
            UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.getCurrentUser()
            );
            assertTrue(exception.getMessage().contains("not authenticated"));
        }
    }
    
    @Test
    void shouldThrowExceptionWhenCurrentUserNotFound() {
        // Given
        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("user123");
            when(userRepository.findById("user123")).thenReturn(Optional.empty());
            
            // When & Then
            ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> authService.getCurrentUser()
            );
            assertTrue(exception.getMessage().contains("User not found"));
        }
    }
}