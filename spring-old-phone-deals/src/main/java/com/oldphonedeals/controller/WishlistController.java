package com.oldphonedeals.controller;

import com.oldphonedeals.dto.request.wishlist.AddToWishlistRequest;
import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.wishlist.WishlistResponse;
import com.oldphonedeals.security.SecurityContextHelper;
import com.oldphonedeals.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户收藏夹控制器
 * <p>
 * 提供用户收藏夹管理的REST API端点，包括添加商品、删除商品和查看收藏夹。
 * 所有端点都需要JWT认证，且用户只能操作自己的收藏夹。
 * </p>
 * 
 * <h3>API端点列表：</h3>
 * <ul>
 *   <li>POST /api/wishlist - 添加商品到收藏夹</li>
 *   <li>DELETE /api/wishlist/{phoneId} - 从收藏夹删除商品</li>
 *   <li>GET /api/wishlist - 获取用户收藏夹</li>
 * </ul>
 * 
 * @author OldPhoneDeals Team
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Slf4j
public class WishlistController {
    
    private final WishlistService wishlistService;
    
    /**
     * 添加商品到收藏夹
     * <p>
     * 验证商品存在且未禁用，检查是否已在收藏夹中（去重）。
     * 需要JWT认证，用户只能操作自己的收藏夹。
     * </p>
     * 
     * @param request 添加到收藏夹请求（包含phoneId）
     * @return 更新后的收藏夹
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(
        @Valid @RequestBody AddToWishlistRequest request
    ) {
        // 从安全上下文获取当前用户ID
        String userId = SecurityContextHelper.getCurrentUserId();
        log.info("Add to wishlist request. User: {}, Phone: {}", userId, request.getPhoneId());
        
        WishlistResponse response = wishlistService.addToWishlist(userId, request.getPhoneId());
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "Phone added to wishlist successfully")
        );
    }
    
    /**
     * 从收藏夹删除商品
     * <p>
     * 从用户的收藏夹中移除指定商品。
     * 需要JWT认证，用户只能操作自己的收藏夹。
     * </p>
     * 
     * @param phoneId 商品ID
     * @return 更新后的收藏夹
     */
    @DeleteMapping("/{phoneId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WishlistResponse>> removeFromWishlist(
        @PathVariable String phoneId
    ) {
        // 从安全上下文获取当前用户ID
        String userId = SecurityContextHelper.getCurrentUserId();
        log.info("Remove from wishlist request. User: {}, Phone: {}", userId, phoneId);
        
        WishlistResponse response = wishlistService.removeFromWishlist(userId, phoneId);
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "Phone removed from wishlist successfully")
        );
    }
    
    /**
     * 获取用户收藏夹
     * <p>
     * 返回用户收藏夹中的所有商品（过滤掉已禁用的商品）。
     * 需要JWT认证，用户只能查看自己的收藏夹。
     * </p>
     * 
     * @return 用户的收藏夹
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WishlistResponse>> getUserWishlist() {
        // 从安全上下文获取当前用户ID
        String userId = SecurityContextHelper.getCurrentUserId();
        log.info("Get wishlist request for user: {}", userId);
        
        WishlistResponse response = wishlistService.getUserWishlist(userId);
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "Wishlist retrieved successfully")
        );
    }
}