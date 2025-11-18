package com.oldphonedeals.service.impl;

import com.oldphonedeals.dto.request.phone.PhoneCreateRequest;
import com.oldphonedeals.dto.request.phone.PhoneUpdateRequest;
import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.PageResponse;
import com.oldphonedeals.dto.response.phone.PhoneListItemResponse;
import com.oldphonedeals.dto.response.phone.PhoneResponse;
import com.oldphonedeals.dto.response.phone.ReviewResponse;
import com.oldphonedeals.entity.Cart;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.repository.CartRepository;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.service.FileStorageService;
import com.oldphonedeals.service.PhoneService;
import com.oldphonedeals.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 手机商品业务服务实现
 * 第1部分：实现基础CRUD功能
 * 
 * 参考 Express.js 实现：
 * - server/app/controllers/phone.controller.js
 */
@Slf4j
@Service
public class PhoneServiceImpl implements PhoneService {

  @Autowired
  private PhoneRepository phoneRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CartRepository cartRepository;

  @Autowired
  private FileStorageService fileStorageService;

  @Autowired
  private ReviewService reviewService;

  @Autowired
  private MongoTemplate mongoTemplate;

  /**
   * 创建手机商品
   * 参考：server/app/controllers/phone.controller.js:11-60
   * 
   * @param request 创建请求
   * @param sellerId 卖家ID（从JWT获取）
   * @return 新创建的商品信息
   */
  @Override
  @Transactional
  public PhoneResponse createPhone(PhoneCreateRequest request, String sellerId) {
    log.info("Creating new phone: {} by seller: {}", request.getTitle(), sellerId);

    // 验证卖家存在
    User seller = userRepository.findById(sellerId)
        .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

    // 创建商品实体
    Phone phone = Phone.builder()
        .title(request.getTitle())
        .brand(request.getBrand())
        .image(request.getImage())
        .stock(request.getStock())
        .price(request.getPrice())
        .seller(seller)
        .reviews(new ArrayList<>()) // 初始化为空列表
        .isDisabled(false)
        .salesCount(0)
        .build();

    // 保存商品
    Phone savedPhone = phoneRepository.save(phone);

    log.info("Phone created successfully with id: {}", savedPhone.getId());

    // 转换为响应DTO
    return convertToPhoneResponse(savedPhone);
  }

  /**
   * 更新手机商品
   * 参考：server/app/controllers/phone.controller.js:62-104
   * 
   * @param phoneId 商品ID
   * @param request 更新请求
   * @param sellerId 卖家ID（从JWT获取）
   * @return 更新后的商品信息
   */
  @Override
  @Transactional
  public PhoneResponse updatePhone(String phoneId, PhoneUpdateRequest request, String sellerId) {
    log.info("Updating phone: {} by seller: {}", phoneId, sellerId);

    // 验证商品存在
    Phone phone = phoneRepository.findById(phoneId)
        .orElseThrow(() -> new ResourceNotFoundException("Phone not found with id: " + phoneId));

    // 权限检查：只有商品的seller可以更新
    if (!phone.getSeller().getId().equals(sellerId)) {
      throw new UnauthorizedException("You are not authorized to update this phone");
    }

    // 更新字段（只更新非null的字段）
    if (request.getTitle() != null) {
      phone.setTitle(request.getTitle());
    }
    if (request.getBrand() != null) {
      phone.setBrand(request.getBrand());
    }
    if (request.getImage() != null) {
      // 如果提供了新图片，删除旧图片
      if (phone.getImage() != null && !phone.getImage().equals(request.getImage())) {
        try {
          fileStorageService.deleteFile(phone.getImage());
        } catch (Exception e) {
          log.warn("Failed to delete old image: {}", phone.getImage(), e);
        }
      }
      phone.setImage(request.getImage());
    }
    if (request.getStock() != null) {
      phone.setStock(request.getStock());
    }
    if (request.getPrice() != null) {
      phone.setPrice(request.getPrice());
    }
    if (request.getIsDisabled() != null) {
      phone.setIsDisabled(request.getIsDisabled());
    }

    // 保存更新
    Phone updatedPhone = phoneRepository.save(phone);

    log.info("Phone updated successfully: {}", phoneId);

    // 转换为响应DTO
    return convertToPhoneResponse(updatedPhone);
  }

