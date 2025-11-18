package com.oldphonedeals.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * <p>
 * 继承 OncePerRequestFilter，确保每个请求只执行一次过滤。
 * 该过滤器负责：
 * 1. 从请求头中提取 JWT Token
 * 2. 验证 Token 的有效性
 * 3. 加载用户详情
 * 4. 设置 Spring Security 认证上下文
 * </p>
 * <p>
 * 与 Express.js 中间件 (server/app/middlewares/checkJWT.js) 保持兼容，
 * 使用相同的 Token 格式：Bearer <token>
 * </p>
 *
 * @author OldPhoneDeals Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final CustomUserDetailsService userDetailsService;

  /**
   * 执行过滤逻辑
   * <p>
   * 处理流程：
   * 1. 从 Authorization 头提取 Bearer Token
   * 2. 如果 Token 存在且有效，验证并加载用户信息
   * 3. 创建认证对象并设置到 SecurityContext
   * 4. 继续过滤链
   * </p>
   *
   * @param request HTTP 请求
   * @param response HTTP 响应
   * @param filterChain 过滤器链
   * @throws ServletException Servlet 异常
   * @throws IOException IO 异常
   */
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    try {
      // 1. 从请求头提取 Token
      String token = extractToken(request);

      // 2. 如果 Token 存在且有效，进行认证
      if (token != null && jwtTokenProvider.validateToken(token)) {
        // 3. 从 Token 提取邮箱
        String email = jwtTokenProvider.getEmailFromToken(token);
        
        // 4. 加载用户详情
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        
        // 5. 创建认证对象
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );
        
        // 6. 设置认证详情（包含请求信息）
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        
        // 7. 将认证对象设置到 SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        log.debug("Set authentication for user: {}", email);
      }
    } catch (Exception ex) {
      // 记录错误但不阻止请求，让 Spring Security 处理未认证的情况
      log.error("Cannot set user authentication: {}", ex.getMessage());
    }

    // 8. 继续过滤链
    filterChain.doFilter(request, response);
  }

  /**
   * 从请求头中提取 JWT Token
   * <p>
   * 从 Authorization 头中提取 Bearer Token。
   * 期望格式：Authorization: Bearer <token>
   * </p>
   *
   * @param request HTTP 请求
   * @return JWT Token 字符串，如果不存在或格式错误则返回 null
   */
  private String extractToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    
    // 检查 Authorization 头是否存在且以 "Bearer " 开头
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      // 提取 "Bearer " 后面的 Token（从索引 7 开始）
      String token = bearerToken.substring(7);
      log.trace("Extracted JWT token from request");
      return token;
    }
    
    return null;
  }
}