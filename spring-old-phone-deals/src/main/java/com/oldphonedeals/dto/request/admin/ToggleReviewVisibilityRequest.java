package com.oldphonedeals.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员切换评论可见性请求 DTO
 * 用于管理员隐藏或显示用户评论
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToggleReviewVisibilityRequest {

    /**
     * 是否隐藏评论
     */
    @NotNull(message = "Hidden status is required")
    private Boolean isHidden;
}