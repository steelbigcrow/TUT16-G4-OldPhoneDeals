package com.oldphonedeals.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oldphonedeals.config.ControllerTestConfig;
import com.oldphonedeals.config.CorsConfig;
import com.oldphonedeals.config.FileStorageProperties;
import com.oldphonedeals.dto.request.admin.ToggleReviewVisibilityRequest;
import com.oldphonedeals.dto.request.phone.ReviewCreateRequest;
import com.oldphonedeals.dto.response.phone.ReviewResponse;
import com.oldphonedeals.dto.response.phone.SellerReviewResponse;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.security.CustomUserDetailsService;
import com.oldphonedeals.security.JwtTokenProvider;
import com.oldphonedeals.service.ReviewService;
import com.oldphonedeals.repository.PhoneRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ReviewController 集成测试
 *
 * 覆盖评论相关接口：
 * - 获取某手机的评论列表
 * - 添加评论
 * - 切换评论可见性
 * - 卖家查看自己的所有评论
 */
@WebMvcTest(value = ReviewController.class,
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
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ReviewController 集成测试")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private PhoneRepository phoneRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private FileStorageProperties fileStorageProperties;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private Phone phoneWithReviews;
    private ReviewResponse review1;
    private ReviewResponse review2;
    private ReviewResponse review3;

    @BeforeEach
    void setUp() {
        User seller = User.builder()
                .id("seller-1")
                .firstName("Alice")
                .lastName("Seller")
                .build();

        phoneWithReviews = Phone.builder()
                .id("phone-1")
                .title("Test Phone")
                .seller(seller)
                .build();

        review1 = ReviewResponse.builder()
                .id("r1")
                .reviewerId("user-1")
                .rating(5)
                .comment("Great")
                .isHidden(false)
                .reviewer("User One")
                .createdAt(LocalDateTime.now())
                .build();

        review2 = ReviewResponse.builder()
                .id("r2")
                .reviewerId("user-2")
                .rating(4)
                .comment("Good")
                .isHidden(false)
                .reviewer("User Two")
                .createdAt(LocalDateTime.now())
                .build();

        review3 = ReviewResponse.builder()
                .id("r3")
                .reviewerId("user-3")
                .rating(3)
                .comment("Ok")
                .isHidden(false)
                .reviewer("User Three")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== 获取手机评论列表 ====================

    @Test
    @DisplayName("应该返回400错误 - 当 page 或 limit 非法时")
    void shouldReturnBadRequest_whenPageOrLimitInvalid() throws Exception {
        // page 小于 1
        mockMvc.perform(get("/api/phones/{phoneId}/reviews", "phone-1")
                        .param("page", "0")
                        .param("limit", "10"))
                .andExpect(status().isBadRequest());

        // limit 小于 1
        mockMvc.perform(get("/api/phones/{phoneId}/reviews", "phone-1")
                        .param("page", "1")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());

        verify(phoneRepository, never()).findById(anyString());
        verify(reviewService, never()).filterVisibleReviews(anyList(), any(), any());
    }

    @Test
    @WithMockUser(username = "user-1")
    @DisplayName("应该返回分页后的评论列表 - 当请求参数有效且手机存在时")
    void shouldReturnPagedReviews_whenRequestValidAndPhoneExists() throws Exception {
        when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(phoneWithReviews));
        when(reviewService.filterVisibleReviews(anyList(), any(), any()))
                .thenReturn(List.of(review1, review2, review3));

        mockMvc.perform(get("/api/phones/{phoneId}/reviews", "phone-1")
                        .param("page", "1")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviews.length()").value(2))
                .andExpect(jsonPath("$.data.totalReviews").value(3))
                .andExpect(jsonPath("$.data.currentPage").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        verify(phoneRepository, times(1)).findById("phone-1");
        verify(reviewService, times(1)).filterVisibleReviews(anyList(), any(), any());
    }

    @Test
    @WithMockUser(username = "user-1")
    @DisplayName("应该返回空列表但状态为200 - 当请求页码超出总页数时")
    void shouldReturnEmptyPage_whenPageExceedsTotal() throws Exception {
        when(phoneRepository.findById("phone-1")).thenReturn(Optional.of(phoneWithReviews));
        when(reviewService.filterVisibleReviews(anyList(), any(), any()))
                .thenReturn(List.of(review1, review2, review3));

        mockMvc.perform(get("/api/phones/{phoneId}/reviews", "phone-1")
                        .param("page", "5")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviews").isArray())
                .andExpect(jsonPath("$.data.reviews").isEmpty())
                .andExpect(jsonPath("$.data.totalReviews").value(3))
                .andExpect(jsonPath("$.data.currentPage").value(5));

        verify(phoneRepository, times(1)).findById("phone-1");
        verify(reviewService, times(1)).filterVisibleReviews(anyList(), any(), any());
    }

    @Test
    @DisplayName("应该返回404错误 - 当手机不存在时")
    void shouldReturnNotFound_whenPhoneDoesNotExist() throws Exception {
        when(phoneRepository.findById("phone-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/phones/{phoneId}/reviews", "phone-1"))
                .andExpect(status().isNotFound());

        verify(phoneRepository, times(1)).findById("phone-1");
        verify(reviewService, never()).filterVisibleReviews(anyList(), any(), any());
    }

    // ==================== 添加评论 ====================

    @Test
    @WithMockUser(username = "user-1")
    @DisplayName("应该成功创建评论 - 当请求有效且用户已认证时")
    void shouldCreateReview_whenRequestValidAndAuthenticated() throws Exception {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(5)
                .comment("Great phone")
                .build();

        ReviewResponse created = ReviewResponse.builder()
                .id("r1")
                .reviewerId("user-1")
                .rating(5)
                .comment("Great phone")
                .isHidden(false)
                .reviewer("User One")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.addReview(eq("phone-1"), any(ReviewCreateRequest.class), eq("user-1")))
                .thenReturn(created);

        mockMvc.perform(post("/api/phones/{phoneId}/reviews", "phone-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review created successfully"))
                .andExpect(jsonPath("$.data.id").value("r1"))
                .andExpect(jsonPath("$.data.reviewerId").value("user-1"))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.comment").value("Great phone"));

        verify(reviewService, times(1)).addReview(eq("phone-1"), any(ReviewCreateRequest.class), eq("user-1"));
    }

    @Test
    @WithMockUser(username = "user-1")
    @DisplayName("应该返回400错误 - 当创建评论请求不符合校验规则时")
    void shouldReturnBadRequest_whenCreateReviewInvalid() throws Exception {
        ReviewCreateRequest invalidRequest = ReviewCreateRequest.builder()
                .rating(0)
                .comment("")
                .build();

        mockMvc.perform(post("/api/phones/{phoneId}/reviews", "phone-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).addReview(anyString(), any(ReviewCreateRequest.class), anyString());
    }

    // ==================== 切换评论可见性 ====================

    @Test
    @WithMockUser(username = "user-1")
    @DisplayName("应该成功切换评论可见性 - 当请求有效且用户已认证时")
    void shouldToggleReviewVisibility_whenRequestValidAndAuthenticated() throws Exception {
        ToggleReviewVisibilityRequest request = ToggleReviewVisibilityRequest.builder()
                .isHidden(true)
                .build();

        ReviewResponse updated = ReviewResponse.builder()
                .id("r1")
                .reviewerId("user-1")
                .rating(5)
                .comment("Great phone")
                .isHidden(true)
                .reviewer("User One")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.toggleReviewVisibility(eq("phone-1"), eq("r1"), eq(true), eq("user-1")))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/phones/{phoneId}/reviews/{reviewId}/visibility", "phone-1", "r1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review visibility updated successfully"))
                .andExpect(jsonPath("$.data.id").value("r1"))
                .andExpect(jsonPath("$.data.isHidden").value(true));

        verify(reviewService, times(1))
                .toggleReviewVisibility("phone-1", "r1", true, "user-1");
    }

    @Test
    @WithMockUser(username = "user-1")
    @DisplayName("应该返回404错误 - 当切换可见性时评论或手机不存在时")
    void shouldReturnNotFound_whenToggleVisibilityTargetNotFound() throws Exception {
        ToggleReviewVisibilityRequest request = ToggleReviewVisibilityRequest.builder()
                .isHidden(true)
                .build();

        when(reviewService.toggleReviewVisibility(anyString(), anyString(), anyBoolean(), anyString()))
                .thenThrow(new ResourceNotFoundException("Review not found"));

        mockMvc.perform(patch("/api/phones/{phoneId}/reviews/{reviewId}/visibility", "phone-1", "missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(reviewService, times(1))
                .toggleReviewVisibility(eq("phone-1"), eq("missing"), eq(true), eq("user-1"));
    }

    // ==================== 卖家查看自己的所有评论 ====================

    @Test
    @WithMockUser(username = "seller-1")
    @DisplayName("应该返回卖家所有商品的评论列表 - 当卖家已认证时")
    void shouldReturnReviewsBySeller_whenSellerAuthenticated() throws Exception {
        SellerReviewResponse sellerReview = SellerReviewResponse.builder()
                .reviewId("r1")
                .phoneId("phone-1")
                .phoneTitle("Test Phone")
                .reviewerId("user-1")
                .reviewerName("User One")
                .rating(5)
                .comment("Great phone")
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.getReviewsBySeller("seller-1"))
                .thenReturn(List.of(sellerReview));

        mockMvc.perform(get("/api/phones/reviews/by-seller"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Seller reviews retrieved successfully"))
                .andExpect(jsonPath("$.data[0].reviewId").value("r1"))
                .andExpect(jsonPath("$.data[0].phoneId").value("phone-1"));

        verify(reviewService, times(1)).getReviewsBySeller("seller-1");
    }
}