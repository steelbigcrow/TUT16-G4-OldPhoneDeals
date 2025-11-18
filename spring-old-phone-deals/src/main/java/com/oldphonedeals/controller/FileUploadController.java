package com.oldphonedeals.controller;

import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.FileUploadResponse;
import com.oldphonedeals.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 * <p>
 * 提供文件上传相关的REST API端点。支持图片文件上传，
 * 返回文件访问URL和详细信息。所有端点需要用户认证。
 * </p>
 * 
 * <h3>API端点列表：</h3>
 * <ul>
 *   <li>POST /api/upload/image - 上传图片文件（需要认证）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * POST /api/upload/image
 * Content-Type: multipart/form-data
 * Authorization: Bearer {JWT_TOKEN}
 * 
 * Form Data:
 * - file: [图片文件]
 * 
 * Response:
 * {
 *   "success": true,
 *   "message": "File uploaded successfully",
 *   "data": {
 *     "fileName": "550e8400-e29b-41d4-a716-446655440000.jpg",
 *     "fileUrl": "/uploads/images/550e8400-e29b-41d4-a716-446655440000.jpg",
 *     "originalName": "my-photo.jpg",
 *     "size": 102400,
 *     "contentType": "image/jpeg"
 *   }
 * }
 * </pre>
 * 
 * @author OldPhoneDeals Team
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

  private final FileStorageService fileStorageService;

  /**
   * 上传图片文件
   * <p>
   * 接收单个图片文件上传，验证文件类型和大小，存储到服务器，
   * 并返回文件访问信息。文件会被重命名为UUID格式以避免冲突。
   * </p>
   * 
   * <p>
   * <b>认证要求：</b> 需要有效的JWT令牌
   * </p>
   * 
   * <p>
   * <b>文件要求：</b>
   * <ul>
   *   <li>允许的格式: JPG, JPEG, PNG, GIF</li>
   *   <li>最大大小: 10MB</li>
   *   <li>必须是有效的图片文件</li>
   * </ul>
   * </p>
   *
   * @param file 上传的图片文件，参数名必须为"file"
   * @return 文件上传响应，包含文件名、访问URL、原始文件名、大小和MIME类型
   * @throws com.oldphonedeals.exception.FileValidationException 如果文件验证失败
   * @throws com.oldphonedeals.exception.FileStorageException 如果文件存储失败
   */
  @PostMapping("/image")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<FileUploadResponse>> uploadImage(
      @RequestParam("file") MultipartFile file
  ) {
    log.info("Image upload request received. Original filename: {}, size: {} bytes",
        file.getOriginalFilename(), file.getSize());

    // 存储文件到images子目录
    String fileName = fileStorageService.storeFile(file, "images");

    // 构建文件访问URL
    String fileUrl = "/uploads/" + fileName;

    // 构建响应
    FileUploadResponse response = FileUploadResponse.builder()
        .fileName(fileName)
        .fileUrl(fileUrl)
        .originalName(file.getOriginalFilename())
        .size(file.getSize())
        .contentType(file.getContentType())
        .build();

    log.info("Image uploaded successfully: {}", fileName);

    return ResponseEntity.ok(
        ApiResponse.success(response, "File uploaded successfully")
    );
  }
}