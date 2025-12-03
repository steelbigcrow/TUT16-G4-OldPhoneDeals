package com.oldphonedeals.controller;

import com.oldphonedeals.dto.request.admin.AdminLoginRequest;
import com.oldphonedeals.dto.request.admin.UpdatePhoneRequest;
import com.oldphonedeals.dto.request.admin.UpdateUserRequest;
import com.oldphonedeals.dto.request.profile.UpdateProfileRequest;
import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.PageResponse;
import com.oldphonedeals.dto.response.admin.*;
import com.oldphonedeals.dto.response.auth.LoginResponse;
import com.oldphonedeals.security.SecurityContextHelper;
import com.oldphonedeals.service.AdminLogService;
import com.oldphonedeals.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员控制器
 * 处理管理员认证、用户管理、商品管理、评论管理、订单管理和操作日志
 * 所有端点（除登录外）都需要ADMIN角色
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminLogService adminLogService;

    // ============================================
    // 管理员认证
    // ============================================

    /**
     * 管理员登录
     * POST /api/admin/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        log.info("Admin login request for email: {}", request.getEmail());
        LoginResponse response = adminService.adminLogin(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success(response, "Admin login successful"));
    }

    /**
     * 获取管理员资料
     * GET /api/admin/profile
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminProfileResponse>> getAdminProfile() {
        String adminId = SecurityContextHelper.getCurrentUserId();
        AdminProfileResponse response = adminService.getAdminProfile(adminId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 更新管理员资料
     * PUT /api/admin/profile
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminProfileResponse>> updateAdminProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        String adminId = SecurityContextHelper.getCurrentUserId();
        AdminProfileResponse response = adminService.updateAdminProfile(adminId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Admin profile updated successfully"));
    }

    /**
     * 获取 Dashboard 统计信息
     * GET /api/admin/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getDashboardStats() {
        AdminStatsResponse response = adminService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ============================================
    // 用户管理
    // ============================================

    /**
     * 获取所有用户（分页，支持搜索和过滤）
     * GET /api/admin/users?page=0&pageSize=10&search=john&isDisabled=false
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserManagementResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isDisabled) {
        PageResponse<UserManagementResponse> response = adminService.getAllUsers(page, pageSize, search, isDisabled);
        if (response == null) {
            response = adminService.getAllUsers(page, pageSize);
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取用户详情
     * GET /api/admin/users/{userId}
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserDetail(@PathVariable String userId) {
        String adminId = SecurityContextHelper.getCurrentUserId();
        UserDetailResponse response = adminService.getUserDetail(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 更新用户信息
     * PUT /api/admin/users/{userId}
     */
    @PutMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserManagementResponse>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRequest request) {
        String adminId = SecurityContextHelper.getCurrentUserId();
        UserManagementResponse response = adminService.updateUser(userId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "User updated successfully"));
    }

    /**
     * 切换用户禁用状态
     * PUT /api/admin/users/{userId}/toggle-disabled
     */
    @PutMapping("/users/{userId}/toggle-disabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserManagementResponse>> toggleUserStatus(@PathVariable String userId) {
        String adminId = SecurityContextHelper.getCurrentUserId();
        UserManagementResponse response = adminService.toggleUserStatus(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "User status toggled successfully"));
    }

    /**
     * 删除用户
     * DELETE /api/admin/users/{userId}
     */
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        String adminId = SecurityContextHelper.getCurrentUserId();
        adminService.deleteUser(userId, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }

    /**
     * 获取指定用户的在售手机
     * GET /api/admin/users/{userId}/phones?page=1&limit=10&sortBy=createdAt&sortOrder=desc&brand=Apple
     */
    @GetMapping("/users/{userId}/phones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserPhoneResponse>>> getUserPhones(
            @PathVariable String userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String brand) {
        if (page < 1 || pageSize < 1) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid pagination parameters"));
        }
        PageResponse<AdminUserPhoneResponse> response = adminService.getUserPhones(userId, page - 1, pageSize, sortBy, sortOrder, brand);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取指定用户提交的评论
     * GET /api/admin/users/{userId}/reviews?page=1&limit=10&sortBy=createdAt&sortOrder=desc&brand=Samsung
     */
    @GetMapping("/users/{userId}/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserReviewResponse>>> getUserReviews(
            @PathVariable String userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String brand) {
        if (page < 1 || pageSize < 1) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid pagination parameters"));
        }
        PageResponse<AdminUserReviewResponse> response = adminService.getUserReviews(userId, page - 1, pageSize, sortBy, sortOrder, brand);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ============================================
    // 商品管理
    // ============================================

    /**
     * 获取所有商品（包含禁用的）
     * GET /api/admin/phones?page=1&pageSize=10
     */
    @GetMapping("/phones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<PhoneManagementResponse>>> getAllPhones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<PhoneManagementResponse> response = adminService.getAllPhonesForAdmin(page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 更新商品信息
     * PUT /api/admin/phones/{phoneId}
     */
    @PutMapping("/phones/{phoneId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PhoneManagementResponse>> updatePhone(
            @PathVariable String phoneId,
            @Valid @RequestBody UpdatePhoneRequest request) {
        String adminId = SecurityContextHelper.getCurrentUserId();
        PhoneManagementResponse response = adminService.updatePhoneByAdmin(phoneId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "Phone updated successfully"));
    }

    /**
     * 切换商品禁用状态
     * PUT /api/admin/phones/{phoneId}/toggle-disabled
     */
    @PutMapping("/phones/{phoneId}/toggle-disabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PhoneManagementResponse>> togglePhoneStatus(@PathVariable String phoneId) {
        String adminId = SecurityContextHelper.getCurrentUserId();
        PhoneManagementResponse response = adminService.togglePhoneStatus(phoneId, adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "Phone status toggled successfully"));
    }

    /**
     * 删除商品
     * DELETE /api/admin/phones/{phoneId}
     */
    @DeleteMapping("/phones/{phoneId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePhone(@PathVariable String phoneId) {
        String adminId = SecurityContextHelper.getCurrentUserId();
        adminService.deletePhone(phoneId, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "Phone deleted successfully"));
    }

    // ============================================
    // 评论管理
    // ============================================

    /**
     * 获取所有评论（包含隐藏的，支持过滤）
     * GET /api/admin/reviews?page=0&pageSize=10&visibility=false&reviewerId=xxx&phoneId=yyy&search=keyword
     */
    @GetMapping("/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ReviewManagementResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Boolean visibility,
            @RequestParam(required = false) String reviewerId,
            @RequestParam(required = false) String phoneId,
            @RequestParam(required = false) String search) {
        PageResponse<ReviewManagementResponse> response = adminService.getAllReviews(page, pageSize, visibility, reviewerId, phoneId, search);
        if (response == null) {
            response = adminService.getAllReviews(page, pageSize);
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取指定商品的评论（分页、排序、可见性、搜索）
     * GET /api/admin/reviews/phones/{phoneId}
     */
    @GetMapping({"/reviews/phones/{phoneId}", "/phones/{phoneId}/reviews"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PhoneReviewListResponse> getPhoneReviews(
            @PathVariable String phoneId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "all") String visibility,
            @RequestParam(required = false) String search) {
        PhoneReviewListResponse response = adminService.getReviewsByPhone(phoneId, page, limit, sortBy, sortOrder, visibility, search);
        if (!response.isSuccess()) {
            HttpStatus status = "Invalid phone ID".equalsIgnoreCase(response.getMessage()) ? HttpStatus.BAD_REQUEST : HttpStatus.NOT_FOUND;
            return new ResponseEntity<>(response, status);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 切换评论可见性
     * PUT /api/admin/reviews/{phoneId}/{reviewId}/toggle-visibility
     */
    @PutMapping("/reviews/{phoneId}/{reviewId}/toggle-visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReviewManagementResponse>> toggleReviewVisibility(
            @PathVariable String phoneId,
            @PathVariable String reviewId) {
        String adminId = SecurityContextHelper.getCurrentUserId();
        ReviewManagementResponse response = adminService.toggleReviewVisibility(phoneId, reviewId, adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "Review visibility toggled successfully"));
    }

    /**
     * 删除评论
     * DELETE /api/admin/reviews/{phoneId}/{reviewId}
     */
    @DeleteMapping("/reviews/{phoneId}/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable String phoneId,
            @PathVariable String reviewId) {
        String adminId = SecurityContextHelper.getCurrentUserId();
        adminService.deleteReview(phoneId, reviewId, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "Review deleted successfully"));
    }

    // ============================================
    // 订单管理
    // ============================================

    /**
     * 获取所有订单（分页，支持过滤）
     * GET /api/admin/orders?page=0&pageSize=10&userId=xxx&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
     */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderManagementResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String brandFilter,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        if (page < 0 || pageSize < 1) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid pagination parameters"));
        }
        try {
            PageResponse<OrderManagementResponse> response = adminService.getAllOrders(
                    page, pageSize, userId, startDate, endDate, searchTerm, brandFilter, sortBy, sortOrder);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    /**
     * 订单导出
     * GET /api/admin/orders/export?format=csv|json
     */
    @GetMapping("/orders/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> exportOrders(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String brandFilter,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            OrderExportResult result = adminService.exportOrders(format, userId, startDate, endDate, searchTerm, brandFilter, sortBy, sortOrder);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getFileName() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, result.getContentType());
            return new ResponseEntity<>(result.getContent(), headers, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    /**
     * 获取订单详情
     * GET /api/admin/orders/{orderId}
     */
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(@PathVariable String orderId) {
        OrderDetailResponse response = adminService.getOrderDetail(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取销售统计
     * GET /api/admin/orders/stats
     */
    @GetMapping("/orders/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SalesStatsResponse>> getSalesStats() {
        SalesStatsResponse response = adminService.getSalesStats();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ============================================
    // 操作日志
    // ============================================

    /**
     * 获取所有操作日志（分页）
     * GET /api/admin/logs?page=1&pageSize=10
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AdminLogResponse>>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResponse<AdminLogResponse> response = adminLogService.getAllLogs(page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
