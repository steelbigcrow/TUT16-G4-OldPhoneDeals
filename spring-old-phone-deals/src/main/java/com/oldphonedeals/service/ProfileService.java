package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.profile.ChangePasswordRequest;
import com.oldphonedeals.dto.request.profile.UpdateProfileRequest;
import com.oldphonedeals.dto.response.user.UserProfileResponse;

/**
 * 用户个人资料服务接口
 * <p>
 * 提供用户个人资料相关功能，包括：
 * - 获取用户资料
 * - 更新用户资料
 * - 修改密码
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
public interface ProfileService {
    
    /**
     * 获取用户资料
     * <p>
     * 返回用户的基本信息，不包含敏感字段（如password、verificationToken等）
     * </p>
     * 
     * @param userId 用户ID
     * @return 用户资料响应
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 如果用户不存在
     */
    UserProfileResponse getUserProfile(String userId);
    
    /**
     * 更新用户资料
     * <p>
     * 仅允许更新firstName和lastName字段。
     * 需要权限检查：只能更新自己的资料。
     * </p>
     * 
     * @param userId 用户ID
     * @param request 更新资料请求（firstName, lastName）
     * @return 更新后的用户资料
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 如果用户不存在
     * @throws com.oldphonedeals.exception.UnauthorizedException 如果无权限修改
     */
    UserProfileResponse updateUserProfile(String userId, UpdateProfileRequest request);
    
    /**
     * 修改密码
     * <p>
     * 需要验证当前密码后才能设置新密码。
     * 需要权限检查：只能修改自己的密码。
     * </p>
     * 
     * @param userId 用户ID
     * @param request 修改密码请求（currentPassword, newPassword）
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 如果用户不存在
     * @throws com.oldphonedeals.exception.UnauthorizedException 如果无权限修改
     * @throws com.oldphonedeals.exception.BadRequestException 如果当前密码错误或新密码不符合要求
     */
    void changePassword(String userId, ChangePasswordRequest request);
}