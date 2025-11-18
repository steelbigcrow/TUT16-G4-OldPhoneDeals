package com.oldphonedeals.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldphonedeals.config.ControllerTestConfig;
import com.oldphonedeals.config.CorsConfig;
import com.oldphonedeals.config.FileStorageProperties;
import com.oldphonedeals.dto.request.phone.PhoneCreateRequest;
import com.oldphonedeals.dto.request.phone.PhoneUpdateRequest;
import com.oldphonedeals.dto.response.phone.PhoneListItemResponse;
import com.oldphonedeals.dto.response.phone.PhoneResponse;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.exception.ForbiddenException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.security.CustomUserDetailsService;
import com.oldphonedeals.security.JwtTokenProvider;
import com.oldphonedeals.service.PhoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PhoneController集成测试
 * 
 * 测试手机商品相关的REST API端点，包括：
 * - CRUD操作（创建、读取、更新、删除）
 * - 权限控制（只能操作自己的商品）
 * - 分页和过滤
 * - 公开访问端点
 * 
 * 使用@WebMvcTest进行Controller层集成测试
 */
@WebMvcTest(value = PhoneController.class,
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
@DisplayName("PhoneController集成测试")
class PhoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PhoneService phoneService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private FileStorageProperties fileStorageProperties;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private PhoneCreateRequest createRequest;
    private PhoneUpdateRequest updateRequest;
    private PhoneResponse phoneResponse;
    private PhoneListItemResponse listItemResponse;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        createRequest = PhoneCreateRequest.builder()
                .title("iPhone 12 Pro")
                .brand(PhoneBrand.APPLE)
                .image("iphone12.jpg")
                .stock(10)
                .price(999.99)
                .seller("user123")
                .build();

        updateRequest = PhoneUpdateRequest.builder()
                .title("iPhone 12 Pro Max")
                .brand(PhoneBrand.APPLE)
                .image("iphone12max.jpg")
                .stock(5)
                .price(1099.99)
                .build();

        phoneResponse = PhoneResponse.builder()
                .id("phone123")
                .title("iPhone 12 Pro")
                .brand(PhoneBrand.APPLE)
                .image("iphone12.jpg")
                .stock(10)
                .price(999.99)
                .isDisabled(false)
                .salesCount(0)
                .averageRating(0.0)
                .seller(PhoneResponse.SellerInfo.builder()
                        .id("user123")
                        .firstName("John")
                        .lastName("Doe")
                        .build())
                .reviews(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        listItemResponse = PhoneListItemResponse.builder()
                .id("phone123")
                .title("iPhone 12 Pro")
                .brand(PhoneBrand.APPLE)
                .image("iphone12.jpg")
                .price(999.99)
                .stock(10)
                .averageRating(0.0)
                .reviewCount(0)
                .build();
    }

    // ==================== 创建商品端点测试 ====================

    @Test
    @WithMockUser(username = "user123")
    @DisplayName("testCreatePhone_ValidRequest_ReturnsCreated")
    void testCreatePhone_ValidRequest_ReturnsCreated() throws Exception {
        // Arrange
        when(phoneService.createPhone(any(PhoneCreateRequest.class), anyString()))
                .thenReturn(phoneResponse);

        // Act & Assert
        mockMvc.perform(post("/api/phones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Phone created successfully"))
                .andExpect(jsonPath("$.data.id").value("phone123"))
                .andExpect(jsonPath("$.data.title").value("iPhone 12 Pro"))
                .andExpect(jsonPath("$.data.price").value(999.99));

        verify(phoneService, times(1)).createPhone(any(PhoneCreateRequest.class), anyString());
    }

    @Test
    @DisplayName("testCreatePhone_MissingTitle_ReturnsBadRequest")
    void testCreatePhone_MissingTitle_ReturnsBadRequest() throws Exception {
        // Arrange
        PhoneCreateRequest invalidRequest = PhoneCreateRequest.builder()
                .title("") // 空标题
                .brand(PhoneBrand.APPLE)
                .image("iphone12.jpg")
                .stock(10)
                .price(999.99)
                .seller("user123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/phones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(phoneService, never()).createPhone(any(PhoneCreateRequest.class), anyString());
    }

    @Test
    @DisplayName("testCreatePhone_NegativePrice_ReturnsBadRequest")
    void testCreatePhone_NegativePrice_ReturnsBadRequest() throws Exception {
        // Arrange
        PhoneCreateRequest invalidRequest = PhoneCreateRequest.builder()
                .title("iPhone 12 Pro")
                .brand(PhoneBrand.APPLE)
                .image("iphone12.jpg")
                .stock(10)
                .price(-100.0) // 负价格
                .seller("user123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/phones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(phoneService, never()).createPhone(any(PhoneCreateRequest.class), anyString());
    }

    @Test
    @DisplayName("testCreatePhone_NegativeStock_ReturnsBadRequest")
    void testCreatePhone_NegativeStock_ReturnsBadRequest() throws Exception {
        // Arrange
        PhoneCreateRequest invalidRequest = PhoneCreateRequest.builder()
                .title("iPhone 12 Pro")
                .brand(PhoneBrand.APPLE)
                .image("iphone12.jpg")
                .stock(-5) // 负库存
                .price(999.99)
                .seller("user123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/phones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(phoneService, never()).createPhone(any(PhoneCreateRequest.class), anyString());
    }

    @Test
    @DisplayName("testCreatePhone_MissingBrand_ReturnsBadRequest")
    void testCreatePhone_MissingBrand_ReturnsBadRequest() throws Exception {
        // Arrange
        PhoneCreateRequest invalidRequest = PhoneCreateRequest.builder()
                .title("iPhone 12 Pro")
                .brand(null) // 缺少品牌
                .image("iphone12.jpg")
                .stock(10)
                .price(999.99)
                .seller("user123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/phones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(phoneService, never()).createPhone(any(PhoneCreateRequest.class), anyString());
    }

    // ==================== 更新商品端点测试 ====================

    @Test
    @WithMockUser(username = "user123")
    @DisplayName("testUpdatePhone_ValidRequest_ReturnsOk")
    void testUpdatePhone_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        PhoneResponse updatedResponse = PhoneResponse.builder()
                .id("phone123")
                .title("iPhone 12 Pro Max")
                .brand(PhoneBrand.APPLE)
                .image("iphone12max.jpg")
                .stock(5)
                .price(1099.99)
                .isDisabled(false)
                .salesCount(0)
                .averageRating(0.0)
                .seller(PhoneResponse.SellerInfo.builder()
                        .id("user123")
                        .firstName("John")
                        .lastName("Doe")
                        .build())
                .reviews(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(phoneService.updatePhone(anyString(), any(PhoneUpdateRequest.class), anyString()))
                .thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/phones/phone123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Phone updated successfully"))
                .andExpect(jsonPath("$.data.title").value("iPhone 12 Pro Max"))
                .andExpect(jsonPath("$.data.price").value(1099.99));

        verify(phoneService, times(1)).updatePhone(anyString(), any(PhoneUpdateRequest.class), anyString());
    }

    @Test
    @WithMockUser(username = "user123")
    @DisplayName("testUpdatePhone_UnauthorizedUser_ReturnsForbidden")
    void testUpdatePhone_UnauthorizedUser_ReturnsForbidden() throws Exception {
        // Arrange
        when(phoneService.updatePhone(anyString(), any(PhoneUpdateRequest.class), anyString()))
                .thenThrow(new ForbiddenException("Not authorized to update this phone"));

        // Act & Assert
        mockMvc.perform(put("/api/phones/phone123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        verify(phoneService, times(1)).updatePhone(anyString(), any(PhoneUpdateRequest.class), anyString());
    }

    @Test
    @WithMockUser(username = "user123")
    @DisplayName("testUpdatePhone_PhoneNotFound_ReturnsNotFound")
    void testUpdatePhone_PhoneNotFound_ReturnsNotFound() throws Exception {
        // Arrange
        when(phoneService.updatePhone(anyString(), any(PhoneUpdateRequest.class), anyString()))
                .thenThrow(new ResourceNotFoundException("Phone not found"));

        // Act & Assert
        mockMvc.perform(put("/api/phones/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        verify(phoneService, times(1)).updatePhone(anyString(), any(PhoneUpdateRequest.class), anyString());
    }

    // ==================== 删除商品端点测试 ====================

    @Test
    @WithMockUser(username = "user123")
    @DisplayName("testDeletePhone_ValidRequest_ReturnsOk")
    void testDeletePhone_ValidRequest_ReturnsOk() throws Exception {
        // Arrange
        doNothing().when(phoneService).deletePhone(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(delete("/api/phones/phone123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Phone deleted successfully"));

        verify(phoneService, times(1)).deletePhone(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "user123")
    @DisplayName("testDeletePhone_UnauthorizedUser_ReturnsForbidden")
    void testDeletePhone_UnauthorizedUser_ReturnsForbidden() throws Exception {
        // Arrange
        doThrow(new ForbiddenException("Not authorized to delete this phone"))
                .when(phoneService).deletePhone(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(delete("/api/phones/phone123"))
                .andExpect(status().isForbidden());

        verify(phoneService, times(1)).deletePhone(anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "user123")
    @DisplayName("testDeletePhone_PhoneNotFound_ReturnsNotFound")
    void testDeletePhone_PhoneNotFound_ReturnsNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Phone not found"))
                .when(phoneService).deletePhone(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(delete("/api/phones/nonexistent"))
                .andExpect(status().isNotFound());

        verify(phoneService, times(1)).deletePhone(anyString(), anyString());
    }

    // ==================== 获取商品详情端点测试 ====================

    @Test
    @DisplayName("testGetPhoneById_ValidId_ReturnsOk")
    void testGetPhoneById_ValidId_ReturnsOk() throws Exception {
        // Arrange
        when(phoneService.getPhoneById(anyString(), isNull()))
                .thenReturn(phoneResponse);

        // Act & Assert
        mockMvc.perform(get("/api/phones/phone123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Phone retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value("phone123"))
                .andExpect(jsonPath("$.data.title").value("iPhone 12 Pro"));

        verify(phoneService, times(1)).getPhoneById(anyString(), isNull());
    }

    @Test
    @DisplayName("testGetPhoneById_PhoneNotFound_ReturnsNotFound")
    void testGetPhoneById_PhoneNotFound_ReturnsNotFound() throws Exception {
        // Arrange
        when(phoneService.getPhoneById(anyString(), isNull()))
                .thenThrow(new ResourceNotFoundException("Phone not found"));

        // Act & Assert
        mockMvc.perform(get("/api/phones/nonexistent"))
                .andExpect(status().isNotFound());

        verify(phoneService, times(1)).getPhoneById(anyString(), isNull());
    }

    // ==================== 获取商品列表端点测试 ====================

    @Test
    @DisplayName("testGetAllPhones_DefaultParameters_ReturnsOk")
    void testGetAllPhones_DefaultParameters_ReturnsOk() throws Exception {
        // Arrange
        Map<String, Object> response = new HashMap<>();
        response.put("phones", List.of(listItemResponse));
        response.put("currentPage", 1);
        response.put("totalPages", 1);
        response.put("totalItems", 1);

        when(phoneService.getPhones(isNull(), isNull(), isNull(), eq("createdAt"), eq("desc"), eq(1), eq(12)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/phones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Phones retrieved successfully"))
                .andExpect(jsonPath("$.data.phones").isArray())
                .andExpect(jsonPath("$.data.currentPage").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));

        verify(phoneService, times(1)).getPhones(isNull(), isNull(), isNull(), eq("createdAt"), eq("desc"), eq(1), eq(12));
    }

    @Test
    @DisplayName("testGetAllPhones_WithPagination_ReturnsCorrectPage")
    void testGetAllPhones_WithPagination_ReturnsCorrectPage() throws Exception {
        // Arrange
        Map<String, Object> response = new HashMap<>();
        response.put("phones", List.of(listItemResponse));
        response.put("currentPage", 2);
        response.put("totalPages", 5);
        response.put("totalItems", 50);

        when(phoneService.getPhones(isNull(), isNull(), isNull(), eq("createdAt"), eq("desc"), eq(2), eq(10)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/phones")
                        .param("page", "2")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentPage").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(5))
                .andExpect(jsonPath("$.data.totalItems").value(50));

        verify(phoneService, times(1)).getPhones(isNull(), isNull(), isNull(), eq("createdAt"), eq("desc"), eq(2), eq(10));
    }

    @Test
    @DisplayName("testGetAllPhones_WithSearchFilter_ReturnsFilteredResults")
    void testGetAllPhones_WithSearchFilter_ReturnsFilteredResults() throws Exception {
        // Arrange
        Map<String, Object> response = new HashMap<>();
        response.put("phones", List.of(listItemResponse));
        response.put("currentPage", 1);
        response.put("totalPages", 1);
        response.put("totalItems", 1);

        when(phoneService.getPhones(eq("iPhone"), isNull(), isNull(), eq("createdAt"), eq("desc"), eq(1), eq(12)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/phones")
                        .param("search", "iPhone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phones").isArray());

        verify(phoneService, times(1)).getPhones(eq("iPhone"), isNull(), isNull(), eq("createdAt"), eq("desc"), eq(1), eq(12));
    }

    @Test
    @DisplayName("testGetAllPhones_WithBrandFilter_ReturnsFilteredResults")
    void testGetAllPhones_WithBrandFilter_ReturnsFilteredResults() throws Exception {
        // Arrange
        Map<String, Object> response = new HashMap<>();
        response.put("phones", List.of(listItemResponse));
        response.put("currentPage", 1);
        response.put("totalPages", 1);
        response.put("totalItems", 1);

        when(phoneService.getPhones(isNull(), eq(PhoneBrand.APPLE), isNull(), eq("createdAt"), eq("desc"), eq(1), eq(12)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/phones")
                        .param("brand", "APPLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phones").isArray());

        verify(phoneService, times(1)).getPhones(isNull(), eq(PhoneBrand.APPLE), isNull(), eq("createdAt"), eq("desc"), eq(1), eq(12));
    }

    @Test
    @DisplayName("testGetAllPhones_WithMaxPriceFilter_ReturnsFilteredResults")
    void testGetAllPhones_WithMaxPriceFilter_ReturnsFilteredResults() throws Exception {
        // Arrange
        Map<String, Object> response = new HashMap<>();
        response.put("phones", List.of(listItemResponse));
        response.put("currentPage", 1);
        response.put("totalPages", 1);
        response.put("totalItems", 1);

        when(phoneService.getPhones(isNull(), isNull(), eq(1000.0), eq("createdAt"), eq("desc"), eq(1), eq(12)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/phones")
                        .param("maxPrice", "1000.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phones").isArray());

        verify(phoneService, times(1)).getPhones(isNull(), isNull(), eq(1000.0), eq("createdAt"), eq("desc"), eq(1), eq(12));
    }

    @Test
    @DisplayName("testGetAllPhones_WithSortByPrice_ReturnsOrderedResults")
    void testGetAllPhones_WithSortByPrice_ReturnsOrderedResults() throws Exception {
        // Arrange
        Map<String, Object> response = new HashMap<>();
        response.put("phones", List.of(listItemResponse));
        response.put("currentPage", 1);
        response.put("totalPages", 1);
        response.put("totalItems", 1);

        when(phoneService.getPhones(isNull(), isNull(), isNull(), eq("price"), eq("asc"), eq(1), eq(12)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/phones")
                        .param("sortBy", "price")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phones").isArray());

        verify(phoneService, times(1)).getPhones(isNull(), isNull(), isNull(), eq("price"), eq("asc"), eq(1), eq(12));
    }

    @Test
    @DisplayName("testGetAllPhones_WithMultipleFilters_ReturnsFilteredResults")
    void testGetAllPhones_WithMultipleFilters_ReturnsFilteredResults() throws Exception {
        // Arrange
        Map<String, Object> response = new HashMap<>();
        response.put("phones", List.of(listItemResponse));
        response.put("currentPage", 1);
        response.put("totalPages", 1);
        response.put("totalItems", 1);

        when(phoneService.getPhones(eq("iPhone"), eq(PhoneBrand.APPLE), eq(1000.0), eq("price"), eq("asc"), eq(1), eq(12)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/phones")
                        .param("search", "iPhone")
                        .param("brand", "APPLE")
                        .param("maxPrice", "1000.0")
                        .param("sortBy", "price")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.phones").isArray());

        verify(phoneService, times(1)).getPhones(eq("iPhone"), eq(PhoneBrand.APPLE), eq(1000.0), eq("price"), eq("asc"), eq(1), eq(12));
    }

    // ==================== Special 列表端点测试 ====================

    @Test
    @DisplayName("testGetAllPhones_WithSpecialSoldOutSoon_ReturnsSpecialList")
    void testGetAllPhones_WithSpecialSoldOutSoon_ReturnsSpecialList() throws Exception {
        // Arrange
        when(phoneService.getSoldOutSoonPhones())
                .thenReturn(List.of(listItemResponse));

        // Act & Assert
        mockMvc.perform(get("/api/phones")
                        .param("special", "soldOutSoon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Sold-out-soon phones retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray());

        verify(phoneService, times(1)).getSoldOutSoonPhones();
        verify(phoneService, never()).getPhones(any(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("testGetAllPhones_WithSpecialBestSellers_ReturnsSpecialList")
    void testGetAllPhones_WithSpecialBestSellers_ReturnsSpecialList() throws Exception {
        // Arrange
        when(phoneService.getBestSellers())
                .thenReturn(List.of(listItemResponse));

        // Act & Assert
        mockMvc.perform(get("/api/phones")
                        .param("special", "bestSellers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Best-seller phones retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray());

        verify(phoneService, times(1)).getBestSellers();
        verify(phoneService, never()).getPhones(any(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    // ==================== 按卖家列出商品端点测试 ====================

    @Test
    @DisplayName("testGetPhonesBySeller_ValidSellerId_ReturnsOk")
    void testGetPhonesBySeller_ValidSellerId_ReturnsOk() throws Exception {
        // Arrange
        when(phoneService.getPhonesBySeller(eq("seller123")))
                .thenReturn(List.of(phoneResponse));

        // Act & Assert
        mockMvc.perform(get("/api/phones/by-seller/seller123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Seller phones retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value("phone123"));

        verify(phoneService, times(1)).getPhonesBySeller(eq("seller123"));
    }

    @Test
    @DisplayName("testGetPhonesBySeller_SellerNotFound_ReturnsNotFound")
    void testGetPhonesBySeller_SellerNotFound_ReturnsNotFound() throws Exception {
        // Arrange
        when(phoneService.getPhonesBySeller(eq("nonexistent")))
                .thenThrow(new ResourceNotFoundException("Seller not found"));

        // Act & Assert
        mockMvc.perform(get("/api/phones/by-seller/nonexistent"))
                .andExpect(status().isNotFound());

        verify(phoneService, times(1)).getPhonesBySeller(eq("nonexistent"));
    }
}