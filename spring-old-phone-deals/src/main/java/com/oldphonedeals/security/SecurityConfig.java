package com.oldphonedeals.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldphonedeals.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置
 * <p>
 * 配置应用的安全策略，包括：
 * - JWT 认证机制（无状态 session）
 * - CORS 跨域配置
 * - 授权规则（公开端点、管理员端点、受保护端点）
 * - 异常处理（返回 JSON 格式）
 * </p>
 * <p>
 * 与 Express.js 后端的授权规则保持一致：
 * - /api/auth/** - 公开访问（登录、注册、密码重置等）
 * - /api/phones/** - 公开访问（浏览商品）
 * - /api/admin/** - 仅管理员访问
 * - 其他端点 - 需要认证
 * </p>
 *
 * @author OldPhoneDeals Team
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ObjectMapper objectMapper;
  @Value("${app.e2e.enabled:false}")
  private boolean e2eEnabled;

  /**
   * 配置安全过滤器链
   * <p>
   * 定义应用的安全策略，包括认证和授权规则
   * </p>
   *
   * @param http HttpSecurity 对象
   * @return SecurityFilterChain
   * @throws Exception 配置异常
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // 禁用 CSRF（因为使用 JWT，不需要 CSRF 保护）
        .csrf(AbstractHttpConfigurer::disable)
        
        // 配置 CORS
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        
        // 配置 Session 管理（无状态）
        .sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        
        // 配置授权规则
        .authorizeHttpRequests(auth -> {
            // 公开端点 - 无需认证
            auth.requestMatchers(
                "/api/auth/**",           // 认证相关：登录、注册、密码重置等
                "/api/public/**",         // 公开 API
                "/error",                 // 错误页面
                "/static/**",             // 静态资源
                "/images/**"              // 图片资源
            ).permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/phones/**").permitAll();
            if (e2eEnabled) {
              auth.requestMatchers("/api/e2e/**").permitAll();
            }
            
            // 管理员端点 - 需要 ROLE_ADMIN
            auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
            
            // 其他所有端点 - 需要认证
            auth.anyRequest().authenticated();
        })
        
        // 配置异常处理
        .exceptionHandling(exception -> exception
            // 未认证时的处理
            .authenticationEntryPoint((request, response, authException) -> {
              response.setStatus(401);
              response.setContentType(MediaType.APPLICATION_JSON_VALUE);
              response.setCharacterEncoding("UTF-8");
              
              ApiResponse<?> apiResponse = ApiResponse.error("Unauthorized: " + authException.getMessage());
              response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            })
            // 无权限时的处理
            .accessDeniedHandler((request, response, accessDeniedException) -> {
              response.setStatus(403);
              response.setContentType(MediaType.APPLICATION_JSON_VALUE);
              response.setCharacterEncoding("UTF-8");
              
              ApiResponse<?> apiResponse = ApiResponse.error("Access Denied: " + accessDeniedException.getMessage());
              response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            })
        )
        
        // 添加 JWT 认证过滤器
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * 配置 CORS
   * <p>
   * 允许前端应用跨域访问 API
   * </p>
   *
   * @return CORS 配置源
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    
    // 允许的源（开发环境和生产环境）
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:4200",      // Angular 开发服务器
        "http://localhost:3000",      // 本地测试
        "https://oldphonedeals.com"   // 生产环境（需要替换为实际域名）
    ));
    
    // 允许的 HTTP 方法
    configuration.setAllowedMethods(Arrays.asList(
        HttpMethod.GET.name(),
        HttpMethod.POST.name(),
        HttpMethod.PUT.name(),
        HttpMethod.PATCH.name(),
        HttpMethod.DELETE.name(),
        HttpMethod.OPTIONS.name()
    ));
    
    // 允许的请求头
    configuration.setAllowedHeaders(List.of("*"));
    
    // 允许发送凭证（cookies）
    configuration.setAllowCredentials(true);
    
    // 暴露的响应头
    configuration.setExposedHeaders(Arrays.asList(
        "Authorization",
        "Content-Type"
    ));
    
    // 预检请求的缓存时间（秒）
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    
    return source;
  }

  /**
   * 配置认证管理器
   * <p>
   * 用于处理认证请求
   * </p>
   *
   * @param config 认证配置
   * @return AuthenticationManager
   * @throws Exception 配置异常
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }
}
