package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.phone.PhoneCreateRequest;
import com.oldphonedeals.dto.request.phone.PhoneUpdateRequest;
import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.phone.PhoneListItemResponse;
import com.oldphonedeals.dto.response.phone.PhoneResponse;
import com.oldphonedeals.entity.Cart;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.exception.UnauthorizedException;
import com.oldphonedeals.repository.CartRepository;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.service.impl.PhoneServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PhoneService单元测试
 * 测试商品服务的核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhoneServiceTest {

    @Mock
    private PhoneRepository phoneRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private PhoneServiceImpl phoneService;

    private User testSeller;
    private Phone testPhone;
    private PhoneCreateRequest createRequest;
    private PhoneUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        // 创建测试用卖家
        testSeller = new User();
        testSeller.setId("seller-id");
        testSeller.setEmail("seller@test.com");
        testSeller.setFirstName("John");
        testSeller.setLastName("Doe");
        testSeller.setPassword("hashedPassword");
        testSeller.setRole("USER");

        // 创建测试用商品
        testPhone = Phone.builder()
                .id("phone-id")
                .title("Test Phone")
                .brand(PhoneBrand.SAMSUNG)
                .image("test.jpg")
                .stock(10)
                .price(999.99)
                .seller(testSeller)
                .reviews(new ArrayList<>())
                .isDisabled(false)
                .salesCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 创建请求对象
        createRequest = PhoneCreateRequest.builder()
                .title("New Phone")
                .brand(PhoneBrand.APPLE)
                .image("new.jpg")
                .stock(5)
                .price(1299.99)
                .seller("seller-id")
                .build();

        updateRequest = PhoneUpdateRequest.builder()
                .title("Updated Phone")
                .price(899.99)
                .build();
    }

    // ==================== 创建商品测试 ====================

    @Test
    void testCreatePhone_Success() {
        // Arrange
        when(userRepository.findById("seller-id")).thenReturn(Optional.of(testSeller));
        when(phoneRepository.save(any(Phone.class))).thenReturn(testPhone);

        // Act
        PhoneResponse response = phoneService.createPhone(createRequest, "seller-id");

        // Assert
        assertNotNull(response);
        assertEquals(testPhone.getId(), response.getId());
        assertEquals(testPhone.getTitle(), response.getTitle());
        verify(userRepository, times(1)).findById("seller-id");
        verify(phoneRepository, times(1)).save(any(Phone.class));
    }

    @Test
    void testCreatePhone_SellerNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById("invalid-seller")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            phoneService.createPhone(createRequest, "invalid-seller");
        });
        verify(userRepository, times(1)).findById("invalid-seller");
        verify(phoneRepository, never()).save(any(Phone.class));
    }

    // ==================== 更新商品测试 ====================

    @Test
    void testUpdatePhone_Success() {
        // Arrange
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(phoneRepository.save(any(Phone.class))).thenReturn(testPhone);

        // Act
        PhoneResponse response = phoneService.updatePhone("phone-id", updateRequest, "seller-id");

        // Assert
        assertNotNull(response);
        verify(phoneRepository, times(1)).findById("phone-id");
        verify(phoneRepository, times(1)).save(any(Phone.class));
    }

    @Test
    void testUpdatePhone_PhoneNotFound_ThrowsException() {
        // Arrange
        when(phoneRepository.findById("invalid-phone")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            phoneService.updatePhone("invalid-phone", updateRequest, "seller-id");
        });
        verify(phoneRepository, times(1)).findById("invalid-phone");
        verify(phoneRepository, never()).save(any(Phone.class));
    }

    @Test
    void testUpdatePhone_UnauthorizedSeller_ThrowsException() {
        // Arrange
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            phoneService.updatePhone("phone-id", updateRequest, "other-seller-id");
        });
        verify(phoneRepository, times(1)).findById("phone-id");
        verify(phoneRepository, never()).save(any(Phone.class));
    }

    @Test
    void testUpdatePhone_WithImageChange_DeletesOldImage() {
        // Arrange
        updateRequest.setImage("new-image.jpg");
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(phoneRepository.save(any(Phone.class))).thenReturn(testPhone);
        doNothing().when(fileStorageService).deleteFile(anyString());

        // Act
        PhoneResponse response = phoneService.updatePhone("phone-id", updateRequest, "seller-id");

        // Assert
        assertNotNull(response);
        verify(fileStorageService, times(1)).deleteFile("test.jpg");
        verify(phoneRepository, times(1)).save(any(Phone.class));
    }

    // ==================== 删除商品测试 ====================

    @Test
    void testDeletePhone_Success() {
        // Arrange
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(cartRepository.findCartsContainingPhone("phone-id")).thenReturn(new ArrayList<>());
        when(userRepository.findAll()).thenReturn(new ArrayList<>());
        doNothing().when(phoneRepository).delete(any(Phone.class));
        doNothing().when(fileStorageService).deleteFile(anyString());

        // Act
        phoneService.deletePhone("phone-id", "seller-id");

        // Assert
        verify(phoneRepository, times(1)).findById("phone-id");
        verify(phoneRepository, times(1)).delete(testPhone);
        verify(fileStorageService, times(1)).deleteFile("test.jpg");
    }

    @Test
    void testDeletePhone_PhoneNotFound_ThrowsException() {
        // Arrange
        when(phoneRepository.findById("invalid-phone")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            phoneService.deletePhone("invalid-phone", "seller-id");
        });
        verify(phoneRepository, times(1)).findById("invalid-phone");
        verify(phoneRepository, never()).delete(any(Phone.class));
    }

    @Test
    void testDeletePhone_UnauthorizedSeller_ThrowsException() {
        // Arrange
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            phoneService.deletePhone("phone-id", "other-seller-id");
        });
        verify(phoneRepository, times(1)).findById("phone-id");
        verify(phoneRepository, never()).delete(any(Phone.class));
    }

    @Test
    void testDeletePhone_RemovesFromCartsAndWishlists() {
        // Arrange
        Cart cart = Cart.builder()
                .id("cart-id")
                .userId("user-id")
                .items(new ArrayList<>(Arrays.asList(
                        Cart.CartItem.builder().phoneId("phone-id").build()
                )))
                .build();
        
        User userWithWishlist = new User();
        userWithWishlist.setId("user-id");
        userWithWishlist.setWishlist(new ArrayList<>(Arrays.asList("phone-id")));

        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(cartRepository.findCartsContainingPhone("phone-id")).thenReturn(Arrays.asList(cart));
        when(userRepository.findAll()).thenReturn(Arrays.asList(userWithWishlist));
        doNothing().when(fileStorageService).deleteFile(anyString());

        // Act
        phoneService.deletePhone("phone-id", "seller-id");

        // Assert
        verify(cartRepository, times(1)).save(cart);
        verify(userRepository, times(1)).save(userWithWishlist);
        verify(phoneRepository, times(1)).delete(testPhone);
        assertTrue(cart.getItems().isEmpty());
        assertFalse(userWithWishlist.getWishlist().contains("phone-id"));
    }

    // ==================== 获取商品测试 ====================

    @Test
    void testGetPhoneById_Success() {
        // Arrange
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        // Act
        PhoneResponse response = phoneService.getPhoneById("phone-id", "user-id");

        // Assert
        assertNotNull(response);
        assertEquals(testPhone.getId(), response.getId());
        assertEquals(testPhone.getTitle(), response.getTitle());
        verify(phoneRepository, times(1)).findById("phone-id");
    }

    @Test
    void testGetPhoneById_PhoneNotFound_ThrowsException() {
        // Arrange
        when(phoneRepository.findById("invalid-phone")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            phoneService.getPhoneById("invalid-phone", "user-id");
        });
        verify(phoneRepository, times(1)).findById("invalid-phone");
    }

    @Test
    void testGetPhones_WithPagination_ReturnsPagedResults() {
        // Arrange
        List<Phone> phones = Arrays.asList(testPhone);
        Page<Phone> phonePage = new PageImpl<>(phones);
        when(phoneRepository.findByIsDisabledFalse(any(Pageable.class))).thenReturn(phonePage);

        // Act
        Map<String, Object> result = phoneService.getPhones(null, null, null, null, null, 1, 12);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("phones"));
        assertTrue(result.containsKey("currentPage"));
        assertTrue(result.containsKey("totalPages"));
        assertEquals(1, result.get("currentPage"));
        verify(phoneRepository, times(1)).findByIsDisabledFalse(any(Pageable.class));
    }

    @Test
    void testGetPhones_WithSearchFilter_ReturnsFilteredResults() {
        // Arrange
        List<Phone> phones = Arrays.asList(testPhone);
        Page<Phone> phonePage = new PageImpl<>(phones);
        when(phoneRepository.searchByTitle(anyString(), any(Pageable.class)))
                .thenReturn(phonePage);

        // Act
        Map<String, Object> result = phoneService.getPhones("Test", null, null, null, null, 1, 12);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("phones"));
        verify(phoneRepository, times(1)).searchByTitle(anyString(), any(Pageable.class));
    }

    @Test
    void testGetPhones_WithBrandFilter_ReturnsFilteredResults() {
        // Arrange
        List<Phone> phones = Arrays.asList(testPhone);
        Page<Phone> phonePage = new PageImpl<>(phones);
        when(phoneRepository.findByBrandAndIsDisabledFalse(eq(PhoneBrand.SAMSUNG), any(Pageable.class)))
                .thenReturn(phonePage);

        // Act
        Map<String, Object> result = phoneService.getPhones(null, PhoneBrand.SAMSUNG, null, null, null, 1, 12);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("phones"));
        verify(phoneRepository, times(1)).findByBrandAndIsDisabledFalse(eq(PhoneBrand.SAMSUNG), any(Pageable.class));
    }

    @Test
    void testGetPhones_WithMaxPriceFilter_ReturnsFilteredResults() {
        // Arrange
        List<Phone> phones = Arrays.asList(testPhone);
        Page<Phone> phonePage = new PageImpl<>(phones);
        when(phoneRepository.findByPriceLessThanEqualAndIsDisabledFalse(eq(1000.0), any(Pageable.class)))
                .thenReturn(phonePage);

        // Act
        Map<String, Object> result = phoneService.getPhones(null, null, 1000.0, null, null, 1, 12);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("phones"));
        verify(phoneRepository, times(1)).findByPriceLessThanEqualAndIsDisabledFalse(eq(1000.0), any(Pageable.class));
    }

    @Test
    void testGetPhones_WithAllFilters_ReturnsFilteredResults() {
        // Arrange
        List<Phone> phones = Arrays.asList(testPhone);
        Page<Phone> phonePage = new PageImpl<>(phones);
        when(phoneRepository.findByTitleAndBrandAndPriceLessThanEqual(
                anyString(), eq(PhoneBrand.SAMSUNG), eq(1000.0), any(Pageable.class)))
                .thenReturn(phonePage);

        // Act
        Map<String, Object> result = phoneService.getPhones("Test", PhoneBrand.SAMSUNG, 1000.0, null, null, 1, 12);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("phones"));
        verify(phoneRepository, times(1)).findByTitleAndBrandAndPriceLessThanEqual(
                anyString(), eq(PhoneBrand.SAMSUNG), eq(1000.0), any(Pageable.class));
    }

    // ==================== 特殊查询测试 ====================

    @Test
    void testGetSoldOutSoonPhones_ReturnsLowStockPhones() {
        // Arrange
        Phone lowStockPhone = Phone.builder()
                .id("low-stock-phone")
                .title("Low Stock Phone")
                .stock(3)
                .isDisabled(false)
                .seller(testSeller)
                .reviews(new ArrayList<>())
                .build();
        
        when(phoneRepository.findByStockLessThanEqualAndIsDisabledFalseOrderByStockAsc(eq(5), any(Pageable.class)))
                .thenReturn(Arrays.asList(lowStockPhone));

        // Act
        List<PhoneListItemResponse> result = phoneService.getSoldOutSoonPhones();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(phoneRepository, times(1))
                .findByStockLessThanEqualAndIsDisabledFalseOrderByStockAsc(eq(5), any(Pageable.class));
    }

    @Test
    void testGetBestSellers_ReturnsTopRatedPhones() {
        // Arrange
        Phone.Review review1 = Phone.Review.builder()
                .id("review-1")
                .reviewerId("user-1")
                .rating(5)
                .comment("Great!")
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        Phone.Review review2 = Phone.Review.builder()
                .id("review-2")
                .reviewerId("user-2")
                .rating(4)
                .comment("Good")
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        testPhone.setReviews(Arrays.asList(review1, review2));
        when(phoneRepository.findAll()).thenReturn(Arrays.asList(testPhone));

        // Act
        List<PhoneListItemResponse> result = phoneService.getBestSellers();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(phoneRepository, times(1)).findAll();
    }

    @Test
    void testTogglePhoneDisabled_Success() {
        // Arrange
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(phoneRepository.save(any(Phone.class))).thenReturn(testPhone);

        // Act
        ApiResponse<String> response = phoneService.togglePhoneDisabled("phone-id", true, "seller-id");

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(phoneRepository, times(1)).findById("phone-id");
        verify(phoneRepository, times(1)).save(testPhone);
    }

    @Test
    void testTogglePhoneDisabled_UnauthorizedSeller_ThrowsException() {
        // Arrange
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> {
            phoneService.togglePhoneDisabled("phone-id", true, "other-seller-id");
        });
        verify(phoneRepository, times(1)).findById("phone-id");
        verify(phoneRepository, never()).save(any(Phone.class));
    }

    @Test
    void testGetPhonesBySeller_ReturnsSellerPhones() {
        // Arrange
        when(userRepository.findById("seller-id")).thenReturn(Optional.of(testSeller));
        when(phoneRepository.findBySellerId("seller-id")).thenReturn(Arrays.asList(testPhone));

        // Act
        List<PhoneResponse> result = phoneService.getPhonesBySeller("seller-id");

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(userRepository, times(1)).findById("seller-id");
        verify(phoneRepository, times(1)).findBySellerId("seller-id");
    }

    @Test
    void testGetPhonesBySeller_SellerNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById("invalid-seller")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            phoneService.getPhonesBySeller("invalid-seller");
        });
        verify(userRepository, times(1)).findById("invalid-seller");
        verify(phoneRepository, never()).findBySellerId(anyString());
    }

    @Test
    void testGetPhoneById_WithReviewVisibilityFiltering_ReturnsTop3Reviews() {
        // Arrange
        // 创建 5 条评论用于测试
        List<Phone.Review> reviews = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            reviews.add(Phone.Review.builder()
                    .id("review-" + i)
                    .reviewerId("reviewer-" + i)
                    .rating(5)
                    .comment("Comment " + i)
                    .isHidden(false)
                    .createdAt(LocalDateTime.now().minusDays(i))
                    .build());
        }
        testPhone.setReviews(reviews);
        
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testSeller));
        
        // Mock ReviewService 返回所有可见评论
        lenient().when(reviewService.filterVisibleReviews(anyList(), anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0)); // 返回所有评论

        // Act
        PhoneResponse response = phoneService.getPhoneById("phone-id", "user-id");

        // Assert
        assertNotNull(response);
        assertNotNull(response.getReviews());
        // 应该只返回前 3 条评论
        assertEquals(3, response.getReviews().size());
        verify(reviewService, times(1)).filterVisibleReviews(anyList(), eq("user-id"), eq("seller-id"));
    }

    @Test
    void testGetPhoneById_WithNoReviews_ReturnsEmptyList() {
        // Arrange
        testPhone.setReviews(new ArrayList<>());
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        // Act
        PhoneResponse response = phoneService.getPhoneById("phone-id", null);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getReviews());
        assertTrue(response.getReviews().isEmpty());
    }
}