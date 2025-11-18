package com.oldphonedeals.service.impl;

import com.oldphonedeals.dto.request.cart.AddToCartRequest;
import com.oldphonedeals.dto.request.cart.UpdateCartItemRequest;
import com.oldphonedeals.dto.response.cart.CartItemResponse;
import com.oldphonedeals.dto.response.cart.CartResponse;
import com.oldphonedeals.entity.Cart;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.repository.CartRepository;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车服务实现
 * 
 * @author OldPhoneDeals Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    
    private final CartRepository cartRepository;
    private final PhoneRepository phoneRepository;
    private final UserRepository userRepository;
    
    @Override
    public CartResponse getUserCart(String userId) {
        log.debug("Getting cart for user: {}", userId);
        
        // 查找或创建购物车
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));
        
        // 构建增强的购物车响应（包含商品详情、评分、卖家信息）
        return buildCartResponse(cart);
    }
    
    @Override
    @Transactional
    public CartResponse addToCart(String userId, AddToCartRequest request) {
        log.debug("Adding item to cart - userId: {}, phoneId: {}, quantity: {}", 
                userId, request.getPhoneId(), request.getQuantity());
        
        // 验证商品
        Phone phone = phoneRepository.findById(request.getPhoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Phone not found"));
        
        if (phone.getIsDisabled()) {
            throw new BadRequestException("Phone is not available");
        }
        
        // 验证库存
        if (request.getQuantity() > phone.getStock()) {
            throw new BadRequestException("Insufficient stock. Available: " + phone.getStock());
        }
        
        // 查找或创建购物车
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));
        
        // 检查商品是否已在购物车中
        Cart.CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getPhoneId().equals(request.getPhoneId()))
                .findFirst()
                .orElse(null);
        
        if (existingItem != null) {
            // 更新现有商品的数量
            existingItem.setQuantity(request.getQuantity());
            existingItem.setPrice(phone.getPrice());
        } else {
            // 添加新商品
            Cart.CartItem newItem = Cart.CartItem.builder()
                    .phoneId(request.getPhoneId())
                    .title(phone.getTitle())
                    .quantity(request.getQuantity())
                    .price(phone.getPrice())
                    .createdAt(LocalDateTime.now())
                    .build();
            cart.getItems().add(newItem);
        }
        
        // 保存购物车
        cart = cartRepository.save(cart);
        log.info("Item added to cart - userId: {}, phoneId: {}", userId, request.getPhoneId());
        
        return buildCartResponse(cart);
    }
    
    @Override
    @Transactional
    public CartResponse updateCartItem(String userId, String phoneId, UpdateCartItemRequest request) {
        log.debug("Updating cart item - userId: {}, phoneId: {}, newQuantity: {}", 
                userId, phoneId, request.getQuantity());
        
        // 查找购物车
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        
        // 查找购物车中的商品
        Cart.CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getPhoneId().equals(phoneId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));
        
        // 验证商品和库存
        Phone phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Phone not found"));
        
        if (request.getQuantity() > phone.getStock()) {
            throw new BadRequestException("Insufficient stock. Available: " + phone.getStock());
        }
        
        // 更新数量
        cartItem.setQuantity(request.getQuantity());
        
        // 保存购物车
        cart = cartRepository.save(cart);
        log.info("Cart item updated - userId: {}, phoneId: {}", userId, phoneId);
        
        return buildCartResponse(cart);
    }
    
    @Override
    @Transactional
    public CartResponse removeFromCart(String userId, String phoneId) {
        log.debug("Removing item from cart - userId: {}, phoneId: {}", userId, phoneId);
        
        // 查找购物车
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        
        // 查找并移除商品
        boolean removed = cart.getItems().removeIf(item -> item.getPhoneId().equals(phoneId));
        
        if (!removed) {
            throw new ResourceNotFoundException("Item not found in cart");
        }
        
        // 保存购物车
        cart = cartRepository.save(cart);
        log.info("Item removed from cart - userId: {}, phoneId: {}", userId, phoneId);
        
        return buildCartResponse(cart);
    }
    
    @Override
    @Transactional
    public void clearCart(String userId) {
        log.debug("Clearing cart for user: {}", userId);
        
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        
        cart.getItems().clear();
        cartRepository.save(cart);
        
        log.info("Cart cleared for user: {}", userId);
    }
    
    /**
     * 创建空购物车
     */
    private Cart createEmptyCart(String userId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return cartRepository.save(cart);
    }
    
    /**
     * 构建购物车响应，包含增强的商品信息
     * <p>
     * 增强信息包括：
     * - 商品的平均评分
     * - 商品的评论数量
     * - 卖家信息（firstName, lastName）
     * </p>
     */
    private CartResponse buildCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::buildCartItemResponse)
                .collect(Collectors.toList());
        
        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
    
    /**
     * 构建购物车商品响应，包含增强信息
     */
    private CartItemResponse buildCartItemResponse(Cart.CartItem item) {
        CartItemResponse.CartItemResponseBuilder builder = CartItemResponse.builder()
                .phoneId(item.getPhoneId())
                .title(item.getTitle())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .createdAt(item.getCreatedAt());
        
        // 获取商品详情以增强信息
        phoneRepository.findById(item.getPhoneId()).ifPresent(phone -> {
            // 计算平均评分
            Double averageRating = phone.getAverageRating();
            builder.averageRating(averageRating);
            
            // 获取评论数量（排除隐藏的评论）
            Integer reviewCount = 0;
            if (phone.getReviews() != null) {
                reviewCount = (int) phone.getReviews().stream()
                        .filter(review -> !review.getIsHidden())
                        .count();
            }
            builder.reviewCount(reviewCount);
            
            // 获取卖家信息
            if (phone.getSeller() != null) {
                User seller = phone.getSeller();
                CartItemResponse.SellerInfo sellerInfo = CartItemResponse.SellerInfo.builder()
                        .id(seller.getId())
                        .firstName(seller.getFirstName())
                        .lastName(seller.getLastName())
                        .build();
                builder.seller(sellerInfo);
            }
            
            // 构建商品信息
            CartItemResponse.PhoneInfo phoneInfo = CartItemResponse.PhoneInfo.builder()
                    .id(phone.getId())
                    .title(phone.getTitle())
                    .brand(phone.getBrand())
                    .image(phone.getImage())
                    .stock(phone.getStock())
                    .price(phone.getPrice())
                    .isDisabled(phone.getIsDisabled())
                    .build();
            builder.phone(phoneInfo);
        });
        
        return builder.build();
    }
}
