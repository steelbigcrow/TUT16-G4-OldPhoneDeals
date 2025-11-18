package com.oldphonedeals.controller;

import com.oldphonedeals.dto.request.phone.PhoneCreateRequest;
import com.oldphonedeals.dto.request.phone.PhoneUpdateRequest;
import com.oldphonedeals.dto.request.phone.ReviewCreateRequest;
import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.phone.PhoneListItemResponse;
import com.oldphonedeals.dto.response.phone.PhoneResponse;
import com.oldphonedeals.dto.response.phone.ReviewResponse;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.service.PhoneService;
import com.oldphonedeals.service.ReviewService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 手机商品控制器
 * 第1部分：实现基础CRUD端点
 * 
 * 参考 Express.js 实现：
 * - server/app/routes/phone.routes.js
 * - server/app/controllers/phone.controller.js
 */
@Slf4j
@RestController
@RequestMapping("/api/phones")
public class PhoneController {

  @Autowired
  private PhoneService phoneService;

  /**
   * 创建手机商品
   * POST /api/phones
   * 需要认证
   * 
   * 参考：server/app/routes/phone.routes.js:5
   *      server/app/controllers/phone.controller.js:11-60
   * 
   * @param request 创建请求
   * @return 新创建的商品信息
   */
  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<PhoneResponse>> createPhone(
      @Valid @RequestBody PhoneCreateRequest request
  ) {
    log.info("POST /api/phones - Creating new phone");

    // 从SecurityContext获取当前用户ID
    String userId = getCurrentUserId();

    // 调用服务层创建商品
    PhoneResponse response = phoneService.createPhone(request, userId);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, "Phone created successfully"));
  }

  /**
   * 更新手机商品
   * PUT /api/phones/{phoneId}
   * 需要认证，只有商品的seller可以更新
   * 
   * 参考：server/app/routes/phone.routes.js:6
   *      server/app/controllers/phone.controller.js:62-104
   * 
   * @param phoneId 商品ID
   * @param request 更新请求
   * @return 更新后的商品信息
   */
  @PutMapping("/{phoneId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<PhoneResponse>> updatePhone(
      @PathVariable String phoneId,
      @Valid @RequestBody PhoneUpdateRequest request
  ) {
    log.info("PUT /api/phones/{} - Updating phone", phoneId);

    // 从SecurityContext获取当前用户ID
    String userId = getCurrentUserId();

    // 调用服务层更新商品（包含权限检查）
    PhoneResponse response = phoneService.updatePhone(phoneId, request, userId);

    return ResponseEntity.ok(
        ApiResponse.success(response, "Phone updated successfully")
    );
  }

  /**
   * 删除手机商品
   * DELETE /api/phones/{phoneId}
   * 需要认证，只有商品的seller可以删除
   * 
   * 参考：server/app/routes/phone.routes.js:7
   *      server/app/controllers/phone.controller.js:106-133
   * 
   * @param phoneId 商品ID
   * @return 删除成功消息
   */
  @DeleteMapping("/{phoneId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<String>> deletePhone(
      @PathVariable String phoneId
  ) {
    log.info("DELETE /api/phones/{} - Deleting phone", phoneId);

    // 从SecurityContext获取当前用户ID
    String userId = getCurrentUserId();

    // 调用服务层删除商品（包含权限检查和关联删除）
    phoneService.deletePhone(phoneId, userId);

    return ResponseEntity.ok(
        ApiResponse.success("Phone deleted successfully")
    );
  }

  /**
   * 获取单个商品详情
   * GET /api/phones/{phoneId}
   * 公开访问
   * 
   * 参考：server/app/routes/phone.routes.js:9
   *      server/app/controllers/phone.controller.js:135-176
   * 
   * @param phoneId 商品ID
   * @return 商品详情
   */
  @GetMapping("/{phoneId}")
  public ResponseEntity<ApiResponse<PhoneResponse>> getPhoneById(
      @PathVariable String phoneId
  ) {
    log.info("GET /api/phones/{} - Fetching phone details", phoneId);

    // 获取当前用户ID（如果已登录）
    String currentUserId = getCurrentUserIdOrNull();

    // 调用服务层获取商品详情
    PhoneResponse response = phoneService.getPhoneById(phoneId, currentUserId);

    return ResponseEntity.ok(
        ApiResponse.success(response, "Phone retrieved successfully")
    );
  }

  /**
   * 获取所有商品（支持分页和排序）
   * GET /api/phones
   * 公开访问
   *
   * 参考：server/app/routes/phone.routes.js:8
   *      server/app/controllers/phone.controller.js:178-234
   *
   * @param search 搜索关键词（可选）
   * @param brand 品牌过滤（可选）
   * @param maxPrice 最高价格过滤（可选）
   * @param special 特殊列表类型（soldOutSoon / bestSellers，可选）
   * @param sortBy 排序字段（默认：createdAt）
   * @param sortOrder 排序方向（asc/desc，默认：desc）
   * @param page 页码（从1开始，默认：1）
   * @param limit 每页数量（默认：12）
   * @return 包含商品列表和分页信息的响应
   */
  @GetMapping
  public ResponseEntity<ApiResponse<?>> getAllPhones(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) PhoneBrand brand,
      @RequestParam(required = false) Double maxPrice,
      @RequestParam(required = false) String special,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "12") Integer limit
  ) {
    log.info("GET /api/phones - Fetching phones with filters: search={}, brand={}, special={}, page={}, limit={}",
        search, brand, special, page, limit);

    // 处理特殊列表查询
    if (special != null && !special.trim().isEmpty()) {
      if ("soldOutSoon".equalsIgnoreCase(special)) {
        List<PhoneListItemResponse> phones = phoneService.getSoldOutSoonPhones();
        return ResponseEntity.ok(
            ApiResponse.success(phones, "Sold-out-soon phones retrieved successfully")
        );
      } else if ("bestSellers".equalsIgnoreCase(special)) {
        List<PhoneListItemResponse> phones = phoneService.getBestSellers();
        return ResponseEntity.ok(
            ApiResponse.success(phones, "Best-seller phones retrieved successfully")
        );
      }
    }

    // 调用服务层获取商品列表
    Map<String, Object> response = phoneService.getPhones(
        search,
        brand,
        maxPrice,
        sortBy,
        sortOrder,
        page,
        limit
    );

    return ResponseEntity.ok(
        ApiResponse.success(response, "Phones retrieved successfully")
    );
  }

  /**
   * 获取卖家的所有商品
   * GET /api/phones/by-seller/{sellerId}
   * 公开访问
   *
   * 参考：server/app/routes/phone.routes.js
   *      server/app/controllers/phone.controller.js:486-514
   *
   * @param sellerId 卖家ID
   * @return 该卖家的所有商品列表
   */
  @GetMapping("/by-seller/{sellerId}")
  public ResponseEntity<ApiResponse<List<PhoneResponse>>> getPhonesBySeller(
      @PathVariable String sellerId
  ) {
    log.info("GET /api/phones/by-seller/{} - Fetching phones by seller", sellerId);

    // 调用服务层获取卖家商品列表
    List<PhoneResponse> phones = phoneService.getPhonesBySeller(sellerId);

    return ResponseEntity.ok(
        ApiResponse.success(phones, "Seller phones retrieved successfully")
    );
  }

  // ==================== 辅助方法 ====================

  /**
   * 从SecurityContext获取当前用户ID
   * 
   * @return 当前用户ID
   * @throws UnauthorizedException 如果用户未认证
   */
  private String getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new com.oldphonedeals.exception.UnauthorizedException("User not authenticated");
    }
    return authentication.getName(); // JWT中的用户ID
  }

  /**
   * 从SecurityContext获取当前用户ID（如果已登录）
   * 
   * @return 当前用户ID，如果未登录则返回null
   */
  private String getCurrentUserIdOrNull() {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.isAuthenticated() 
          && !"anonymousUser".equals(authentication.getPrincipal())) {
        return authentication.getName();
      }
    } catch (Exception e) {
      log.debug("Failed to get current user ID: {}", e.getMessage());
    }
    return null;
  }
}