  /**
   * 删除手机商品
   * 参考：server/app/controllers/phone.controller.js:106-133
   * 
   * @param phoneId 商品ID
   * @param sellerId 卖家ID（从JWT获取）
   */
  @Override
  @Transactional
  public void deletePhone(String phoneId, String sellerId) {
    log.info("Deleting phone: {} by seller: {}", phoneId, sellerId);

    // 验证商品存在
    Phone phone = phoneRepository.findById(phoneId)
        .orElseThrow(() -> new ResourceNotFoundException("Phone not found with id: " + phoneId));

    // 权限检查：只有商品的seller可以删除
    if (!phone.getSeller().getId().equals(sellerId)) {
      throw new UnauthorizedException("You are not authorized to delete this phone");
    }

    // 1. 删除商品图片
    if (phone.getImage() != null) {
      try {
        fileStorageService.deleteFile(phone.getImage());
        log.info("Deleted phone image: {}", phone.getImage());
      } catch (Exception e) {
        log.warn("Failed to delete phone image: {}", phone.getImage(), e);
      }
    }

    // 2. 从所有用户的购物车中移除该商品
    List<Cart> affectedCarts = cartRepository.findCartsContainingPhone(phoneId);
    for (Cart cart : affectedCarts) {
      cart.getItems().removeIf(item -> item.getPhoneId().equals(phoneId));
      cartRepository.save(cart);
    }
    log.info("Removed phone from {} carts", affectedCarts.size());

    // 3. 从所有用户的收藏夹中移除该商品
    List<User> usersWithWishlist = userRepository.findAll().stream()
        .filter(user -> user.getWishlist() != null && user.getWishlist().contains(phoneId))
        .collect(Collectors.toList());
    
    for (User user : usersWithWishlist) {
      user.getWishlist().remove(phoneId);
      userRepository.save(user);
    }
    log.info("Removed phone from {} wishlists", usersWithWishlist.size());

    // 4. 删除商品
    phoneRepository.delete(phone);

    log.info("Phone deleted successfully: {}", phoneId);
  }

  /**
   * 获取单个商品详情
   * 参考：server/app/controllers/phone.controller.js:135-176
   *
   * @param phoneId 商品ID
   * @param currentUserId 当前用户ID（可选）
   * @return 商品详情
   */
  @Override
  public PhoneResponse getPhoneById(String phoneId, String currentUserId) {
    log.info("Fetching phone details: {} for user: {}", phoneId, currentUserId);

    // 查询商品
    Phone phone = phoneRepository.findById(phoneId)
        .orElseThrow(() -> new ResourceNotFoundException("Phone not found with id: " + phoneId));

    // 使用 ReviewService 过滤可见评论
    List<ReviewResponse> visibleReviews = reviewService.filterVisibleReviews(
        phone.getReviews(),
        currentUserId,
        phone.getSeller().getId()
    );

    // 只取前 3 条评论（按创建时间倒序已在 filterVisibleReviews 中处理）
    List<ReviewResponse> top3Reviews = visibleReviews.stream()
        .limit(3)
        .collect(Collectors.toList());

    // 构建响应
    return PhoneResponse.builder()
        .id(phone.getId())
        .title(phone.getTitle())
        .brand(phone.getBrand())
        .image(phone.getImage())
        .stock(phone.getStock())
        .price(phone.getPrice())
        .isDisabled(phone.getIsDisabled())
        .salesCount(phone.getSalesCount())
        .averageRating(phone.getAverageRating())
        .seller(convertToSellerInfo(phone.getSeller()))
        .reviews(top3Reviews)
        .createdAt(phone.getCreatedAt())
        .updatedAt(phone.getUpdatedAt())
        .build();
  }

  /**
   * 获取所有商品（分页）
   * 参考：server/app/controllers/phone.controller.js:178-234
   * 
   * @param search 搜索关键词
   * @param brand 品牌过滤
   * @param maxPrice 最高价格过滤
   * @param sortBy 排序字段
   * @param sortOrder 排序方向
   * @param page 页码
   * @param limit 每页数量
   * @return 包含商品列表、分页信息的Map
   */
  @Override
  public Map<String, Object> getPhones(
      String search,
      PhoneBrand brand,
      Double maxPrice,
      String sortBy,
      String sortOrder,
      Integer page,
      Integer limit
  ) {
    // 设置默认值
    int pageNum = (page != null && page > 0) ? page - 1 : 0; // Spring Data页码从0开始
    int pageSize = (limit != null && limit > 0) ? limit : 12;
    String sortField = (sortBy != null && !sortBy.isEmpty()) ? sortBy : "createdAt";
    Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;

    // 创建分页和排序对象
    Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(direction, sortField));

    // 根据不同的过滤条件组合查询商品
    Page<Phone> phonePage;
    
    boolean hasSearch = search != null && !search.trim().isEmpty();
    boolean hasBrand = brand != null;
    boolean hasMaxPrice = maxPrice != null && maxPrice > 0;
    
