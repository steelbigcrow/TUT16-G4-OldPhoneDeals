package com.oldphonedeals.service.impl;

import com.oldphonedeals.config.FileStorageProperties;
import com.oldphonedeals.exception.FileStorageException;
import com.oldphonedeals.exception.FileValidationException;
import com.oldphonedeals.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 文件存储服务实现
 * <p>
 * 提供文件上传、删除、加载和验证功能的具体实现。
 * 使用本地文件系统存储文件，支持自动创建目录和文件名生成。
 * </p>
 *
 * @author OldPhoneDeals Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

  private final FileStorageProperties fileStorageProperties;

  /**
   * 存储上传的文件到指定子目录
   *
   * @param file 上传的文件
   * @param subDirectory 子目录名称
   * @return 生成的文件名（包含子目录路径）
   */
  @Override
  public String storeFile(MultipartFile file, String subDirectory) {
    // 验证文件
    validateFile(file);

    // 获取原始文件名并清理
    String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
    log.info("Storing file: {}, size: {} bytes", originalFileName, file.getSize());

    try {
      // 获取文件扩展名
      String fileExtension = getFileExtension(originalFileName);

      // 生成唯一文件名：UUID + 扩展名
      String fileName = generateUniqueFileName(fileExtension);

      // 构建完整的存储路径
      Path uploadPath = Paths.get(fileStorageProperties.getDir(), subDirectory);

      // 创建目录（如果不存在）
      createDirectoryIfNotExists(uploadPath);

      // 目标文件路径
      Path targetLocation = uploadPath.resolve(fileName);

      // 复制文件到目标位置
      try (InputStream inputStream = file.getInputStream()) {
        Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        log.info("File stored successfully: {}", targetLocation);
      }

      // 返回相对路径：subDirectory/fileName
      return subDirectory + "/" + fileName;

    } catch (IOException ex) {
      log.error("Failed to store file: {}", originalFileName, ex);
      throw new FileStorageException(
          "Could not store file " + originalFileName + ". Please try again!", ex);
    }
  }

  /**
   * 删除指定的文件
   *
   * @param fileName 要删除的文件名（相对路径）
   */
  @Override
  public void deleteFile(String fileName) {
    try {
      Path filePath = Paths.get(fileStorageProperties.getDir()).resolve(fileName).normalize();

      // 检查文件是否存在
      if (Files.exists(filePath)) {
        Files.delete(filePath);
        log.info("File deleted successfully: {}", fileName);
      } else {
        log.warn("File not found for deletion: {}", fileName);
      }

    } catch (IOException ex) {
      log.error("Failed to delete file: {}", fileName, ex);
      throw new FileStorageException("Could not delete file " + fileName, ex);
    }
  }

  /**
   * 加载文件为Resource资源
   *
   * @param fileName 文件名（相对路径）
   * @return 文件资源
   */
  @Override
  public Resource loadFileAsResource(String fileName) {
    try {
      Path filePath = Paths.get(fileStorageProperties.getDir())
          .resolve(fileName)
          .normalize();

      Resource resource = new UrlResource(filePath.toUri());

      if (resource.exists() && resource.isReadable()) {
        log.debug("File loaded successfully: {}", fileName);
        return resource;
      } else {
        throw new FileStorageException("File not found or not readable: " + fileName);
      }

    } catch (MalformedURLException ex) {
      log.error("File path is invalid: {}", fileName, ex);
      throw new FileStorageException("File path is invalid: " + fileName, ex);
    }
  }

  /**
   * 验证上传的文件
   *
   * @param file 要验证的文件
   */
  @Override
  public void validateFile(MultipartFile file) {
    // 1. 检查文件是否为空
    if (file == null || file.isEmpty()) {
      throw new FileValidationException("File is empty. Please select a file to upload");
    }

    // 2. 检查文件名
    String originalFileName = file.getOriginalFilename();
    if (originalFileName == null || originalFileName.trim().isEmpty()) {
      throw new FileValidationException("File name is invalid");
    }

    // 3. 检查文件名是否包含路径遍历字符
    if (originalFileName.contains("..")) {
      throw new FileValidationException(
          "File name contains invalid path sequence: " + originalFileName);
    }

    // 4. 获取文件扩展名
    String fileExtension = getFileExtension(originalFileName);
    if (fileExtension.isEmpty()) {
      throw new FileValidationException("File has no extension");
    }

    // 5. 验证文件扩展名
    if (!fileStorageProperties.isExtensionAllowed(fileExtension)) {
      throw new FileValidationException(
          "File extension '" + fileExtension + "' is not allowed. Allowed types: " +
              String.join(", ", fileStorageProperties.getAllowedExtensions()));
    }

    // 6. 验证MIME类型
    String contentType = file.getContentType();
    if (contentType == null || !fileStorageProperties.isMimeTypeAllowed(contentType)) {
      throw new FileValidationException(
          "File type '" + contentType + "' is not allowed. Allowed types: " +
              String.join(", ", fileStorageProperties.getAllowedMimeTypes()));
    }

    // 7. 验证文件大小
    if (!fileStorageProperties.isSizeValid(file.getSize())) {
      long maxSizeMB = fileStorageProperties.getMaxSize() / (1024 * 1024);
      throw new FileValidationException(
          "File size exceeds maximum allowed size of " + maxSizeMB + "MB");
    }

    log.debug("File validation passed: {}", originalFileName);
  }

  /**
   * 创建目录（如果不存在）
   *
   * @param path 目录路径
   */
  private void createDirectoryIfNotExists(Path path) {
    try {
      if (!Files.exists(path)) {
        Files.createDirectories(path);
        log.info("Created directory: {}", path);
      }
    } catch (IOException ex) {
      log.error("Failed to create directory: {}", path, ex);
      throw new FileStorageException("Could not create upload directory", ex);
    }
  }

  /**
   * 生成唯一文件名
   *
   * @param extension 文件扩展名
   * @return UUID格式的文件名
   */
  private String generateUniqueFileName(String extension) {
    String uuid = UUID.randomUUID().toString();
    return extension.isEmpty() ? uuid : uuid + "." + extension;
  }

  /**
   * 获取文件扩展名（不包含点）
   *
   * @param fileName 文件名
   * @return 文件扩展名（小写，不含点）
   */
  private String getFileExtension(String fileName) {
    if (fileName == null || fileName.isEmpty()) {
      return "";
    }

    int lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
      return "";
    }

    return fileName.substring(lastDotIndex + 1).toLowerCase();
  }
}