package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.order.CheckoutRequest;
import com.oldphonedeals.dto.response.order.OrderResponse;
import com.oldphonedeals.entity.Cart;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.repository.CartRepository;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderService单元测试
 * 测试订单服务的核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PhoneRepository phoneRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private User testSeller;
    private Phone testPhone;
    private Cart testCart;
    private Order testOrder;
    private CheckoutRequest checkoutRequest;

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

        // 创建测试订单
        Order.OrderItem orderItem = Order.OrderItem.builder()
                .phoneId("phone-id")
                .title("Test Phone")
                .quantity(2)
                .price(999.99)
                .build();

        Order.Address orderAddress = Order.Address.builder()
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
                .totalAmount(1999.98)
                .address(orderAddress)
                .createdAt(LocalDateTime.now())
                .build();

        // 创建结账请求
        CheckoutRequest.AddressInfo addressInfo = CheckoutRequest.AddressInfo.builder()
                .street("123 Test St")
                .city("Sydney")
                .state("NSW")
                .zip("2000")
                .country("Australia")
                .build();

        checkoutRequest = CheckoutRequest.builder()
                .address(addressInfo)
                .build();
    }

    // ==================== 结账测试 ====================

    @Test
    void testCheckout_Success() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.checkout("user-id", checkoutRequest);

        // Assert
        assertNotNull(response);
        assertEquals("order-id", response.getId());
        assertEquals("user-id", response.getUserId());
        verify(cartRepository, times(1)).findByUserId("user-id");
        verify(phoneRepository, times(1)).findById("phone-id");
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(phoneRepository, times(1)).save(testPhone); // 验证库存更新
        verify(cartRepository, times(1)).save(testCart); // 验证购物车清空
    }

    @Test
    void testCheckout_CartNotFound_ThrowsException() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.checkout("user-id", checkoutRequest);
        });
        verify(cartRepository, times(1)).findByUserId("user-id");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCheckout_EmptyCart_ThrowsException() {
        // Arrange
        testCart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            orderService.checkout("user-id", checkoutRequest);
        });
        assertTrue(exception.getMessage().contains("Cart is empty"));
        verify(cartRepository, times(1)).findByUserId("user-id");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCheckout_PhoneNotFound_ThrowsException() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.checkout("user-id", checkoutRequest);
        });
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCheckout_DisabledPhone_ThrowsException() {
        // Arrange
        testPhone.setIsDisabled(true);
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            orderService.checkout("user-id", checkoutRequest);
        });
        assertTrue(exception.getMessage().contains("not available"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCheckout_InsufficientStock_ThrowsException() {
        // Arrange
        testPhone.setStock(1); // 库存不足
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            orderService.checkout("user-id", checkoutRequest);
        });
        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCheckout_UpdatesStockAndSalesCount() {
        // Arrange
        int initialStock = testPhone.getStock();
        int initialSalesCount = testPhone.getSalesCount();
        int orderQuantity = testCart.getItems().get(0).getQuantity();

        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.checkout("user-id", checkoutRequest);

        // Assert
        assertNotNull(response);
        assertEquals(initialStock - orderQuantity, testPhone.getStock());
        assertEquals(initialSalesCount + orderQuantity, testPhone.getSalesCount());
        verify(phoneRepository, times(1)).save(testPhone);
    }

    @Test
    void testCheckout_ClearsCart() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.checkout("user-id", checkoutRequest);

        // Assert
        assertNotNull(response);
        assertTrue(testCart.getItems().isEmpty());
        verify(cartRepository, times(1)).save(testCart);
    }

    @Test
    void testCheckout_MultipleItems_Success() {
        // Arrange
        Phone secondPhone = Phone.builder()
                .id("phone-id-2")
                .title("Second Phone")
                .brand(PhoneBrand.APPLE)
                .stock(5)
                .price(1299.99)
                .seller(testSeller)
                .reviews(new ArrayList<>())
                .isDisabled(false)
                .salesCount(0)
                .build();

        Cart.CartItem secondItem = Cart.CartItem.builder()
                .phoneId("phone-id-2")
                .title("Second Phone")
                .quantity(1)
                .price(1299.99)
                .build();

        testCart.getItems().add(secondItem);

        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(phoneRepository.findById("phone-id-2")).thenReturn(Optional.of(secondPhone));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.checkout("user-id", checkoutRequest);

        // Assert
        assertNotNull(response);
        verify(phoneRepository, times(2)).save(any(Phone.class));
    }

    // ==================== 获取订单测试 ====================

    @Test
    void testGetUserOrders_ReturnsOrdersSortedByDate() {
        // Arrange
        Order oldOrder = Order.builder()
                .id("old-order-id")
                .userId("user-id")
                .items(new ArrayList<>())
                .totalAmount(500.0)
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();

        List<Order> orders = Arrays.asList(oldOrder, testOrder);
        when(orderRepository.findByUserId("user-id")).thenReturn(orders);

        // Act
        List<OrderResponse> result = orderService.getUserOrders("user-id");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        // 验证按时间倒序排列（最新的在前）
        assertEquals("order-id", result.get(0).getId());
        assertEquals("old-order-id", result.get(1).getId());
        verify(orderRepository, times(1)).findByUserId("user-id");
    }

    @Test
    void testGetUserOrders_EmptyList_ReturnsEmptyList() {
        // Arrange
        when(orderRepository.findByUserId("user-id")).thenReturn(new ArrayList<>());

        // Act
        List<OrderResponse> result = orderService.getUserOrders("user-id");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderRepository, times(1)).findByUserId("user-id");
    }

    @Test
    void testGetOrderById_Success() {
        // Arrange
        when(orderRepository.findById("order-id")).thenReturn(Optional.of(testOrder));

        // Act
        OrderResponse response = orderService.getOrderById("order-id", "user-id");

        // Assert
        assertNotNull(response);
        assertEquals("order-id", response.getId());
        assertEquals("user-id", response.getUserId());
        verify(orderRepository, times(1)).findById("order-id");
    }

    @Test
    void testGetOrderById_OrderNotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findById("invalid-order")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.getOrderById("invalid-order", "user-id");
        });
        verify(orderRepository, times(1)).findById("invalid-order");
    }

    @Test
    void testGetOrderById_UnauthorizedUser_ThrowsException() {
        // Arrange
        when(orderRepository.findById("order-id")).thenReturn(Optional.of(testOrder));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            orderService.getOrderById("order-id", "other-user-id");
        });
        assertTrue(exception.getMessage().contains("permission"));
        verify(orderRepository, times(1)).findById("order-id");
    }

    // ==================== 边界条件测试 ====================

    @Test
    void testCheckout_StockBecomesZero_Success() {
        // Arrange
        testPhone.setStock(2); // 恰好等于购物车数量
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.checkout("user-id", checkoutRequest);

        // Assert
        assertNotNull(response);
        assertEquals(0, testPhone.getStock());
        verify(phoneRepository, times(1)).save(testPhone);
    }

    @Test
    void testCheckout_TotalAmountCalculatedCorrectly() {
        // Arrange
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            // 验证总金额计算正确
            double expectedTotal = 999.99 * 2; // price * quantity
            assertEquals(expectedTotal, savedOrder.getTotalAmount(), 0.01);
            return testOrder;
        });

        // Act
        OrderResponse response = orderService.checkout("user-id", checkoutRequest);

        // Assert
        assertNotNull(response);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCheckout_InitialSalesCountIsNull_HandledCorrectly() {
        // Arrange
        testPhone.setSalesCount(null); // 测试null情况
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.checkout("user-id", checkoutRequest);

        // Assert
        assertNotNull(response);
        // 验证从null开始也能正确累加
        assertEquals(2, testPhone.getSalesCount());
        verify(phoneRepository, times(1)).save(testPhone);
    }

    @Test
    void testGetUserOrders_MultipleOrders_AllReturned() {
        // Arrange
        List<Order> orders = Arrays.asList(
                testOrder,
                Order.builder()
                        .id("order-2")
                        .userId("user-id")
                        .items(new ArrayList<>())
                        .totalAmount(500.0)
                        .createdAt(LocalDateTime.now().minusDays(1))
                        .build(),
                Order.builder()
                        .id("order-3")
                        .userId("user-id")
                        .items(new ArrayList<>())
                        .totalAmount(300.0)
                        .createdAt(LocalDateTime.now().minusDays(2))
                        .build()
        );
        when(orderRepository.findByUserId("user-id")).thenReturn(orders);

        // Act
        List<OrderResponse> result = orderService.getUserOrders("user-id");

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(orderRepository, times(1)).findByUserId("user-id");
    }
}