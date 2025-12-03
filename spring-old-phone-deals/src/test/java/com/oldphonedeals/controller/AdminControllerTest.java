package com.oldphonedeals.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldphonedeals.config.ControllerTestConfig;
import com.oldphonedeals.config.CorsConfig;
import com.oldphonedeals.config.FileStorageProperties;
import com.oldphonedeals.dto.request.admin.AdminLoginRequest;
import com.oldphonedeals.security.CustomUserDetailsService;
import com.oldphonedeals.security.JwtTokenProvider;
import com.oldphonedeals.dto.request.admin.UpdatePhoneRequest;
import com.oldphonedeals.dto.request.admin.UpdateUserRequest;
import com.oldphonedeals.dto.response.PageResponse;
import com.oldphonedeals.dto.response.admin.*;
import com.oldphonedeals.dto.response.auth.LoginResponse;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.exception.ForbiddenException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.security.SecurityContextHelper;
import com.oldphonedeals.service.AdminLogService;
import com.oldphonedeals.service.AdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminController集成测试
 * 
 * 测试管理员相关的REST API端点，包括：
 * - 管理员登录和认证
 * - 用户管理（查看、更新、删除）
 * - 商品管理（查看、更新、删除）
 * - 评论管理
 * - 订单管理
 * - 操作日志
 * 
 * 使用@WebMvcTest进行Controller层集成测试
 */
@WebMvcTest(value = AdminController.class,
    excludeAutoConfiguration = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        MongoRepositoriesAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = CorsConfig.class
    ))
