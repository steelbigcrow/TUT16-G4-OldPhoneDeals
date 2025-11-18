package com.oldphonedeals.service.impl;

import com.oldphonedeals.dto.request.order.CheckoutRequest;
import com.oldphonedeals.dto.response.order.OrderItemResponse;
import com.oldphonedeals.dto.response.order.OrderResponse;
import com.oldphonedeals.entity.Cart;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.repository.CartRepository;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 * 
 * @author OldPhoneDeals Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final PhoneRepository phoneRepository;
    
    @Override
    @Transactional
    public OrderResponse checkout(String userId, CheckoutRequest request) {
        log.debug("Starting checkout for user: {}", userId);
        
        // 1. 获取购物车
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }
        
        // 2. 验证每个商品的库存和状态
        List<Phone> phones = new ArrayList<>();
        for (Cart.CartItem cartItem : cart.getItems()) {
            Phone phone = phoneRepository.findById(cartItem.getPhoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Phone not found: " + cartItem.getPhoneId()));
            
            // 验证商品是否被禁用
            if (phone.getIsDisabled()) {
                throw new BadRequestException("Phone " + phone.getTitle() + " is not available");
            }
            
            // 验证库存
            if (cartItem.getQuantity() > phone.getStock()) {
                throw new BadRequestException("Insufficient stock for phone " + phone.getTitle() + 
                        ". Available: " + phone.getStock() + ", Requested: " + cartItem.getQuantity());
            }
            
            phones.add(phone);
        }
        
        // 3. 计算总价
        double totalAmount = 0;
        for (Cart.CartItem item : cart.getItems()) {
            totalAmount += item.getPrice() * item.getQuantity();
        }
        
        // 4. 创建订单对象
        List<Order.OrderItem> orderItems = cart.getItems().stream()
                .map(item -> Order.OrderItem.builder()
                        .phoneId(item.getPhoneId())
                        .title(item.getTitle())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());
        
        Order.Address orderAddress = Order.Address.builder()
                .street(request.getAddress().getStreet())
                .city(request.getAddress().getCity())
                .state(request.getAddress().getState())
                .zip(request.getAddress().getZip())
                .country(request.getAddress().getCountry())
                .build();
        
        Order order = Order.builder()
                .userId(userId)
                .items(orderItems)
                .totalAmount(totalAmount)
                .address(orderAddress)
                .createdAt(LocalDateTime.now())
                .build();
        
        // 5. 保存订单
        order = orderRepository.save(order);
        log.info("Order created: {}", order.getId());
        
        // 6. 扣减库存并增加销售计数
        for (int i = 0; i < cart.getItems().size(); i++) {
            Cart.CartItem cartItem = cart.getItems().get(i);
            Phone phone = phones.get(i);
            
            // 扣减库存
            phone.setStock(phone.getStock() - cartItem.getQuantity());
            
            // 增加销售计数
            Integer salesCount = phone.getSalesCount() != null ? phone.getSalesCount() : 0;
            phone.setSalesCount(salesCount + cartItem.getQuantity());
            
            phoneRepository.save(phone);
            log.debug("Updated phone stock and sales - phoneId: {}, newStock: {}, newSalesCount: {}", 
                    phone.getId(), phone.getStock(), phone.getSalesCount());
        }
        
        // 7. 清空购物车
        cart.getItems().clear();
        cartRepository.save(cart);
        log.info("Cart cleared for user: {}", userId);
        
        // 8. 返回订单响应
        return buildOrderResponse(order);
    }
    
    @Override
    public List<OrderResponse> getUserOrders(String userId) {
        log.debug("Getting orders for user: {}", userId);
        
        // 按创建时间降序排序
        List<Order> orders = orderRepository.findByUserId(userId);
        orders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        
        return orders.stream()
                .map(this::buildOrderResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public OrderResponse getOrderById(String orderId, String userId) {
        log.debug("Getting order details - orderId: {}, userId: {}", orderId, userId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        // 权限检查：只有订单的买家可以查看
        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("You don't have permission to view this order");
        }
        
        return buildOrderResponse(order);
    }
    
    /**
     * 构建订单响应对象
     */
    private OrderResponse buildOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .phoneId(item.getPhoneId())
                        .title(item.getTitle())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());
        
        OrderResponse.AddressInfo addressInfo = null;
        if (order.getAddress() != null) {
            addressInfo = OrderResponse.AddressInfo.builder()
                    .street(order.getAddress().getStreet())
                    .city(order.getAddress().getCity())
                    .state(order.getAddress().getState())
                    .zip(order.getAddress().getZip())
                    .country(order.getAddress().getCountry())
                    .build();
        }
        
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .items(itemResponses)
                .totalAmount(order.getTotalAmount())
                .address(addressInfo)
                .createdAt(order.getCreatedAt())
                .build();
    }
}