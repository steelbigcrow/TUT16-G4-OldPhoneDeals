package com.oldphonedeals.service;

import com.oldphonedeals.dto.message.OrderPostProcessMessage;
import com.oldphonedeals.dto.request.order.CheckoutRequest;
import com.oldphonedeals.dto.response.order.OrderResponse;
import com.oldphonedeals.entity.Cart;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.entity.User;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.enums.PhoneBrand;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.producer.OrderMessageProducer;
import com.oldphonedeals.repository.CartRepository;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.repository.custom.PhoneStockRepository;
import com.oldphonedeals.service.impl.OrderServiceImpl;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(phoneStockRepository.decreaseStockAndIncreaseSales("phone-id", 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-id");
            return order;
        });

        OrderResponse response = orderService.checkout("user-id", checkoutRequest);

        assertEquals("order-id", response.getId());
        verify(phoneStockRepository).decreaseStockAndIncreaseSales("phone-id", 2);
        verify(cartRepository).save(argThat(cart -> cart.getItems().isEmpty()));
        verify(orderRepository).save(argThat(order ->
            order.getPostProcessStatus() == OrderPostProcessStatus.PENDING
        ));
        verify(orderMessageProducer).publishOrderPostProcessMessage(argThat(message ->
            "order-id".equals(message.getOrderId())
                && "user-id".equals(message.getUserId())
                && message.getItems().size() == 1
        ));
    }

    @Test
    void shouldThrowWhenCartNotFound() {
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            orderService.checkout("user-id", checkoutRequest)
        );

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderMessageProducer, never()).publishOrderPostProcessMessage(any(OrderPostProcessMessage.class));
    }

    @Test
    void shouldThrowWhenStockDeductionFailsAtomically() {
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(phoneStockRepository.decreaseStockAndIncreaseSales("phone-id", 2)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            orderService.checkout("user-id", checkoutRequest)
        );

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderMessageProducer, never()).publishOrderPostProcessMessage(any(OrderPostProcessMessage.class));
    }

    @Test
    void shouldThrowAndRollbackWhenOrderMessagePublishFails() {
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(phoneStockRepository.decreaseStockAndIncreaseSales("phone-id", 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("order-id");
            return order;
        });
        doThrow(new IllegalStateException("publish failed"))
            .when(orderMessageProducer)
            .publishOrderPostProcessMessage(any(OrderPostProcessMessage.class));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            orderService.checkout("user-id", checkoutRequest)
        );

        assertTrue(exception.getMessage().contains("Failed to submit order"));
    }

    @Test
    void shouldCalculateTotalAmountFromCartItems() {
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));
        when(phoneStockRepository.decreaseStockAndIncreaseSales("phone-id", 2)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.checkout("user-id", checkoutRequest);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(1999.98, orderCaptor.getValue().getTotalAmount(), 0.01);
    }

    @Test
    void shouldRejectOrderWhenPhoneDisabled() {
        testPhone.setIsDisabled(true);
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));
        when(phoneRepository.findById("phone-id")).thenReturn(Optional.of(testPhone));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            orderService.checkout("user-id", checkoutRequest)
        );

        assertTrue(exception.getMessage().contains("not available"));
        verify(phoneStockRepository, never()).decreaseStockAndIncreaseSales(any(), anyInt());
    }

    @Test
    void shouldRejectWhenCartIsEmpty() {
        testCart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId("user-id")).thenReturn(Optional.of(testCart));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
            orderService.checkout("user-id", checkoutRequest)
        );

        assertTrue(exception.getMessage().contains("Cart is empty"));
        verify(orderRepository, never()).save(any(Order.class));
    }
}
