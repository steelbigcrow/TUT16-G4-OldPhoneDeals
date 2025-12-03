package com.oldphonedeals.service;

import com.oldphonedeals.dto.request.admin.UpdatePhoneRequest;
import com.oldphonedeals.dto.request.admin.UpdateUserRequest;
import com.oldphonedeals.dto.request.profile.UpdateProfileRequest;
import com.oldphonedeals.dto.response.PageResponse;
import com.oldphonedeals.dto.response.admin.*;
import com.oldphonedeals.dto.response.auth.LoginResponse;

/**
 * 管理员服务接口
 * 包含管理员认证、用户管理、商品管理、评论管理、订单管理五个子模块
 */
public interface AdminService {

    // ============================================
    // 管理员认证模块
    // ============================================

    /**
     * 管理员登录
     *
     * @param email    邮箱
     * @param password 密码
     * @return 登录响应（包含JWT token）
     */
    LoginResponse adminLogin(String email, String password);

    /**
     * 获取管理员资料
     *
     * @param adminId 管理员ID
     * @return 管理员资料
     */
    AdminProfileResponse getAdminProfile(String adminId);

    /**
     * 更新管理员资料
     *
     * @param adminId 管理员ID
     * @param request 更新请求
     * @return 更新后的管理员资料
     */
    AdminProfileResponse updateAdminProfile(String adminId, UpdateProfileRequest request);

    /**
     * 获取管理员仪表板统计数据
     *
     * @return 统计数据
     */
    AdminStatsResponse getDashboardStats();

    // ============================================
    // 用户管理模块
    // ============================================

    /**
     * 获取所有用户（分页，支持搜索和过滤）
     *
     * @param page       页码（从0开始）
     * @param pageSize   每页大小
     * @param search     搜索关键词（可选，搜索姓名/邮箱）
     * @param isDisabled 禁用状态过滤（可选）
     * @return 用户分页响应
     */
    PageResponse<UserManagementResponse> getAllUsers(int page, int pageSize, String search, Boolean isDisabled);

    /**
     * 获取所有用户（分页，兼容无参数版本）
     *
     * @param page     页码（从0开始）
     * @param pageSize 每页大小
     * @return 用户分页响应
     */
    PageResponse<UserManagementResponse> getAllUsers(int page, int pageSize);

    /**
     * 获取用户详情
     *
     * @param userId  用户ID
     * @param adminId 管理员ID（用于记录日志）
     * @return 用户详情（包含统计信息）
     */
    UserDetailResponse getUserDetail(String userId, String adminId);

    /**
     * 更新用户信息
     *
     * @param userId  用户ID
     * @param request 更新请求
     * @param adminId 管理员ID（用于记录日志）
     * @return 更新后的用户信息
     */
    UserManagementResponse updateUser(String userId, UpdateUserRequest request, String adminId);

    /**
     * 切换用户禁用状态
     *
     * @param userId  用户ID
     * @param adminId 管理员ID（用于记录日志）
     * @return 更新后的用户信息
     */
    UserManagementResponse toggleUserStatus(String userId, String adminId);

    /**
     * 删除用户（级联删除所有关联数据）
     *
     * @param userId  用户ID
     * @param adminId 管理员ID（用于记录日志）
     */
    void deleteUser(String userId, String adminId);

    // ============================================
    // 商品管理模块
    // ============================================

    /**
     * 获取所有商品（包含禁用的商品）
     *
     * @param page     页码（从0开始）
     * @param pageSize 每页大小
     * @return 商品分页响应
     */
    PageResponse<PhoneManagementResponse> getAllPhonesForAdmin(int page, int pageSize);

    /**
     * 管理员更新商品信息
     *
     * @param phoneId 商品ID
     * @param request 更新请求
     * @param adminId 管理员ID（用于记录日志）
     * @return 更新后的商品信息
     */
    PhoneManagementResponse updatePhoneByAdmin(String phoneId, UpdatePhoneRequest request, String adminId);

