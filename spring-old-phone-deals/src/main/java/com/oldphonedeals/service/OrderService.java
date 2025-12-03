package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.order.CheckoutRequest;
import com.oldphonedeals.dto.response.order.OrderPageResponse;
import com.oldphonedeals.dto.response.order.OrderResponse;

import java.util.List;

/**
 * 订单服务接口
 * <p>
 * 提供订单管理功能，包括：
 * - 结账（创建订单）
 * - 获取用户订单列表
 * - 获取订单详情
 * </p>
 * 
 * @author OldPhoneDeals Team
 */
public interface OrderService {
    
    /**
     * 结账（创建订单）
     * <p>
     * 这是一个关键事务操作，包含以下步骤（必须原子性执行）：
     * 1. 验证购物车不为空
     * 2. 遍历购物车items，验证每个商品：
     *    - 商品存在且未禁用
     *    - 库存充足（quantity <= phone.stock）
     * 3. 计算总价：totalAmount = sum(item.quantity * phone.price)
     * 4. 创建订单对象
     * 5. 扣减每个商品的库存：phone.stock -= item.quantity
     * 6. 增加商品的销售计数：phone.salesCount += item.quantity
     * 7. 清空购物车：cart.items = []
     * 8. 保存订单、商品、购物车
     * 9. 返回订单对象
     * </p>
     * 
     * @param userId 用户ID
     * @param request 结账请求（包含收货地址）
     * @return 订单响应对象
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 购物车或商品不存在
     * @throws com.oldphonedeals.exception.BadRequestException 购物车为空、商品已禁用或库存不足
     */
    OrderResponse checkout(String userId, CheckoutRequest request);
    
    /**
     * 获取用户订单列表
     * <p>
     * 按创建时间降序排序
     * </p>
     * 
     * @param userId 用户ID
     * @return 订单列表
     */
    List<OrderResponse> getUserOrders(String userId);

    /**
     * 获取用户订单列表（分页）
     * <p>
     * 按创建时间降序排序，页码从 1 开始。
     * </p>
     *
     * @param userId 用户ID
     * @param page 页码（从 1 开始）
     * @param pageSize 每页数量
     * @return 包含订单列表和分页信息的响应
     */
    OrderPageResponse getUserOrders(String userId, int page, int pageSize);
    
    /**
     * 获取订单详情
     * <p>
     * 权限检查：只有订单的买家可以查看
     * </p>
     * 
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 订单响应对象
     * @throws com.oldphonedeals.exception.ResourceNotFoundException 订单不存在
     * @throws com.oldphonedeals.exception.BadRequestException 无权查看该订单
     */
    OrderResponse getOrderById(String orderId, String userId);
}