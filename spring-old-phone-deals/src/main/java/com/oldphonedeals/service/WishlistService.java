package com.oldphonedeals.service;

import com.oldphonedeals.dto.response.wishlist.WishlistResponse;

/**
 * 用户收藏夹服务接口
 * <p>
 * 提供用户收藏夹相关功能，包括：
 * - 添加商品到收藏夹
 * - 从收藏夹删除商品
 * - 获取用户收藏夹
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
public interface WishlistService {
    
    /**
     * 添加商品到收藏夹
     * <p>
     * 验证商品是否存在且未禁用，检查是否已在收藏夹中（去重）。
     * 权限检查：只能操作自己的收藏夹。
     * </p>
     * 
     * @param userId 用户ID
     * @param phoneId 商品ID
     * @return 更新后的收藏夹
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 如果用户或商品不存在
     * @throws com.oldphonedeals.exception.UnauthorizedException 如果无权限操作
     * @throws com.oldphonedeals.exception.BadRequestException 如果商品已在收藏夹中或已禁用
     */
    WishlistResponse addToWishlist(String userId, String phoneId);
    
    /**
     * 从收藏夹删除商品
     * <p>
     * 从用户的收藏夹中移除指定商品。
     * 权限检查：只能操作自己的收藏夹。
     * </p>
     * 
     * @param userId 用户ID
     * @param phoneId 商品ID
     * @return 更新后的收藏夹
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 如果用户不存在
     * @throws com.oldphonedeals.exception.UnauthorizedException 如果无权限操作
     */
    WishlistResponse removeFromWishlist(String userId, String phoneId);
    
    /**
     * 获取用户收藏夹
     * <p>
     * 返回用户收藏夹中的所有商品（过滤掉已禁用的商品）。
     * 权限检查：只能查看自己的收藏夹。
     * </p>
     * 
     * @param userId 用户ID
     * @return 用户的收藏夹
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 如果用户不存在
     * @throws com.oldphonedeals.exception.UnauthorizedException 如果无权限查看
     */
    WishlistResponse getUserWishlist(String userId);
}