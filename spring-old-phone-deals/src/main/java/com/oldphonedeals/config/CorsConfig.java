package com.oldphonedeals.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web配置类
 * 配置CORS、静态资源映射等
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Value("${frontend.url}")
  private String frontendUrl;

  @Autowired
  private FileStorageProperties fileStorageProperties;

  /**
   * 配置CORS
   * 允许前端应用访问API
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(frontendUrl)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600); // 预检请求缓存1小时
  }

  /**
   * 配置静态资源映射
   * 将/uploads/**映射到文件上传目录
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 映射上传的文件
    String uploadPath = Paths.get(fileStorageProperties.getDir())
        .toAbsolutePath()
        .toString();
    
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:" + uploadPath + "/")
        .setCachePeriod(3600); // 缓存1小时
    
    // 如果还有其他静态资源，保留classpath映射
    registry.addResourceHandler("/images/**")
        .addResourceLocations("classpath:/static/images/");
  }
}