    if (hasSearch && hasBrand && hasMaxPrice) {
      // 搜索 + 品牌 + 价格
      phonePage = phoneRepository.findByTitleAndBrandAndPriceLessThanEqual(
          search.trim(), brand, maxPrice, pageable);
    } else if (hasSearch && hasBrand) {
      // 搜索 + 品牌
      phonePage = phoneRepository.findByTitleAndBrand(search.trim(), brand, pageable);
    } else if (hasSearch && hasMaxPrice) {
      // 搜索 + 价格
      phonePage = phoneRepository.findByTitleAndPriceLessThanEqual(
          search.trim(), maxPrice, pageable);
    } else if (hasBrand && hasMaxPrice) {
      // 品牌 + 价格
      phonePage = phoneRepository.findByBrandAndPriceLessThanEqualAndIsDisabledFalse(
          brand, maxPrice, pageable);
    } else if (hasSearch) {
      // 仅搜索
      phonePage = phoneRepository.searchByTitle(search.trim(), pageable);
    } else if (hasBrand) {
      // 仅品牌
      phonePage = phoneRepository.findByBrandAndIsDisabledFalse(brand, pageable);
    } else if (hasMaxPrice) {
      // 仅价格
      phonePage = phoneRepository.findByPriceLessThanEqualAndIsDisabledFalse(maxPrice, pageable);
    } else {
      // 无过滤条件
      phonePage = phoneRepository.findByIsDisabledFalse(pageable);
    }

    // 转换为响应DTO
    Page<PhoneListItemResponse> responsePage = phonePage.map(this::convertToPhoneListItemResponse);

