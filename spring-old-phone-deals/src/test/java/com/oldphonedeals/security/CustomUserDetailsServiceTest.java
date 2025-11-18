package com.oldphonedeals.security;

import com.oldphonedeals.TestDataFactory;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CustomUserDetailsService 测试
 */
@DisplayName("CustomUserDetailsService Tests")
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private CustomUserDetailsService userDetailsService;

  @Test
  @DisplayName("应该成功加载用户 - 当用户存在时")
  void shouldLoadUser_whenUserExists() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

    // When
    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

    // Then
    assertNotNull(userDetails);
    assertEquals(user.getEmail(), userDetails.getUsername());
    assertEquals(user.getPassword(), userDetails.getPassword());
    assertTrue(userDetails.isEnabled());
    assertTrue(userDetails.isAccountNonLocked());
    verify(userRepository, times(1)).findByEmail(user.getEmail());
  }

  @Test
  @DisplayName("应该抛出异常 - 当用户不存在时")
  void shouldThrowException_whenUserNotFound() {
    // Given
    String email = "nonexistent@example.com";
    when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

    // When & Then
    UsernameNotFoundException exception = assertThrows(
        UsernameNotFoundException.class,
        () -> userDetailsService.loadUserByUsername(email)
    );
    assertTrue(exception.getMessage().contains(email));
    verify(userRepository, times(1)).findByEmail(email);
  }

  @Test
  @DisplayName("应该加载管理员用户 - 并分配 ROLE_ADMIN 权限")
  void shouldLoadAdminUser_withAdminRole() {
    // Given
    User adminUser = TestDataFactory.createAdminUser();
    when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

    // When
    UserDetails userDetails = userDetailsService.loadUserByUsername(adminUser.getEmail());

    // Then
    assertNotNull(userDetails);
    assertEquals(2, userDetails.getAuthorities().size());
    assertTrue(userDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(role -> role.equals("ROLE_ADMIN")));
    assertTrue(userDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(role -> role.equals("ROLE_USER")));
  }

  @Test
  @DisplayName("应该加载普通用户 - 只分配 ROLE_USER 权限")
  void shouldLoadRegularUser_withUserRoleOnly() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

    // When
    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

    // Then
    assertNotNull(userDetails);
    assertEquals(1, userDetails.getAuthorities().size());
    assertTrue(userDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(role -> role.equals("ROLE_USER")));
    assertFalse(userDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(role -> role.equals("ROLE_ADMIN")));
  }

  @Test
  @DisplayName("应该抛出异常 - 当用户被禁用时")
  void shouldThrowException_whenUserIsDisabled() {
    // Given
    User disabledUser = TestDataFactory.createDisabledUser();
    when(userRepository.findByEmail(disabledUser.getEmail())).thenReturn(Optional.of(disabledUser));

    // When & Then
    UsernameNotFoundException exception = assertThrows(
        UsernameNotFoundException.class,
        () -> userDetailsService.loadUserByUsername(disabledUser.getEmail())
    );
    assertTrue(exception.getMessage().contains("disabled"));
    verify(userRepository, times(1)).findByEmail(disabledUser.getEmail());
  }

  @Test
  @DisplayName("应该抛出异常 - 当用户被封禁时")
  void shouldThrowException_whenUserIsBanned() {
    // Given
    User bannedUser = TestDataFactory.createBannedUser();
    when(userRepository.findByEmail(bannedUser.getEmail())).thenReturn(Optional.of(bannedUser));

    // When & Then
    UsernameNotFoundException exception = assertThrows(
        UsernameNotFoundException.class,
        () -> userDetailsService.loadUserByUsername(bannedUser.getEmail())
    );
    assertTrue(exception.getMessage().contains("banned"));
    verify(userRepository, times(1)).findByEmail(bannedUser.getEmail());
  }

  @Test
  @DisplayName("应该正确设置账户状态 - 对于正常用户")
  void shouldSetAccountStatus_forNormalUser() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

    // When
    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

    // Then
    assertTrue(userDetails.isEnabled());
    assertTrue(userDetails.isAccountNonLocked());
    assertTrue(userDetails.isAccountNonExpired());
    assertTrue(userDetails.isCredentialsNonExpired());
  }

  @Test
  @DisplayName("应该处理 null 邮箱")
  void shouldHandleNullEmail() {
    // When & Then
    assertThrows(
        Exception.class,
        () -> userDetailsService.loadUserByUsername(null)
    );
  }

  @Test
  @DisplayName("应该处理空邮箱")
  void shouldHandleEmptyEmail() {
    // Given
    when(userRepository.findByEmail("")).thenReturn(Optional.empty());

    // When & Then
    assertThrows(
        UsernameNotFoundException.class,
        () -> userDetailsService.loadUserByUsername("")
    );
  }

  @Test
  @DisplayName("应该缓存用户详情 - 多次调用使用相同邮箱")
  void shouldNotCacheUserDetails_multipleCallsWithSameEmail() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

    // When
    UserDetails userDetails1 = userDetailsService.loadUserByUsername(user.getEmail());
    UserDetails userDetails2 = userDetailsService.loadUserByUsername(user.getEmail());

    // Then
    assertNotNull(userDetails1);
    assertNotNull(userDetails2);
    // Note: Without caching, repository should be called twice
    verify(userRepository, times(2)).findByEmail(user.getEmail());
  }

  @Test
  @DisplayName("应该正确映射用户字段到 UserDetails")
  void shouldCorrectlyMapUserFieldsToUserDetails() {
    // Given
    User user = TestDataFactory.createDefaultUser();
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

    // When
    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

    // Then
    assertEquals(user.getEmail(), userDetails.getUsername());
    assertEquals(user.getPassword(), userDetails.getPassword());
    assertNotNull(userDetails.getAuthorities());
    assertFalse(userDetails.getAuthorities().isEmpty());
  }

  @Test
  @DisplayName("应该区分大小写 - 在邮箱查找时")
  void shouldBeCaseSensitive_whenLookingUpEmail() {
    // Given
    String originalEmail = "Test@Example.com";
    String lowerCaseEmail = "test@example.com";
    User user = TestDataFactory.createDefaultUser();
    user.setEmail(originalEmail);

    when(userRepository.findByEmail(originalEmail)).thenReturn(Optional.of(user));
    when(userRepository.findByEmail(lowerCaseEmail)).thenReturn(Optional.empty());

    // When & Then - 使用原始大小写应该成功
    UserDetails userDetails = userDetailsService.loadUserByUsername(originalEmail);
    assertNotNull(userDetails);

    // 使用不同大小写应该失败
    assertThrows(
        UsernameNotFoundException.class,
        () -> userDetailsService.loadUserByUsername(lowerCaseEmail)
    );
  }

  @Test
  @DisplayName("应该正确处理未验证的用户")
  void shouldHandleUnverifiedUser() {
    // Given
    User unverifiedUser = TestDataFactory.createUnverifiedUser();
    when(userRepository.findByEmail(unverifiedUser.getEmail())).thenReturn(Optional.of(unverifiedUser));

    // When
    UserDetails userDetails = userDetailsService.loadUserByUsername(unverifiedUser.getEmail());

    // Then
    // 未验证的用户仍然可以加载（验证状态不影响 UserDetails 加载）
    assertNotNull(userDetails);
    assertEquals(unverifiedUser.getEmail(), userDetails.getUsername());
  }

  @Test
  @DisplayName("应该为所有用户分配至少 ROLE_USER 权限")
  void shouldAssignRoleUser_toAllUsers() {
    // Given
    User regularUser = TestDataFactory.createDefaultUser();
    User adminUser = TestDataFactory.createAdminUser();

    when(userRepository.findByEmail(regularUser.getEmail())).thenReturn(Optional.of(regularUser));
    when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

    // When
    UserDetails regularUserDetails = userDetailsService.loadUserByUsername(regularUser.getEmail());
    UserDetails adminUserDetails = userDetailsService.loadUserByUsername(adminUser.getEmail());

    // Then
    assertTrue(regularUserDetails.getAuthorities().stream()
        .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
    assertTrue(adminUserDetails.getAuthorities().stream()
        .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
  }
}