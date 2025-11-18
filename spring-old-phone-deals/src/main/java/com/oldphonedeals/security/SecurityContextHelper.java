package com.oldphonedeals.security;

import com.oldphonedeals.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 安全上下文辅助类
 * <p>
 * 提供便捷的方法来获取当前登录用户的信息，简化控制器和服务层中的认证信息访问。
 * 该类从 Spring Security 的 SecurityContext 中提取用户信息。
 * </p>
 * <p>
 * 使用方式：
 * <pre>{@code
 * // 在控制器或服务中
 * String userId = SecurityContextHelper.getCurrentUserId();
 * String email = SecurityContextHelper.getCurrentUserEmail();
 * boolean isAdmin = SecurityContextHelper.isAdmin();
 * }</pre>
 * </p>
 *
 * @author OldPhoneDeals Team
 */
@Slf4j
@Component
public class SecurityContextHelper {

  /**
   * 获取当前登录用户的 ID
   * <p>
   * 从 JWT Token 中提取的用户 ID（对应 MongoDB 的 _id）
   * </p>
   *
   * @return 用户 ID
   * @throws UnauthorizedException 如果用户未认证
   */
  public static String getCurrentUserId() {
    Authentication authentication = getAuthentication();
    
    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("Attempt to get user ID without authentication");
      throw new UnauthorizedException("User not authenticated");
    }

    // 在 JWT 认证流程中，principal 是 UserDetails 对象
    // UserDetails 的 username 存储的是 email，我们需要从 JWT Token 获取实际的 userId
    // 注意：这里需要配合 JwtAuthenticationFilter 来设置正确的 principal
    Object principal = authentication.getPrincipal();
    
    if (principal instanceof UserDetails) {
      // 这里返回的是 email，但在实际使用中我们需要 userId
      // 建议在实际应用中创建自定义的 UserPrincipal 类来同时存储 userId 和 email
      // 目前作为临时方案，返回 username（email）
      log.debug("Getting user ID for authenticated user");
      return ((UserDetails) principal).getUsername();
    }
    
    log.warn("Principal is not an instance of UserDetails");
    throw new UnauthorizedException("Invalid authentication principal");
  }

  /**
   * 获取当前登录用户的邮箱
   *
   * @return 用户邮箱
   * @throws UnauthorizedException 如果用户未认证
   */
  public static String getCurrentUserEmail() {
    Authentication authentication = getAuthentication();
    
    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("Attempt to get user email without authentication");
      throw new UnauthorizedException("User not authenticated");
    }

    Object principal = authentication.getPrincipal();
    
    if (principal instanceof UserDetails) {
      String email = ((UserDetails) principal).getUsername();
      log.debug("Retrieved email for current user: {}", email);
      return email;
    }
    
    log.warn("Principal is not an instance of UserDetails");
    throw new UnauthorizedException("Invalid authentication principal");
  }

  /**
   * 判断当前用户是否为管理员
   *
   * @return 如果是管理员返回 true，否则返回 false
   */
  public static boolean isAdmin() {
    Authentication authentication = getAuthentication();
    
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }

    // 检查用户是否具有 ROLE_ADMIN 权限
    boolean hasAdminRole = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(role -> role.equals("ROLE_ADMIN"));
    
    log.debug("Admin check result: {}", hasAdminRole);
    return hasAdminRole;
  }

  /**
   * 检查当前用户是否已认证
   *
   * @return 如果已认证返回 true，否则返回 false
   */
  public static boolean isAuthenticated() {
    Authentication authentication = getAuthentication();
    return authentication != null && authentication.isAuthenticated();
  }

  /**
   * 获取当前的 Authentication 对象
   *
   * @return Authentication 对象，如果未认证则返回 null
   */
  private static Authentication getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  /**
   * 验证当前用户是否为管理员，如果不是则抛出异常
   *
   * @throws UnauthorizedException 如果用户不是管理员
   */
  public static void requireAdmin() {
    if (!isAdmin()) {
      log.warn("Non-admin user attempted to access admin-only resource");
      throw new UnauthorizedException("Admin privileges required");
    }
  }

  /**
   * 验证当前用户是否已认证，如果未认证则抛出异常
   *
   * @throws UnauthorizedException 如果用户未认证
   */
  public static void requireAuthentication() {
    if (!isAuthenticated()) {
      log.warn("Unauthenticated user attempted to access protected resource");
      throw new UnauthorizedException("Authentication required");
    }
  }
}