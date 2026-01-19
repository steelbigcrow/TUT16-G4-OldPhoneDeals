package com.oldphonedeals.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Lightweight JWT-backed principal.
 *
 * We set the username to the MongoDB user id (JWT subject) so that
 * SecurityContextHelper.getCurrentUserId() returns a real user id, not the email.
 */
public class UserPrincipal implements UserDetails {

  private final String userId;
  private final String email;
  private final Collection<? extends GrantedAuthority> authorities;

  public UserPrincipal(
      String userId,
      String email,
      Collection<? extends GrantedAuthority> authorities
  ) {
    this.userId = userId;
    this.email = email;
    this.authorities = authorities;
  }

  public String getUserId() {
    return userId;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    // Not stored on the principal; JWT is the credential.
    return "";
  }

  @Override
  public String getUsername() {
    // Spring Security uses "username" as the Authentication name; map it to userId.
    return userId;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}

