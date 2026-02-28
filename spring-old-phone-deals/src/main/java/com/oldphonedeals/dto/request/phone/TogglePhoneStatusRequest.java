package com.oldphonedeals.dto.request.phone;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ??/?????? DTO
 */
@Data
public class TogglePhoneStatusRequest {

  /**
   * 乐观锁版本号（可选）。
   */
  private Long version;

  /**
   * ??????
   */
  @NotNull(message = "isDisabled is required")
  private Boolean isDisabled;
}
