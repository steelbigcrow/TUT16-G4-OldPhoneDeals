# API 端点映射表

## Express.js 到 Spring Boot 的路由映射

### 1️⃣ 用户认证模块

| Express 路由 | HTTP方法 | Spring Boot 路由 | Controller方法 | 说明 |
|-------------|---------|-----------------|---------------|------|
| `/api/login` | POST | `/api/login` | `AuthController.login()` | 用户登录 |
| `/api/register` | POST | `/api/register` | `AuthController.register()` | 用户注册 |
| `/api/verify-email` | GET | `/api/verify-email` | `AuthController.verifyEmail()` | 邮箱验证 |
| `/api/send-reset-password-email` | POST | `/api/send-reset-password-email` | `AuthController.sendResetPasswordEmail()` | 发送重置密码邮件 |
| `/api/reset-password` | POST | `/api/reset-password` | `AuthController.resetPassword()` | 重置密码 |
| `/api/check-verified` | GET | `/api/check-verified` | `AuthController.checkVerified()` | 检查邮箱是否验证 |
| `/api/user-info` | GET | `/api/user-info` | `AuthController.getUserInfo()` | 获取当前用户信息 |

### 2️⃣ 商品管理模块

| Express 路由 | HTTP方法 | Spring Boot 路由 | Controller方法 | 说明 |
|-------------|---------|-----------------|---------------|------|
| `/api/phones` | GET | `/api/phones` | `PhoneController.getAllPhones()` | 获取商品列表（支持搜索、筛选、分页、special列表） |
| `/api/phones/:phoneId` | GET | `/api/phones/{phoneId}` | `PhoneController.getPhoneById()` | 获取商品详情（包含前3条可见评论） |
| `/api/phones/by-seller/:sellerId` | GET | `/api/phones/by-seller/{sellerId}` | `PhoneController.getPhonesBySeller()` | 获取卖家的所有商品 |
| `/api/phones/:phoneId/reviews` | GET | `/api/phones/{phoneId}/reviews` | `ReviewController.getMoreReviews()` | 获取更多评论 |
| `/api/phones/:phoneId/reviews` | POST | `/api/phones/{phoneId}/reviews` | `ReviewController.addReview()` | 添加评论 |
| `/api/phones/:phoneId/reviews/:reviewId/visibility` | PATCH | `/api/phones/{phoneId}/reviews/{reviewId}/visibility` | `ReviewController.toggleReviewVisibility()` | 切换评论可见性 |
| `/api/phones/:phoneId` | DELETE | `/api/phones/{phoneId}` | `PhoneController.deletePhone()` | 删除商品 |
| `/api/phones/disable-phone/:phoneId` | PUT | `/api/phones/{phoneId}/disable` | `PhoneController.togglePhoneDisabled()` | 启用/禁用商品 |
| `/api/phones` | POST | `/api/phones` | `PhoneController.createPhone()` | 创建商品 |
| `/api/phones/:phoneId` | PUT | `/api/phones/{phoneId}` | `PhoneController.updatePhone()` | 更新商品 |
| `/api/upload-image` | POST | `/api/upload-image` | `FileUploadController.uploadImage()` | 上传图片 |
| `/api/phones/reviews/get-reviews-by-id` | GET | `/api/phones/reviews/by-seller` | `ReviewController.getReviewsBySeller()` | 获取卖家商品的所有评论 |

### 3️⃣ 购物车模块

| Express 路由 | HTTP方法 | Spring Boot 路由 | Controller方法 | 说明 |
|-------------|---------|-----------------|---------------|------|
| `/api/cart` | GET | `/api/cart` | `CartController.getCart()` | 获取购物车 |
| `/api/cart/items` | POST | `/api/cart/items` | `CartController.addOrUpdateItem()` | 添加或更新购物车项 |
| `/api/cart/items/:phoneId` | PATCH | `/api/cart/items/{phoneId}` | `CartController.updateItemQuantity()` | 更新购物车项数量 |
| `/api/cart/items/:phoneId` | DELETE | `/api/cart/items/{phoneId}` | `CartController.removeItem()` | 删除购物车项 |

### 4️⃣ 订单模块

| Express 路由 | HTTP方法 | Spring Boot 路由 | Controller方法 | 说明 |
|-------------|---------|-----------------|---------------|------|
| `/api/orders` | POST | `/api/orders` | `OrderController.checkout()` | 结账创建订单 |
| `/api/orders` | GET | `/api/orders` | `OrderController.getOrders()` | 获取订单历史 |
| `/api/orders/:orderId` | GET | `/api/orders/{orderId}` | `OrderController.getOrder()` | 获取订单详情 |

### 5️⃣ 收藏夹模块

