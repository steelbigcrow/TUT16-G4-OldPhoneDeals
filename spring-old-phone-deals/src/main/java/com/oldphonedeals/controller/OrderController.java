package com.oldphonedeals.controller;

import com.oldphonedeals.dto.request.order.CheckoutRequest;
import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.order.OrderResponse;
import com.oldphonedeals.security.SecurityContextHelper;
import com.oldphonedeals.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 * <p>
 * 实现订单管理功能，对应3个API端点：
 * - POST /api/orders/checkout - 结账（创建订单）
 * - GET /api/orders/user/{userId} - 获取用户订单列表
 * - GET /api/orders/{orderId} - 获取订单详情
 * </p>
 * 
 * 参考 Express.js 实现：
 * - server/app/routes/order.routes.js
 * - server/app/controllers/order.controller.js
 * 
 * @author OldPhoneDeals Team
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    /**
     * 结账（创建订单）
     * <p>
     * POST /api/orders/checkout
     * 需要认证
     * </p>
     * 
     * 这是一个关键事务操作，包含以下步骤：
     * 1. 验证购物车不为空
     * 2. 验证每个商品的库存和状态
     * 3. 计算总价
     * 4. 创建订单
     * 5. 扣减库存
     * 6. 增加销售计数
     * 7. 清空购物车
     * 
     * 参考：server/app/controllers/order.controller.js:11-82
     * 
     * @param request 结账请求（包含收货地址）
     * @return 订单响应对象
     */
    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request
    ) {
        String userId = SecurityContextHelper.getCurrentUserId();
        log.info("POST /api/orders/checkout - Checking out for user: {}", userId);
        
        OrderResponse response = orderService.checkout(userId, request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Order created successfully"));
    }
    
    /**
     * 获取用户订单列表
     * <p>
     * GET /api/orders/user/{userId}
     * 需要认证，只能查看自己的订单
     * </p>
     * 
     * 按创建时间降序排序
     * 
     * 参考：server/app/controllers/order.controller.js:88-124
     * 
     * @param userId 用户ID
     * @return 订单列表
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(
            @PathVariable String userId
    ) {
        String currentUserId = SecurityContextHelper.getCurrentUserId();
        log.info("GET /api/orders/user/{} - Getting orders for user", userId);
        
        // 权限检查：只能查看自己的订单
        if (!currentUserId.equals(userId)) {
            log.warn("User {} attempted to view orders of user {}", currentUserId, userId);
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You can only view your own orders"));
        }
        
        List<OrderResponse> response = orderService.getUserOrders(userId);
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "Orders retrieved successfully")
        );
    }
    
    /**
     * 获取订单详情
     * <p>
     * GET /api/orders/{orderId}
     * 需要认证，只能查看自己的订单
     * </p>
     * 
     * 参考：server/app/controllers/order.controller.js:130-155
     * 
     * @param orderId 订单ID
     * @return 订单响应对象
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable String orderId
    ) {
        String userId = SecurityContextHelper.getCurrentUserId();
        log.info("GET /api/orders/{} - Getting order details for user: {}", orderId, userId);
        
        OrderResponse response = orderService.getOrderById(orderId, userId);
        
        return ResponseEntity.ok(
            ApiResponse.success(response, "Order retrieved successfully")
        );
    }
}