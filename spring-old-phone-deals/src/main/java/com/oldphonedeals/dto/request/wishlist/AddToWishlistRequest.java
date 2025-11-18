package com.oldphonedeals.dto.request.wishlist;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加商品到收藏夹请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToWishlistRequest {

    /**
     * 商品ID
     */
    @NotBlank(message = "Phone ID is required")
    private String phoneId;
}