    /**
     * 切换商品禁用状态
     *
     * @param phoneId 商品ID
     * @param adminId 管理员ID（用于记录日志）
     * @return 更新后的商品信息
     */
    PhoneManagementResponse togglePhoneStatus(String phoneId, String adminId);

    /**
     * 删除商品（级联删除所有关联数据）
     *
     * @param phoneId 商品ID
     * @param adminId 管理员ID（用于记录日志）
     */
    void deletePhone(String phoneId, String adminId);

    // ============================================
    // 评论管理模块
    // ============================================

    /**
     * 获取所有评论（包含隐藏的评论，支持过滤）
     *
     * @param page       页码（从0开始）
     * @param pageSize   每页大小
     * @param visibility 可见性过滤（可选，true=仅显示隐藏的，false=仅显示公开的）
     * @param reviewerId 评论者ID过滤（可选）
     * @param phoneId    商品ID过滤（可选）
     * @param search     搜索关键词（可选，搜索评论内容）
     * @return 评论分页响应
     */
    PageResponse<ReviewManagementResponse> getAllReviews(int page, int pageSize, Boolean visibility,
                                                         String reviewerId, String phoneId, String search);

    /**
     * 获取所有评论（包含隐藏的评论，兼容无参数版本）
     *
     * @param page     页码（从0开始）
     * @param pageSize 每页大小
     * @return 评论分页响应
     */
    PageResponse<ReviewManagementResponse> getAllReviews(int page, int pageSize);

    /**
     * 切换评论可见性（隐藏/显示）
     *
     * @param phoneId  商品ID
     * @param reviewId 评论ID
     * @param adminId  管理员ID（用于记录日志）
     * @return 更新后的评论信息
     */
    ReviewManagementResponse toggleReviewVisibility(String phoneId, String reviewId, String adminId);

    /**
     * 删除评论
     *
     * @param phoneId  商品ID
     * @param reviewId 评论ID
     * @param adminId  管理员ID（用于记录日志）
     */
    void deleteReview(String phoneId, String reviewId, String adminId);

    // ============================================
    // 订单管理模块
    // ============================================

    /**
     * 获取所有订单（分页，支持过滤）
     *
     * @param page      页码（从0开始）
     * @param pageSize  每页大小
     * @param userId    用户ID过滤（可选）
     * @param startDate 开始日期过滤（可选，ISO格式字符串）
     * @param endDate   结束日期过滤（可选，ISO格式字符串）
     * @param searchTerm 搜索关键词（可选，搜索订单内容）
     * @param brandFilter 品牌过滤（可选）
     * @param sortBy     排序字段（可选）
     * @param sortOrder  排序顺序（可选）
     * @return 订单分页响应
     */
    PageResponse<OrderManagementResponse> getAllOrders(int page, int pageSize, String userId,
                                                       String startDate, String endDate,
                                                       String searchTerm, String brandFilter,
                                                       String sortBy, String sortOrder);

    /**
     * 获取所有订单（分页，兼容无参数版本）
     *
     * @param page     页码（从0开始）
     * @param pageSize 每页大小
     * @return 订单分页响应
     */
    PageResponse<OrderManagementResponse> getAllOrders(int page, int pageSize);

    /**
     * 获取销售统计数据
     *
     * @return 销售统计
     */
    SalesStatsResponse getSalesStats();

    /**
     * 获取订单详情
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    OrderDetailResponse getOrderDetail(String orderId);

    /**
     * 导出订单数据
     *
     * @param format     导出格式（csv/json）
     * @param userId     用户ID（可选）
     * @param startDate  开始日期（可选，ISO格式字符串）
     * @param endDate    结束日期（可选，ISO格式字符串）
     * @param searchTerm 搜索关键词（可选）
     * @param brandFilter 品牌过滤（可选）
     * @param sortBy     排序字段（可选）
     * @param sortOrder  排序顺序（可选）
     * @return 导出结果（包含文件名和下载URL）
     */
    OrderExportResult exportOrders(String format, String userId, String startDate,
                                   String endDate, String searchTerm, String brandFilter,
                                   String sortBy, String sortOrder);
}