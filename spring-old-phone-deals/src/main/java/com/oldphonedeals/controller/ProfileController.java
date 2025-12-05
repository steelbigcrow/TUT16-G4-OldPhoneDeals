package com.oldphonedeals.controller;

import com.oldphonedeals.dto.request.profile.ChangePasswordRequest;
import com.oldphonedeals.dto.request.profile.UpdateProfileRequest;
import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.user.UserProfileResponse;
import com.oldphonedeals.security.SecurityContextHelper;
import com.oldphonedeals.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User profile REST controller.
 * <p>
 * Provides endpoints for viewing and updating the authenticated user's profile
 * as well as legacy id-based variants kept for compatibility.
 * </p>
 *
 * <h3>Primary API endpoints</h3>
 * <ul>
 *   <li>GET /api/profile - get current user profile (JWT based)</li>
 *   <li>PUT /api/profile - update current user profile</li>
 *   <li>PUT /api/profile/change-password - change current user's password</li>
 * </ul>
 *
 * <h3>Legacy/internal endpoints</h3>
 * <ul>
 *   <li>GET /api/profile/{userId}</li>
 *   <li>PUT /api/profile/{userId}</li>
 *   <li>PUT /api/profile/{userId}/change-password</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Get the current authenticated user's profile (JWT-based).
     *
     * @return current user profile wrapped in ApiResponse
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile() {
        String userId = SecurityContextHelper.getCurrentUserId();
        log.info("Get current user profile request for user ID: {}", userId);

        UserProfileResponse response = profileService.getUserProfile(userId);

        return ResponseEntity.ok(
            ApiResponse.success(response, "User profile retrieved successfully")
        );
    }

    /**
     * Legacy/internal endpoint: get profile by explicit user id.
     *
     * @param userId user id
     * @return user profile
     */
    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
        @PathVariable String userId
    ) {
        log.info("Get user profile request for user ID: {}", userId);

        UserProfileResponse response = profileService.getUserProfile(userId);

        return ResponseEntity.ok(
            ApiResponse.success(response, "User profile retrieved successfully")
        );
    }

    /**
     * Legacy/internal endpoint: update profile by explicit user id.
     *
     * @param userId  user id
     * @param request update request (firstName, lastName, email, optional currentPassword)
     * @return updated profile
     */
    @PutMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserProfile(
        @PathVariable String userId,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        log.info("Update user profile request for user ID: {}", userId);

        UserProfileResponse response = profileService.updateUserProfile(userId, request);

        return ResponseEntity.ok(
            ApiResponse.success(response, "Profile updated successfully")
        );
    }

    /**
     * Update the current authenticated user's profile using the JWT identity.
     *
     * @param request profile update request (firstName, lastName, email, optional currentPassword)
     * @return updated profile
     */
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUserProfile(
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        String userId = SecurityContextHelper.getCurrentUserId();
        log.info("Update current user profile request for user ID: {}", userId);

        UserProfileResponse response = profileService.updateUserProfile(userId, request);

        return ResponseEntity.ok(
            ApiResponse.success(response, "Profile updated successfully")
        );
    }

    /**
     * Legacy/internal endpoint: change password for the specified user id.
     *
     * @param userId  user id
     * @param request password change request
     * @return empty success response
     */
    @PutMapping("/{userId}/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
        @PathVariable String userId,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        log.info("Change password request for user ID: {}", userId);

        profileService.changePassword(userId, request);

        return ResponseEntity.ok(
            ApiResponse.success(null, "Password changed successfully")
        );
    }

    /**
     * Change the current authenticated user's password (JWT-based).
     *
     * @param request password change request
     * @return empty success response
     */
    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changeCurrentUserPassword(
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        String userId = SecurityContextHelper.getCurrentUserId();
        log.info("Change password request for current user ID: {}", userId);

        profileService.changePassword(userId, request);

        return ResponseEntity.ok(
            ApiResponse.success(null, "Password changed successfully")
        );
    }
}

