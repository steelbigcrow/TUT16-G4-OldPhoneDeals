package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.cart.AddToCartRequest;
import com.oldphonedeals.dto.request.cart.UpdateCartItemRequest;
import com.oldphonedeals.dto.response.cart.CartResponse;

/**
 * 购物车服务接口
 * <p>
 * 提供购物车的增删改查功能，包括：
 * - 获取用户购物车
 * - 添加商品到购物车
 * - 更新购物车商品数量
 * - 从购物车删除商品
 * - 清空购物车
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
public interface CartService {
    
    /**
     * 获取用户购物车
     * <p>
     * 如果购物车不存在，将自动创建一个空购物车
     * </p>
     * 
     * @param userId 用户ID
     * @return 购物车响应对象，包含购物车商品列表及增强信息（评分、卖家等）
     */
    CartResponse getUserCart(String userId);
    
    /**
     * 添加商品到购物车
     * <p>
     * 业务规则：
     * - 验证商品存在且未禁用
     * - 验证库存充足（quantity <= phone.stock）
     * - 如果商品已在购物车中，则增加数量
     * - 如果是新商品，添加到购物车items数组
     * </p>
     * 
     * @param userId 用户ID
     * @param request 添加到购物车请求（包含phoneId和quantity）
     * @return 更新后的购物车响应对象
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 商品不存在
     * @throws com.oldphonedeals.exception.BadRequestException 商品已禁用或库存不足
     */
    CartResponse addToCart(String userId, AddToCartRequest request);
    
    /**
     * 更新购物车商品数量
     * <p>
     * 业务规则：
     * - 验证新数量不超过库存
     * - 更新指定商品的数量
     * </p>
     * 
     * @param userId 用户ID
     * @param phoneId 商品ID
     * @param request 更新购物车商品请求（包含新数量）
     * @return 更新后的购物车响应对象
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 购物车或商品不存在
     * @throws com.oldphonedeals.exception.BadRequestException 库存不足
     */
    CartResponse updateCartItem(String userId, String phoneId, UpdateCartItemRequest request);
    
    /**
     * 从购物车删除商品
     * <p>
     * 从购物车items数组中移除指定商品
     * </p>
     * 
     * @param userId 用户ID
     * @param phoneId 商品ID
     * @return 更新后的购物车响应对象
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 购物车或商品不存在
     */
    CartResponse removeFromCart(String userId, String phoneId);
    
    /**
     * 清空购物车
     * <p>
     * 用于结账后清空购物车items数组
     * </p>
     * 
     * @param userId 用户ID
     */
    void clearCart(String userId);
}