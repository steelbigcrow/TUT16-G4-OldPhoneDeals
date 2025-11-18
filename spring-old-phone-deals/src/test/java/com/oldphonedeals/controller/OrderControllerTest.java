package com.oldphonedeals.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldphonedeals.config.ControllerTestConfig;
import com.oldphonedeals.config.CorsConfig;
import com.oldphonedeals.config.FileStorageProperties;
import com.oldphonedeals.dto.request.order.CheckoutRequest;
import com.oldphonedeals.dto.response.order.OrderItemResponse;
import com.oldphonedeals.dto.response.order.OrderResponse;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.ForbiddenException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.security.CustomUserDetailsService;
import com.oldphonedeals.security.JwtTokenProvider;
import com.oldphonedeals.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OrderController集成测试
 * <p>
 * 测试订单相关的REST API端点，包括：
 * - 结账（创建订单）
 * - 获取用户订单列表
 * - 获取订单详情
 * </p>
 * 
 * 使用@WebMvcTest进行Controller层集成测试
 */
@WebMvcTest(value = OrderController.class,
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
@DisplayName("OrderController集成测试")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private FileStorageProperties fileStorageProperties;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private CheckoutRequest checkoutRequest;
    private OrderResponse orderResponse;
    private OrderItemResponse orderItem1;
    private OrderItemResponse orderItem2;
    private List<OrderResponse> orderList;

    @BeforeEach
    void setUp() {
        // 准备测试数据 - 结账请求
        checkoutRequest = CheckoutRequest.builder()
                .address(CheckoutRequest.AddressInfo.builder()
                        .street("123 Main St")
                        .city("Sydney")
                        .state("NSW")
                        .zip("2000")
                        .country("Australia")
                        .build())
                .build();

        // 准备测试数据 - 订单商品项1
        orderItem1 = OrderItemResponse.builder()
                .phoneId("phone123")
                .title("iPhone 12 Pro")
                .quantity(2)
                .price(999.99)
                .build();

        // 准备测试数据 - 订单商品项2
        orderItem2 = OrderItemResponse.builder()
                .phoneId("phone456")
                .title("Samsung Galaxy S21")
                .quantity(1)
                .price(799.99)
                .build();

        // 准备测试数据 - 订单响应
        orderResponse = OrderResponse.builder()
                .id("order123")
                .userId("user123")
                .items(List.of(orderItem1, orderItem2))
                .totalAmount(2799.97)
                .address(OrderResponse.AddressInfo.builder()
                        .street("123 Main St")
                        .city("Sydney")
                        .state("NSW")
                        .zip("2000")
                        .country("Australia")
                        .build())
                .createdAt(LocalDateTime.now())
                .build();

        // 准备测试数据 - 订单列表
        OrderResponse order2 = OrderResponse.builder()
                .id("order456")
                .userId("user123")
                .items(List.of(orderItem1))
                .totalAmount(1999.98)
                .address(OrderResponse.AddressInfo.builder()
                        .street("456 Oak Ave")
                        .city("Melbourne")
                        .state("VIC")
                        .zip("3000")
                        .country("Australia")
                        .build())
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        orderList = List.of(orderResponse, order2);
    }

    // ==================== 结账端点测试 ====================

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功创建订单 - 当结账请求有效时")
    void shouldCreateOrder_whenCheckoutRequestValid() throws Exception {
        // Arrange
        when(orderService.checkout(anyString(), any(CheckoutRequest.class)))
                .thenReturn(orderResponse);

        // Act & Assert
        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order created successfully"))
                .andExpect(jsonPath("$.data.id").value("order123"))
                .andExpect(jsonPath("$.data.userId").value("user123"))
                .andExpect(jsonPath("$.data.totalAmount").value(2799.97))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].phoneId").value("phone123"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].price").value(999.99))
                .andExpect(jsonPath("$.data.address.street").value("123 Main St"))
                .andExpect(jsonPath("$.data.address.city").value("Sydney"));

        verify(orderService, times(1)).checkout(anyString(), any(CheckoutRequest.class));
    }

    @Test
    @DisplayName("应该返回400错误 - 当地址为空时")
    void shouldReturnBadRequest_whenAddressIsNull() throws Exception {
        // Arrange
        CheckoutRequest invalidRequest = CheckoutRequest.builder()
                .address(null)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).checkout(anyString(), any(CheckoutRequest.class));
    }

    @Test
    @DisplayName("应该返回400错误 - 当街道地址为空时")
    void shouldReturnBadRequest_whenStreetIsBlank() throws Exception {
        // Arrange
        CheckoutRequest invalidRequest = CheckoutRequest.builder()
                .address(CheckoutRequest.AddressInfo.builder()
                        .street("")
                        .city("Sydney")
                        .state("NSW")
                        .zip("2000")
                        .country("Australia")
                        .build())
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).checkout(anyString(), any(CheckoutRequest.class));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回400错误 - 当购物车为空时")
    void shouldReturnBadRequest_whenCartIsEmpty() throws Exception {
        // Arrange
        when(orderService.checkout(anyString(), any(CheckoutRequest.class)))
                .thenThrow(new BadRequestException("Cart is empty"));

        // Act & Assert
        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isBadRequest());

        verify(orderService, times(1)).checkout(anyString(), any(CheckoutRequest.class));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回400错误 - 当商品库存不足时")
    void shouldReturnBadRequest_whenInsufficientStock() throws Exception {
        // Arrange
        when(orderService.checkout(anyString(), any(CheckoutRequest.class)))
                .thenThrow(new BadRequestException("Insufficient stock for phone: iPhone 12 Pro"));

        // Act & Assert
        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isBadRequest());

        verify(orderService, times(1)).checkout(anyString(), any(CheckoutRequest.class));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回404错误 - 当购物车中的商品不存在时")
    void shouldReturnNotFound_whenPhoneInCartNotFound() throws Exception {
        // Arrange
        when(orderService.checkout(anyString(), any(CheckoutRequest.class)))
                .thenThrow(new ResourceNotFoundException("Phone not found"));

        // Act & Assert
        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isNotFound());

        verify(orderService, times(1)).checkout(anyString(), any(CheckoutRequest.class));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回400错误 - 当商品已禁用时")
    void shouldReturnBadRequest_whenPhoneIsDisabled() throws Exception {
        // Arrange
        when(orderService.checkout(anyString(), any(CheckoutRequest.class)))
                .thenThrow(new BadRequestException("Phone is disabled: iPhone 12 Pro"));

        // Act & Assert
        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isBadRequest());

        verify(orderService, times(1)).checkout(anyString(), any(CheckoutRequest.class));
    }

    // ==================== 获取用户订单列表端点测试 ====================

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功获取订单列表 - 当用户查看自己的订单时")
    void shouldReturnOrderList_whenUserViewsOwnOrders() throws Exception {
        // Arrange
        when(orderService.getUserOrders(anyString())).thenReturn(orderList);

        // Act & Assert
        mockMvc.perform(get("/api/orders/user/user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Orders retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("order123"))
                .andExpect(jsonPath("$.data[0].userId").value("user123"))
                .andExpect(jsonPath("$.data[0].totalAmount").value(2799.97))
                .andExpect(jsonPath("$.data[1].id").value("order456"));

        verify(orderService, times(1)).getUserOrders("user123");
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回空列表 - 当用户没有订单时")
    void shouldReturnEmptyList_whenUserHasNoOrders() throws Exception {
        // Arrange
        when(orderService.getUserOrders(anyString())).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get("/api/orders/user/user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Orders retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(orderService, times(1)).getUserOrders("user123");
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回403错误 - 当用户尝试查看其他用户的订单时")
    void shouldReturnForbidden_whenUserViewsOthersOrders() throws Exception {
        // Act & Assert
        // 注意：这个测试需要在启用Security过滤器时才能真正测试权限
        // 在实际应用中，SecurityContext会检查当前用户ID是否与路径参数中的userId匹配
        // 由于我们禁用了过滤器，这里模拟Controller层的权限检查
        mockMvc.perform(get("/api/orders/user/other-user"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You can only view your own orders"));

        verify(orderService, never()).getUserOrders("other-user");
    }

    // ==================== 获取订单详情端点测试 ====================

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功获取订单详情 - 当订单属于当前用户时")
    void shouldReturnOrderDetails_whenOrderBelongsToUser() throws Exception {
        // Arrange
        when(orderService.getOrderById(anyString(), anyString())).thenReturn(orderResponse);

        // Act & Assert
        mockMvc.perform(get("/api/orders/order123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value("order123"))
                .andExpect(jsonPath("$.data.userId").value("user123"))
                .andExpect(jsonPath("$.data.totalAmount").value(2799.97))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.address").exists())
                .andExpect(jsonPath("$.data.address.street").value("123 Main St"))
                .andExpect(jsonPath("$.data.createdAt").exists());

        verify(orderService, times(1)).getOrderById(eq("order123"), anyString());
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回404错误 - 当订单不存在时")
    void shouldReturnNotFound_whenOrderDoesNotExist() throws Exception {
        // Arrange
        when(orderService.getOrderById(anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("Order not found"));

        // Act & Assert
        mockMvc.perform(get("/api/orders/nonexistent"))
                .andExpect(status().isNotFound());

        verify(orderService, times(1)).getOrderById(eq("nonexistent"), anyString());
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回403错误 - 当订单不属于当前用户时")
    void shouldReturnForbidden_whenOrderDoesNotBelongToUser() throws Exception {
        // Arrange
        when(orderService.getOrderById(anyString(), anyString()))
                .thenThrow(new ForbiddenException("You can only view your own orders"));

        // Act & Assert
        mockMvc.perform(get("/api/orders/order123"))
                .andExpect(status().isForbidden());

        verify(orderService, times(1)).getOrderById(eq("order123"), anyString());
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该包含完整的订单商品信息 - 当获取订单详情时")
    void shouldIncludeCompleteItemInfo_whenGettingOrderDetails() throws Exception {
        // Arrange
        when(orderService.getOrderById(anyString(), anyString())).thenReturn(orderResponse);

        // Act & Assert
        mockMvc.perform(get("/api/orders/order123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].phoneId").value("phone123"))
                .andExpect(jsonPath("$.data.items[0].title").value("iPhone 12 Pro"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].price").value(999.99))
                .andExpect(jsonPath("$.data.items[1].phoneId").value("phone456"))
                .andExpect(jsonPath("$.data.items[1].title").value("Samsung Galaxy S21"))
                .andExpect(jsonPath("$.data.items[1].quantity").value(1))
                .andExpect(jsonPath("$.data.items[1].price").value(799.99));

        verify(orderService, times(1)).getOrderById(eq("order123"), anyString());
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该包含完整的地址信息 - 当获取订单详情时")
    void shouldIncludeCompleteAddressInfo_whenGettingOrderDetails() throws Exception {
        // Arrange
        when(orderService.getOrderById(anyString(), anyString())).thenReturn(orderResponse);

        // Act & Assert
        mockMvc.perform(get("/api/orders/order123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address.street").value("123 Main St"))
                .andExpect(jsonPath("$.data.address.city").value("Sydney"))
                .andExpect(jsonPath("$.data.address.state").value("NSW"))
                .andExpect(jsonPath("$.data.address.zip").value("2000"))
                .andExpect(jsonPath("$.data.address.country").value("Australia"));

        verify(orderService, times(1)).getOrderById(eq("order123"), anyString());
    }
}