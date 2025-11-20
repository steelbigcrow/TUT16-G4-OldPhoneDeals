package com.oldphonedeals.dto.request.phone;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ??/?????? DTO
 */
@Data
public class TogglePhoneStatusRequest {

  /**
   * ??????
   */
  @NotNull(message = "isDisabled is required")
  private Boolean isDisabled;
}
