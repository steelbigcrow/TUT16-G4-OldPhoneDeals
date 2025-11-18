package com.oldphonedeals.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.List;

/**
 * 文件存储配置属性
 * 用于读取和管理文件上传相关配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "file.upload")
@Validated
public class FileStorageProperties {

  /**
   * 文件上传目录
   */
  @NotBlank(message = "Upload directory cannot be blank")
  private String dir = "./uploads/images";

  /**
   * 最大文件大小（字节）
   * 默认10MB，与Express.js的5MB相比稍大一些
   */
  private long maxSize = 10485760; // 10MB

  /**
   * 允许的图片文件扩展名
   * 参考Express.js配置：jpeg, jpg, png
   */
  private List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif");

  /**
   * 允许的MIME类型
   */
  private List<String> allowedMimeTypes = Arrays.asList(
      "image/jpeg",
      "image/jpg",
      "image/png",
      "image/gif"
  );

  /**
   * 是否覆盖同名文件
   */
  private boolean overwrite = false;

  /**
   * 文件名生成策略
   * uuid: 使用UUID生成唯一文件名
   * timestamp: 使用时间戳生成文件名
   * original: 使用原始文件名（不推荐，可能有安全风险）
   */
  private String namingStrategy = "uuid";

  /**
   * 检查文件扩展名是否允许
   */
  public boolean isExtensionAllowed(String extension) {
    if (extension == null) {
      return false;
    }
    return allowedExtensions.stream()
        .anyMatch(ext -> ext.equalsIgnoreCase(extension));
  }

  /**
   * 检查MIME类型是否允许
   */
  public boolean isMimeTypeAllowed(String mimeType) {
    if (mimeType == null) {
      return false;
    }
    return allowedMimeTypes.stream()
        .anyMatch(type -> type.equalsIgnoreCase(mimeType));
  }

  /**
   * 检查文件大小是否超过限制
   */
  public boolean isSizeValid(long size) {
    return size > 0 && size <= maxSize;
  }
}