| Express 路由 | HTTP方法 | Spring Boot 路由 | Controller方法 | 说明 |
|-------------|---------|-----------------|---------------|------|
| `/api/wishlist` | GET | `/api/wishlist` | `WishlistController.getWishlist()` | 获取收藏夹 |
| `/api/wishlist/:phoneId` | POST | `/api/wishlist/{phoneId}` | `WishlistController.addToWishlist()` | 添加到收藏夹 |
| `/api/wishlist/:phoneId` | DELETE | `/api/wishlist/{phoneId}` | `WishlistController.removeFromWishlist()` | 从收藏夹移除 |

### 6️⃣ 用户资料模块

| Express 路由 | HTTP方法 | Spring Boot 路由 | Controller方法 | 说明 |
|-------------|---------|-----------------|---------------|------|
| `/api/profile` | GET | `/api/profile` | `ProfileController.getProfile()` | 获取用户资料 |
| `/api/profile` | PUT | `/api/profile` | `ProfileController.updateProfile()` | 更新用户资料 |
| `/api/profile/change-password` | POST | `/api/profile/change-password` | `ProfileController.changePassword()` | 修改密码 |

### 7️⃣ 管理员认证与统计模块

| Express 路由 | HTTP方法 | Spring Boot 路由 | Controller方法 | 说明 |
|-------------|---------|-----------------|---------------|------|
| `/api/admin/login` | POST | `/api/admin/login` | `AdminController.adminLogin()` | 管理员登录 |
| `/api/admin/profile` | GET | `/api/admin/profile` | `AdminController.getAdminProfile()` | 获取管理员资料 |
| `/api/admin/profile` | PUT | `/api/admin/profile` | `AdminController.updateAdminProfile()` | 更新管理员资料 |
| `/api/admin/stats` | GET | `/api/admin/stats` | `AdminController.getDashboardStats()` | 获取Dashboard统计（用户数、商品数、评论数、订单数） |

### 8️⃣ 管理员用户管理模块

| Express 路由 | HTTP方法 | Spring Boot 路由 | Controller方法 | 说明 |
|-------------|---------|-----------------|---------------|------|
| `/api/admin/users` | GET | `/api/admin/users` | `AdminController.getAllUsers()` | 获取用户列表（支持search、isDisabled过滤） |
| `/api/admin/users/:userId` | GET | `/api/admin/users/{userId}` | `AdminController.getUserDetail()` | 获取用户详情 |
| `/api/admin/users/:userId` | PUT | `/api/admin/users/{userId}` | `AdminController.updateUser()` | 更新用户信息 |
| `/api/admin/users/:userId` | DELETE | `/api/admin/users/{userId}` | `AdminController.deleteUser()` | 删除用户 |
| `/api/admin/users/:userId/toggle-disabled` | PUT | `/api/admin/users/{userId}/toggle-disabled` | `AdminController.toggleUserStatus()` | 切换用户禁用状态 |

### 9️⃣ 管理员商品管理模块

| Express 路由 | HTTP方法 | Spring Boot 路由 | Controller方法 | 说明 |
|-------------|---------|-----------------|---------------|------|
| `/api/admin/phones` | GET | `/api/admin/phones` | `AdminController.getAllPhones()` | 获取所有商品（包括禁用的） |
| `/api/admin/phones/:phoneId` | PUT | `/api/admin/phones/{phoneId}` | `AdminController.updatePhone()` | 更新商品信息 |
| `/api/admin/phones/:phoneId` | DELETE | `/api/admin/phones/{phoneId}` | `AdminController.deletePhone()` | 删除商品 |
| `/api/admin/phones/:phoneId/toggle-disabled` | PUT | `/api/admin/phones/{phoneId}/toggle-disabled` | `AdminController.togglePhoneStatus()` | 切换商品禁用状态 |

### 🔟 管理员评论管理模块

| Express 路由 | HTTP方法 | Spring Boot 路由 | Controller方法 | 说明 |
|-------------|---------|-----------------|---------------|------|
| `/api/admin/reviews` | GET | `/api/admin/reviews` | `AdminController.getAllReviews()` | 获取所有评论（支持visibility、reviewerId、phoneId、search过滤） |
| `/api/admin/reviews/:phoneId/:reviewId/toggle-visibility` | PUT | `/api/admin/reviews/{phoneId}/{reviewId}/toggle-visibility` | `AdminController.toggleReviewVisibility()` | 切换评论可见性 |
| `/api/admin/reviews/:phoneId/:reviewId` | DELETE | `/api/admin/reviews/{phoneId}/{reviewId}` | `AdminController.deleteReview()` | 删除评论 |

### 1️⃣1️⃣ 管理员订单管理模块

| Express 路由 | HTTP方法 | Spring Boot 路由 | Controller方法 | 说明 |
|-------------|---------|-----------------|---------------|------|
| `/api/admin/orders` | GET | `/api/admin/orders` | `AdminController.getAllOrders()` | 获取所有订单（支持userId、startDate、endDate过滤） |
| `/api/admin/orders/:orderId` | GET | `/api/admin/orders/{orderId}` | `AdminController.getOrderDetail()` | 获取订单详情 |
| `/api/admin/orders/stats` | GET | `/api/admin/orders/stats` | `AdminController.getSalesStats()` | 获取销售统计（总销售额、总交易数） |

