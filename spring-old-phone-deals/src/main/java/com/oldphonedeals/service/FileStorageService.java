package com.oldphonedeals.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 * <p>
 * 提供文件上传、删除、加载和验证功能。
 * 主要用于处理图片等文件的存储操作。
 * </p>
 *
 * @author OldPhoneDeals Team
 */
public interface FileStorageService {

  /**
   * 存储上传的文件到指定子目录
   * <p>
   * 文件名生成策略：使用UUID + 原始文件扩展名，确保唯一性。
   * 例如：原始文件名为 "photo.jpg"，生成的文件名可能为 "550e8400-e29b-41d4-a716-446655440000.jpg"
   * </p>
   *
   * @param file 上传的文件
   * @param subDirectory 子目录名称（例如："images"）
   * @return 生成的文件名（不包含路径）
   * @throws com.oldphonedeals.exception.FileValidationException 如果文件验证失败
   * @throws com.oldphonedeals.exception.FileStorageException 如果文件存储失败
   */
  String storeFile(MultipartFile file, String subDirectory);

  /**
   * 删除指定的文件
   * <p>
   * 从文件系统中删除指定的文件。如果文件不存在，不会抛出异常。
   * </p>
   *
   * @param fileName 要删除的文件名（相对于上传目录的路径，例如："images/uuid.jpg"）
   * @throws com.oldphonedeals.exception.FileStorageException 如果删除操作失败
   */
  void deleteFile(String fileName);

  /**
   * 加载文件为Resource资源
   * <p>
   * 用于文件下载或访问。返回的Resource可以被Spring MVC用于文件响应。
   * </p>
   *
   * @param fileName 文件名（相对于上传目录的路径，例如："images/uuid.jpg"）
   * @return 文件资源
   * @throws com.oldphonedeals.exception.FileStorageException 如果文件不存在或无法读取
   */
  Resource loadFileAsResource(String fileName);

  /**
   * 验证上传的文件
   * <p>
   * 执行以下验证：
   * <ul>
   *   <li>文件不为空</li>
   *   <li>文件扩展名在允许列表中</li>
   *   <li>MIME类型在允许列表中</li>
   *   <li>文件大小未超过限制</li>
   *   <li>文件名不包含路径遍历字符（..）</li>
   * </ul>
   * </p>
   *
   * @param file 要验证的文件
   * @throws com.oldphonedeals.exception.FileValidationException 如果验证失败
   */
  void validateFile(MultipartFile file);
}