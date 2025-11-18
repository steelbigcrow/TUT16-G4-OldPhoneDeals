package com.oldphonedeals.controller;

import com.oldphonedeals.dto.request.cart.AddToCartRequest;
import com.oldphonedeals.dto.request.cart.UpdateCartItemRequest;
import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.cart.CartResponse;
import com.oldphonedeals.security.SecurityContextHelper;
import com.oldphonedeals.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 购物车控制器
 * <p>
 * 实现购物车的增删改查功能，对应4个API端点：
 * - GET /api/cart - 获取购物车
 * - POST /api/cart - 添加商品到购物车
 * - PUT /api/cart/{phoneId} - 更新购物车商品数量
 * - DELETE /api/cart/{phoneId} - 从购物车删除商品
 * </p>
 * 
 * 参考 Express.js 实现：
 * - server/app/routes/cart.routes.js
 * - server/app/controllers/cart.controller.js
 * 
 * @author OldPhoneDeals Team
 */
@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    
    private final CartService cartService;
    
    /**
     * 获取购物车
     * <p>
     * GET /api/cart
     * 需要认证
     * </p>
     * 
     * 参考：server/app/controllers/cart.controller.js:11-63
     * 
     * @return 购物车响应对象，包含商品列表及增强信息（评分、卖家等）
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        String userId = SecurityContextHelper.getCurrentUserId();
        log.info("GET /api/cart - Getting cart for user: {}", userId);
        
        CartResponse response = cartService.getUserCart(userId);
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "Cart retrieved successfully")
        );
    }
    
    /**
     * 添加商品到购物车
     * <p>
     * POST /api/cart
     * 需要认证
     * </p>
     * 
     * 业务逻辑：
     * - 验证商品存在且未禁用
     * - 验证库存充足
     * - 如果商品已在购物车中，更新数量
     * - 如果是新商品，添加到购物车
     * 
     * 参考：server/app/controllers/cart.controller.js:69-120
     * 
     * @param request 添加到购物车请求（phoneId, quantity）
     * @return 更新后的购物车响应对象
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request
    ) {
        String userId = SecurityContextHelper.getCurrentUserId();
        log.info("POST /api/cart - Adding item to cart - userId: {}, phoneId: {}, quantity: {}", 
                userId, request.getPhoneId(), request.getQuantity());
        
        CartResponse response = cartService.addToCart(userId, request);
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "Item added to cart successfully")
        );
    }
    
    /**
     * 更新购物车商品数量
     * <p>
     * PUT /api/cart/{phoneId}
     * 需要认证
     * </p>
     * 
     * 业务逻辑：
     * - 验证商品在购物车中存在
     * - 验证新数量不超过库存
     * - 更新数量
     * 
     * 参考：server/app/controllers/cart.controller.js:126-168
     * 
     * @param phoneId 商品ID
     * @param request 更新购物车商品请求（quantity）
     * @return 更新后的购物车响应对象
     */
    @PutMapping("/{phoneId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable String phoneId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        String userId = SecurityContextHelper.getCurrentUserId();
        log.info("PUT /api/cart/{} - Updating cart item - userId: {}, newQuantity: {}", 
                phoneId, userId, request.getQuantity());
        
        CartResponse response = cartService.updateCartItem(userId, phoneId, request);
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "Cart item updated successfully")
        );
    }
    
    /**
     * 从购物车删除商品
     * <p>
     * DELETE /api/cart/{phoneId}
     * 需要认证
     * </p>
     * 
     * 业务逻辑：
     * - 从购物车items数组中移除指定商品
     * 
     * 参考：server/app/controllers/cart.controller.js:174-203
     * 
     * @param phoneId 商品ID
     * @return 更新后的购物车响应对象
     */
    @DeleteMapping("/{phoneId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable String phoneId
    ) {
        String userId = SecurityContextHelper.getCurrentUserId();
        log.info("DELETE /api/cart/{} - Removing item from cart - userId: {}", phoneId, userId);
        
        CartResponse response = cartService.removeFromCart(userId, phoneId);
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "Item removed from cart successfully")
        );
    }
}