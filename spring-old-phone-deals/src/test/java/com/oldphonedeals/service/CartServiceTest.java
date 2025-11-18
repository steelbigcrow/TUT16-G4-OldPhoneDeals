package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.cart.AddToCartRequest;
import com.oldphonedeals.dto.request.cart.UpdateCartItemRequest;
import com.oldphonedeals.dto.response.cart.CartItemResponse;
import com.oldphonedeals.dto.response.cart.CartResponse;
import com.oldphonedeals.entity.Cart;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.repository.CartRepository;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.repository.UserRepository;
import com.oldphonedeals.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CartService单元测试
 * 测试购物车服务的核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PhoneRepository phoneRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User testUser;
    private User testSeller;
    private Phone testPhone;
    private Cart testCart;
    private AddToCartRequest addToCartRequest;
    private UpdateCartItemRequest updateCartItemRequest;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId("user-id");
        testUser.setEmail("user@test.com");
        testUser.setFirstName("Jane");
        testUser.setLastName("Smith");

        // 创建测试卖家
        testSeller = new User();
        testSeller.setId("seller-id");
        testSeller.setEmail("seller@test.com");
        testSeller.setFirstName("John");
        testSeller.setLastName("Doe");

        // 创建测试商品
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
                .build();

        // 创建测试购物车
        Cart.CartItem cartItem = Cart.CartItem.builder()
                .phoneId("phone-id")
                .title("Test Phone")
                .quantity(2)
                .price(999.99)
                .createdAt(LocalDateTime.now())
                .build();

        testCart = Cart.builder()
                .id("cart-id")
                .userId("user-id")
                .items(new ArrayList<>(Arrays.asList(cartItem)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 创建请求对象
        addToCartRequest = AddToCartRequest.builder()
                .phoneId("phone-id")
                .quantity(1)
                .build();

        updateCartItemRequest = UpdateCartItemRequest.builder()
                .quantity(3)
                .build();
    }

    // ==================== 获取购物车测试 ====================

    @Test
    void testGetUserCart_ExistingCart_ReturnsCart() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        // Act
        CartResponse response = cartService.getUserCart("user-id");

        // Assert
        assertNotNull(response);
        assertEquals("cart-id", response.getId());
        assertEquals("user-id", response.getUserId());
        assertFalse(response.getItems().isEmpty());
        verify(cartRepository, times(1)).findByUserId("user-id");
    }

    @Test
    void testGetUserCart_NoCart_CreatesNewCart() {
        // Arrange
        Cart newCart = Cart.builder()
                .id("new-cart-id")
                .userId("user-id")
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        // Act
        CartResponse response = cartService.getUserCart("user-id");

        // Assert
        assertNotNull(response);
        assertEquals("user-id", response.getUserId());
        assertTrue(response.getItems().isEmpty());
        verify(cartRepository, times(1)).findByUserId("user-id");
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    // ==================== 添加到购物车测试 ====================

    @Test
    void testAddToCart_NewItem_Success() {
        // Arrange
        Cart emptyCart = Cart.builder()
                .id("cart-id")
                .userId("user-id")
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);

        // Act
        CartResponse response = cartService.addToCart("user-id", addToCartRequest);

        // Assert
        assertNotNull(response);
        verify(phoneRepository, atLeastOnce()).findById("phone-id");
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testAddToCart_ExistingItem_UpdatesQuantity() {
        // Arrange
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // Act
        CartResponse response = cartService.addToCart("user-id", addToCartRequest);

        // Assert
        assertNotNull(response);
        verify(phoneRepository, atLeastOnce()).findById("phone-id");
        verify(cartRepository, times(1)).save(any(Cart.class));
        // 验证数量更新而不是添加新条目
        assertEquals(1, testCart.getItems().size());
    }

    @Test
    void testAddToCart_PhoneNotFound_ThrowsException() {
        // Arrange
        when(phoneRepository.findById("invalid-phone")).thenReturn(Optional.empty());
        addToCartRequest.setPhoneId("invalid-phone");

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.addToCart("user-id", addToCartRequest);
        });
        verify(phoneRepository, times(1)).findById("invalid-phone");
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void testAddToCart_DisabledPhone_ThrowsException() {
        // Arrange
        testPhone.setIsDisabled(true);
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            cartService.addToCart("user-id", addToCartRequest);
        });
        verify(phoneRepository, times(1)).findById("phone-id");
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void testAddToCart_InsufficientStock_ThrowsException() {
        // Arrange
        addToCartRequest.setQuantity(20); // 超过库存
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            cartService.addToCart("user-id", addToCartRequest);
        });
        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(phoneRepository, times(1)).findById("phone-id");
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void testAddToCart_NoExistingCart_CreatesCartAndAddsItem() {
        // Arrange
        Cart newCart = Cart.builder()
                .id("new-cart-id")
                .userId("user-id")
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        // Act
        CartResponse response = cartService.addToCart("user-id", addToCartRequest);

        // Assert
        assertNotNull(response);
        verify(cartRepository, times(2)).save(any(Cart.class)); // 创建购物车 + 添加商品
    }

    // ==================== 更新购物车商品测试 ====================

    @Test
    void testUpdateCartItem_Success() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // Act
        CartResponse response = cartService.updateCartItem("user-id", "phone-id", updateCartItemRequest);

        // Assert
        assertNotNull(response);
        verify(cartRepository, times(1)).findByUserId("user-id");
        verify(phoneRepository, atLeastOnce()).findById("phone-id"); // 可能被调用多次（业务逻辑+构建响应）
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testUpdateCartItem_CartNotFound_ThrowsException() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.updateCartItem("user-id", "phone-id", updateCartItemRequest);
        });
        verify(cartRepository, times(1)).findByUserId("user-id");
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void testUpdateCartItem_ItemNotInCart_ThrowsException() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.updateCartItem("user-id", "non-existing-phone", updateCartItemRequest);
        });
        verify(cartRepository, times(1)).findByUserId("user-id");
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void testUpdateCartItem_InsufficientStock_ThrowsException() {
        // Arrange
        updateCartItemRequest.setQuantity(20); // 超过库存
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            cartService.updateCartItem("user-id", "phone-id", updateCartItemRequest);
        });
        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    // ==================== 从购物车移除商品测试 ====================

    @Test
    void testRemoveFromCart_Success() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // Act
        CartResponse response = cartService.removeFromCart("user-id", "phone-id");

        // Assert
        assertNotNull(response);
        verify(cartRepository, times(1)).findByUserId("user-id");
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testRemoveFromCart_CartNotFound_ThrowsException() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.removeFromCart("user-id", "phone-id");
        });
        verify(cartRepository, times(1)).findByUserId("user-id");
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void testRemoveFromCart_ItemNotInCart_ThrowsException() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.removeFromCart("user-id", "non-existing-phone");
        });
        verify(cartRepository, times(1)).findByUserId("user-id");
        verify(cartRepository, never()).save(any(Cart.class));
    }

    // ==================== 清空购物车测试 ====================

    @Test
    void testClearCart_Success() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

        // Act
        cartService.clearCart("user-id");

        // Assert
        verify(cartRepository, times(1)).findByUserId("user-id");
        verify(cartRepository, times(1)).save(any(Cart.class));
        assertTrue(testCart.getItems().isEmpty());
    }

    @Test
    void testClearCart_CartNotFound_ThrowsException() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            cartService.clearCart("user-id");
        });
        verify(cartRepository, times(1)).findByUserId("user-id");
        verify(cartRepository, never()).save(any(Cart.class));
    }

    // ==================== 边界条件测试 ====================

    @Test
    void testAddToCart_StockExactlyEqualToQuantity_Success() {
        // Arrange
        testPhone.setStock(1);
        addToCartRequest.setQuantity(1);
        
        Cart emptyCart = Cart.builder()
                .id("cart-id")
                .userId("user-id")
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(emptyCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(emptyCart);

        // Act
        CartResponse response = cartService.addToCart("user-id", addToCartRequest);

        // Assert
        assertNotNull(response);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testGetUserCart_WithMultipleItems_ReturnsAllItems() {
        // Arrange
        Cart.CartItem item2 = Cart.CartItem.builder()
                .phoneId("phone-id-2")
                .title("Another Phone")
                .quantity(1)
                .price(799.99)
                .createdAt(LocalDateTime.now())
                .build();
        
        testCart.getItems().add(item2);
        
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById(anyString())).thenReturn(Optional.of(testPhone));

        // Act
        CartResponse response = cartService.getUserCart("user-id");

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getItems().size());
        verify(cartRepository, times(1)).findByUserId("user-id");
    }
}