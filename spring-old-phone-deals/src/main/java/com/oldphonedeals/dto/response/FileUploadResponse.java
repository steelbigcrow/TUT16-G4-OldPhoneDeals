package com.oldphonedeals.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应 DTO
 * <p>
 * 用于返回文件上传成功后的详细信息，包括文件名、访问URL、
 * 原始文件名、文件大小和MIME类型。
 * </p>
 *
 * @author OldPhoneDeals Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponse {

  /**
   * 存储的文件名（UUID格式）
   * 例如：550e8400-e29b-41d4-a716-446655440000.jpg
   */
  private String fileName;

  /**
   * 文件访问URL
   * 例如：/uploads/images/550e8400-e29b-41d4-a716-446655440000.jpg
   */
  private String fileUrl;

  /**
   * 原始文件名
   * 例如：my-photo.jpg
   */
  private String originalName;

  /**
   * 文件大小（字节）
   */
  private Long size;

  /**
   * MIME类型
   * 例如：image/jpeg, image/png
   */
  private String contentType;
}