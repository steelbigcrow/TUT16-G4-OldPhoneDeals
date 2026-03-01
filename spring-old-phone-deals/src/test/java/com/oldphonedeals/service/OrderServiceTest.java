package com.oldphonedeals.service;

import com.oldphonedeals.dto.message.OrderPostProcessMessage;
import com.oldphonedeals.dto.request.order.CheckoutRequest;
import com.oldphonedeals.dto.response.order.OrderPageResponse;
import com.oldphonedeals.dto.response.order.OrderResponse;
import com.oldphonedeals.entity.Cart;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.OrderCheckoutStatus;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.DuplicateResourceException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.producer.OrderMessageProducer;
import com.oldphonedeals.repository.CartRepository;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.repository.custom.PhoneStockRepository;
import com.oldphonedeals.service.impl.OrderServiceImpl;
import com.oldphonedeals.service.result.CheckoutResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String IDEMPOTENCY_KEY = "123e4567-e89b-12d3-a456-426614174000";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PhoneRepository phoneRepository;

    @Mock
    private PhoneStockRepository phoneStockRepository;

    @Mock
    private OrderMessageProducer orderMessageProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Phone testPhone;
    private Cart testCart;
    private CheckoutRequest checkoutRequest;

    @BeforeEach
    void setUp() {
        User seller = User.builder()
            .id("seller-id")
            .firstName("John")
            .lastName("Seller")
            .email("seller@test.com")
            .build();

        testPhone = Phone.builder()
            .id("phone-id")
            .title("Test Phone")
            .brand(PhoneBrand.SAMSUNG)
            .stock(10)
            .price(999.99)
            .seller(seller)
            .isDisabled(false)
            .salesCount(0)
            .createdAt(LocalDateTime.now())
            .build();

        Cart.CartItem item = Cart.CartItem.builder()
            .phoneId("phone-id")
            .title("Test Phone")
            .quantity(2)
            .price(999.99)
            .createdAt(LocalDateTime.now())
            .build();

        testCart = Cart.builder()
            .id("cart-id")
            .userId("user-id")
            .items(new ArrayList<>(List.of(item)))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        checkoutRequest = CheckoutRequest.builder()
            .address(CheckoutRequest.AddressInfo.builder()
                .street("123 Test St")
                .city("Sydney")
                .state("NSW")
                .zip("2000")
                .country("Australia")
                .build())
            .build();
    }

    @Test
    void shouldCheckoutAndPublishPostProcessMessage() {
        when(orderRepository.insert((Order) any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-id");
            return order;
        });
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(phoneStockRepository.decreaseStockAndIncreaseSales("phone-id", 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutResult result = orderService.checkout("user-id", checkoutRequest, IDEMPOTENCY_KEY);
        OrderResponse response = result.getOrder();

        assertEquals("order-id", response.getId());
        assertEquals(false, result.isReplayed());
        ArgumentCaptor<Order> insertCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).insert(insertCaptor.capture());
        assertEquals(IDEMPOTENCY_KEY, insertCaptor.getValue().getIdempotencyKey());
        verify(phoneStockRepository).decreaseStockAndIncreaseSales("phone-id", 2);
        verify(cartRepository).save(argThat(cart -> cart.getItems().isEmpty()));
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderMessageProducer).publishOrderPostProcessMessage(argThat(message ->
            "order-id".equals(message.getOrderId())
                && "user-id".equals(message.getUserId())
                && message.getItems().size() == 1
        ));
    }

    @Test
    void shouldKeepOrderProcessingUntilPostProcessMessagePublished() {
        when(orderRepository.insert((Order) any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-id");
            return order;
        });
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(phoneStockRepository.decreaseStockAndIncreaseSales("phone-id", 2)).thenReturn(true);

        List<String> callSequence = new ArrayList<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            callSequence.add("save:" + order.getCheckoutStatus());
            return order;
        });
        doAnswer(invocation -> {
            callSequence.add("publish");
            return null;
        }).when(orderMessageProducer).publishOrderPostProcessMessage(any(OrderPostProcessMessage.class));

        orderService.checkout("user-id", checkoutRequest, IDEMPOTENCY_KEY);

        assertEquals(List.of("save:PROCESSING", "publish", "save:COMPLETED"), callSequence);
    }

    @Test
    void shouldThrowWhenCartNotFound() {
        when(orderRepository.insert((Order) any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-id");
            return order;
        });
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            orderService.checkout("user-id", checkoutRequest, IDEMPOTENCY_KEY)
        );

        verify(orderMessageProducer, never()).publishOrderPostProcessMessage(any(OrderPostProcessMessage.class));
    }

    @Test
    void shouldThrowWhenStockDeductionFailsAtomically() {
        when(orderRepository.insert((Order) any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-id");
            return order;
        });
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(phoneStockRepository.decreaseStockAndIncreaseSales("phone-id", 2)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            orderService.checkout("user-id", checkoutRequest, IDEMPOTENCY_KEY)
        );

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(orderMessageProducer, never()).publishOrderPostProcessMessage(any(OrderPostProcessMessage.class));
    }

    @Test
    void shouldThrowAndRollbackWhenOrderMessagePublishFails() {
        when(orderRepository.insert((Order) any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-id");
            return order;
        });
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(phoneStockRepository.decreaseStockAndIncreaseSales("phone-id", 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("publish failed"))
            .when(orderMessageProducer)
            .publishOrderPostProcessMessage(any(OrderPostProcessMessage.class));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            orderService.checkout("user-id", checkoutRequest, IDEMPOTENCY_KEY)
        );

        assertTrue(exception.getMessage().contains("Failed to submit order"));
    }

    @Test
    void shouldCalculateTotalAmountFromCartItems() {
        when(orderRepository.insert((Order) any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-id");
            return order;
        });
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(phoneStockRepository.decreaseStockAndIncreaseSales("phone-id", 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.checkout("user-id", checkoutRequest, IDEMPOTENCY_KEY);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).save(orderCaptor.capture());
        assertEquals(1999.98, orderCaptor.getAllValues().get(0).getTotalAmount(), 0.01);
    }

    @Test
    void shouldRejectOrderWhenPhoneDisabled() {
        when(orderRepository.insert((Order) any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-id");
            return order;
        });
        testPhone.setIsDisabled(true);
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            orderService.checkout("user-id", checkoutRequest, IDEMPOTENCY_KEY)
        );

        assertTrue(exception.getMessage().contains("not available"));
        verify(phoneStockRepository, never()).decreaseStockAndIncreaseSales(any(), anyInt());
    }

    @Test
    void shouldRejectWhenCartIsEmpty() {
        when(orderRepository.insert((Order) any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-id");
            return order;
        });
        testCart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            orderService.checkout("user-id", checkoutRequest, IDEMPOTENCY_KEY)
        );

        assertTrue(exception.getMessage().contains("Cart is empty"));
    }

    @Test
    void shouldReplayCompletedOrderWhenIdempotencyKeyAlreadyUsed() {
        when(orderRepository.findByUserIdAndIdempotencyKey("user-id", IDEMPOTENCY_KEY))
            .thenReturn(Optional.of(buildCompletedOrder("order-replayed")));

        CheckoutResult result = orderService.checkout("user-id", checkoutRequest, IDEMPOTENCY_KEY);

        assertEquals(true, result.isReplayed());
        assertEquals("order-replayed", result.getOrder().getId());
        verify(phoneStockRepository, never()).decreaseStockAndIncreaseSales(any(), anyInt());
        verify(orderMessageProducer, never()).publishOrderPostProcessMessage(any(OrderPostProcessMessage.class));
    }

    @Test
    void shouldNormalizeIdempotencyKeyInServiceLayer() {
        String uppercaseUuid = IDEMPOTENCY_KEY.toUpperCase(Locale.ROOT);
        when(orderRepository.findByUserIdAndIdempotencyKey(eq("user-id"), anyString()))
            .thenReturn(Optional.of(buildCompletedOrder("order-replayed")));

        CheckoutResult result = orderService.checkout("user-id", checkoutRequest, uppercaseUuid);

        assertTrue(result.isReplayed());
        assertEquals("order-replayed", result.getOrder().getId());

        ArgumentCaptor<String> idempotencyKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderRepository, atLeastOnce()).findByUserIdAndIdempotencyKey(eq("user-id"), idempotencyKeyCaptor.capture());
        assertTrue(idempotencyKeyCaptor.getAllValues().stream().allMatch(IDEMPOTENCY_KEY::equals));
        verify(orderRepository, never()).insert(any(Order.class));
    }

    @Test
    void shouldRejectReplayedIdempotencyKeyWhenRequestPayloadDiffers() {
        when(orderRepository.findByUserIdAndIdempotencyKey("user-id", IDEMPOTENCY_KEY))
            .thenReturn(Optional.of(buildCompletedOrder("order-replayed")));

        CheckoutRequest differentRequest = CheckoutRequest.builder()
            .address(CheckoutRequest.AddressInfo.builder()
                .street("999 Other St")
                .city("Sydney")
                .state("NSW")
                .zip("2000")
                .country("Australia")
                .build())
            .build();

        assertThrows(DuplicateResourceException.class, () ->
            orderService.checkout("user-id", differentRequest, IDEMPOTENCY_KEY)
        );

        verify(phoneStockRepository, never()).decreaseStockAndIncreaseSales(any(), anyInt());
        verify(orderMessageProducer, never()).publishOrderPostProcessMessage(any(OrderPostProcessMessage.class));
    }

    @Test
    void shouldFailStaleProcessingCheckoutToPreventInfiniteReplays() {
        Order processingOrder = Order.builder()
            .id("order-processing")
            .userId("user-id")
            .idempotencyKey(IDEMPOTENCY_KEY)
            .checkoutStatus(OrderCheckoutStatus.PROCESSING)
            .checkoutError(null)
            .items(List.of())
            .totalAmount(0.0)
            .createdAt(LocalDateTime.now().minusMinutes(10))
            .build();

        when(orderRepository.findByUserIdAndIdempotencyKey("user-id", IDEMPOTENCY_KEY))
            .thenReturn(Optional.of(processingOrder));
        when(orderRepository.findById("order-processing")).thenReturn(Optional.of(processingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            orderService.checkout("user-id", checkoutRequest, IDEMPOTENCY_KEY)
        );

        assertTrue(exception.getMessage().toLowerCase(Locale.ROOT).contains("timed out"));
        verify(orderRepository, atLeastOnce()).save(argThat(order -> order.getCheckoutStatus() == OrderCheckoutStatus.FAILED));
    }

    @Test
    void shouldThrowWhenDuplicateIdempotencyKeyHasFailedOrder() {
        when(orderRepository.findByUserIdAndIdempotencyKey("user-id", IDEMPOTENCY_KEY))
            .thenReturn(Optional.of(Order.builder()
                .id("order-failed")
                .userId("user-id")
                .idempotencyKey(IDEMPOTENCY_KEY)
                .checkoutStatus(OrderCheckoutStatus.FAILED)
                .checkoutError("Cart is empty")
                .items(List.of())
                .totalAmount(0.0)
                .createdAt(LocalDateTime.now())
                .build()));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            orderService.checkout("user-id", checkoutRequest, IDEMPOTENCY_KEY)
        );

        assertTrue(exception.getMessage().contains("Cart is empty"));
    }

    @Test
    void shouldHideFailedAndProcessingOrdersFromUserOrderList() {
        Order completedOrder = buildCompletedOrder("order-completed");
        completedOrder.setCreatedAt(LocalDateTime.now().minusMinutes(2));

        Order legacyOrder = buildCompletedOrder("order-legacy");
        legacyOrder.setCheckoutStatus(null);
        legacyOrder.setCreatedAt(LocalDateTime.now().minusMinutes(1));

        Order failedOrder = Order.builder()
            .id("order-failed")
            .userId("user-id")
            .idempotencyKey("failed-key")
            .checkoutStatus(OrderCheckoutStatus.FAILED)
            .checkoutError("checkout failed")
            .items(List.of())
            .totalAmount(0.0)
            .createdAt(LocalDateTime.now())
            .build();

        Order processingOrder = Order.builder()
            .id("order-processing")
            .userId("user-id")
            .idempotencyKey("processing-key")
            .checkoutStatus(OrderCheckoutStatus.PROCESSING)
            .items(List.of())
            .totalAmount(0.0)
            .createdAt(LocalDateTime.now().plusMinutes(1))
            .build();

        Order compensatedOrder = Order.builder()
            .id("order-compensated")
            .userId("user-id")
            .idempotencyKey("compensated-key")
            .checkoutStatus(OrderCheckoutStatus.FAILED)
            .postProcessStatus(OrderPostProcessStatus.COMPENSATED)
            .checkoutError("Order compensated: post-process failed")
            .items(List.of())
            .totalAmount(0.0)
            .createdAt(LocalDateTime.now().plusMinutes(2))
            .build();

        when(orderRepository.findByUserId("user-id"))
            .thenReturn(List.of(compensatedOrder, failedOrder, processingOrder, legacyOrder, completedOrder));

        List<OrderResponse> orders = orderService.getUserOrders("user-id");

        assertEquals(2, orders.size());
        assertEquals("order-legacy", orders.get(0).getId());
        assertEquals("order-completed", orders.get(1).getId());
    }

    @Test
    void shouldHideFailedAndProcessingOrdersFromPaginatedUserOrderList() {
        Order completedOrder = buildCompletedOrder("order-completed");
        completedOrder.setCreatedAt(LocalDateTime.now().minusMinutes(2));

        Order legacyOrder = buildCompletedOrder("order-legacy");
        legacyOrder.setCheckoutStatus(null);
        legacyOrder.setCreatedAt(LocalDateTime.now().minusMinutes(1));

        Order failedOrder = Order.builder()
            .id("order-failed")
            .userId("user-id")
            .idempotencyKey("failed-key")
            .checkoutStatus(OrderCheckoutStatus.FAILED)
            .checkoutError("checkout failed")
            .items(List.of())
            .totalAmount(0.0)
            .createdAt(LocalDateTime.now())
            .build();

        Order processingOrder = Order.builder()
            .id("order-processing")
            .userId("user-id")
            .idempotencyKey("processing-key")
            .checkoutStatus(OrderCheckoutStatus.PROCESSING)
            .items(List.of())
            .totalAmount(0.0)
            .createdAt(LocalDateTime.now().plusMinutes(1))
            .build();

        Order compensatedOrder = Order.builder()
            .id("order-compensated")
            .userId("user-id")
            .idempotencyKey("compensated-key")
            .checkoutStatus(OrderCheckoutStatus.FAILED)
            .postProcessStatus(OrderPostProcessStatus.COMPENSATED)
            .checkoutError("Order compensated: post-process failed")
            .items(List.of())
            .totalAmount(0.0)
            .createdAt(LocalDateTime.now().plusMinutes(2))
            .build();

        when(orderRepository.findByUserId("user-id"))
            .thenReturn(List.of(compensatedOrder, failedOrder, processingOrder, legacyOrder, completedOrder));

        OrderPageResponse response = orderService.getUserOrders("user-id", 1, 10);

        assertEquals(2, response.getItems().size());
        assertEquals("order-legacy", response.getItems().get(0).getId());
        assertEquals("order-completed", response.getItems().get(1).getId());
        assertEquals(2, response.getPagination().getTotalItems());
    }

    private Order buildCompletedOrder(String orderId) {
        return Order.builder()
            .id(orderId)
            .userId("user-id")
            .idempotencyKey(IDEMPOTENCY_KEY)
            .checkoutStatus(OrderCheckoutStatus.COMPLETED)
            .items(List.of(Order.OrderItem.builder()
                .phoneId("phone-id")
                .title("Test Phone")
                .quantity(2)
                .price(999.99)
                .build()))
            .totalAmount(1999.98)
            .address(Order.Address.builder()
                .street("123 Test St")
                .city("Sydney")
                .state("NSW")
                .zip("2000")
                .country("Australia")
                .build())
            .createdAt(LocalDateTime.now())
            .build();
    }
}
