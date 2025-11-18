package com.oldphonedeals.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldphonedeals.config.ControllerTestConfig;
import com.oldphonedeals.config.CorsConfig;
import com.oldphonedeals.config.FileStorageProperties;
import com.oldphonedeals.dto.request.wishlist.AddToWishlistRequest;
import com.oldphonedeals.dto.response.phone.PhoneListItemResponse;
import com.oldphonedeals.dto.response.wishlist.WishlistResponse;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.security.CustomUserDetailsService;
import com.oldphonedeals.security.JwtTokenProvider;
import com.oldphonedeals.service.WishlistService;
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
 * WishlistController集成测试
 * <p>
 * 测试用户收藏夹相关的REST API端点，包括：
 * - 添加商品到收藏夹
 * - 从收藏夹删除商品
 * - 获取用户收藏夹
 * </p>
 * 
 * 使用@WebMvcTest进行Controller层集成测试
 */
@WebMvcTest(value = WishlistController.class,
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
@DisplayName("WishlistController集成测试")
class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WishlistService wishlistService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private FileStorageProperties fileStorageProperties;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private String testUserId;
    private String testPhoneId1;
    private String testPhoneId2;
    private PhoneListItemResponse phone1;
    private PhoneListItemResponse phone2;
    private WishlistResponse emptyWishlistResponse;
    private WishlistResponse wishlistWithItemsResponse;
    private AddToWishlistRequest addToWishlistRequest;

    @BeforeEach
    void setUp() {
        // 准备测试数据 - 用户ID和商品ID
        testUserId = "user123";
        testPhoneId1 = "phone123";
        testPhoneId2 = "phone456";

        // 准备测试数据 - 商品1
        phone1 = PhoneListItemResponse.builder()
                .id(testPhoneId1)
                .title("iPhone 12 Pro")
                .brand(PhoneBrand.APPLE)
                .image("/images/iphone12pro.jpg")
                .stock(10)
                .price(999.99)
                .averageRating(4.5)
                .reviewCount(25)
                .seller(PhoneListItemResponse.SellerInfo.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .build())
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();

        // 准备测试数据 - 商品2
        phone2 = PhoneListItemResponse.builder()
                .id(testPhoneId2)
                .title("Samsung Galaxy S21")
                .brand(PhoneBrand.SAMSUNG)
                .image("/images/galaxys21.jpg")
                .stock(5)
                .price(799.99)
                .averageRating(4.3)
                .reviewCount(18)
                .seller(PhoneListItemResponse.SellerInfo.builder()
                        .firstName("Jane")
                        .lastName("Smith")
                        .build())
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        // 准备测试数据 - 空收藏夹
        emptyWishlistResponse = WishlistResponse.builder()
                .userId(testUserId)
                .phones(new ArrayList<>())
                .totalItems(0)
                .build();

        // 准备测试数据 - 有商品的收藏夹
        wishlistWithItemsResponse = WishlistResponse.builder()
                .userId(testUserId)
                .phones(List.of(phone1, phone2))
                .totalItems(2)
                .build();

        // 准备测试数据 - 添加到收藏夹请求
        addToWishlistRequest = AddToWishlistRequest.builder()
                .phoneId(testPhoneId1)
                .build();
    }

    // ==================== 获取收藏夹端点测试 ====================

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功获取空收藏夹 - 当用户已认证且收藏夹为空时")
    void shouldReturnEmptyWishlist_whenUserAuthenticatedAndWishlistEmpty() throws Exception {
        // Arrange
        when(wishlistService.getUserWishlist(anyString())).thenReturn(emptyWishlistResponse);

        // Act & Assert
        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Wishlist retrieved successfully"))
                .andExpect(jsonPath("$.data.userId").value(testUserId))
                .andExpect(jsonPath("$.data.phones").isArray())
                .andExpect(jsonPath("$.data.phones").isEmpty())
                .andExpect(jsonPath("$.data.totalItems").value(0));

        verify(wishlistService, times(1)).getUserWishlist(anyString());
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功获取包含商品的收藏夹 - 当用户已认证且收藏夹有商品时")
    void shouldReturnWishlistWithItems_whenUserAuthenticatedAndWishlistHasItems() throws Exception {
        // Arrange
        when(wishlistService.getUserWishlist(anyString())).thenReturn(wishlistWithItemsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Wishlist retrieved successfully"))
                .andExpect(jsonPath("$.data.userId").value(testUserId))
                .andExpect(jsonPath("$.data.phones").isArray())
                .andExpect(jsonPath("$.data.phones.length()").value(2))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.phones[0].id").value(testPhoneId1))
                .andExpect(jsonPath("$.data.phones[0].title").value("iPhone 12 Pro"))
                .andExpect(jsonPath("$.data.phones[0].price").value(999.99))
                .andExpect(jsonPath("$.data.phones[1].id").value(testPhoneId2));

        verify(wishlistService, times(1)).getUserWishlist(anyString());
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回404错误 - 当用户不存在时")
    void shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        // Arrange
        when(wishlistService.getUserWishlist(anyString()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isNotFound());

        verify(wishlistService, times(1)).getUserWishlist(anyString());
    }

    // ==================== 添加商品到收藏夹端点测试 ====================

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功添加商品到收藏夹 - 当请求有效时")
    void shouldAddItemToWishlist_whenValidRequest() throws Exception {
        // Arrange
        WishlistResponse updatedWishlist = WishlistResponse.builder()
                .userId(testUserId)
                .phones(List.of(phone1))
                .totalItems(1)
                .build();

        when(wishlistService.addToWishlist(anyString(), eq(testPhoneId1)))
                .thenReturn(updatedWishlist);

        // Act & Assert
        mockMvc.perform(post("/api/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToWishlistRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Phone added to wishlist successfully"))
                .andExpect(jsonPath("$.data.userId").value(testUserId))
                .andExpect(jsonPath("$.data.phones.length()").value(1))
                .andExpect(jsonPath("$.data.phones[0].id").value(testPhoneId1))
                .andExpect(jsonPath("$.data.totalItems").value(1));

        verify(wishlistService, times(1)).addToWishlist(anyString(), eq(testPhoneId1));
    }

    @Test
    @DisplayName("应该返回400错误 - 当phoneId为空时")
    void shouldReturnBadRequest_whenPhoneIdIsBlank() throws Exception {
        // Arrange
        AddToWishlistRequest invalidRequest = AddToWishlistRequest.builder()
                .phoneId("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(wishlistService, never()).addToWishlist(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回404错误 - 当商品不存在时")
    void shouldReturnNotFound_whenPhoneDoesNotExist() throws Exception {
        // Arrange
        when(wishlistService.addToWishlist(anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("Phone not found"));

        // Act & Assert
        mockMvc.perform(post("/api/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToWishlistRequest)))
                .andExpect(status().isNotFound());

        verify(wishlistService, times(1)).addToWishlist(anyString(), eq(testPhoneId1));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回400错误 - 当商品已在收藏夹中时")
    void shouldReturnBadRequest_whenPhoneAlreadyInWishlist() throws Exception {
        // Arrange
        when(wishlistService.addToWishlist(anyString(), anyString()))
                .thenThrow(new BadRequestException("Phone is already in wishlist"));

        // Act & Assert
        mockMvc.perform(post("/api/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToWishlistRequest)))
                .andExpect(status().isBadRequest());

        verify(wishlistService, times(1)).addToWishlist(anyString(), eq(testPhoneId1));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回400错误 - 当商品已禁用时")
    void shouldReturnBadRequest_whenPhoneIsDisabled() throws Exception {
        // Arrange
        when(wishlistService.addToWishlist(anyString(), anyString()))
                .thenThrow(new BadRequestException("Cannot add disabled phone to wishlist"));

        // Act & Assert
        mockMvc.perform(post("/api/wishlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addToWishlistRequest)))
                .andExpect(status().isBadRequest());

        verify(wishlistService, times(1)).addToWishlist(anyString(), eq(testPhoneId1));
    }

    // ==================== 从收藏夹删除商品端点测试 ====================

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功从收藏夹删除商品 - 当商品存在于收藏夹时")
    void shouldRemoveItemFromWishlist_whenItemExistsInWishlist() throws Exception {
        // Arrange
        WishlistResponse updatedWishlist = WishlistResponse.builder()
                .userId(testUserId)
                .phones(List.of(phone2))
                .totalItems(1)
                .build();

        when(wishlistService.removeFromWishlist(anyString(), eq(testPhoneId1)))
                .thenReturn(updatedWishlist);

        // Act & Assert
        mockMvc.perform(delete("/api/wishlist/{phoneId}", testPhoneId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Phone removed from wishlist successfully"))
                .andExpect(jsonPath("$.data.userId").value(testUserId))
                .andExpect(jsonPath("$.data.phones.length()").value(1))
                .andExpect(jsonPath("$.data.phones[0].id").value(testPhoneId2))
                .andExpect(jsonPath("$.data.totalItems").value(1));

        verify(wishlistService, times(1)).removeFromWishlist(anyString(), eq(testPhoneId1));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该成功删除商品后返回空收藏夹 - 当删除最后一个商品时")
    void shouldReturnEmptyWishlist_whenRemovingLastItem() throws Exception {
        // Arrange
        when(wishlistService.removeFromWishlist(anyString(), eq(testPhoneId1)))
                .thenReturn(emptyWishlistResponse);

        // Act & Assert
        mockMvc.perform(delete("/api/wishlist/{phoneId}", testPhoneId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Phone removed from wishlist successfully"))
                .andExpect(jsonPath("$.data.phones").isEmpty())
                .andExpect(jsonPath("$.data.totalItems").value(0));

        verify(wishlistService, times(1)).removeFromWishlist(anyString(), eq(testPhoneId1));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回404错误 - 当删除不在收藏夹中的商品时")
    void shouldReturnNotFound_whenRemovingNonexistentItem() throws Exception {
        // Arrange
        when(wishlistService.removeFromWishlist(anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("Phone not found in wishlist"));

        // Act & Assert
        mockMvc.perform(delete("/api/wishlist/{phoneId}", "nonexistent"))
                .andExpect(status().isNotFound());

        verify(wishlistService, times(1)).removeFromWishlist(anyString(), eq("nonexistent"));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该返回404错误 - 当用户不存在时")
    void shouldReturnNotFound_whenUserDoesNotExistWhenRemoving() throws Exception {
        // Arrange
        when(wishlistService.removeFromWishlist(anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(delete("/api/wishlist/{phoneId}", testPhoneId1))
                .andExpect(status().isNotFound());

        verify(wishlistService, times(1)).removeFromWishlist(anyString(), eq(testPhoneId1));
    }

    @Test
    @WithMockUser(username = "user123", roles = "USER")
    @DisplayName("应该包含完整的商品信息 - 当获取收藏夹时")
    void shouldIncludeCompletePhoneInfo_whenGettingWishlist() throws Exception {
        // Arrange
        when(wishlistService.getUserWishlist(anyString())).thenReturn(wishlistWithItemsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phones[0].id").value(testPhoneId1))
                .andExpect(jsonPath("$.data.phones[0].title").value("iPhone 12 Pro"))
                .andExpect(jsonPath("$.data.phones[0].brand").value("APPLE"))
                .andExpect(jsonPath("$.data.phones[0].price").value(999.99))
                .andExpect(jsonPath("$.data.phones[0].stock").value(10))
                .andExpect(jsonPath("$.data.phones[0].averageRating").value(4.5))
                .andExpect(jsonPath("$.data.phones[0].reviewCount").value(25))
                .andExpect(jsonPath("$.data.phones[0].seller").exists())
                .andExpect(jsonPath("$.data.phones[0].seller.firstName").value("John"))
                .andExpect(jsonPath("$.data.phones[0].seller.lastName").value("Doe"));

        verify(wishlistService, times(1)).getUserWishlist(anyString());
    }
}