package com.oldphonedeals.security;

import com.oldphonedeals.entity.User;
import com.oldphonedeals.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义用户详情服务
 * <p>
 * 实现 Spring Security 的 UserDetailsService 接口，
 * 用于从数据库加载用户信息并转换为 Spring Security 的 UserDetails 对象。
 * </p>
 * <p>
 * 权限映射规则：
 * - 所有用户都有 ROLE_USER 权限
 * - 管理员用户额外具有 ROLE_ADMIN 权限
 * </p>
 *
 * @author OldPhoneDeals Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  /**
   * 根据邮箱加载用户信息
   * <p>
   * Spring Security 在认证过程中会调用此方法来加载用户详情。
   * 该方法通过邮箱查询数据库中的用户信息，并将其转换为 Spring Security
   * 所需的 UserDetails 对象。
   * </p>
   *
   * @param email 用户邮箱（作为用户名）
   * @return UserDetails 对象，包含用户信息和权限
   * @throws UsernameNotFoundException 如果用户不存在
   */
  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    log.debug("Loading user by email: {}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("User not found with email: {}", email);
          return new UsernameNotFoundException("User not found with email: " + email);
        });

    // 检查用户状态
    if (Boolean.TRUE.equals(user.getIsDisabled())) {
      log.warn("User account is disabled: {}", email);
      throw new UsernameNotFoundException("User account is disabled: " + email);
    }

    if (Boolean.TRUE.equals(user.getIsBan())) {
      log.warn("User account is banned: {}", email);
      throw new UsernameNotFoundException("User account is banned: " + email);
    }

    // 构建权限列表
    List<GrantedAuthority> authorities = buildAuthorities(user);

    log.debug("User loaded successfully: {} with authorities: {}", email, authorities);

    // 返回 Spring Security 的 User 对象（注意：不是我们的 User 实体）
    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getEmail())
        .password(user.getPassword())
        .authorities(authorities)
        .accountExpired(false)
        .accountLocked(Boolean.TRUE.equals(user.getIsBan()))
        .credentialsExpired(false)
        .disabled(Boolean.TRUE.equals(user.getIsDisabled()))
        .build();
  }

  /**
   * 构建用户权限列表
   * <p>
   * 根据用户的角色分配相应的权限：
   * - 所有用户：ROLE_USER
   * - 管理员：ROLE_USER + ROLE_ADMIN
   * </p>
   *
   * @param user 用户实体
   * @return 权限列表
   */
  private List<GrantedAuthority> buildAuthorities(User user) {
    List<GrantedAuthority> authorities = new ArrayList<>();
    
    // 所有用户都有 ROLE_USER 权限
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    
    // 根据role字段添加ROLE_ADMIN权限（兼容isAdmin字段）
    if ("ADMIN".equals(user.getRole()) || Boolean.TRUE.equals(user.getIsAdmin())) {
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
      log.debug("Admin role granted for user: {}", user.getEmail());
    }
    
    return authorities;
  }
}