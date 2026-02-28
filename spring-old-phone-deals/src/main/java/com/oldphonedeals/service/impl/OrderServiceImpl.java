package com.oldphonedeals.service.impl;

import com.oldphonedeals.dto.message.OrderPostProcessMessage;
import com.oldphonedeals.dto.request.order.CheckoutRequest;
import com.oldphonedeals.dto.response.order.OrderItemResponse;
import com.oldphonedeals.dto.response.order.OrderPageResponse;
import com.oldphonedeals.dto.response.order.OrderResponse;
import com.oldphonedeals.entity.Cart;
import com.oldphonedeals.entity.Order;
import com.oldphonedeals.entity.Phone;
import com.oldphonedeals.enums.OrderCheckoutStatus;
import com.oldphonedeals.enums.OrderPostProcessStatus;
import com.oldphonedeals.exception.BadRequestException;
import com.oldphonedeals.exception.DuplicateResourceException;
import com.oldphonedeals.exception.ResourceNotFoundException;
import com.oldphonedeals.producer.OrderMessageProducer;
import com.oldphonedeals.repository.CartRepository;
import com.oldphonedeals.repository.OrderRepository;
import com.oldphonedeals.repository.PhoneRepository;
import com.oldphonedeals.repository.custom.PhoneStockRepository;
import com.oldphonedeals.service.OrderService;
import com.oldphonedeals.service.result.CheckoutResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
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

    private static final int REPLAY_WAIT_MAX_ATTEMPTS = 20;
    private static final long REPLAY_WAIT_MILLIS = 100L;
    private static final long PROCESSING_STALE_AFTER_SECONDS = 120L;

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final PhoneRepository phoneRepository;
    private final PhoneStockRepository phoneStockRepository;
    private final OrderMessageProducer orderMessageProducer;

    @Override
    @Transactional
    public CheckoutResult checkout(String userId, CheckoutRequest request, String idempotencyKey) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        String requestSignature = buildCheckoutRequestSignature(request);

        log.debug("Starting checkout for user: {}, idempotencyKey: {}", userId, normalizedIdempotencyKey);

        var existingOrder = orderRepository.findByUserIdAndIdempotencyKey(userId, normalizedIdempotencyKey);
        if (existingOrder != null && existingOrder.isPresent()) {
            return replayExistingCheckout(userId, normalizedIdempotencyKey, requestSignature);
        }

        Order processingOrder;
        try {
            processingOrder = orderRepository.insert(buildProcessingOrder(userId, request, normalizedIdempotencyKey));
        } catch (DuplicateKeyException ex) {
            return replayExistingCheckout(userId, normalizedIdempotencyKey, requestSignature);
        }

        try {
            // 1. 获取购物车
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

            if (cart.getItems().isEmpty()) {
                throw new BadRequestException("Cart is empty");
            }

            // 2. 验证每个商品状态，并执行原子扣库存
            for (Cart.CartItem cartItem : cart.getItems()) {
                Phone phone = phoneRepository.findById(cartItem.getPhoneId())
                        .orElseThrow(() -> new ResourceNotFoundException("Phone not found: " + cartItem.getPhoneId()));

                // 验证商品是否被禁用
                if (phone.getIsDisabled()) {
                    throw new BadRequestException("Phone " + phone.getTitle() + " is not available");
                }

                // 预检库存
                if (cartItem.getQuantity() > phone.getStock()) {
                    throw new BadRequestException("Insufficient stock for phone " + phone.getTitle() +
                            ". Available: " + phone.getStock() + ", Requested: " + cartItem.getQuantity());
                }

                // 原子扣减库存，防并发超卖
                boolean stockUpdated = phoneStockRepository.decreaseStockAndIncreaseSales(
                        cartItem.getPhoneId(),
                        cartItem.getQuantity()
                );
                if (!stockUpdated) {
                    throw new BadRequestException("Insufficient stock for phone " + phone.getTitle());
                }
            }

            // 3. 计算总价
            double totalAmount = 0;
            for (Cart.CartItem item : cart.getItems()) {
                totalAmount += item.getPrice() * item.getQuantity();
            }

            // 4. 填充订单对象
            List<Order.OrderItem> orderItems = cart.getItems().stream()
                    .map(item -> Order.OrderItem.builder()
                            .phoneId(item.getPhoneId())
                            .title(item.getTitle())
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .build())
                    .collect(Collectors.toList());

            processingOrder.setItems(orderItems);
            processingOrder.setTotalAmount(totalAmount);
            processingOrder.setPostProcessStatus(OrderPostProcessStatus.PENDING);
            processingOrder.setCheckoutStatus(OrderCheckoutStatus.PROCESSING);
            processingOrder.setCheckoutError(null);

            // 5. 先保存为处理中，发布后置消息成功后再更新为完成
            Order order = orderRepository.save(processingOrder);
            log.info("Order created: {}", order.getId());

            // 6. 清空购物车
            cart.getItems().clear();
            cartRepository.save(cart);
            log.info("Cart cleared for user: {}", userId);

            // 7. 发布订单后置处理消息；失败时标记失败并抛出异常
            try {
                orderMessageProducer.publishOrderPostProcessMessage(buildOrderPostProcessMessage(order));
            } catch (RuntimeException ex) {
                markCheckoutFailed(order.getId(), "Failed to submit order, please try again");
                throw new BadRequestException("Failed to submit order, please try again");
            }

            order.setCheckoutStatus(OrderCheckoutStatus.COMPLETED);
            order.setCheckoutError(null);
            Order completedOrder = orderRepository.save(order);

            // 8. 返回订单响应
            return CheckoutResult.created(buildOrderResponse(completedOrder));
        } catch (RuntimeException ex) {
            markCheckoutFailed(processingOrder.getId(), ex.getMessage());
            throw ex;
        }
    }

    @Override
    public List<OrderResponse> getUserOrders(String userId) {
        log.debug("Getting orders for user: {}", userId);

        // 按创建时间降序排序
        List<Order> orders = getVisibleOrdersForUser(userId);

        return orders.stream()
                .map(this::buildOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderPageResponse getUserOrders(String userId, int page, int pageSize) {
        log.debug("Getting paginated orders for user: {}, page: {}, pageSize: {}", userId, page, pageSize);

        int safePage = page > 0 ? page : 1;
        int safePageSize = pageSize > 0 ? pageSize : 10;
        List<Order> visibleOrders = getVisibleOrdersForUser(userId);

        int fromIndex = Math.min((safePage - 1) * safePageSize, visibleOrders.size());
        int toIndex = Math.min(fromIndex + safePageSize, visibleOrders.size());

        List<OrderResponse> items = visibleOrders.subList(fromIndex, toIndex).stream()
                .map(this::buildOrderResponse)
                .collect(Collectors.toList());

        long totalItems = visibleOrders.size();
        int totalPages = (int) Math.ceil(totalItems / (double) safePageSize);

        OrderPageResponse.Pagination pagination = OrderPageResponse.Pagination.builder()
                .currentPage(safePage)
                .pageSize(safePageSize)
                .totalPages(totalPages)
                .totalItems(totalItems)
                .build();

        return OrderPageResponse.builder()
                .items(items)
                .pagination(pagination)
                .build();
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

    private Order buildProcessingOrder(String userId, CheckoutRequest request, String idempotencyKey) {
        Order.Address orderAddress = Order.Address.builder()
                .street(request.getAddress().getStreet())
                .city(request.getAddress().getCity())
                .state(request.getAddress().getState())
                .zip(request.getAddress().getZip())
                .country(request.getAddress().getCountry())
                .build();

        return Order.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .items(new ArrayList<>())
                .totalAmount(0.0)
                .address(orderAddress)
                .checkoutStatus(OrderCheckoutStatus.PROCESSING)
                .checkoutError(null)
                .postProcessStatus(OrderPostProcessStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CheckoutResult replayExistingCheckout(String userId, String idempotencyKey, String requestSignature) {
        for (int attempt = 0; attempt < REPLAY_WAIT_MAX_ATTEMPTS; attempt++) {
            Order existingOrder = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                    .orElse(null);

            if (existingOrder == null) {
                pauseReplayPoll();
                continue;
            }

            validateRequestReplayMatches(existingOrder, requestSignature);

            OrderCheckoutStatus status = existingOrder.getCheckoutStatus();
            if (status == null || status == OrderCheckoutStatus.COMPLETED) {
                return CheckoutResult.replayed(buildOrderResponse(existingOrder));
            }

            if (status == OrderCheckoutStatus.FAILED) {
                String errorMessage = existingOrder.getCheckoutError();
                if (errorMessage == null || errorMessage.isBlank()) {
                    errorMessage = "Checkout failed";
                }
                throw new BadRequestException(errorMessage);
            }

            if (isStaleProcessingCheckout(existingOrder)) {
                String message = "Checkout timed out. Please try again.";
                markCheckoutFailed(existingOrder.getId(), message);
                throw new BadRequestException(message);
            }

            pauseReplayPoll();
        }

        throw new DuplicateResourceException("Checkout is still processing for this idempotency key");
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        try {
            if (idempotencyKey == null) {
                throw new BadRequestException("Idempotency-Key must be a valid UUID");
            }
            return UUID.fromString(idempotencyKey.trim()).toString();
        } catch (RuntimeException ex) {
            throw new BadRequestException("Idempotency-Key must be a valid UUID");
        }
    }

    private static String normalizeSignaturePart(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String buildCheckoutRequestSignature(CheckoutRequest request) {
        if (request == null || request.getAddress() == null) {
            return "";
        }

        CheckoutRequest.AddressInfo address = request.getAddress();
        return String.join("|",
            normalizeSignaturePart(address.getStreet()),
            normalizeSignaturePart(address.getCity()),
            normalizeSignaturePart(address.getState()),
            normalizeSignaturePart(address.getZip()),
            normalizeSignaturePart(address.getCountry())
        );
    }

    private String buildExistingOrderSignature(Order order) {
        if (order == null || order.getAddress() == null) {
            return "";
        }

        Order.Address address = order.getAddress();
        return String.join("|",
            normalizeSignaturePart(address.getStreet()),
            normalizeSignaturePart(address.getCity()),
            normalizeSignaturePart(address.getState()),
            normalizeSignaturePart(address.getZip()),
            normalizeSignaturePart(address.getCountry())
        );
    }

    private void validateRequestReplayMatches(Order existingOrder, String requestSignature) {
        if (existingOrder == null) {
            return;
        }

        String existingSignature = buildExistingOrderSignature(existingOrder);
        if (existingSignature.isBlank() || requestSignature == null || requestSignature.isBlank()) {
            return;
        }

        if (!existingSignature.equals(requestSignature)) {
            throw new DuplicateResourceException("Idempotency-Key already used for a different checkout request");
        }
    }

    private boolean isStaleProcessingCheckout(Order order) {
        if (order == null || order.getCheckoutStatus() != OrderCheckoutStatus.PROCESSING) {
            return false;
        }

        LocalDateTime createdAt = order.getCreatedAt();
        if (createdAt == null) {
            return false;
        }

        return createdAt.isBefore(LocalDateTime.now().minusSeconds(PROCESSING_STALE_AFTER_SECONDS));
    }

    private void pauseReplayPoll() {
        try {
            Thread.sleep(REPLAY_WAIT_MILLIS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new DuplicateResourceException("Checkout status polling interrupted");
        }
    }

    private void markCheckoutFailed(String orderId, String errorMessage) {
        if (orderId == null) {
            return;
        }

        orderRepository.findById(orderId).ifPresent(order -> {
            order.setCheckoutStatus(OrderCheckoutStatus.FAILED);
            order.setCheckoutError(errorMessage);
            orderRepository.save(order);
        });
    }

    private List<Order> getVisibleOrdersForUser(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .filter(this::isVisibleInOrderHistory)
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    private boolean isVisibleInOrderHistory(Order order) {
        OrderCheckoutStatus status = order.getCheckoutStatus();
        return status == null || status == OrderCheckoutStatus.COMPLETED;
    }

    private OrderPostProcessMessage buildOrderPostProcessMessage(Order order) {
        return OrderPostProcessMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .orderId(order.getId())
                .userId(order.getUserId())
                .items(order.getItems().stream()
                        .map(item -> OrderPostProcessMessage.Item.builder()
                                .phoneId(item.getPhoneId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .totalAmount(order.getTotalAmount())
                .timestamp(Instant.now())
                .build();
    }
}
