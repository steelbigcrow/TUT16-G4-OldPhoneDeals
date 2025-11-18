package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.admin.UpdatePhoneRequest;
import com.oldphonedeals.dto.request.admin.UpdateUserRequest;
import com.oldphonedeals.dto.request.profile.UpdateProfileRequest;
import com.oldphonedeals.dto.response.PageResponse;
import com.oldphonedeals.dto.response.admin.*;
import com.oldphonedeals.dto.response.auth.LoginResponse;
import com.oldphonedeals.entity.*;
import com.oldphonedeals.enums.AdminAction;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.enums.TargetType;
import com.oldphonedeals.exception.ForbiddenException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.repository.*;
import com.oldphonedeals.security.JwtTokenProvider;
import com.oldphonedeals.service.impl.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AdminService单元测试
 * 测试管理员服务的核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PhoneRepository phoneRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AdminLogRepository adminLogRepository;

    @Mock
    private AdminLogService adminLogService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User testAdmin;
    private User testUser;
    private Phone testPhone;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // 创建测试管理员
        testAdmin = new User();
        testAdmin.setId("admin-id");
        testAdmin.setEmail("admin@test.com");
        testAdmin.setFirstName("Admin");
        testAdmin.setLastName("User");
        testAdmin.setPassword("$2a$10$hashedPassword");
        testAdmin.setRole("ADMIN");
        testAdmin.setIsDisabled(false);
        testAdmin.setIsVerified(true);
        testAdmin.setLastLogin(LocalDateTime.now());
        testAdmin.setCreatedAt(LocalDateTime.now());

        // 创建测试用户
        testUser = new User();
        testUser.setId("user-id");
        testUser.setEmail("user@test.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPassword("hashedPassword");
        testUser.setRole("USER");
        testUser.setIsDisabled(false);
        testUser.setIsVerified(true);
        testUser.setWishlist(new ArrayList<>());
        testUser.setCreatedAt(LocalDateTime.now());

        // 创建测试商品
        testPhone = Phone.builder()
                .id("phone-id")
                .title("Test Phone")
                .brand(PhoneBrand.SAMSUNG)
                .image("test.jpg")
                .stock(10)
                .price(999.99)
                .seller(testUser)
                .reviews(new ArrayList<>())
                .isDisabled(false)
                .salesCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        // 创建测试订单
        Order.OrderItem orderItem = Order.OrderItem.builder()
                .phoneId("phone-id")
                .title("Test Phone")
                .quantity(1)
                .price(999.99)
                .build();

        Order.Address address = Order.Address.builder()
                .street("123 Test St")
                .city("Sydney")
                .state("NSW")
                .zip("2000")
                .country("Australia")
                .build();

        testOrder = Order.builder()
                .id("order-id")
                .userId("user-id")
                .items(Arrays.asList(orderItem))
                .totalAmount(999.99)
                .address(address)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== 管理员认证测试 ====================

    @Test
    void testAdminLogin_Success() {
        // Arrange
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches("password", testAdmin.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(testAdmin)).thenReturn("test-jwt-token");

        // Act
        LoginResponse response = adminService.adminLogin("admin@test.com", "password");

        // Assert
        assertNotNull(response);
        assertEquals("test-jwt-token", response.getToken());
        assertNotNull(response.getUser());
        assertEquals("admin-id", response.getUser().getId());
        verify(userRepository, times(1)).findByEmail("admin@test.com");
        verify(userRepository, times(1)).save(testAdmin); // 更新最后登录时间
    }

    @Test
    void testAdminLogin_InvalidEmail_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("invalid@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            adminService.adminLogin("invalid@test.com", "password");
        });
        verify(userRepository, times(1)).findByEmail("invalid@test.com");
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void testAdminLogin_InvalidPassword_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches("wrongpassword", testAdmin.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            adminService.adminLogin("admin@test.com", "wrongpassword");
        });
        verify(passwordEncoder, times(1)).matches("wrongpassword", testAdmin.getPassword());
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void testAdminLogin_NotAdminRole_ThrowsException() {
        // Arrange
        testAdmin.setRole("USER");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches("password", testAdmin.getPassword())).thenReturn(true);

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            adminService.adminLogin("admin@test.com", "password");
        });
        assertTrue(exception.getMessage().contains("Admin privileges required"));
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void testAdminLogin_DisabledAccount_ThrowsException() {
        // Arrange
        testAdmin.setIsDisabled(true);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches("password", testAdmin.getPassword())).thenReturn(true);

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            adminService.adminLogin("admin@test.com", "password");
        });
        assertTrue(exception.getMessage().contains("disabled"));
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    // ==================== 管理员个人资料测试 ====================

    @Test
    void testGetAdminProfile_Success() {
        // Arrange
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(testAdmin));

        // Act
        AdminProfileResponse response = adminService.getAdminProfile("admin-id");

        // Assert
        assertNotNull(response);
        assertEquals("admin-id", response.getId());
        assertEquals("admin@test.com", response.getEmail());
        assertEquals("ADMIN", response.getRole());
        verify(userRepository, times(1)).findById("admin-id");
    }

    @Test
    void testUpdateAdminProfile_Success() {
        // Arrange
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(testAdmin));

        // Act
        AdminProfileResponse response = adminService.updateAdminProfile("admin-id", request);

        // Assert
        assertNotNull(response);
        verify(userRepository, times(1)).save(testAdmin);
        assertEquals("Updated", testAdmin.getFirstName());
        assertEquals("Name", testAdmin.getLastName());
    }

    // ==================== Dashboard 统计测试 ====================

    @Test
    void testGetDashboardStats_ReturnsCorrectCounts() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, testAdmin));
        when(phoneRepository.count()).thenReturn(10L);
        when(phoneRepository.findAll()).thenReturn(Arrays.asList(testPhone));
        when(orderRepository.count()).thenReturn(5L);

        // Act
        AdminStatsResponse response = adminService.getDashboardStats();

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalUsers()); // 只统计非管理员用户
        assertEquals(10, response.getTotalListings());
        assertEquals(5, response.getTotalSales());
        verify(userRepository, times(1)).findAll();
        verify(phoneRepository, times(1)).count();
        verify(orderRepository, times(1)).count();
    }

    // ==================== 用户管理测试 ====================

    @Test
    void testGetAllUsers_ReturnsPagedResults() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser));

        // Act
        PageResponse<UserManagementResponse> response = adminService.getAllUsers(0, 10);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testGetAllUsers_WithSearchFilter_ReturnsFilteredResults() {
        // Arrange
        User matchingUser = new User();
        matchingUser.setId("user2");
        matchingUser.setFirstName("Jane");
        matchingUser.setLastName("Smith");
        matchingUser.setEmail("jane@test.com");
        matchingUser.setRole("USER");
        matchingUser.setIsDisabled(false);
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, matchingUser));

        // Act
        PageResponse<UserManagementResponse> response = adminService.getAllUsers(0, 10, "jane", null);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("jane@test.com", response.getContent().get(0).getEmail());
    }

    @Test
    void testGetAllUsers_WithIsDisabledFilter_ReturnsFilteredResults() {
        // Arrange
        User disabledUser = new User();
        disabledUser.setId("user2");
        disabledUser.setFirstName("Disabled");
        disabledUser.setLastName("User");
        disabledUser.setEmail("disabled@test.com");
        disabledUser.setRole("USER");
        disabledUser.setIsDisabled(true);
        disabledUser.setCreatedAt(LocalDateTime.now());
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, disabledUser));

        // Act
        PageResponse<UserManagementResponse> response = adminService.getAllUsers(0, 10, null, true);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("disabled@test.com", response.getContent().get(0).getEmail());
    }

    @Test
    void testGetUserDetail_Success() {
        // Arrange
        when(userRepository.findById("user-id")).thenReturn(Optional.of(testUser));
        when(phoneRepository.findBySellerId("user-id")).thenReturn(new ArrayList<>());
        when(orderRepository.findByUserId("user-id")).thenReturn(new ArrayList<>());
        when(phoneRepository.findAll()).thenReturn(new ArrayList<>());

        // Act
        UserDetailResponse response = adminService.getUserDetail("user-id", "admin-id");

        // Assert
        assertNotNull(response);
        assertEquals("user-id", response.getId());
        assertNotNull(response.getStats());
        verify(userRepository, times(1)).findById("user-id");
    }

    @Test
    void testUpdateUser_Success() {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Updated");
        request.setIsDisabled(true);
        
        when(userRepository.findById("user-id")).thenReturn(Optional.of(testUser));
        doNothing().when(adminLogService).logAction(anyString(), any(), any(), anyString(), anyString());

        // Act
        UserManagementResponse response = adminService.updateUser("user-id", request, "admin-id");

        // Assert
        assertNotNull(response);
        verify(userRepository, times(1)).save(testUser);
        verify(adminLogService, times(1)).logAction(eq("admin-id"), eq(AdminAction.UPDATE_USER), 
                eq(TargetType.USER), eq("user-id"), anyString());
    }

    @Test
    void testToggleUserStatus_Success() {
        // Arrange
        boolean initialStatus = testUser.getIsDisabled();
        when(userRepository.findById("user-id")).thenReturn(Optional.of(testUser));
        doNothing().when(adminLogService).logAction(anyString(), any(), any(), anyString(), anyString());

        // Act
        UserManagementResponse response = adminService.toggleUserStatus("user-id", "admin-id");

        // Assert
        assertNotNull(response);
        assertEquals(!initialStatus, testUser.getIsDisabled());
        verify(userRepository, times(1)).save(testUser);
        verify(adminLogService, times(1)).logAction(anyString(), any(), any(), anyString(), anyString());
    }

    @Test
    void testDeleteUser_WithCascadeOperations_Success() {
        // Arrange
        when(userRepository.findById("user-id")).thenReturn(Optional.of(testUser));
        when(phoneRepository.findBySellerId("user-id")).thenReturn(Arrays.asList(testPhone));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(cartRepository.findCartsContainingPhone("phone-id")).thenReturn(new ArrayList<>());
        when(userRepository.findAll()).thenReturn(new ArrayList<>());
        when(phoneRepository.findAll()).thenReturn(new ArrayList<>());
        doNothing().when(adminLogService).logAction(anyString(), any(), any(), anyString(), anyString());

        // Act
        adminService.deleteUser("user-id", "admin-id");

        // Assert
        verify(userRepository, times(1)).delete(testUser);
        verify(cartRepository, times(1)).deleteByUserId("user-id");
        verify(orderRepository, times(1)).deleteByUserId("user-id");
        verify(adminLogService, times(1)).logAction(eq("admin-id"), eq(AdminAction.DELETE_USER), 
                eq(TargetType.USER), eq("user-id"), anyString());
    }

    // ==================== 商品管理测试 ====================

    @Test
    void testGetAllPhonesForAdmin_ReturnsPagedResults() {
        // Arrange
        List<Phone> phones = Arrays.asList(testPhone);
        Page<Phone> phonePage = new PageImpl<>(phones);
        when(phoneRepository.findAll(any(Pageable.class))).thenReturn(phonePage);

        // Act
        PageResponse<PhoneManagementResponse> response = adminService.getAllPhonesForAdmin(0, 10);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(phoneRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testUpdatePhoneByAdmin_Success() {
        // Arrange
        UpdatePhoneRequest request = new UpdatePhoneRequest();
        request.setTitle("Updated Phone");
        request.setPrice(899.99);
        
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        doNothing().when(adminLogService).logAction(anyString(), any(), any(), anyString(), anyString());

        // Act
        PhoneManagementResponse response = adminService.updatePhoneByAdmin("phone-id", request, "admin-id");

        // Assert
        assertNotNull(response);
        verify(phoneRepository, times(1)).save(testPhone);
        verify(adminLogService, times(1)).logAction(eq("admin-id"), eq(AdminAction.UPDATE_PHONE), 
                eq(TargetType.PHONE), eq("phone-id"), anyString());
    }

    @Test
    void testTogglePhoneStatus_Success() {
        // Arrange
        boolean initialStatus = testPhone.getIsDisabled();
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        doNothing().when(adminLogService).logAction(anyString(), any(), any(), anyString(), anyString());

        // Act
        PhoneManagementResponse response = adminService.togglePhoneStatus("phone-id", "admin-id");

        // Assert
        assertNotNull(response);
        assertEquals(!initialStatus, testPhone.getIsDisabled());
        verify(phoneRepository, times(1)).save(testPhone);
        verify(adminLogService, times(1)).logAction(anyString(), any(), any(), anyString(), anyString());
    }

    @Test
    void testDeletePhone_WithCascadeOperations_Success() {
        // Arrange
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(cartRepository.findCartsContainingPhone("phone-id")).thenReturn(new ArrayList<>());
        when(userRepository.findAll()).thenReturn(new ArrayList<>());
        doNothing().when(adminLogService).logAction(anyString(), any(), any(), anyString(), anyString());

        // Act
        adminService.deletePhone("phone-id", "admin-id");

        // Assert
        verify(phoneRepository, times(1)).delete(testPhone);
        verify(adminLogService, times(1)).logAction(eq("admin-id"), eq(AdminAction.DELETE_PHONE), 
                eq(TargetType.PHONE), eq("phone-id"), anyString());
    }

    // ==================== 评论管理测试 ====================

    @Test
    void testGetAllReviews_ReturnsPagedResults() {
        // Arrange
        Phone.Review review = Phone.Review.builder()
                .id("review-id")
                .reviewerId("user-id")
                .rating(5)
                .comment("Great!")
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        testPhone.setReviews(Arrays.asList(review));
        when(phoneRepository.findAll()).thenReturn(Arrays.asList(testPhone));
        when(userRepository.findById("user-id")).thenReturn(Optional.of(testUser));

        // Act
        PageResponse<ReviewManagementResponse> response = adminService.getAllReviews(0, 10);

        // Assert
        assertNotNull(response);
        assertFalse(response.getContent().isEmpty());
        verify(phoneRepository, times(1)).findAll();
    }

    @Test
    void testGetAllReviews_WithVisibilityFilter_ReturnsFilteredResults() {
        // Arrange
        Phone.Review hiddenReview = Phone.Review.builder()
                .id("review-hidden")
                .reviewerId("user-id")
                .rating(3)
                .comment("Hidden review")
                .isHidden(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        Phone.Review visibleReview = Phone.Review.builder()
                .id("review-visible")
                .reviewerId("user-id")
                .rating(5)
                .comment("Visible review")
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        testPhone.setReviews(Arrays.asList(hiddenReview, visibleReview));
        when(phoneRepository.findAll()).thenReturn(Arrays.asList(testPhone));
        when(userRepository.findById("user-id")).thenReturn(Optional.of(testUser));

        // Act
        PageResponse<ReviewManagementResponse> response = adminService.getAllReviews(0, 10, false, null, null, null);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("review-hidden", response.getContent().get(0).getReviewId());
    }

    @Test
    void testGetAllReviews_WithPhoneIdFilter_ReturnsFilteredResults() {
        // Arrange
        Phone anotherPhone = Phone.builder()
                .id("phone-2")
                .title("Another Phone")
                .reviews(new ArrayList<>())
                .build();
        
        Phone.Review review = Phone.Review.builder()
                .id("review-id")
                .reviewerId("user-id")
                .rating(5)
                .comment("Great!")
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        testPhone.setReviews(Arrays.asList(review));
        when(phoneRepository.findAll()).thenReturn(Arrays.asList(testPhone, anotherPhone));
        when(userRepository.findById("user-id")).thenReturn(Optional.of(testUser));

        // Act
        PageResponse<ReviewManagementResponse> response = adminService.getAllReviews(0, 10, null, null, "phone-id", null);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("phone-id", response.getContent().get(0).getPhoneId());
    }

    @Test
    void testToggleReviewVisibility_Success() {
        // Arrange
        Phone.Review review = Phone.Review.builder()
                .id("review-id")
                .reviewerId("user-id")
                .rating(5)
                .comment("Great!")
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        testPhone.setReviews(new ArrayList<>(Arrays.asList(review)));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(userRepository.findById("user-id")).thenReturn(Optional.of(testUser));
        doNothing().when(adminLogService).logAction(anyString(), any(), any(), anyString(), anyString());

        // Act
        ReviewManagementResponse response = adminService.toggleReviewVisibility("phone-id", "review-id", "admin-id");

        // Assert
        assertNotNull(response);
        assertTrue(review.getIsHidden()); // 应该变为隐藏
        verify(phoneRepository, times(1)).save(testPhone);
        verify(adminLogService, times(1)).logAction(anyString(), any(), any(), anyString(), anyString());
    }

    @Test
    void testDeleteReview_Success() {
        // Arrange
        Phone.Review review = Phone.Review.builder()
                .id("review-id")
                .reviewerId("user-id")
                .rating(5)
                .comment("Great!")
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        testPhone.setReviews(new ArrayList<>(Arrays.asList(review)));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        doNothing().when(adminLogService).logAction(anyString(), any(), any(), anyString(), anyString());

        // Act
        adminService.deleteReview("phone-id", "review-id", "admin-id");

        // Assert
        assertTrue(testPhone.getReviews().isEmpty());
        verify(phoneRepository, times(1)).save(testPhone);
        verify(adminLogService, times(1)).logAction(eq("admin-id"), eq(AdminAction.DELETE_REVIEW), 
                eq(TargetType.REVIEW), eq("review-id"), anyString());
    }

    @Test
    void testDeleteReview_ReviewNotFound_ThrowsException() {
        // Arrange
        testPhone.setReviews(new ArrayList<>());
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            adminService.deleteReview("phone-id", "non-existing-review", "admin-id");
        });
        verify(phoneRepository, never()).save(any());
    }

    // ==================== 订单管理测试 ====================

    @Test
    void testGetAllOrders_ReturnsPagedResults() {
        // Arrange
        List<Order> orders = Arrays.asList(testOrder);
        Page<Order> orderPage = new PageImpl<>(orders);
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(orderPage);
        when(userRepository.findById("user-id")).thenReturn(Optional.of(testUser));

        // Act
        PageResponse<OrderManagementResponse> response = adminService.getAllOrders(0, 10);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(orderRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGetOrderDetail_Success() {
        // Arrange
        when(orderRepository.findById("order-id")).thenReturn(Optional.of(testOrder));
        when(userRepository.findById("user-id")).thenReturn(Optional.of(testUser));

        // Act
        OrderDetailResponse response = adminService.getOrderDetail("order-id");

        // Assert
        assertNotNull(response);
        assertEquals("order-id", response.getId());
        assertNotNull(response.getUser());
        assertEquals("user-id", response.getUser().getId());
        verify(orderRepository, times(1)).findById("order-id");
    }

    @Test
    void testGetOrderDetail_OrderNotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findById("invalid-order")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            adminService.getOrderDetail("invalid-order");
        });
        verify(orderRepository, times(1)).findById("invalid-order");
    }

    @Test
    void testGetAllOrders_WithUserIdFilter_ReturnsFilteredResults() {
        // Arrange
        Order anotherOrder = Order.builder()
                .id("order-2")
                .userId("user-2")
                .items(new ArrayList<>())
                .totalAmount(500.0)
                .createdAt(LocalDateTime.now())
                .build();
        
        when(orderRepository.findAll()).thenReturn(Arrays.asList(testOrder, anotherOrder));
        when(userRepository.findById("user-id")).thenReturn(Optional.of(testUser));

        // Act
        PageResponse<OrderManagementResponse> response = adminService.getAllOrders(0, 10, "user-id", null, null);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("user-id", response.getContent().get(0).getUserId());
    }

    @Test
    void testGetSalesStats_ReturnsCorrectStats() {
        // Arrange
        Order order1 = Order.builder()
                .id("order-1")
                .userId("user-1")
                .totalAmount(999.99)
                .build();
        
        Order order2 = Order.builder()
                .id("order-2")
                .userId("user-2")
                .totalAmount(500.00)
                .build();
        
        when(orderRepository.findAll()).thenReturn(Arrays.asList(order1, order2));

        // Act
        SalesStatsResponse response = adminService.getSalesStats();

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getTotalTransactions());
        assertEquals(1499.99, response.getTotalSales().doubleValue(), 0.01);
    }

    // ==================== 边界条件和错误处理测试 ====================

    @Test
    void testGetUserDetail_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById("invalid-user")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            adminService.getUserDetail("invalid-user", "admin-id");
        });
        verify(userRepository, times(1)).findById("invalid-user");
    }

    @Test
    void testUpdatePhoneByAdmin_PhoneNotFound_ThrowsException() {
        // Arrange
        UpdatePhoneRequest request = new UpdatePhoneRequest();
        when(phoneRepository.findById("invalid-phone")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            adminService.updatePhoneByAdmin("invalid-phone", request, "admin-id");
        });
        verify(phoneRepository, never()).save(any());
    }

    @Test
    void testToggleReviewVisibility_PhoneNotFound_ThrowsException() {
        // Arrange
        when(phoneRepository.findById("invalid-phone")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            adminService.toggleReviewVisibility("invalid-phone", "review-id", "admin-id");
        });
        verify(phoneRepository, never()).save(any());
    }

    @Test
    void testToggleReviewVisibility_ReviewNotFound_ThrowsException() {
        // Arrange
        testPhone.setReviews(new ArrayList<>());
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            adminService.toggleReviewVisibility("phone-id", "non-existing-review", "admin-id");
        });
        verify(phoneRepository, never()).save(any());
    }
}