@Import(ControllerTestConfig.class)
@AutoConfigureMockMvc(addFilters = false) // 禁用Security过滤器以简化测试
@DisplayName("AdminController集成测试")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private AdminLogService adminLogService;

    @MockBean
    private FileStorageProperties fileStorageProperties;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockedStatic<SecurityContextHelper> securityContextHelperMock;

    private AdminLoginRequest adminLoginRequest;
    private LoginResponse loginResponse;
    private UserManagementResponse userManagementResponse;
    private PhoneManagementResponse phoneManagementResponse;
    private ReviewManagementResponse reviewManagementResponse;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        adminLoginRequest = AdminLoginRequest.builder()
                .email("admin@example.com")
                .password("admin123")
                .build();

        loginResponse = LoginResponse.builder()
                .token("admin-jwt-token")
                .user(LoginResponse.UserInfo.builder()
                        .id("admin123")
                        .firstName("Admin")
                        .lastName("User")
                        .email("admin@example.com")
                        .build())
                .build();

        userManagementResponse = UserManagementResponse.builder()
                .id("user123")
                .firstName("John")
                .lastName("Doe")
                .email("user@example.com")
                .role("USER")
                .isDisabled(false)
                .isBan(false)
                .isVerified(true)
                .build();

        phoneManagementResponse = PhoneManagementResponse.builder()
                .id("phone123")
                .title("iPhone 12 Pro")
                .price(999.99)
                .stock(10)
                .isDisabled(false)
                .build();

        reviewManagementResponse = ReviewManagementResponse.builder()
                .reviewId("review123")
                .phoneId("phone123")
                .rating(5)
                .comment("Great phone!")
                .isHidden(false)
                .build();

        // Mock static SecurityContextHelper methods
        securityContextHelperMock = mockStatic(SecurityContextHelper.class);
        securityContextHelperMock.when(SecurityContextHelper::getCurrentUserId).thenReturn("admin123");
    }

    @AfterEach
    void tearDown() {
        if (securityContextHelperMock != null) {
            securityContextHelperMock.close();
        }
    }

    // ==================== 管理员登录测试 ====================

    @Test
    @DisplayName("testAdminLogin_ValidCredentials_ReturnsOk")
    void testAdminLogin_ValidCredentials_ReturnsOk() throws Exception {
        // Arrange
        when(adminService.adminLogin(anyString(), anyString())).thenReturn(loginResponse);

        // Act & Assert
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Admin login successful"))
                .andExpect(jsonPath("$.data.token").value("admin-jwt-token"))
                .andExpect(jsonPath("$.data.user.email").value("admin@example.com"));

        verify(adminService, times(1)).adminLogin(anyString(), anyString());
    }

    @Test
    @DisplayName("testAdminLogin_InvalidCredentials_ReturnsUnauthorized")
    void testAdminLogin_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        // Arrange
        when(adminService.adminLogin(anyString(), anyString()))
                .thenThrow(new UnauthorizedException("Invalid admin credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLoginRequest)))
                .andExpect(status().isUnauthorized());

        verify(adminService, times(1)).adminLogin(anyString(), anyString());
    }

    @Test
    @DisplayName("testAdminLogin_NonAdminUser_ReturnsForbidden")
    void testAdminLogin_NonAdminUser_ReturnsForbidden() throws Exception {
        // Arrange
        when(adminService.adminLogin(anyString(), anyString()))
                .thenThrow(new ForbiddenException("User is not an admin"));

        // Act & Assert
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLoginRequest)))
                .andExpect(status().isForbidden());

        verify(adminService, times(1)).adminLogin(anyString(), anyString());
    }

    // ==================== Dashboard 统计测试 ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetDashboardStats_ReturnsStats")
    void testGetDashboardStats_ReturnsStats() throws Exception {
        // Arrange
        AdminStatsResponse statsResponse = AdminStatsResponse.builder()
                .totalUsers(100L)
                .totalListings(50L)
                .totalReviews(200L)
                .totalSales(75L)
                .build();

        when(adminService.getDashboardStats()).thenReturn(statsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(100))
                .andExpect(jsonPath("$.data.totalListings").value(50))
                .andExpect(jsonPath("$.data.totalReviews").value(200))
                .andExpect(jsonPath("$.data.totalSales").value(75));

        verify(adminService, times(1)).getDashboardStats();
    }

    // ==================== 用户管理测试 ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetAllUsers_WithPagination_ReturnsPagedResult")
    void testGetAllUsers_WithPagination_ReturnsPagedResult() throws Exception {
        // Arrange
        PageResponse<UserManagementResponse> pageResponse = PageResponse.<UserManagementResponse>builder()
                .content(List.of(userManagementResponse))
                .currentPage(0)
                .totalPages(5)
                .totalItems(50L)
                .itemsPerPage(10)
                .build();

        when(adminService.getAllUsers(anyInt(), anyInt())).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(5))
                .andExpect(jsonPath("$.data.totalItems").value(50));

        verify(adminService, times(1)).getAllUsers(anyInt(), anyInt(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetAllUsers_WithSearchParam_ReturnsFilteredResult")
    void testGetAllUsers_WithSearchParam_ReturnsFilteredResult() throws Exception {
        // Arrange
        PageResponse<UserManagementResponse> pageResponse = PageResponse.<UserManagementResponse>builder()
                .content(List.of(userManagementResponse))
                .currentPage(0)
                .totalPages(1)
                .totalItems(1L)
                .itemsPerPage(10)
                .build();

        when(adminService.getAllUsers(anyInt(), anyInt(), eq("john"), any())).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("pageSize", "10")
                        .param("search", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(1));

        verify(adminService, times(1)).getAllUsers(eq(0), eq(10), eq("john"), isNull());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetAllUsers_WithIsDisabledParam_ReturnsFilteredResult")
    void testGetAllUsers_WithIsDisabledParam_ReturnsFilteredResult() throws Exception {
        // Arrange
        PageResponse<UserManagementResponse> pageResponse = PageResponse.<UserManagementResponse>builder()
                .content(List.of(userManagementResponse))
                .currentPage(0)
                .totalPages(1)
                .totalItems(1L)
                .itemsPerPage(10)
                .build();

        when(adminService.getAllUsers(anyInt(), anyInt(), any(), eq(false))).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("pageSize", "10")
                        .param("isDisabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(adminService, times(1)).getAllUsers(eq(0), eq(10), isNull(), eq(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetUserDetail_ValidUserId_ReturnsUserDetail")
    void testGetUserDetail_ValidUserId_ReturnsUserDetail() throws Exception {
        // Arrange
        UserDetailResponse userDetail = UserDetailResponse.builder()
                .id("user123")
                .firstName("John")
                .lastName("Doe")
                .email("user@example.com")
                .build();

        when(adminService.getUserDetail(anyString(), anyString())).thenReturn(userDetail);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users/user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("user123"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"));

        verify(adminService, times(1)).getUserDetail(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testUpdateUser_ValidRequest_ReturnsUpdatedUser")
    void testUpdateUser_ValidRequest_ReturnsUpdatedUser() throws Exception {
        // Arrange
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .build();

        when(adminService.updateUser(anyString(), any(UpdateUserRequest.class), anyString()))
                .thenReturn(userManagementResponse);

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User updated successfully"));

        verify(adminService, times(1)).updateUser(anyString(), any(UpdateUserRequest.class), anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testToggleUserStatus_ValidUserId_ReturnsToggledStatus")
    void testToggleUserStatus_ValidUserId_ReturnsToggledStatus() throws Exception {
        // Arrange
        when(adminService.toggleUserStatus(anyString(), anyString()))
                .thenReturn(userManagementResponse);

        // Act & Assert
        mockMvc.perform(put("/api/admin/users/user123/toggle-disabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User status toggled successfully"));

        verify(adminService, times(1)).toggleUserStatus(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testDeleteUser_ValidUserId_ReturnsSuccess")
    void testDeleteUser_ValidUserId_ReturnsSuccess() throws Exception {
        // Arrange
        doNothing().when(adminService).deleteUser(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(delete("/api/admin/users/user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(adminService, times(1)).deleteUser(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testDeleteUser_UserNotFound_ReturnsNotFound")
    void testDeleteUser_UserNotFound_ReturnsNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("User not found"))
                .when(adminService).deleteUser(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(delete("/api/admin/users/nonexistent"))
                .andExpect(status().isNotFound());

        verify(adminService, times(1)).deleteUser(anyString(), anyString());
    }

    // ==================== 商品管理测试 ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetAllPhones_WithPagination_ReturnsPagedResult")
    void testGetAllPhones_WithPagination_ReturnsPagedResult() throws Exception {
        // Arrange
        PageResponse<PhoneManagementResponse> pageResponse = PageResponse.<PhoneManagementResponse>builder()
                .content(List.of(phoneManagementResponse))
                .currentPage(0)
                .totalPages(3)
                .totalItems(30L)
                .itemsPerPage(10)
                .build();

        when(adminService.getAllPhonesForAdmin(anyInt(), anyInt())).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/phones")
                        .param("page", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(3));

        verify(adminService, times(1)).getAllPhonesForAdmin(anyInt(), anyInt());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testUpdatePhone_ValidRequest_ReturnsUpdatedPhone")
    void testUpdatePhone_ValidRequest_ReturnsUpdatedPhone() throws Exception {
        // Arrange
        UpdatePhoneRequest updateRequest = UpdatePhoneRequest.builder()
                .title("iPhone 12 Pro Max")
                .brand(PhoneBrand.APPLE)
                .price(1099.99)
                .stock(15)
                .build();

        when(adminService.updatePhoneByAdmin(anyString(), any(UpdatePhoneRequest.class), anyString()))
                .thenReturn(phoneManagementResponse);

        // Act & Assert
        mockMvc.perform(put("/api/admin/phones/phone123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Phone updated successfully"));

        verify(adminService, times(1)).updatePhoneByAdmin(anyString(), any(UpdatePhoneRequest.class), anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testTogglePhoneStatus_ValidPhoneId_ReturnsToggledStatus")
    void testTogglePhoneStatus_ValidPhoneId_ReturnsToggledStatus() throws Exception {
        // Arrange
        when(adminService.togglePhoneStatus(anyString(), anyString()))
                .thenReturn(phoneManagementResponse);

        // Act & Assert
        mockMvc.perform(put("/api/admin/phones/phone123/toggle-disabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Phone status toggled successfully"));

        verify(adminService, times(1)).togglePhoneStatus(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testDeletePhone_ValidPhoneId_ReturnsSuccess")
    void testDeletePhone_ValidPhoneId_ReturnsSuccess() throws Exception {
        // Arrange
        doNothing().when(adminService).deletePhone(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(delete("/api/admin/phones/phone123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Phone deleted successfully"));

        verify(adminService, times(1)).deletePhone(anyString(), anyString());
    }

    // ==================== 评论管理测试 ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetAllReviews_WithPagination_ReturnsPagedResult")
    void testGetAllReviews_WithPagination_ReturnsPagedResult() throws Exception {
        // Arrange
        PageResponse<ReviewManagementResponse> pageResponse = PageResponse.<ReviewManagementResponse>builder()
                .content(List.of(reviewManagementResponse))
                .currentPage(0)
                .totalPages(2)
                .totalItems(20L)
                .itemsPerPage(10)
                .build();

        when(adminService.getAllReviews(anyInt(), anyInt())).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reviews")
                        .param("page", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.currentPage").value(0));

        verify(adminService, times(1)).getAllReviews(anyInt(), anyInt(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetAllReviews_WithFilters_ReturnsFilteredResult")
    void testGetAllReviews_WithFilters_ReturnsFilteredResult() throws Exception {
        // Arrange
        PageResponse<ReviewManagementResponse> pageResponse = PageResponse.<ReviewManagementResponse>builder()
                .content(List.of(reviewManagementResponse))
                .currentPage(0)
                .totalPages(1)
                .totalItems(1L)
                .itemsPerPage(10)
                .build();

        when(adminService.getAllReviews(anyInt(), anyInt(), eq(false), eq("user123"), eq("phone123"), eq("great")))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reviews")
                        .param("page", "0")
                        .param("pageSize", "10")
                        .param("visibility", "false")
                        .param("reviewerId", "user123")
                        .param("phoneId", "phone123")
                        .param("search", "great"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(adminService, times(1)).getAllReviews(eq(0), eq(10), eq(false), eq("user123"), eq("phone123"), eq("great"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testToggleReviewVisibility_ValidReviewId_ReturnsToggledStatus")
    void testToggleReviewVisibility_ValidReviewId_ReturnsToggledStatus() throws Exception {
        // Arrange
        when(adminService.toggleReviewVisibility(anyString(), anyString(), anyString()))
                .thenReturn(reviewManagementResponse);

        // Act & Assert
        mockMvc.perform(put("/api/admin/reviews/phone123/review123/toggle-visibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review visibility toggled successfully"));

        verify(adminService, times(1)).toggleReviewVisibility(anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testDeleteReview_ValidReviewId_ReturnsSuccess")
    void testDeleteReview_ValidReviewId_ReturnsSuccess() throws Exception {
        // Arrange
        doNothing().when(adminService).deleteReview(anyString(), anyString(), anyString());

        // Act & Assert
        mockMvc.perform(delete("/api/admin/reviews/phone123/review123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review deleted successfully"));

        verify(adminService, times(1)).deleteReview(anyString(), anyString(), anyString());
    }

    // ==================== 订单管理测试 ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetAllOrders_WithPagination_ReturnsPagedResult")
    void testGetAllOrders_WithPagination_ReturnsPagedResult() throws Exception {
        // Arrange
        OrderManagementResponse orderResponse = OrderManagementResponse.builder()
                .id("order123")
                .userId("user123")
                .totalAmount(999.99)
                .build();

        PageResponse<OrderManagementResponse> pageResponse = PageResponse.<OrderManagementResponse>builder()
                .content(List.of(orderResponse))
                .currentPage(0)
                .totalPages(4)
                .totalItems(40L)
                .itemsPerPage(10)
                .build();

        when(adminService.getAllOrders(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/orders")
                        .param("page", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalPages").value(4));

        verify(adminService, times(1)).getAllOrders(eq(0), eq(10), isNull(), isNull(), isNull(), isNull(), isNull(), eq("createdAt"), eq("desc"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetAllOrders_WithFilters_ReturnsFilteredResult")
    void testGetAllOrders_WithFilters_ReturnsFilteredResult() throws Exception {
        // Arrange
        OrderManagementResponse orderResponse = OrderManagementResponse.builder()
                .id("order123")
                .userId("user123")
                .totalAmount(999.99)
                .build();

        PageResponse<OrderManagementResponse> pageResponse = PageResponse.<OrderManagementResponse>builder()
                .content(List.of(orderResponse))
                .currentPage(0)
                .totalPages(1)
                .totalItems(1L)
                .itemsPerPage(10)
                .build();

        when(adminService.getAllOrders(eq(0), eq(10), eq("user123"), isNull(), isNull(),
                eq("john"), eq("Nokia"), eq("totalAmount"), eq("asc")))
                .thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/orders")
                        .param("page", "0")
                        .param("pageSize", "10")
                        .param("userId", "user123")
                        .param("searchTerm", "john")
                        .param("brandFilter", "Nokia")
                        .param("sortBy", "totalAmount")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(1));

        verify(adminService, times(1)).getAllOrders(eq(0), eq(10), eq("user123"), isNull(), isNull(),
                eq("john"), eq("Nokia"), eq("totalAmount"), eq("asc"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetAllOrders_InvalidPagination_ReturnsBadRequest")
    void testGetAllOrders_InvalidPagination_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .param("page", "-1")
                        .param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        verify(adminService, never()).getAllOrders(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testExportOrders_Csv_ReturnsFile")
    void testExportOrders_Csv_ReturnsFile() throws Exception {
        OrderExportResult exportResult = OrderExportResult.builder()
                .fileName("orders.csv")
                .contentType("text/csv")
                .content("Timestamp,Buyer Name,Buyer Email,Items,Total Amount".getBytes())
                .build();

        when(adminService.exportOrders(eq("csv"), isNull(), isNull(), isNull(), isNull(), isNull(), eq("createdAt"), eq("desc")))
                .thenReturn(exportResult);

        mockMvc.perform(get("/api/admin/orders/export").param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders.csv\""));

        verify(adminService, times(1)).exportOrders(eq("csv"), isNull(), isNull(), isNull(), isNull(), isNull(), eq("createdAt"), eq("desc"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testExportOrders_InvalidFormat_ReturnsBadRequest")
    void testExportOrders_InvalidFormat_ReturnsBadRequest() throws Exception {
        when(adminService.exportOrders(eq("pdf"), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid format. Supported formats: csv, json"));

        mockMvc.perform(get("/api/admin/orders/export").param("format", "pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid format. Supported formats: csv, json"));

        verify(adminService, times(1)).exportOrders(eq("pdf"), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetSalesStats_ReturnsStats")
    void testGetSalesStats_ReturnsStats() throws Exception {
        // Arrange
        SalesStatsResponse statsResponse = SalesStatsResponse.builder()
                .totalSales(java.math.BigDecimal.valueOf(99999.99))
                .totalTransactions(150L)
                .build();

        when(adminService.getSalesStats()).thenReturn(statsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/orders/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSales").value(99999.99))
                .andExpect(jsonPath("$.data.totalTransactions").value(150));

        verify(adminService, times(1)).getSalesStats();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetOrderDetail_ValidOrderId_ReturnsOrderDetail")
    void testGetOrderDetail_ValidOrderId_ReturnsOrderDetail() throws Exception {
        // Arrange
        OrderDetailResponse orderDetail = OrderDetailResponse.builder()
                .id("order123")
                .user(OrderDetailResponse.UserInfo.builder()
                        .id("user123")
                        .firstName("John")
                        .lastName("Doe")
                        .email("user@example.com")
                        .build())
                .totalAmount(999.99)
                .build();

        when(adminService.getOrderDetail(anyString())).thenReturn(orderDetail);

        // Act & Assert
        mockMvc.perform(get("/api/admin/orders/order123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("order123"));

        verify(adminService, times(1)).getOrderDetail(anyString());
    }

    // ==================== 操作日志测试 ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("testGetAllLogs_WithPagination_ReturnsPagedResult")
    void testGetAllLogs_WithPagination_ReturnsPagedResult() throws Exception {
        // Arrange
        AdminLogResponse logResponse = AdminLogResponse.builder()
                .id("log123")
                .adminUserId("admin123")
                .adminName("Admin User")
                .targetId("user123")
                .build();

        PageResponse<AdminLogResponse> pageResponse = PageResponse.<AdminLogResponse>builder()
                .content(List.of(logResponse))
                .currentPage(0)
                .totalPages(10)
                .totalItems(100L)
                .itemsPerPage(10)
                .build();

        when(adminLogService.getAllLogs(anyInt(), anyInt())).thenReturn(pageResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/logs")
                        .param("page", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalPages").value(10));

        verify(adminLogService, times(1)).getAllLogs(anyInt(), anyInt());
    }
}
