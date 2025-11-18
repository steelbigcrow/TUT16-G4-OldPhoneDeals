package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.profile.ChangePasswordRequest;
import com.oldphonedeals.dto.request.profile.UpdateProfileRequest;
import com.oldphonedeals.dto.response.user.UserProfileResponse;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.security.SecurityContextHelper;
import com.oldphonedeals.service.ProfileService;
import com.oldphonedeals.service.impl.ProfileServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProfileService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ProfileService profileService;

    private User user;

    @BeforeEach
    void setUp() {
        profileService = new ProfileServiceImpl(userRepository, passwordEncoder);

        user = User.builder()
            .id("user-1")
            .email("test@example.com")
            .firstName("John")
            .lastName("Doe")
            .isVerified(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .password("encoded")
            .build();
    }

    @Test
    void getUserProfile_shouldReturnProfile_whenUserExists() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        UserProfileResponse response = profileService.getUserProfile("user-1");

        assertNotNull(response);
        assertEquals("user-1", response.getId());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void getUserProfile_shouldThrow_whenUserNotFound() {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> profileService.getUserProfile("user-1"));
    }

    @Test
    void updateUserProfile_shouldUpdateNames_whenAuthorized() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("user-1");

            UserProfileResponse response = profileService.updateUserProfile("user-1", request);

            assertEquals("Jane", response.getFirstName());
            assertEquals("Smith", response.getLastName());
            verify(userRepository).save(user);
        }
    }

    @Test
    void updateUserProfile_shouldThrowUnauthorized_whenUserMismatch() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");

        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("another-user");

            UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> profileService.updateUserProfile("user-1", request));
            assertTrue(ex.getMessage().contains("own profile"));
            verifyNoInteractions(userRepository);
        }
    }

    @Test
    void changePassword_shouldUpdatePassword_whenCurrentMatchesAndValidNew() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old");
        request.setNewPassword("new-password");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");

        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("user-1");

            assertDoesNotThrow(() -> profileService.changePassword("user-1", request));
            verify(userRepository).save(user);
        }
    }

    @Test
    void changePassword_shouldThrowUnauthorized_whenUserMismatch() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old");
        request.setNewPassword("new-password");

        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("another-user");

            assertThrows(UnauthorizedException.class,
                () -> profileService.changePassword("user-1", request));
            verifyNoInteractions(userRepository);
        }
    }

    @Test
    void changePassword_shouldThrowWhenUserNotFoundOrCurrentPasswordIncorrectOrNewTooShort() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old");
        request.setNewPassword("short");

        // user not found
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());
        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("user-1");
            assertThrows(ResourceNotFoundException.class,
                () -> profileService.changePassword("user-1", request));
        }

        // user found but current password incorrect
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", user.getPassword())).thenReturn(false);
        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("user-1");
            assertThrows(BadRequestException.class,
                () -> profileService.changePassword("user-1", request));
        }

        // current password ok but new password too short
        when(passwordEncoder.matches("old", user.getPassword())).thenReturn(true);
        request.setNewPassword("123");
        try (MockedStatic<SecurityContextHelper> mockedStatic = mockStatic(SecurityContextHelper.class)) {
            mockedStatic.when(SecurityContextHelper::getCurrentUserId).thenReturn("user-1");
            assertThrows(BadRequestException.class,
                () -> profileService.changePassword("user-1", request));
        }
    }
}
