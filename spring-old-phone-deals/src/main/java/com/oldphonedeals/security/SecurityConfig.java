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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ObjectMapper objectMapper;

  @Value("${app.e2e.enabled:false}")
  private boolean e2eEnabled;

  @Value("${frontend.url:http://localhost:5173}")
  private String frontendUrl;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(auth -> {
          // Public endpoints
          auth.requestMatchers(
              "/api/auth/**",
              "/api/public/**",
              "/error",
              "/static/**",
              "/images/**"
          ).permitAll();

          // Admin login must be public (otherwise can't get JWT)
          auth.requestMatchers(HttpMethod.POST, "/api/admin/login").permitAll();

          // Public browsing
          auth.requestMatchers(HttpMethod.GET, "/api/phones/**").permitAll();

          // Uploaded images are static resources; permit GET so guests can see product images.
          auth.requestMatchers(HttpMethod.GET, "/uploads/**").permitAll();

          if (e2eEnabled) {
            auth.requestMatchers("/api/e2e/**").permitAll();
          }

          // Admin endpoints
          auth.requestMatchers("/api/admin/**").hasRole("ADMIN");

          // Everything else requires authentication
          auth.anyRequest().authenticated();
        })
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint((request, response, authException) -> {
              response.setStatus(401);
              response.setContentType(MediaType.APPLICATION_JSON_VALUE);
              response.setCharacterEncoding("UTF-8");

              ApiResponse<?> apiResponse = ApiResponse.error("Unauthorized: " + authException.getMessage());
              response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
              response.setStatus(403);
              response.setContentType(MediaType.APPLICATION_JSON_VALUE);
              response.setCharacterEncoding("UTF-8");

              ApiResponse<?> apiResponse = ApiResponse.error("Access Denied: " + accessDeniedException.getMessage());
              response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            })
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(Arrays.asList(
        frontendUrl,
        "http://localhost:4200",
        "http://localhost:3000",
        "https://oldphonedeals.com"
    ));

    configuration.setAllowedMethods(Arrays.asList(
        HttpMethod.GET.name(),
        HttpMethod.POST.name(),
        HttpMethod.PUT.name(),
        HttpMethod.PATCH.name(),
        HttpMethod.DELETE.name(),
        HttpMethod.OPTIONS.name()
    ));

    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }
}

