package com.oldphonedeals.service.impl;

import com.oldphonedeals.dto.response.phone.PhoneListItemResponse;
import com.oldphonedeals.dto.response.wishlist.WishlistResponse;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.security.SecurityContextHelper;
import com.oldphonedeals.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户收藏夹服务实现类
 * <p>
 * 实现用户收藏夹管理的业务逻辑，包括添加、删除和查看收藏的商品。
 * 所有操作都需要进行权限检查，确保用户只能操作自己的收藏夹。
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {
    
    private final UserRepository userRepository;
    private final PhoneRepository phoneRepository;
    
    /**
     * 添加商品到收藏夹
     * <p>
     * 流程：
     * 1. 权限检查
     * 2. 验证用户和商品存在
     * 3. 验证商品未禁用
     * 4. 检查商品是否已在收藏夹中（去重）
     * 5. 添加商品ID到用户的wishlist数组
     * 6. 返回更新后的收藏夹
     * </p>
     * 
     * @param userId 用户ID
     * @param phoneId 商品ID
     * @return 更新后的收藏夹
     * @throws ResourceNotFoundException 如果用户或商品不存在
     * @throws UnauthorizedException 如果无权限操作
     * @throws BadRequestException 如果商品已在收藏夹中或已禁用
     */
    @Override
    @Transactional
    public WishlistResponse addToWishlist(String userId, String phoneId) {
        log.info("Adding phone {} to wishlist for user {}", phoneId, userId);
        
        // 1. 权限检查：只能操作自己的收藏夹
        String currentUserId = SecurityContextHelper.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            log.warn("Unauthorized wishlist operation. Current user: {}, Target user: {}", 
                currentUserId, userId);
            throw new UnauthorizedException("You can only manage your own wishlist");
        }
        
        // 2. 查找用户
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // 3. 验证商品存在
        Phone phone = phoneRepository.findById(phoneId)
            .orElseThrow(() -> new ResourceNotFoundException("Phone not found"));
        
        // 4. 验证商品未禁用
        if (phone.getIsDisabled()) {
            throw new BadRequestException("Cannot add disabled phone to wishlist");
        }
        
        // 5. 检查商品是否已在收藏夹中（去重）
        if (user.getWishlist() != null && user.getWishlist().contains(phoneId)) {
            throw new BadRequestException("Phone already in wishlist");
        }
        
        // 6. 初始化wishlist（如果为null）
        if (user.getWishlist() == null) {
            user.setWishlist(new ArrayList<>());
        }
        
        // 7. 添加商品ID到收藏夹
        user.getWishlist().add(phoneId);
        userRepository.save(user);
        
        log.info("Phone {} added to wishlist for user {}", phoneId, userId);
        
        // 8. 返回更新后的收藏夹
        return buildWishlistResponse(user);
    }
    
    /**
     * 从收藏夹删除商品
     * <p>
     * 从用户的wishlist数组中移除指定的商品ID。
     * </p>
     * 
     * @param userId 用户ID
     * @param phoneId 商品ID
     * @return 更新后的收藏夹
     * @throws ResourceNotFoundException 如果用户不存在
     * @throws UnauthorizedException 如果无权限操作
     */
    @Override
    @Transactional
    public WishlistResponse removeFromWishlist(String userId, String phoneId) {
        log.info("Removing phone {} from wishlist for user {}", phoneId, userId);
        
        // 1. 权限检查：只能操作自己的收藏夹
        String currentUserId = SecurityContextHelper.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            log.warn("Unauthorized wishlist operation. Current user: {}, Target user: {}", 
                currentUserId, userId);
            throw new UnauthorizedException("You can only manage your own wishlist");
        }
        
        // 2. 查找用户
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // 3. 从收藏夹移除商品（如果存在）
        if (user.getWishlist() != null) {
            user.getWishlist().remove(phoneId);
            userRepository.save(user);
            log.info("Phone {} removed from wishlist for user {}", phoneId, userId);
        }
        
        // 4. 返回更新后的收藏夹
        return buildWishlistResponse(user);
    }
    
    /**
     * 获取用户收藏夹
     * <p>
     * 返回用户收藏夹中的所有商品，过滤掉已禁用的商品。
     * </p>
     * 
     * @param userId 用户ID
     * @return 用户的收藏夹
     * @throws ResourceNotFoundException 如果用户不存在
     * @throws UnauthorizedException 如果无权限查看
     */
    @Override
    public WishlistResponse getUserWishlist(String userId) {
        log.info("Getting wishlist for user {}", userId);
        
        // 1. 权限检查：只能查看自己的收藏夹
        String currentUserId = SecurityContextHelper.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            log.warn("Unauthorized wishlist access. Current user: {}, Target user: {}", 
                currentUserId, userId);
            throw new UnauthorizedException("You can only view your own wishlist");
        }
        
        // 2. 查找用户
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // 3. 返回收藏夹
        return buildWishlistResponse(user);
    }
    
    /**
     * 构建收藏夹响应对象
     * <p>
     * 将用户的wishlist中的商品ID转换为完整的商品信息，并过滤掉已禁用的商品。
     * </p>
     *
     * @param user 用户实体
     * @return 收藏夹响应DTO
     */
    private WishlistResponse buildWishlistResponse(User user) {
        List<PhoneListItemResponse> phoneResponses = new ArrayList<>();
        
        // 如果用户有收藏夹
        if (user.getWishlist() != null && !user.getWishlist().isEmpty()) {
            // 批量查询所有收藏的商品
            List<Phone> phones = phoneRepository.findAllById(user.getWishlist());
            
            // 转换为响应DTO并过滤已禁用的商品
            phoneResponses = phones.stream()
                .filter(phone -> !phone.getIsDisabled()) // 过滤已禁用的商品
                .map(this::convertToPhoneListItemResponse)
                .collect(Collectors.toList());
        }
        
        return WishlistResponse.builder()
            .userId(user.getId())
            .phones(phoneResponses)
            .totalItems(phoneResponses.size())
            .build();
    }
    
    /**
     * 将Phone实体转换为PhoneListItemResponse
     *
     * @param phone 商品实体
     * @return 商品列表项响应DTO
     */
    private PhoneListItemResponse convertToPhoneListItemResponse(Phone phone) {
        // 构建卖家信息
        PhoneListItemResponse.SellerInfo sellerInfo = null;
        if (phone.getSeller() != null) {
            sellerInfo = PhoneListItemResponse.SellerInfo.builder()
                .firstName(phone.getSeller().getFirstName())
                .lastName(phone.getSeller().getLastName())
                .build();
        }
        
        // 计算评论数量（排除隐藏的评论）
        int reviewCount = 0;
        if (phone.getReviews() != null) {
            reviewCount = (int) phone.getReviews().stream()
                .filter(review -> !review.getIsHidden())
                .count();
        }
        
        return PhoneListItemResponse.builder()
            .id(phone.getId())
            .title(phone.getTitle())
            .brand(phone.getBrand())
            .image(phone.getImage())
            .stock(phone.getStock())
            .price(phone.getPrice())
            .averageRating(phone.getAverageRating())
            .reviewCount(reviewCount)
            .seller(sellerInfo)
            .createdAt(phone.getCreatedAt())
            .build();
    }
}