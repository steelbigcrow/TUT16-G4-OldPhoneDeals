package com.oldphonedeals.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Mockito.mock;

/**
 * 测试配置类 - 为Controller测试提供必要的Mock Bean
 *
 * 此配置解决了@WebMvcTest与@EnableMongoAuditing以及CorsConfig的冲突问题
 */
@TestConfiguration
public class ControllerTestConfig implements WebMvcConfigurer {

    @Bean
    @Primary
    public MongoMappingContext mongoMappingContext() {
        return mock(MongoMappingContext.class);
    }

    @Bean
    @Primary
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("test-user");
    }

    /**
     * 提供FileStorageProperties的真实配置Bean
     * 为文件上传提供测试配置
     */
    @Bean
    @Primary
    public FileStorageProperties fileStorageProperties() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setDir("./test-uploads");
        properties.setMaxSize(10485760L); // 10MB
        properties.setAllowedExtensions(Arrays.asList("jpg", "jpeg", "png", "gif"));
        properties.setAllowedMimeTypes(Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/gif"));
        properties.setOverwrite(false);
        properties.setNamingStrategy("uuid");
        return properties;
    }
    
    /**
     * 替换CorsConfig，避免在测试环境中访问文件系统
     * 测试环境不需要实际的静态资源映射
     */
    // WebMvcConfigurer的默认实现已经足够，不需要添加ResourceHandlers
}