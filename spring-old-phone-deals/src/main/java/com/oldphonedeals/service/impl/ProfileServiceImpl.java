package com.oldphonedeals.service.impl;

import com.oldphonedeals.dto.request.profile.ChangePasswordRequest;
import com.oldphonedeals.dto.request.profile.UpdateProfileRequest;
import com.oldphonedeals.dto.response.user.UserProfileResponse;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.DuplicateResourceException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.security.SecurityContextHelper;
import com.oldphonedeals.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service implementation for user profile operations.
 * Mirrors the legacy Express.js behaviour while following Spring best practices.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Retrieve a user's profile by id.
     *
     * @param userId user id
     * @return user profile response
     */
    @Override
    public UserProfileResponse getUserProfile(String userId) {
        log.info("Getting user profile for user ID: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildUserProfileResponse(user);
    }

    /**
     * Update the current user's profile.
     * Supports name-only updates without password, and email changes that require the
     * current password plus email uniqueness checks.
     *
     * @param userId  user id (must match the authenticated user)
     * @param request profile update request
     * @return updated profile
     */
    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(String userId, UpdateProfileRequest request) {
        log.info("Updating profile for user ID: {}", userId);

        // Permission check: can only update own profile
        String currentUserId = SecurityContextHelper.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            log.warn(
                "Unauthorized profile update attempt. Current user: {}, Target user: {}",
                currentUserId,
                userId
            );
            throw new UnauthorizedException("You can only update your own profile");
        }

        // Load user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Always allow updating names
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        // Handle optional email change
        String requestedEmail = request.getEmail();
        boolean hasEmail = requestedEmail != null && !requestedEmail.isBlank();
        boolean emailChanged = hasEmail && !requestedEmail.equalsIgnoreCase(user.getEmail());

        if (emailChanged) {
            // Require current password when changing email
            String currentPassword = request.getCurrentPassword();
            if (currentPassword == null || currentPassword.isBlank()) {
                log.warn("Email change requested without current password for user: {}", user.getEmail());
                throw new BadRequestException("Current password is required to change email");
            }

            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                log.warn(
                    "Current password incorrect for user during email change: {}",
                    user.getEmail()
                );
                throw new BadRequestException("Current password is incorrect");
            }

            // Enforce email uniqueness (allow keeping the same email)
            if (userRepository.existsByEmail(requestedEmail)
                && !requestedEmail.equalsIgnoreCase(user.getEmail())) {
                log.warn("Email update failed - email already exists: {}", requestedEmail);
                throw new DuplicateResourceException("Account with that email already exists");
            }

            user.setEmail(requestedEmail);
        }

        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        log.info("Profile updated successfully for user: {}", user.getEmail());

        return buildUserProfileResponse(user);
    }

    /**
     * Change the current user's password.
     * Requires the correct current password and enforces a minimum length.
     *
     * @param userId  user id
     * @param request password change request
     */
    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        log.info("Changing password for user ID: {}", userId);

        // Permission check: can only change own password
        String currentUserId = SecurityContextHelper.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            log.warn(
                "Unauthorized password change attempt. Current user: {}, Target user: {}",
                currentUserId,
                userId
            );
            throw new UnauthorizedException("You can only change your own password");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Current password incorrect for user: {}", user.getEmail());
            throw new BadRequestException("Current password is incorrect");
        }

        // Enforce minimum new password length (at least 6 characters)
        if (request.getNewPassword().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getEmail());

        // Note: changing password does not invalidate the current JWT,
        // matching the legacy Express.js implementation.
    }

    /**
     * Build a safe profile response DTO from the User entity.
     */
    private UserProfileResponse buildUserProfileResponse(User user) {
        return UserProfileResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .emailVerified(user.getIsVerified())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .build();
    }
}

