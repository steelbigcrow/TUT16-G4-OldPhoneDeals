package com.oldphonedeals.service.impl;

import com.oldphonedeals.dto.request.admin.UpdatePhoneRequest;
import com.oldphonedeals.dto.request.admin.UpdateUserRequest;
import com.oldphonedeals.dto.request.profile.UpdateProfileRequest;
import com.oldphonedeals.dto.response.PageResponse;
import com.oldphonedeals.dto.response.admin.*;
import com.oldphonedeals.dto.response.auth.LoginResponse;
import com.oldphonedeals.dto.response.order.OrderItemResponse;
import com.oldphonedeals.entity.*;
import com.oldphonedeals.enums.AdminAction;
import com.oldphonedeals.enums.TargetType;
import com.oldphonedeals.exception.ForbiddenException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.repository.*;
import com.oldphonedeals.security.JwtTokenProvider;
import com.oldphonedeals.service.AdminLogService;
import com.oldphonedeals.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员服务实现
 * 包含管理员认证、用户管理、商品管理、评论管理、订单管理五个子模块
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final PhoneRepository phoneRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final AdminLogRepository adminLogRepository;
    private final AdminLogService adminLogService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // ============================================
    // 管理员认证模块
    // ============================================

    @Override
    public LoginResponse adminLogin(String email, String password) {
        log.info("Admin login attempt for email: {}", email);

        // 查找用户
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // 验证管理员角色
        if (!"ADMIN".equals(user.getRole())) {
            throw new ForbiddenException("Access denied. Admin privileges required.");
        }

        // 验证账户状态
        if (user.getIsDisabled()) {
            throw new ForbiddenException("Account is disabled");
        }

        // 生成JWT token
        String token = jwtTokenProvider.generateToken(user);

        // 更新最后登录时间
        user.setLastLogin(java.time.LocalDateTime.now());
        userRepository.save(user);

        log.info("Admin login successful for email: {}", email);

        return LoginResponse.builder()
                .token(token)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .build())
                .build();
    }

    @Override
    public AdminProfileResponse getAdminProfile(String adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        return AdminProfileResponse.builder()
                .id(admin.getId())
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .email(admin.getEmail())
                .role(admin.getRole())
                .isVerified(admin.getIsVerified())
                .lastLogin(admin.getLastLogin())
                .createdAt(admin.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public AdminProfileResponse updateAdminProfile(String adminId, UpdateProfileRequest request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (request.getFirstName() != null) {
            admin.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            admin.setLastName(request.getLastName());
        }

        userRepository.save(admin);
        log.info("Admin profile updated for adminId: {}", adminId);

        return getAdminProfile(adminId);
    }

    @Override
    public AdminStatsResponse getDashboardStats() {
        log.info("Fetching dashboard statistics");

        // 统计非管理员用户数
        long totalUsers = userRepository.findAll().stream()
                .filter(user -> !"ADMIN".equals(user.getRole()))
                .count();

        // 统计商品总数
        long totalListings = phoneRepository.count();

        // 统计评论总数
        long totalReviews = phoneRepository.findAll().stream()
                .mapToLong(phone -> phone.getReviews() != null ? phone.getReviews().size() : 0)
                .sum();

        // 统计订单总数
        long totalSales = orderRepository.count();

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalListings(totalListings)
                .totalReviews(totalReviews)
                .totalSales(totalSales)
                .build();
    }

    // ============================================
    // 用户管理模块
    // ============================================

    @Override
    public PageResponse<UserManagementResponse> getAllUsers(int page, int pageSize, String search, Boolean isDisabled) {
        log.info("Fetching users with filters: search={}, isDisabled={}", search, isDisabled);

        // 获取所有用户
        List<User> allUsers = userRepository.findAll();

        // 应用过滤条件
        List<User> filteredUsers = allUsers.stream()
                .filter(user -> {
                    // 搜索过滤（姓名或邮箱）
                    if (search != null && !search.trim().isEmpty()) {
                        String searchLower = search.toLowerCase();
                        String fullName = (user.getFirstName() + " " + user.getLastName()).toLowerCase();
                        String email = user.getEmail().toLowerCase();
                        if (!fullName.contains(searchLower) && !email.contains(searchLower)) {
                            return false;
                        }
                    }
                    // 禁用状态过滤
                    if (isDisabled != null && !user.getIsDisabled().equals(isDisabled)) {
                        return false;
                    }
                    return true;
                })
                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .collect(Collectors.toList());

        // 手动分页
        int start = page * pageSize;
        int end = Math.min(start + pageSize, filteredUsers.size());
        List<User> pageContent = start < filteredUsers.size()
                ? filteredUsers.subList(start, end)
                : new ArrayList<>();

        int totalPages = (int) Math.ceil((double) filteredUsers.size() / pageSize);

        List<UserManagementResponse> users = pageContent.stream()
                .map(this::convertToUserManagementResponse)
                .collect(Collectors.toList());

        return PageResponse.<UserManagementResponse>builder()
                .content(users)
                .currentPage(page + 1)
                .totalPages(totalPages)
                .totalItems((long) filteredUsers.size())
                .itemsPerPage(pageSize)
                .hasNext(end < filteredUsers.size())
                .hasPrevious(page > 0)
                .build();
    }

    @Override
    public PageResponse<UserManagementResponse> getAllUsers(int page, int pageSize) {
        return getAllUsers(page, pageSize, null, null);
    }

    @Override
    public UserDetailResponse getUserDetail(String userId, String adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 计算统计信息
        long listedPhonesCount = phoneRepository.findBySellerId(userId).size();
        List<Order> userOrders = orderRepository.findByUserId(userId);
        long ordersCount = userOrders.size();
        double totalSpent = userOrders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();

        // 计算评论数量（从所有商品中统计该用户的评论）
        long reviewsCount = phoneRepository.findAll().stream()
                .flatMap(phone -> phone.getReviews().stream())
                .filter(review -> review.getReviewerId().equals(userId))
                .count();

        UserDetailResponse.UserStats stats = UserDetailResponse.UserStats.builder()
                .listedPhonesCount(listedPhonesCount)
                .ordersCount(ordersCount)
                .reviewsCount(reviewsCount)
                .totalSpent(totalSpent)
                .build();

        return UserDetailResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .isDisabled(user.getIsDisabled())
                .isBan(user.getIsBan())
                .wishlist(user.getWishlist())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .stats(stats)
                .build();
    }

    @Override
    @Transactional
    public UserManagementResponse updateUser(String userId, UpdateUserRequest request, String adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getIsDisabled() != null) {
            user.setIsDisabled(request.getIsDisabled());
        }

        userRepository.save(user);

        // 记录日志
        adminLogService.logAction(adminId, AdminAction.UPDATE_USER, TargetType.USER, 
                userId, "Updated user information");

        log.info("User {} updated by admin {}", userId, adminId);

        return convertToUserManagementResponse(user);
    }

    @Override
    @Transactional
    public UserManagementResponse toggleUserStatus(String userId, String adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsDisabled(!user.getIsDisabled());
        userRepository.save(user);

        // 记录日志
        AdminAction action = user.getIsDisabled() ? AdminAction.DISABLE_USER : AdminAction.ENABLE_USER;
        adminLogService.logAction(adminId, action, TargetType.USER, 
                userId, "Toggled user disabled status");

        log.info("User {} status toggled to disabled={} by admin {}", 
                userId, user.getIsDisabled(), adminId);

        return convertToUserManagementResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(String userId, String adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 1. 删除用户发布的所有商品（包括级联操作）
        List<Phone> userPhones = phoneRepository.findBySellerId(userId);
        for (Phone phone : userPhones) {
            deletePhoneCascade(phone.getId());
        }

        // 2. 删除用户的购物车
        cartRepository.deleteByUserId(userId);

        // 3. 删除用户的订单
        orderRepository.deleteByUserId(userId);

        // 4. 从所有商品的reviews中删除该用户的评论
        List<Phone> allPhones = phoneRepository.findAll();
        for (Phone phone : allPhones) {
            boolean removed = phone.getReviews().removeIf(review -> 
                    review.getReviewerId().equals(userId));
            if (removed) {
                phoneRepository.save(phone);
            }
        }

        // 5. 从其他用户的收藏夹中删除该用户的商品
        List<User> usersWithWishlist = userRepository.findAll();
        for (User u : usersWithWishlist) {
            if (u.getWishlist() != null && !u.getWishlist().isEmpty()) {
                List<String> userPhoneIds = userPhones.stream()
                        .map(Phone::getId)
                        .collect(Collectors.toList());
                boolean removed = u.getWishlist().removeAll(userPhoneIds);
                if (removed) {
                    userRepository.save(u);
                }
            }
        }

        // 6. 删除用户
        userRepository.delete(user);

        // 7. 记录日志
        adminLogService.logAction(adminId, AdminAction.DELETE_USER, TargetType.USER, 
                userId, "Deleted user and all associated data");

        log.info("User {} deleted by admin {} with all associated data", userId, adminId);
    }

    // ============================================
    // 商品管理模块
    // ============================================

    @Override
    public PageResponse<PhoneManagementResponse> getAllPhonesForAdmin(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Phone> phonePage = phoneRepository.findAll(pageable);

        List<PhoneManagementResponse> phones = phonePage.getContent().stream()
                .map(this::convertToPhoneManagementResponse)
                .collect(Collectors.toList());

        return PageResponse.<PhoneManagementResponse>builder()
                .content(phones)
                .currentPage(phonePage.getNumber() + 1)
                .totalPages(phonePage.getTotalPages())
                .totalItems(phonePage.getTotalElements())
                .itemsPerPage(phonePage.getSize())
                .hasNext(phonePage.hasNext())
                .hasPrevious(phonePage.hasPrevious())
                .build();
    }

    @Override
    @Transactional
    public PhoneManagementResponse updatePhoneByAdmin(String phoneId, UpdatePhoneRequest request, String adminId) {
        Phone phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Phone not found"));

        if (request.getTitle() != null) {
            phone.setTitle(request.getTitle());
        }
        if (request.getBrand() != null) {
            phone.setBrand(request.getBrand());
        }
        if (request.getPrice() != null) {
            phone.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            phone.setStock(request.getStock());
        }
        if (request.getIsDisabled() != null) {
            phone.setIsDisabled(request.getIsDisabled());
        }

        phoneRepository.save(phone);

        // 记录日志
        adminLogService.logAction(adminId, AdminAction.UPDATE_PHONE, TargetType.PHONE, 
                phoneId, "Updated phone information");

        log.info("Phone {} updated by admin {}", phoneId, adminId);

        return convertToPhoneManagementResponse(phone);
    }

    @Override
    @Transactional
    public PhoneManagementResponse togglePhoneStatus(String phoneId, String adminId) {
        Phone phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Phone not found"));

        phone.setIsDisabled(!phone.getIsDisabled());
        phoneRepository.save(phone);

        // 记录日志
        AdminAction action = phone.getIsDisabled() ? AdminAction.DISABLE_PHONE : AdminAction.ENABLE_PHONE;
        adminLogService.logAction(adminId, action, TargetType.PHONE, 
                phoneId, "Toggled phone disabled status");

        log.info("Phone {} status toggled to disabled={} by admin {}", 
                phoneId, phone.getIsDisabled(), adminId);

        return convertToPhoneManagementResponse(phone);
    }

    @Override
    @Transactional
    public void deletePhone(String phoneId, String adminId) {
        deletePhoneCascade(phoneId);

        // 记录日志
        adminLogService.logAction(adminId, AdminAction.DELETE_PHONE, TargetType.PHONE, 
                phoneId, "Deleted phone and removed from carts/wishlists");

        log.info("Phone {} deleted by admin {}", phoneId, adminId);
    }

    /**
     * 级联删除商品
     */
    private void deletePhoneCascade(String phoneId) {
        Phone phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Phone not found"));

        // 1. 从所有购物车中删除该商品
        List<Cart> carts = cartRepository.findCartsContainingPhone(phoneId);
        for (Cart cart : carts) {
            cart.getItems().removeIf(item -> item.getPhoneId().equals(phoneId));
            cartRepository.save(cart);
        }

        // 2. 从所有用户的收藏夹中删除该商品
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getWishlist() != null && user.getWishlist().contains(phoneId)) {
                user.getWishlist().remove(phoneId);
                userRepository.save(user);
            }
        }

        // 3. 删除商品
        phoneRepository.delete(phone);
    }

    // ============================================
    // 评论管理模块
    // ============================================

    @Override
    public PageResponse<ReviewManagementResponse> getAllReviews(int page, int pageSize, Boolean visibility,
                                                                String reviewerId, String phoneId, String search) {
        log.info("Fetching reviews with filters: visibility={}, reviewerId={}, phoneId={}, search={}",
                visibility, reviewerId, phoneId, search);

        List<Phone> allPhones = phoneRepository.findAll();
        List<ReviewManagementResponse> allReviews = new ArrayList<>();

        for (Phone phone : allPhones) {
            // 如果指定了 phoneId，跳过不匹配的商品
            if (phoneId != null && !phoneId.isEmpty() && !phone.getId().equals(phoneId)) {
                continue;
            }

            for (Phone.Review review : phone.getReviews()) {
                // 应用过滤条件
                // 可见性过滤
                if (visibility != null) {
                    boolean includeHidden = Boolean.FALSE.equals(visibility);
                    if (includeHidden && !Boolean.TRUE.equals(review.getIsHidden())) {
                        continue;
                    }
                    if (!includeHidden && Boolean.TRUE.equals(review.getIsHidden())) {
                        continue;
                    }
                }

                // 评论者ID过滤
                if (reviewerId != null && !reviewerId.isEmpty() && !review.getReviewerId().equals(reviewerId)) {
                    continue;
                }

                // 搜索过滤（评论内容）
                if (search != null && !search.trim().isEmpty()) {
                    if (review.getComment() == null ||
                        !review.getComment().toLowerCase().contains(search.toLowerCase())) {
                        continue;
                    }
                }

                // 获取评论者信息
                String reviewerName = "";
                try {
                    User reviewer = userRepository.findById(review.getReviewerId()).orElse(null);
                    if (reviewer != null) {
                        reviewerName = reviewer.getFirstName() + " " + reviewer.getLastName();
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch reviewer: {}", e.getMessage());
                }

                ReviewManagementResponse response = ReviewManagementResponse.builder()
                        .reviewId(review.getId())
                        .phoneId(phone.getId())
                        .phoneTitle(phone.getTitle())
                        .reviewerId(review.getReviewerId())
                        .reviewerName(reviewerName)
                        .rating(review.getRating())
                        .comment(review.getComment())
                        .isHidden(review.getIsHidden())
                        .createdAt(review.getCreatedAt())
                        .build();
                allReviews.add(response);
            }
        }

        // 按时间倒序排列
        allReviews.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // 手动分页
        int start = page * pageSize;
        int end = Math.min(start + pageSize, allReviews.size());
        List<ReviewManagementResponse> pageData = start < allReviews.size()
                ? allReviews.subList(start, end)
                : new ArrayList<>();

        int totalPages = (int) Math.ceil((double) allReviews.size() / pageSize);

        return PageResponse.<ReviewManagementResponse>builder()
                .content(pageData)
                .currentPage(page + 1)
                .totalPages(totalPages)
                .totalItems((long) allReviews.size())
                .itemsPerPage(pageSize)
                .hasNext(end < allReviews.size())
                .hasPrevious(page > 0)
                .build();
    }

    @Override
    public PageResponse<ReviewManagementResponse> getAllReviews(int page, int pageSize) {
        return getAllReviews(page, pageSize, null, null, null, null);
    }

    @Override
    @Transactional
    public ReviewManagementResponse toggleReviewVisibility(String phoneId, String reviewId, String adminId) {
        Phone phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Phone not found"));

        Phone.Review review = phone.getReviews().stream()
                .filter(r -> r.getId().equals(reviewId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        review.setIsHidden(!review.getIsHidden());
        phoneRepository.save(phone);

        // 记录日志
        AdminAction action = review.getIsHidden() ? AdminAction.HIDE_REVIEW : AdminAction.SHOW_REVIEW;
        adminLogService.logAction(adminId, action, TargetType.REVIEW, 
                reviewId, "Toggled review visibility");

        log.info("Review {} visibility toggled to hidden={} by admin {}", 
                reviewId, review.getIsHidden(), adminId);

        // 获取评论者信息
        String reviewerName = "";
        try {
            User reviewer = userRepository.findById(review.getReviewerId()).orElse(null);
            if (reviewer != null) {
                reviewerName = reviewer.getFirstName() + " " + reviewer.getLastName();
            }
        } catch (Exception e) {
            log.error("Failed to fetch reviewer: {}", e.getMessage());
        }

        return ReviewManagementResponse.builder()
                .reviewId(review.getId())
                .phoneId(phone.getId())
                .phoneTitle(phone.getTitle())
                .reviewerId(review.getReviewerId())
                .reviewerName(reviewerName)
                .rating(review.getRating())
                .comment(review.getComment())
                .isHidden(review.getIsHidden())
                .createdAt(review.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteReview(String phoneId, String reviewId, String adminId) {
        Phone phone = phoneRepository.findById(phoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Phone not found"));

        boolean removed = phone.getReviews().removeIf(review -> review.getId().equals(reviewId));
        if (!removed) {
            throw new ResourceNotFoundException("Review not found");
        }

        phoneRepository.save(phone);

        // 记录日志
        adminLogService.logAction(adminId, AdminAction.DELETE_REVIEW, TargetType.REVIEW, 
                reviewId, "Deleted review");

        log.info("Review {} deleted by admin {}", reviewId, adminId);
    }

    // ============================================
    // 订单管理模块
    // ============================================

    @Override
    public PageResponse<OrderManagementResponse> getAllOrders(int page, int pageSize, String userId,
                                                              String startDate, String endDate) {
        log.info("Fetching orders with filters: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        boolean hasFilters = (userId != null && !userId.isEmpty())
                || (startDate != null && !startDate.isEmpty())
                || (endDate != null && !endDate.isEmpty());

        if (!hasFilters) {
            Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Order> orderPage = orderRepository.findAll(pageable);
            Page<OrderManagementResponse> mappedPage = orderPage.map(this::convertToOrderManagementResponse);
            return PageResponse.of(mappedPage);
        }

        // 获取所有订单
        List<Order> allOrders = orderRepository.findAll();

        // 应用过滤条件
        List<Order> filteredOrders = allOrders.stream()
                .filter(order -> {
                    // 用户ID过滤
                    if (userId != null && !userId.isEmpty() && !order.getUserId().equals(userId)) {
                        return false;
                    }

                    // 日期范围过滤
                    if (startDate != null && !startDate.isEmpty()) {
                        try {
                            LocalDateTime startDt = LocalDateTime.parse(startDate);
                            if (order.getCreatedAt().isBefore(startDt)) {
                                return false;
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse startDate: {}", startDate);
                        }
                    }

                    if (endDate != null && !endDate.isEmpty()) {
                        try {
                            LocalDateTime endDt = LocalDateTime.parse(endDate);
                            if (order.getCreatedAt().isAfter(endDt)) {
                                return false;
                            }
                        } catch (Exception e) {
                            log.warn("Failed to parse endDate: {}", endDate);
                        }
                    }

                    return true;
                })
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .collect(Collectors.toList());

        int startIdx = page * pageSize;
        int endIdx = Math.min(startIdx + pageSize, filteredOrders.size());
        List<Order> pageContent = startIdx < filteredOrders.size()
                ? filteredOrders.subList(startIdx, endIdx)
                : new ArrayList<>();

        Page<Order> pageImpl = new PageImpl<>(pageContent, PageRequest.of(page, pageSize), filteredOrders.size());
        Page<OrderManagementResponse> mappedPage = pageImpl.map(this::convertToOrderManagementResponse);
        return PageResponse.of(mappedPage);
    }

    @Override
    public PageResponse<OrderManagementResponse> getAllOrders(int page, int pageSize) {
        return getAllOrders(page, pageSize, null, null, null);
    }

    @Override
    public SalesStatsResponse getSalesStats() {
        log.info("Fetching sales statistics");

        List<Order> allOrders = orderRepository.findAll();

        long totalTransactions = allOrders.size();
        double totalSales = allOrders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();

        return SalesStatsResponse.builder()
                .totalSales(java.math.BigDecimal.valueOf(totalSales))
                .totalTransactions(totalTransactions)
                .build();
    }

    @Override
    public OrderDetailResponse getOrderDetail(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // 获取用户信息
        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        OrderDetailResponse.UserInfo userInfo = OrderDetailResponse.UserInfo.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();

        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .phoneId(item.getPhoneId())
                        .title(item.getTitle())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        OrderDetailResponse.AddressInfo addressInfo = OrderDetailResponse.AddressInfo.builder()
                .street(order.getAddress().getStreet())
                .city(order.getAddress().getCity())
                .state(order.getAddress().getState())
                .zip(order.getAddress().getZip())
                .country(order.getAddress().getCountry())
                .build();

        return OrderDetailResponse.builder()
                .id(order.getId())
                .user(userInfo)
                .items(items)
                .totalAmount(order.getTotalAmount())
                .address(addressInfo)
                .createdAt(order.getCreatedAt())
                .build();
    }

    // ============================================
    // 辅助方法 - DTO转换
    // ============================================

    private UserManagementResponse convertToUserManagementResponse(User user) {
        return UserManagementResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .isDisabled(user.getIsDisabled())
                .isBan(user.getIsBan())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private PhoneManagementResponse convertToPhoneManagementResponse(Phone phone) {
        PhoneManagementResponse.SellerInfo sellerInfo = null;
        if (phone.getSeller() != null) {
            sellerInfo = PhoneManagementResponse.SellerInfo.builder()
                    .id(phone.getSeller().getId())
                    .firstName(phone.getSeller().getFirstName())
                    .lastName(phone.getSeller().getLastName())
                    .email(phone.getSeller().getEmail())
                    .build();
        }

        return PhoneManagementResponse.builder()
                .id(phone.getId())
                .title(phone.getTitle())
                .brand(phone.getBrand())
                .image(phone.getImage())
                .price(phone.getPrice())
                .stock(phone.getStock())
                .isDisabled(phone.getIsDisabled())
                .salesCount(phone.getSalesCount())
                .averageRating(phone.getAverageRating())
                .reviewCount(phone.getReviews() != null ? phone.getReviews().size() : 0)
                .seller(sellerInfo)
                .createdAt(phone.getCreatedAt())
                .updatedAt(phone.getUpdatedAt())
                .build();
    }

    private OrderManagementResponse convertToOrderManagementResponse(Order order) {
        // 获取用户信息
        String userName = "";
        String userEmail = "";
        try {
            User user = userRepository.findById(order.getUserId()).orElse(null);
            if (user != null) {
                userName = user.getFirstName() + " " + user.getLastName();
                userEmail = user.getEmail();
            }
        } catch (Exception e) {
            log.error("Failed to fetch user for order: {}", e.getMessage());
        }

        // 生成地址摘要
        String addressSummary = "";
        if (order.getAddress() != null) {
            addressSummary = order.getAddress().getCity() + ", " + 
                           order.getAddress().getState() + ", " + 
                           order.getAddress().getCountry();
        }

        return OrderManagementResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .userName(userName)
                .userEmail(userEmail)
                .itemCount(order.getItems() != null ? order.getItems().size() : 0)
                .totalAmount(order.getTotalAmount())
                .addressSummary(addressSummary)
                .createdAt(order.getCreatedAt())
                .build();
    }
}