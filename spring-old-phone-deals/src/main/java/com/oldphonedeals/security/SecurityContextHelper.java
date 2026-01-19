package com.oldphonedeals.security;

import com.oldphonedeals.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * SecurityContext helper utilities.
 *
 * The app uses JWT where the subject (sub) is the MongoDB user id.
 * Our JwtAuthenticationFilter sets a UserPrincipal so that "current user id"
 * is a real user id (not an email).
 */
@Slf4j
@Component
public class SecurityContextHelper {

  public static String getCurrentUserId() {
    Authentication authentication = getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("Attempt to get user ID without authentication");
      throw new UnauthorizedException("User not authenticated");
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof UserPrincipal userPrincipal) {
      return userPrincipal.getUserId();
    }

    // Legacy fallback: some tests / older configs may store UserDetails/String here.
    if (principal instanceof UserDetails userDetails) {
      return userDetails.getUsername();
    }
    if (principal instanceof String s) {
      return s;
    }

    log.warn("Unsupported principal type: {}", principal != null ? principal.getClass() : null);
    throw new UnauthorizedException("Invalid authentication principal");
  }

  public static String getCurrentUserEmail() {
    Authentication authentication = getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("Attempt to get user email without authentication");
      throw new UnauthorizedException("User not authenticated");
    }

    Object principal = authentication.getPrincipal();

    if (principal instanceof UserPrincipal userPrincipal) {
      return userPrincipal.getEmail();
    }

    // Legacy fallback: principal stored email as username.
    if (principal instanceof UserDetails userDetails) {
      return userDetails.getUsername();
    }

    log.warn("Unsupported principal type for email: {}", principal != null ? principal.getClass() : null);
    throw new UnauthorizedException("Invalid authentication principal");
  }

  public static boolean isAdmin() {
    Authentication authentication = getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }

    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(role -> role.equals("ROLE_ADMIN"));
  }

  public static boolean isAuthenticated() {
    Authentication authentication = getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getPrincipal());
  }

  private static Authentication getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  public static void requireAdmin() {
    if (!isAdmin()) {
      log.warn("Non-admin user attempted to access admin-only resource");
      throw new UnauthorizedException("Admin privileges required");
    }
  }

  public static void requireAuthentication() {
    if (!isAuthenticated()) {
      log.warn("Unauthenticated user attempted to access protected resource");
      throw new UnauthorizedException("Authentication required");
    }
  }
}