    // 返回Map格式（兼容Express.js响应格式）
    return Map.of(
        "phones", responsePage.getContent(),
        "currentPage", responsePage.getNumber() + 1, // 转换为从1开始
        "totalPages", responsePage.getTotalPages(),
        "total", responsePage.getTotalElements()
    );
  }

  // ==================== 第2部分：复杂查询方法 ====================

  /**
   * 获取低库存商品
   * 参考：server/app/controllers/phone.controller.js:56-64
   *
   * @return 低库存商品列表（最多6个）
   */
  @Override
  public List<PhoneListItemResponse> getSoldOutSoonPhones() {
    log.info("Fetching sold-out-soon phones");

    // 查询库存 <= 5 的未禁用商品，按库存升序排序，取前6个
    Pageable pageable = PageRequest.of(0, 6);
    List<Phone> phones = phoneRepository.findByStockLessThanEqualAndIsDisabledFalseOrderByStockAsc(5, pageable);

    // 转换为响应DTO
    return phones.stream()
        .map(this::convertToPhoneListItemResponse)
        .collect(Collectors.toList());
  }

  /**
   * 获取畅销商品
   * 参考：server/app/controllers/phone.controller.js:66-98
   *
   * 实现逻辑：
   * 1. 筛选至少有2条评论的未禁用商品
   * 2. 计算平均评分（包含隐藏评论）
   * 3. 按平均评分降序排序
   * 4. 取前10个
   *
   * @return 畅销商品列表（最多10个）
   */
  @Override
  public List<PhoneListItemResponse> getBestSellers() {
    log.info("Fetching best sellers");

    // 1. 获取所有未禁用的商品
    List<Phone> allPhones = phoneRepository.findAll().stream()
        .filter(phone -> !phone.getIsDisabled())
        .collect(Collectors.toList());

    // 2. 筛选至少有2条评论的商品，计算平均评分并排序
    List<Phone> bestSellers = allPhones.stream()
        .filter(phone -> phone.getReviews() != null && phone.getReviews().size() >= 2)
        .sorted((p1, p2) -> {
          // 计算平均评分（包含所有评论，包括隐藏的）
          double avgRating1 = p1.getReviews().stream()
              .mapToInt(Phone.Review::getRating)
              .average()
              .orElse(0.0);
          double avgRating2 = p2.getReviews().stream()
              .mapToInt(Phone.Review::getRating)
              .average()
              .orElse(0.0);
          return Double.compare(avgRating2, avgRating1); // 降序
        })
        .limit(10)
        .collect(Collectors.toList());

    // 3. 转换为响应DTO
    return bestSellers.stream()
        .map(this::convertToPhoneListItemResponse)
        .collect(Collectors.toList());
  }

  /**
   * 启用/禁用商品
   * 参考：server/app/controllers/phone.controller.js:546-584
   *
   * @param phoneId 商品ID
   * @param isDisabled 是否禁用
   * @param sellerId 卖家ID（从JWT获取）
   * @return 操作结果消息
   */
  @Override
  @Transactional
  public ApiResponse<String> togglePhoneDisabled(String phoneId, Boolean isDisabled, String sellerId) {
    log.info("Toggling phone disabled status: phoneId={}, isDisabled={}, sellerId={}",
        phoneId, isDisabled, sellerId);

    // 1. 验证商品存在
    Phone phone = phoneRepository.findById(phoneId)
        .orElseThrow(() -> new ResourceNotFoundException("Phone not found with id: " + phoneId));

    // 2. 权限检查：只有商品的seller可以操作
    if (!phone.getSeller().getId().equals(sellerId)) {
      throw new UnauthorizedException("You are not authorized to update this phone");
    }

    // 3. 更新状态
    phone.setIsDisabled(isDisabled);
    phoneRepository.save(phone);

    String message = isDisabled ? "Phone disabled successfully" : "Phone enabled successfully";
    log.info(message + ": {}", phoneId);

    return ApiResponse.success(message);
  }

  /**
   * 获取卖家的所有商品
   * 参考：server/app/controllers/phone.controller.js:486-514
   *
   * @param sellerId 卖家ID
   * @return 该卖家的所有商品列表
   */
  @Override
  public List<PhoneResponse> getPhonesBySeller(String sellerId) {
    log.info("Fetching phones by seller: {}", sellerId);

    // 验证卖家存在
    User seller = userRepository.findById(sellerId)
        .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

    // 查询该卖家的所有商品（包含禁用的）
    List<Phone> phones = phoneRepository.findBySellerId(sellerId);

    // 转换为响应DTO
    return phones.stream()
        .map(this::convertToPhoneResponse)
        .collect(Collectors.toList());
  }

  @Override
  public Map<String, Object> adminGetPhones(
      String searchTerm,
      PhoneBrand brand,
      String sellerId,
      Integer page,
      Integer limit,
      String sortBy,
      String sortOrder
  ) {
    throw new UnsupportedOperationException("This method will be implemented in Part 2");
  }

  @Override
  public void adminUpdatePhone(String phoneId, PhoneUpdateRequest request, String adminId) {
    throw new UnsupportedOperationException("This method will be implemented in Part 2");
  }

  @Override
  public void adminDeletePhone(String phoneId, String adminId) {
    throw new UnsupportedOperationException("This method will be implemented in Part 2");
  }

  // ==================== 辅助方法：DTO转换 ====================

  /**
   * 转换Phone实体为PhoneResponse
   */
  private PhoneResponse convertToPhoneResponse(Phone phone) {
    // 转换评论列表
    List<ReviewResponse> reviewResponses = phone.getReviews() != null
        ? phone.getReviews().stream()
            .map(this::convertToReviewResponse)
            .collect(Collectors.toList())
        : new ArrayList<>();

    return PhoneResponse.builder()
        .id(phone.getId())
        .title(phone.getTitle())
        .brand(phone.getBrand())
        .image(phone.getImage())
        .stock(phone.getStock())
        .price(phone.getPrice())
        .isDisabled(phone.getIsDisabled())
        .salesCount(phone.getSalesCount())
        .averageRating(phone.getAverageRating())
        .seller(convertToSellerInfo(phone.getSeller()))
        .reviews(reviewResponses)
        .createdAt(phone.getCreatedAt())
        .updatedAt(phone.getUpdatedAt())
        .build();
  }

  /**
   * 转换Phone实体为PhoneListItemResponse（简化版）
   */
  private PhoneListItemResponse convertToPhoneListItemResponse(Phone phone) {
    return PhoneListItemResponse.builder()
        .id(phone.getId())
        .title(phone.getTitle())
        .brand(phone.getBrand())
        .image(phone.getImage())
        .stock(phone.getStock())
        .price(phone.getPrice())
        .averageRating(phone.getAverageRating())
        .reviewCount(phone.getReviews() != null ? phone.getReviews().size() : 0)
        .seller(PhoneListItemResponse.SellerInfo.builder()
            .firstName(phone.getSeller().getFirstName())
            .lastName(phone.getSeller().getLastName())
            .build())
        .createdAt(phone.getCreatedAt())
        .build();
  }

  /**
   * 转换User为SellerInfo
   */
  private PhoneResponse.SellerInfo convertToSellerInfo(User seller) {
    return PhoneResponse.SellerInfo.builder()
        .id(seller.getId())
        .firstName(seller.getFirstName())
        .lastName(seller.getLastName())
        .build();
  }

  /**
   * 转换Phone.Review为ReviewResponse
   */
  private ReviewResponse convertToReviewResponse(Phone.Review review) {
    // 注意：这里简化处理，实际评论者姓名需要从User表查询
    // 第2部分实现ReviewService时会完善此逻辑
    return ReviewResponse.builder()
        .id(review.getId())
        .reviewerId(review.getReviewerId())
        .rating(review.getRating())
        .comment(review.getComment())
        .isHidden(review.getIsHidden())
        .reviewer("User") // 占位符，第2部分实现
        .createdAt(review.getCreatedAt())
        .build();
  }
}