### 1️⃣2️⃣ 管理员操作日志模块

| Express 路由 | HTTP方法 | Spring Boot 路由 | Controller方法 | 说明 |
|-------------|---------|-----------------|---------------|------|
| `/api/admin/logs` | GET | `/api/admin/logs` | `AdminController.getAllLogs()` | 获取操作日志列表 |

---

## 🔍 请求/响应格式对比

### Express.js 响应格式
```json
// 成功响应
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}

// 错误响应
{
  "success": false,
  "message": "Error message"
}
```

### Spring Boot 响应格式（保持一致）
```json
// 成功响应
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}

// 错误响应
{
  "success": false,
  "message": "Error message",
  "data": null
}
```

---

## 🔐 认证方式

### Express.js
- Header: `Authorization: Bearer <token>`
- JWT验证中间件：`checkJWT.js`
- 管理员验证中间件：`checkAdmin.js`

### Spring Boot
- Header: `Authorization: Bearer <token>`
- JWT过滤器：`JwtAuthenticationFilter`
- 权限注解：`@PreAuthorize("hasRole('ADMIN')")`

---

## 📦 分页参数

### Express.js & Spring Boot（保持一致）
```
GET /api/phones?page=1&limit=10&sortBy=createdAt&sortOrder=desc
```

**请求参数：**
- `page`: 页码（从1开始）
- `limit`: 每页数量
- `sortBy`: 排序字段
- `sortOrder`: 排序方向（asc/desc）

**响应格式：**
```json
{
  "phones": [...],
  "currentPage": 1,
  "totalPages": 10,
  "total": 100
}
```

---

## 🔄 特殊端点说明

### 1. 商品列表特殊查询参数
```
# 获取即将售罄商品（库存低于等于5的商品，按库存升序）
GET /api/phones?special=soldOutSoon

# 获取畅销商品（按平均评分倒序，返回前10个）
GET /api/phones?special=bestSellers

# 常规搜索和筛选
GET /api/phones?search=iPhone&brand=APPLE&maxPrice=1000&sortBy=price&sortOrder=asc
```

**支持的参数：**
- `search`: 商品名称模糊搜索
- `brand`: 品牌筛选（枚举值）
- `maxPrice`: 最高价格筛选
- `sortBy`: 排序字段（price、createdAt等）
- `sortOrder`: 排序方向（asc/desc）
- `special`: 特殊列表（soldOutSoon/bestSellers）
- `page`: 页码（从1开始）
- `limit`: 每页数量

### 2. 商品详情评论规则
**评论可见性规则：**
- 未登录用户：只能看到公开评论（isHidden=false）
- 已登录用户：可以看到公开评论 + 自己的隐藏评论
- 商品卖家：可以看到该商品的所有评论

**评论数量限制：**
- 商品详情接口（`GET /api/phones/{phoneId}`）仅返回前3条可见评论
- 获取更多评论需调用专门的评论列表接口（`GET /api/phones/{phoneId}/reviews`）

### 3. 管理员过滤参数
```
# 用户列表过滤
GET /api/admin/users?page=0&pageSize=10&search=john&isDisabled=false

# 评论列表过滤
GET /api/admin/reviews?page=0&pageSize=10&visibility=false&reviewerId=xxx&phoneId=yyy&search=keyword

# 订单列表过滤
GET /api/admin/orders?page=0&pageSize=10&userId=xxx&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
```

**管理员用户列表支持参数：**
- `search`: 按姓名或邮箱模糊搜索
- `isDisabled`: 按禁用状态过滤（true/false）

**管理员评论列表支持参数：**
- `visibility`: 按可见性过滤（false=隐藏, true=不传或null=所有）
- `reviewerId`: 按评论者ID过滤
- `phoneId`: 按商品ID过滤
- `search`: 按评论内容模糊搜索

**管理员订单列表支持参数：**
- `userId`: 按用户ID过滤
- `startDate`: 开始日期（ISO 8601格式）
- `endDate`: 结束日期（ISO 8601格式）

### 4. 文件上传
- Content-Type: `multipart/form-data`
- 字段名：`image`
- 返回：`{ "success": true, "url": "/static/images/xxx.jpg" }`

---

## 🚀 迁移注意事项

1. **保持URL路径一致**：确保前端无需修改API调用
2. **保持响应格式一致**：使用`ApiResponse`包装器
3. **保持错误码一致**：HTTP状态码和错误消息
4. **保持分页逻辑一致**：页码从1开始，返回格式相同
5. **保持认证方式一致**：JWT Token格式和验证逻辑
6. **保持文件存储路径一致**：`/static/images/` 前缀