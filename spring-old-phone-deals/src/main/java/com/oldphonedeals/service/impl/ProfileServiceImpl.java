package com.oldphonedeals.service.impl;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户个人资料服务实现类
 * <p>
 * 实现用户个人资料管理的业务逻辑，包括资料查看、更新和密码修改。
 * 所有操作都需要进行权限检查，确保用户只能操作自己的资料。
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 获取用户资料
     * <p>
     * 返回用户的基本信息，过滤敏感字段。
     * </p>
     * 
     * @param userId 用户ID
     * @return 用户资料响应
     * @throws ResourceNotFoundException 如果用户不存在
     */
    @Override
    public UserProfileResponse getUserProfile(String userId) {
        log.info("Getting user profile for user ID: {}", userId);
        
        // 查找用户
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // 构建并返回用户资料（不包含敏感字段）
        return buildUserProfileResponse(user);
    }
    
    /**
     * 更新用户资料
     * <p>
     * 仅允许更新firstName和lastName。
     * 权限检查：只能更新自己的资料。
     * </p>
     * 
     * @param userId 用户ID
     * @param request 更新资料请求
     * @return 更新后的用户资料
     * @throws ResourceNotFoundException 如果用户不存在
     * @throws UnauthorizedException 如果无权限修改
     */
    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(String userId, UpdateProfileRequest request) {
        log.info("Updating profile for user ID: {}", userId);
        
        // 权限检查：只能更新自己的资料
        String currentUserId = SecurityContextHelper.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            log.warn("Unauthorized profile update attempt. Current user: {}, Target user: {}", 
                currentUserId, userId);
            throw new UnauthorizedException("You can only update your own profile");
        }
        
        // 查找用户
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // 更新允许的字段
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUpdatedAt(LocalDateTime.now());
        
        // 保存更新
        user = userRepository.save(user);
        
        log.info("Profile updated successfully for user: {}", user.getEmail());
        
        // 返回更新后的资料
        return buildUserProfileResponse(user);
    }
    
    /**
     * 修改密码
     * <p>
     * 需要验证当前密码后才能设置新密码。
     * 权限检查：只能修改自己的密码。
     * </p>
     * 
     * @param userId 用户ID
     * @param request 修改密码请求
     * @throws ResourceNotFoundException 如果用户不存在
     * @throws UnauthorizedException 如果无权限修改
     * @throws BadRequestException 如果当前密码错误或新密码不符合要求
     */
    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        log.info("Changing password for user ID: {}", userId);
        
        // 权限检查：只能修改自己的密码
        String currentUserId = SecurityContextHelper.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            log.warn("Unauthorized password change attempt. Current user: {}, Target user: {}", 
                currentUserId, userId);
            throw new UnauthorizedException("You can only change your own password");
        }
        
        // 查找用户
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // 验证当前密码
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Current password incorrect for user: {}", user.getEmail());
            throw new BadRequestException("Current password is incorrect");
        }
        
        // 验证新密码长度（至少6位）
        if (request.getNewPassword().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters");
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        
        // 保存更新
        userRepository.save(user);
        
        log.info("Password changed successfully for user: {}", user.getEmail());
        
        // 注意：修改密码后不会使当前JWT失效（与Express.js实现一致）
    }
    
    /**
     * 构建用户资料响应对象
     * <p>
     * 过滤敏感字段，仅返回安全的用户信息。
     * </p>
     *
     * @param user 用户实体
     * @return 用户资料响应DTO
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