package com.oldphonedeals.dto.response.wishlist;

import com.oldphonedeals.dto.response.phone.PhoneListItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 收藏夹响应 DTO
 * 返回用户的收藏夹商品列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistResponse {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 收藏的商品列表（过滤掉已禁用的商品）
     */
    private List<PhoneListItemResponse> phones;

    /**
     * 收藏夹中的商品总数
     */
    private Integer totalItems;
}