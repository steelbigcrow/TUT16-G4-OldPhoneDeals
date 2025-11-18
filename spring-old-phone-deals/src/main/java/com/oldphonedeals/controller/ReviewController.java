package com.oldphonedeals.controller;

import com.oldphonedeals.dto.request.admin.ToggleReviewVisibilityRequest;
import com.oldphonedeals.dto.request.phone.ReviewCreateRequest;
import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.phone.ReviewPageResponse;
import com.oldphonedeals.dto.response.phone.ReviewResponse;
import com.oldphonedeals.dto.response.phone.SellerReviewResponse;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.security.SecurityContextHelper;
import com.oldphonedeals.service.ReviewService;
import com.oldphonedeals.repository.PhoneRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论相关接口
 *
 * 对应 Express.js 中与手机评论有关的路由：
 * - GET  /api/phones/:phoneId/reviews
 * - POST /api/phones/:phoneId/reviews
 * - PATCH /api/phones/:phoneId/reviews/:reviewId/visibility
 * - GET  /api/phones/get-reviews-by-id/:sellerId
 */
@Slf4j
@RestController
@RequestMapping("/api/phones")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;
  private final PhoneRepository phoneRepository;

  /**
   * 获取某个手机的评论列表（分页）
   */
  @GetMapping("/{phoneId}/reviews")
  public ResponseEntity<ApiResponse<ReviewPageResponse>> getReviews(
      @PathVariable String phoneId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int limit
  ) {
    log.info("GET /api/phones/{}/reviews?page={}&limit={}", phoneId, page, limit);

    if (page < 1 || limit < 1) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Invalid page or limit"));
    }

    Phone phone = phoneRepository.findById(phoneId)
        .orElseThrow(() -> new ResourceNotFoundException("Phone not found with id: " + phoneId));

    String currentUserId = null;
    try {
      if (SecurityContextHelper.isAuthenticated()) {
        currentUserId = SecurityContextHelper.getCurrentUserId();
      }
    } catch (Exception ex) {
      // 未登录视为匿名用户
      currentUserId = null;
    }

    List<ReviewResponse> allVisibleReviews = reviewService.filterVisibleReviews(
        phone.getReviews(),
        currentUserId,
        phone.getSeller() != null ? phone.getSeller().getId() : null
    );

    int totalReviews = allVisibleReviews.size();
    int fromIndex = (page - 1) * limit;
    if (fromIndex >= totalReviews) {
      // 超出范围返回空列表
      ReviewPageResponse empty = ReviewPageResponse.builder()
          .reviews(List.of())
          .totalReviews(totalReviews)
          .currentPage(page)
          .totalPages((int) Math.ceil(totalReviews / (double) limit))
          .build();
      return ResponseEntity.ok(ApiResponse.success(empty));
    }

    int toIndex = Math.min(fromIndex + limit, totalReviews);
    List<ReviewResponse> pageContent = allVisibleReviews.subList(fromIndex, toIndex);
    int totalPages = (int) Math.ceil(totalReviews / (double) limit);

    ReviewPageResponse response = ReviewPageResponse.builder()
        .reviews(pageContent)
        .totalReviews(totalReviews)
        .currentPage(page)
        .totalPages(totalPages)
        .build();

    return ResponseEntity.ok(ApiResponse.success(response, "Reviews retrieved successfully"));
  }

  /**
   * 添加评论（需要登录并且满足业务规则）
   */
  @PostMapping("/{phoneId}/reviews")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
      @PathVariable String phoneId,
      @Valid @RequestBody ReviewCreateRequest request
  ) {
    String userId = SecurityContextHelper.getCurrentUserId();
    log.info("POST /api/phones/{}/reviews - userId={} rating={}", phoneId, userId, request.getRating());

    ReviewResponse review = reviewService.addReview(phoneId, request, userId);

    return ResponseEntity.ok(ApiResponse.success(review, "Review created successfully"));
  }

  /**
   * 切换评论可见性（评论者或卖家）
   */
  @PatchMapping("/{phoneId}/reviews/{reviewId}/visibility")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<ReviewResponse>> toggleReviewVisibility(
      @PathVariable String phoneId,
      @PathVariable String reviewId,
      @Valid @RequestBody ToggleReviewVisibilityRequest request
  ) {
    String userId = SecurityContextHelper.getCurrentUserId();
    log.info("PATCH /api/phones/{}/reviews/{}/visibility - userId={}, isHidden={}",
        phoneId, reviewId, userId, request.getIsHidden());

    ReviewResponse response = reviewService.toggleReviewVisibility(
        phoneId,
        reviewId,
        request.getIsHidden(),
        userId
    );

    return ResponseEntity.ok(ApiResponse.success(response, "Review visibility updated successfully"));
  }

  /**
   * 卖家查看自己所有商品收到的评论
   *
   * 对应 Express.js: GET /api/phones/get-reviews-by-id/:sellerId
   * 在 Spring 版本中，sellerId 由当前登录用户自动推断。
   */
  @GetMapping("/reviews/by-seller")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<SellerReviewResponse>>> getReviewsBySeller() {
    String sellerId = SecurityContextHelper.getCurrentUserId();
    log.info("GET /api/phones/reviews/by-seller - sellerId={}", sellerId);

    List<SellerReviewResponse> reviews = reviewService.getReviewsBySeller(sellerId);

    return ResponseEntity.ok(ApiResponse.success(reviews, "Seller reviews retrieved successfully"));
  }
}

