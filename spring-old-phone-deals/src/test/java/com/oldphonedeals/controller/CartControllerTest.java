package com.oldphonedeals.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldphonedeals.config.ControllerTestConfig;
import com.oldphonedeals.config.CorsConfig;
import com.oldphonedeals.config.FileStorageProperties;
import com.oldphonedeals.dto.request.cart.AddToCartRequest;
import com.oldphonedeals.dto.request.cart.UpdateCartItemRequest;
import com.oldphonedeals.dto.response.cart.CartItemResponse;
import com.oldphonedeals.dto.response.cart.CartResponse;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.security.CustomUserDetailsService;
import com.oldphonedeals.security.JwtTokenProvider;
import com.oldphonedeals.service.CartService;
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
 * CartController集成测试
 * <p>
 * 测试购物车相关的REST API端点，包括：
 * - 获取购物车
 * - 添加商品到购物车
 * - 更新购物车商品数量
 * - 删除购物车商品
 * </p>
 * 
 * 使用@WebMvcTest进行Controller层集成测试
 */
@WebMvcTest(value = CartController.class,
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
@DisplayName("CartController集成测试")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private FileStorageProperties fileStorageProperties;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private CartResponse emptyCartResponse;
    private CartResponse cartWithItemsResponse;
    private CartItemResponse cartItem1;
    private CartItemResponse cartItem2;
    private AddToCartRequest addToCartRequest;
    private UpdateCartItemRequest updateCartItemRequest;

    @BeforeEach
    void setUp() {
        // 准备测试数据 - 空购物车
        emptyCartResponse = CartResponse.builder()
                .id("cart123")
                .userId("user123")
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 准备测试数据 - 购物车商品项1
        cartItem1 = CartItemResponse.builder()
                .phoneId("phone123")
                .title("iPhone 12 Pro")
                .quantity(2)
                .price(999.99)
                .averageRating(4.5)
                .reviewCount(10)
                .seller(CartItemResponse.SellerInfo.builder()
                        .id("seller123")
                        .firstName("John")
                        .lastName("Doe")
                        .build())
                .createdAt(LocalDateTime.now())
                .build();

        // 准备测试数据 - 购物车商品项2
        cartItem2 = CartItemResponse.builder()
                .phoneId("phone456")
                .title("Samsung Galaxy S21")
                .quantity(1)
                .price(799.99)
                .averageRating(4.3)
                .reviewCount(8)
                .seller(CartItemResponse.SellerInfo.builder()
                        .id("seller456")
                        .firstName("Jane")
                        .lastName("Smith")
                        .build())
                .createdAt(LocalDateTime.now())
                .build();

        // 准备测试数据 - 有商品的购物车
        cartWithItemsResponse = CartResponse.builder()
                .id("cart123")
                .userId("user123")
                .items(List.of(cartItem1, cartItem2))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 准备测试数据 - 添加到购物车请求
        addToCartRequest = AddToCartRequest.builder()
                .phoneId("phone123")
                .quantity(2)
                .build();

        // 准备测试数据 - 更新购物车商品请求
        updateCartItemRequest = UpdateCartItemRequest.builder()
                .quantity(3)
                .build();
    }

    // ==================== 获取购物车端点测试 ====================

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功获取空购物车 - 当用户已认证且购物车为空时")
    void shouldReturnEmptyCart_whenUserAuthenticatedAndCartEmpty() throws Exception {
        // Arrange
        when(cartService.getUserCart(anyString())).thenReturn(emptyCartResponse);

        // Act & Assert
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cart retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value("cart123"))
                .andExpect(jsonPath("$.data.userId").value("user123"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").isEmpty());

        verify(cartService, times(1)).getUserCart(anyString());
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功获取包含商品的购物车 - 当用户已认证且购物车有商品时")
    void shouldReturnCartWithItems_whenUserAuthenticatedAndCartHasItems() throws Exception {
        // Arrange
        when(cartService.getUserCart(anyString())).thenReturn(cartWithItemsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cart retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value("cart123"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].phoneId").value("phone123"))
                .andExpect(jsonPath("$.data.items[0].title").value("iPhone 12 Pro"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].price").value(999.99))
                .andExpect(jsonPath("$.data.items[1].phoneId").value("phone456"));

        verify(cartService, times(1)).getUserCart(anyString());
    }

    // ==================== 添加商品到购物车端点测试 ====================

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功添加商品到购物车 - 当请求有效时")
    void shouldAddItemToCart_whenValidRequest() throws Exception {
        // Arrange
        CartResponse updatedCart = CartResponse.builder()
                .id("cart123")
                .userId("user123")
                .items(List.of(cartItem1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartService.addToCart(anyString(), any(AddToCartRequest.class)))
                .thenReturn(updatedCart);

        // Act & Assert
        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCartRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item added to cart successfully"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].phoneId").value("phone123"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        verify(cartService, times(1)).addToCart(anyString(), any(AddToCartRequest.class));
    }

    @Test
    @DisplayName("应该返回400错误 - 当phoneId为空时")
    void shouldReturnBadRequest_whenPhoneIdIsBlank() throws Exception {
        // Arrange
        AddToCartRequest invalidRequest = AddToCartRequest.builder()
                .phoneId("")
                .quantity(2)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).addToCart(anyString(), any(AddToCartRequest.class));
    }

    @Test
    @DisplayName("应该返回400错误 - 当数量小于1时")
    void shouldReturnBadRequest_whenQuantityLessThanOne() throws Exception {
        // Arrange
        AddToCartRequest invalidRequest = AddToCartRequest.builder()
                .phoneId("phone123")
                .quantity(0)
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).addToCart(anyString(), any(AddToCartRequest.class));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回404错误 - 当商品不存在时")
    void shouldReturnNotFound_whenPhoneDoesNotExist() throws Exception {
        // Arrange
        when(cartService.addToCart(anyString(), any(AddToCartRequest.class)))
                .thenThrow(new ResourceNotFoundException("Phone not found"));

        // Act & Assert
        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCartRequest)))
                .andExpect(status().isNotFound());

        verify(cartService, times(1)).addToCart(anyString(), any(AddToCartRequest.class));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回400错误 - 当商品库存不足时")
    void shouldReturnBadRequest_whenInsufficientStock() throws Exception {
        // Arrange
        when(cartService.addToCart(anyString(), any(AddToCartRequest.class)))
                .thenThrow(new BadRequestException("Insufficient stock"));

        // Act & Assert
        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCartRequest)))
                .andExpect(status().isBadRequest());

        verify(cartService, times(1)).addToCart(anyString(), any(AddToCartRequest.class));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回400错误 - 当商品已禁用时")
    void shouldReturnBadRequest_whenPhoneIsDisabled() throws Exception {
        // Arrange
        when(cartService.addToCart(anyString(), any(AddToCartRequest.class)))
                .thenThrow(new BadRequestException("Phone is disabled"));

        // Act & Assert
        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToCartRequest)))
                .andExpect(status().isBadRequest());

        verify(cartService, times(1)).addToCart(anyString(), any(AddToCartRequest.class));
    }

    // ==================== 更新购物车商品数量端点测试 ====================

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功更新购物车商品数量 - 当请求有效时")
    void shouldUpdateCartItemQuantity_whenValidRequest() throws Exception {
        // Arrange
        CartItemResponse updatedItem = CartItemResponse.builder()
                .phoneId("phone123")
                .title("iPhone 12 Pro")
                .quantity(3)
                .price(999.99)
                .averageRating(4.5)
                .reviewCount(10)
                .seller(CartItemResponse.SellerInfo.builder()
                        .id("seller123")
                        .firstName("John")
                        .lastName("Doe")
                        .build())
                .createdAt(LocalDateTime.now())
                .build();

        CartResponse updatedCart = CartResponse.builder()
                .id("cart123")
                .userId("user123")
                .items(List.of(updatedItem))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartService.updateCartItem(anyString(), anyString(), any(UpdateCartItemRequest.class)))
                .thenReturn(updatedCart);

        // Act & Assert
        mockMvc.perform(put("/api/cart/phone123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCartItemRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cart item updated successfully"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3));

        verify(cartService, times(1)).updateCartItem(anyString(), eq("phone123"), any(UpdateCartItemRequest.class));
    }

    @Test
    @DisplayName("应该返回400错误 - 当更新数量小于1时")
    void shouldReturnBadRequest_whenUpdateQuantityLessThanOne() throws Exception {
        // Arrange
        UpdateCartItemRequest invalidRequest = UpdateCartItemRequest.builder()
                .quantity(0)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/cart/phone123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).updateCartItem(anyString(), anyString(), any(UpdateCartItemRequest.class));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回404错误 - 当购物车中没有该商品时")
    void shouldReturnNotFound_whenItemNotInCart() throws Exception {
        // Arrange
        when(cartService.updateCartItem(anyString(), anyString(), any(UpdateCartItemRequest.class)))
                .thenThrow(new ResourceNotFoundException("Item not found in cart"));

        // Act & Assert
        mockMvc.perform(put("/api/cart/phone999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCartItemRequest)))
                .andExpect(status().isNotFound());

        verify(cartService, times(1)).updateCartItem(anyString(), eq("phone999"), any(UpdateCartItemRequest.class));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回400错误 - 当更新数量超过库存时")
    void shouldReturnBadRequest_whenUpdateQuantityExceedsStock() throws Exception {
        // Arrange
        when(cartService.updateCartItem(anyString(), anyString(), any(UpdateCartItemRequest.class)))
                .thenThrow(new BadRequestException("Quantity exceeds available stock"));

        // Act & Assert
        mockMvc.perform(put("/api/cart/phone123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCartItemRequest)))
                .andExpect(status().isBadRequest());

        verify(cartService, times(1)).updateCartItem(anyString(), eq("phone123"), any(UpdateCartItemRequest.class));
    }

    // ==================== 删除购物车商品端点测试 ====================

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功删除购物车商品 - 当商品存在时")
    void shouldRemoveItemFromCart_whenItemExists() throws Exception {
        // Arrange
        when(cartService.removeFromCart(anyString(), anyString()))
                .thenReturn(emptyCartResponse);

        // Act & Assert
        mockMvc.perform(delete("/api/cart/phone123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item removed from cart successfully"))
                .andExpect(jsonPath("$.data.items").isEmpty());

        verify(cartService, times(1)).removeFromCart(anyString(), eq("phone123"));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回404错误 - 当删除不存在的商品时")
    void shouldReturnNotFound_whenRemovingNonExistentItem() throws Exception {
        // Arrange
        when(cartService.removeFromCart(anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("Item not found in cart"));

        // Act & Assert
        mockMvc.perform(delete("/api/cart/phone999"))
                .andExpect(status().isNotFound());

        verify(cartService, times(1)).removeFromCart(anyString(), eq("phone999"));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功删除商品后返回剩余商品 - 当购物车还有其他商品时")
    void shouldReturnRemainingItems_whenRemovingOneItemFromMultiple() throws Exception {
        // Arrange
        CartResponse cartWithOneItem = CartResponse.builder()
                .id("cart123")
                .userId("user123")
                .items(List.of(cartItem2))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cartService.removeFromCart(anyString(), eq("phone123")))
                .thenReturn(cartWithOneItem);

        // Act & Assert
        mockMvc.perform(delete("/api/cart/phone123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item removed from cart successfully"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].phoneId").value("phone456"));

        verify(cartService, times(1)).removeFromCart(anyString(), eq("phone123"));
    }
}