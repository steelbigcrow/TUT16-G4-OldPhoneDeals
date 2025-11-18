package com.oldphonedeals.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldphonedeals.config.ControllerTestConfig;
import com.oldphonedeals.config.CorsConfig;
import com.oldphonedeals.config.FileStorageProperties;
import com.oldphonedeals.dto.request.profile.ChangePasswordRequest;
import com.oldphonedeals.dto.request.profile.UpdateProfileRequest;
import com.oldphonedeals.dto.response.user.UserProfileResponse;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.security.CustomUserDetailsService;
import com.oldphonedeals.security.JwtTokenProvider;
import com.oldphonedeals.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProfileController集成测试
 * <p>
 * 测试用户资料管理相关的REST API端点，包括：
 * - 获取用户资料
 * - 更新用户资料
 * - 修改密码
 * </p>
 * 
 * 使用@WebMvcTest进行Controller层集成测试
 */
@WebMvcTest(value = ProfileController.class,
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
@DisplayName("ProfileController集成测试")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private FileStorageProperties fileStorageProperties;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private String testUserId;
    private UserProfileResponse userProfileResponse;
    private UpdateProfileRequest updateProfileRequest;
    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    void setUp() {
        // 准备测试数据 - 用户ID
        testUserId = "user123";

        // 准备测试数据 - 用户资料响应
        userProfileResponse = UserProfileResponse.builder()
                .id(testUserId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .emailVerified(true)
                .createdAt(LocalDateTime.now().minusMonths(3))
                .updatedAt(LocalDateTime.now())
                .build();

        // 准备测试数据 - 更新资料请求
        updateProfileRequest = UpdateProfileRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .build();

        // 准备测试数据 - 修改密码请求
        changePasswordRequest = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123")
                .newPassword("NewPassword456")
                .build();
    }

    // ==================== 获取用户资料端点测试 ====================

    @Test
    @DisplayName("应该成功获取用户资料 - 当用户存在时")
    void shouldReturnUserProfile_whenUserExists() throws Exception {
        // Arrange
        when(profileService.getUserProfile(testUserId)).thenReturn(userProfileResponse);

        // Act & Assert
        mockMvc.perform(get("/api/profile/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User profile retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(testUserId))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.data.emailVerified").value(true));

        verify(profileService, times(1)).getUserProfile(testUserId);
    }

    @Test
    @DisplayName("应该返回404错误 - 当用户不存在时")
    void shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        // Arrange
        when(profileService.getUserProfile(anyString()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(get("/api/profile/{userId}", "nonexistent"))
                .andExpect(status().isNotFound());

        verify(profileService, times(1)).getUserProfile("nonexistent");
    }

    @Test
    @DisplayName("应该返回完整的用户资料信息 - 包括创建和更新时间")
    void shouldReturnCompleteProfileInfo_whenGettingProfile() throws Exception {
        // Arrange
        when(profileService.getUserProfile(testUserId)).thenReturn(userProfileResponse);

        // Act & Assert
        mockMvc.perform(get("/api/profile/{userId}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andExpect(jsonPath("$.data.emailVerified").exists());

        verify(profileService, times(1)).getUserProfile(testUserId);
    }

    // ==================== 更新用户资料端点测试 ====================

    @Test
    @DisplayName("应该成功更新用户资料 - 当请求有效时")
    void shouldUpdateUserProfile_whenValidRequest() throws Exception {
        // Arrange
        UserProfileResponse updatedProfile = UserProfileResponse.builder()
                .id(testUserId)
                .firstName("Jane")
                .lastName("Smith")
                .email("john.doe@example.com")
                .emailVerified(true)
                .createdAt(LocalDateTime.now().minusMonths(3))
                .updatedAt(LocalDateTime.now())
                .build();

        when(profileService.updateUserProfile(eq(testUserId), any(UpdateProfileRequest.class)))
                .thenReturn(updatedProfile);

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data.id").value(testUserId))
                .andExpect(jsonPath("$.data.firstName").value("Jane"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"));

        verify(profileService, times(1)).updateUserProfile(eq(testUserId), any(UpdateProfileRequest.class));
    }

    @Test
    @DisplayName("应该返回400错误 - 当firstName为空时")
    void shouldReturnBadRequest_whenFirstNameIsBlank() throws Exception {
        // Arrange
        UpdateProfileRequest invalidRequest = UpdateProfileRequest.builder()
                .firstName("")
                .lastName("Smith")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).updateUserProfile(anyString(), any(UpdateProfileRequest.class));
    }

    @Test
    @DisplayName("应该返回400错误 - 当lastName为空时")
    void shouldReturnBadRequest_whenLastNameIsBlank() throws Exception {
        // Arrange
        UpdateProfileRequest invalidRequest = UpdateProfileRequest.builder()
                .firstName("Jane")
                .lastName("")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).updateUserProfile(anyString(), any(UpdateProfileRequest.class));
    }

    @Test
    @DisplayName("应该返回400错误 - 当firstName太短时")
    void shouldReturnBadRequest_whenFirstNameTooShort() throws Exception {
        // Arrange
        UpdateProfileRequest invalidRequest = UpdateProfileRequest.builder()
                .firstName("J")
                .lastName("Smith")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).updateUserProfile(anyString(), any(UpdateProfileRequest.class));
    }

    @Test
    @DisplayName("应该返回400错误 - 当lastName太短时")
    void shouldReturnBadRequest_whenLastNameTooShort() throws Exception {
        // Arrange
        UpdateProfileRequest invalidRequest = UpdateProfileRequest.builder()
                .firstName("Jane")
                .lastName("S")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).updateUserProfile(anyString(), any(UpdateProfileRequest.class));
    }

    @Test
    @DisplayName("应该返回404错误 - 当更新不存在的用户资料时")
    void shouldReturnNotFound_whenUpdatingNonexistentUser() throws Exception {
        // Arrange
        when(profileService.updateUserProfile(anyString(), any(UpdateProfileRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isNotFound());

        verify(profileService, times(1)).updateUserProfile(eq("nonexistent"), any(UpdateProfileRequest.class));
    }

    @Test
    @DisplayName("应该返回401错误 - 当尝试更新其他用户的资料时")
    void shouldReturnUnauthorized_whenUpdatingOtherUserProfile() throws Exception {
        // Arrange
        when(profileService.updateUserProfile(anyString(), any(UpdateProfileRequest.class)))
                .thenThrow(new UnauthorizedException("You can only update your own profile"));

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}", "other-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isUnauthorized());

        verify(profileService, times(1)).updateUserProfile(eq("other-user"), any(UpdateProfileRequest.class));
    }

    // ==================== 修改密码端点测试 ====================

    @Test
    @DisplayName("应该成功修改密码 - 当请求有效且当前密码正确时")
    void shouldChangePassword_whenValidRequestAndCorrectCurrentPassword() throws Exception {
        // Arrange
        doNothing().when(profileService).changePassword(eq(testUserId), any(ChangePasswordRequest.class));

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}/change-password", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(profileService, times(1)).changePassword(eq(testUserId), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("应该返回400错误 - 当当前密码为空时")
    void shouldReturnBadRequest_whenCurrentPasswordIsBlank() throws Exception {
        // Arrange
        ChangePasswordRequest invalidRequest = ChangePasswordRequest.builder()
                .currentPassword("")
                .newPassword("NewPassword456")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}/change-password", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).changePassword(anyString(), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("应该返回400错误 - 当新密码为空时")
    void shouldReturnBadRequest_whenNewPasswordIsBlank() throws Exception {
        // Arrange
        ChangePasswordRequest invalidRequest = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123")
                .newPassword("")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}/change-password", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).changePassword(anyString(), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("应该返回400错误 - 当新密码太短时")
    void shouldReturnBadRequest_whenNewPasswordTooShort() throws Exception {
        // Arrange
        ChangePasswordRequest invalidRequest = ChangePasswordRequest.builder()
                .currentPassword("OldPassword123")
                .newPassword("12345")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}/change-password", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(profileService, never()).changePassword(anyString(), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("应该返回400错误 - 当当前密码错误时")
    void shouldReturnBadRequest_whenCurrentPasswordIncorrect() throws Exception {
        // Arrange
        doThrow(new BadRequestException("Current password is incorrect"))
                .when(profileService).changePassword(eq(testUserId), any(ChangePasswordRequest.class));

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}/change-password", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isBadRequest());

        verify(profileService, times(1)).changePassword(eq(testUserId), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("应该返回404错误 - 当修改不存在用户的密码时")
    void shouldReturnNotFound_whenChangingPasswordForNonexistentUser() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("User not found"))
                .when(profileService).changePassword(anyString(), any(ChangePasswordRequest.class));

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}/change-password", "nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isNotFound());

        verify(profileService, times(1)).changePassword(eq("nonexistent"), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("应该返回401错误 - 当尝试修改其他用户的密码时")
    void shouldReturnUnauthorized_whenChangingOtherUserPassword() throws Exception {
        // Arrange
        doThrow(new UnauthorizedException("You can only change your own password"))
                .when(profileService).changePassword(anyString(), any(ChangePasswordRequest.class));

        // Act & Assert
        mockMvc.perform(put("/api/profile/{userId}/change-password", "other-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isUnauthorized());

        verify(profileService, times(1)).changePassword(eq("other-user"), any(ChangePasswordRequest.class));
    }
}