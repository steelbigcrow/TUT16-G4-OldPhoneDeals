package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.phone.PhoneCreateRequest;
import com.oldphonedeals.dto.request.phone.PhoneUpdateRequest;
import com.oldphonedeals.dto.response.ApiResponse;
import com.oldphonedeals.dto.response.phone.PhoneListItemResponse;
import com.oldphonedeals.dto.response.phone.PhoneResponse;
import com.oldphonedeals.enums.PhoneBrand;

import java.util.List;
import java.util.Map;

/**
 * 手机商品业务服务接口
 * 提供商品查询、创建、更新、删除等核心业务逻辑
 * 
 * 参考 Express.js 实现：
 * - server/app/controllers/phone.controller.js
 * - server/app/controllers/adminPhone.controller.js
 */
public interface PhoneService {

    /**
     * 获取商品列表（支持搜索、筛选、分页、排序）
     * 
     * @param search 搜索关键词（标题模糊匹配）
     * @param brand 品牌过滤
     * @param maxPrice 最高价格过滤
     * @param sortBy 排序字段（默认：createdAt）
     * @param sortOrder 排序方向（asc/desc，默认：desc）
     * @param page 页码（从 1 开始）
     * @param limit 每页数量（默认：10）
     * @return 包含商品列表、分页信息的 Map
     *         {
     *           "phones": List<PhoneListItemResponse>,
     *           "currentPage": Integer,
     *           "totalPages": Integer,
     *           "total": Long
     *         }
     */
    Map<String, Object> getPhones(
            String search,
            PhoneBrand brand,
            Double maxPrice,
            String sortBy,
            String sortOrder,
            Integer page,
            Integer limit
    );

    /**
     * 获取低库存商品（库存 > 0 且按库存升序，取前 5 个）
     * 用于首页"即将售罄"展示区域
     * 
     * @return 低库存商品列表（最多 5 个）
     */
    List<PhoneListItemResponse> getSoldOutSoonPhones();

    /**
     * 获取畅销商品（至少 2 条评论，按平均评分降序，取前 5 个）
     * 用于首页"畅销商品"展示区域
     * 
     * 实现逻辑：
     * 1. 筛选条件：isDisabled = false 且至少有 2 条评论
     * 2. 计算平均评分（包含隐藏评论）
     * 3. 按平均评分降序排序
     * 4. 取前 5 个
     * 
     * @return 畅销商品列表（最多 5 个）
     */
    List<PhoneListItemResponse> getBestSellers();

    /**
     * 获取商品详情（含评论可见性过滤）
     * 
     * 评论可见性规则：
     * - 未登录用户：只看到未隐藏的评论
     * - 已登录用户：
     *   - 所有未隐藏评论
     *   - 自己的隐藏评论
     *   - 作为卖家时，商品的所有隐藏评论
     * 
     * @param phoneId 商品 ID
     * @param currentUserId 当前用户 ID（可选，用于评论可见性判断）
     * @return 商品详情（包含前 3 条可见评论）
     */
    PhoneResponse getPhoneById(String phoneId, String currentUserId);

    /**
     * 创建商品
     * 
     * @param request 创建请求
     * @param sellerId 卖家 ID（从 JWT 获取）
     * @return 新创建的商品信息
     */
    PhoneResponse createPhone(PhoneCreateRequest request, String sellerId);

    /**
     * 更新商品（仅卖家可操作）
     * 
     * 权限验证：只有商品的卖家才能更新
     * 
     * @param phoneId 商品 ID
     * @param request 更新请求
     * @param sellerId 卖家 ID（从 JWT 获取）
     * @return 更新后的商品信息
     */
    PhoneResponse updatePhone(String phoneId, PhoneUpdateRequest request, String sellerId);

    /**
     * 删除商品（仅卖家可操作）
     * 
     * 权限验证：只有商品的卖家才能删除
     * 
     * @param phoneId 商品 ID
     * @param sellerId 卖家 ID（从 JWT 获取）
     */
    void deletePhone(String phoneId, String sellerId);

    /**
     * 启禁用商品（仅卖家可操作）
     * 
     * 权限验证：只有商品的卖家才能启禁用
     * 
     * @param phoneId 商品 ID
     * @param isDisabled 是否禁用
     * @param sellerId 卖家 ID（从 JWT 获取）
     * @return 操作结果消息
     */
    ApiResponse<String> togglePhoneDisabled(String phoneId, Boolean isDisabled, String sellerId);

    /**
     * 获取卖家的所有商品
     * 
     * @param sellerId 卖家 ID
     * @return 该卖家的所有商品列表
     */
    List<PhoneResponse> getPhonesBySeller(String sellerId);

    /**
     * 管理员获取所有商品（含禁用商品）
     * 
     * 管理员视角：可以看到所有商品，包括被禁用的商品
     * 
     * @param searchTerm 搜索关键词
     * @param brand 品牌过滤
     * @param sellerId 卖家 ID 过滤
     * @param page 页码（从 1 开始）
     * @param limit 每页数量
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return 包含商品列表、分页信息的 Map
     */
    Map<String, Object> adminGetPhones(
            String searchTerm,
            PhoneBrand brand,
            String sellerId,
            Integer page,
            Integer limit,
            String sortBy,
            String sortOrder
    );

    /**
     * 管理员更新商品（记录管理日志）
     * 
     * 功能：
     * 1. 更新商品信息
     * 2. 创建管理日志（AdminLog）
     * 3. 区分操作类型：
     *    - 仅修改 isDisabled：记录为 ENABLE_PHONE 或 DISABLE_PHONE
     *    - 修改其他字段：记录为 UPDATE_PHONE
     * 
     * @param phoneId 商品 ID
     * @param request 更新请求
     * @param adminId 管理员 ID（从 JWT 获取）
     */
    void adminUpdatePhone(String phoneId, PhoneUpdateRequest request, String adminId);

    /**
     * 管理员删除商品（记录管理日志）
     * 
     * 功能：
     * 1. 删除商品
     * 2. 创建管理日志（AdminLog，action = DELETE_PHONE）
     * 
     * @param phoneId 商品 ID
     * @param adminId 管理员 ID（从 JWT 获取）
     */
    void adminDeletePhone(String phoneId, String adminId);
}