package com.oldphonedeals.controller;

import com.oldphonedeals.dto.request.profile.ChangePasswordRequest;
import com.oldphonedeals.dto.request.profile.UpdateProfileRequest;
import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.user.UserProfileResponse;
import com.oldphonedeals.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户个人资料控制器
 * <p>
 * 提供用户个人资料管理的REST API端点，包括资料查看、更新和密码修改。
 * 所有端点都需要JWT认证，且用户只能操作自己的资料。
 * </p>
 * 
 * <h3>API端点列表：</h3>
 * <ul>
 *   <li>GET /api/profile/{userId} - 获取用户资料</li>
 *   <li>PUT /api/profile/{userId} - 更新用户资料</li>
 *   <li>PUT /api/profile/{userId}/change-password - 修改密码</li>
 * </ul>
 * 
 * @author OldPhoneDeals Team
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {
    
    private final ProfileService profileService;
    
    /**
     * 获取用户资料
     * <p>
     * 返回用户的基本信息，不包含敏感字段（如password、verificationToken等）。
     * 需要JWT认证。
     * </p>
     * 
     * @param userId 用户ID
     * @return 用户资料响应
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
     * 更新用户资料
     * <p>
     * 仅允许更新firstName和lastName字段。
     * 需要JWT认证，且用户只能更新自己的资料。
     * </p>
     * 
     * @param userId 用户ID
     * @param request 更新资料请求（firstName, lastName）
     * @return 更新后的用户资料
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
     * 修改密码
     * <p>
     * 需要验证当前密码后才能设置新密码。
     * 需要JWT认证，且用户只能修改自己的密码。
     * </p>
     * 
     * @param userId 用户ID
     * @param request 修改密码请求（currentPassword, newPassword）
     * @return 成功消息
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
}