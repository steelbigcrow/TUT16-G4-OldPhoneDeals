package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.auth.*;
import com.oldphonedeals.dto.response.auth.*;

/**
 * 用户认证服务接口
 * <p>
 * 提供用户认证相关的核心功能，包括：
 * - 用户登录：验证用户凭证并生成JWT令牌
 * - 用户注册：创建新用户账户并发送验证邮件
 * - 邮箱验证：激活用户账户
 * - 密码重置：发送密码重置邮件和重置密码
 * - 获取当前用户：从JWT令牌中获取当前登录用户信息
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
public interface AuthService {
    
    /**
     * 用户登录
     * <p>
     * 验证用户的邮箱和密码，检查账户状态（是否已验证邮箱、是否被禁用），
     * 生成JWT令牌并更新最后登录时间。
     * </p>
     * 
     * @param request 登录请求，包含邮箱和密码
     * @return 登录响应，包含JWT令牌和用户基本信息
     * @throws com.oldphonedeals.exception.UnauthorizedException 如果邮箱或密码错误
     * @throws com.oldphonedeals.exception.ForbiddenException 如果邮箱未验证或账户被禁用
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 用户注册
     * <p>
     * 创建新用户账户，生成唯一的验证令牌，发送验证邮件。
     * 新注册的用户需要验证邮箱后才能登录。
     * </p>
     * 
     * @param request 注册请求，包含firstName、lastName、email、password
     * @return 用户响应，包含用户基本信息
     * @throws com.oldphonedeals.exception.DuplicateResourceException 如果邮箱已被注册
     */
    AuthUserResponse register(RegisterRequest request);
    
    /**
     * 验证邮箱
     * <p>
     * 通过验证令牌激活用户账户，设置isVerified为true，清除验证令牌。
     * </p>
     * 
     * @param request 验证请求，包含验证令牌
     * @throws com.oldphonedeals.exception.BadRequestException 如果令牌无效或已过期
     */
    void verifyEmail(VerifyEmailRequest request);
    
    /**
     * 发送密码重置邮件
     * <p>
     * 检查邮箱是否存在，如果存在则发送包含重置链接的邮件。
     * </p>
     * 
     * @param request 密码重置邮件请求，包含邮箱地址
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 如果邮箱不存在
     */
    void sendPasswordResetEmail(SendResetPasswordEmailRequest request);
    
    /**
     * 重置密码
     * <p>
     * 重置用户密码。如果提供了当前密码，则验证当前密码；
     * 如果未提供，则直接重置密码（用于忘记密码场景）。
     * </p>
     * 
     * @param request 密码重置请求，包含邮箱、当前密码（可选）、新密码
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 如果用户不存在
     * @throws com.oldphonedeals.exception.UnauthorizedException 如果当前密码错误
     * @throws com.oldphonedeals.exception.BadRequestException 如果新密码不符合要求
     */
    void resetPassword(ResetPasswordRequest request);
    
    /**
     * 获取当前登录用户信息
     * <p>
     * 从安全上下文中获取当前已认证用户的详细信息。
     * </p>
     *
     * @return 用户响应，包含用户详细信息
     * @throws com.oldphonedeals.exception.UnauthorizedException 如果用户未登录
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 如果用户不存在
     */
    AuthUserResponse getCurrentUser();
    
    /**
     * 请求密码重置（生成6位数字重置码）
     * <p>
     * 为用户生成6位数字重置码，设置1小时有效期，并发送包含重置码的邮件。
     * 即使用户不存在也返回成功，以防止邮箱枚举攻击。
     * </p>
     *
     * @param email 用户邮箱
     */
    void requestPasswordReset(String email);
    
    /**
     * 验证密码重置码
     * <p>
     * 验证用户提供的6位数字重置码是否有效且未过期。
     * </p>
     *
     * @param email 用户邮箱
     * @param code 6位数字重置码
     * @return 如果重置码有效返回true，否则返回false
     */
    boolean verifyResetCode(String email, String code);
    
    /**
     * 使用重置码重置密码
     * <p>
     * 验证重置码后，重置用户密码并清除重置码。
     * </p>
     *
     * @param email 用户邮箱
     * @param code 6位数字重置码
     * @param newPassword 新密码
     * @throws com.oldphonedeals.exception.BadRequestException 如果重置码无效或已过期
     */
    void resetPasswordWithCode(String email, String code, String newPassword);
    
    /**
     * 重新发送验证邮件
     * <p>
     * 为未验证的用户重新生成验证token并发送验证邮件。
     * </p>
     *
     * @param email 用户邮箱
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 如果用户不存在
     * @throws com.oldphonedeals.exception.BadRequestException 如果用户已验证
     */
    void resendVerification(String email);
}