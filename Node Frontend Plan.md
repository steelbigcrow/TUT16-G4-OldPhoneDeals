# Node Frontend Plan - React + TypeScript + Tailwind CSS

## 项目概述
使用 React + TypeScript + Tailwind 重构 OldPhoneDeals 前端，替换现有 Angular 实现，接入 Spring Boot REST API（默认 `http://localhost:8080/api`），保持现有功能和 UI 体验。

---

## 一、技术栈选择

| 类别 | 技术选型 | 说明 |
|------|----------|------|
| **框架** | React 18+ | 组件化开发，生态成熟 |
| **语言** | TypeScript | 类型安全，与后端 DTO 对齐 |
| **样式** | Tailwind CSS | 原子化 CSS，快速开发 |
| **路由** | React Router v6 | 声明式路由，支持嵌套和守卫 |
| **状态管理** | Context API | React 原生方案，无额外依赖 |
| **HTTP 客户端** | Axios + TanStack Query | 请求缓存、重试、状态管理 |
| **表单处理** | React Hook Form + Zod | 表单验证和类型安全 |
| **UI 组件** | Headless UI | Tailwind Labs 官方出品，完美集成 |
| **构建工具** | Vite | 快速开发服务器和构建 |

**项目位置**: `TUT16-G4-OldPhoneDeals/node-frontend/` (Monorepo 内部)

---

## 二、目录结构规划

```
node-frontend/
├── src/
│   ├── main.tsx                    # 应用入口
│   ├── App.tsx                     # 根组件和路由配置
│   │
│   ├── types/                      # TypeScript 类型定义
│   │   ├── user.ts
│   │   ├── phone.ts
│   │   ├── cart.ts
│   │   ├── order.ts
│   │   ├── review.ts
│   │   └── api.ts                  # API 响应类型
│   │
│   ├── api/                        # API 层
│   │   ├── client.ts               # Axios 实例配置
│   │   ├── auth.ts                 # 认证相关 API
│   │   ├── phones.ts               # 商品 API
│   │   ├── cart.ts                 # 购物车 API
│   │   ├── orders.ts               # 订单 API
│   │   ├── profile.ts              # 用户资料 API
│   │   ├── wishlist.ts             # 收藏夹 API
│   │   └── admin.ts                # 管理员 API
│   │
│   ├── hooks/                      # 自定义 Hooks
│   │   ├── useAuth.ts              # 认证状态
│   │   ├── useCart.ts              # 购物车操作
│   │   ├── usePhones.ts            # 商品查询
│   │   └── useNotification.ts      # 消息通知
│   │
│   ├── contexts/                   # React Context 全局状态
│   │   ├── AuthContext.tsx         # 用户认证上下文
│   │   ├── CartContext.tsx         # 购物车上下文
│   │   └── NotificationContext.tsx # 通知上下文
│   │
│   ├── components/                 # 共享组件
│   │   ├── layout/
│   │   │   ├── Header.tsx          # 用户导航栏
│   │   │   ├── AdminHeader.tsx     # 管理员导航栏
│   │   │   ├── Footer.tsx
│   │   │   └── Layout.tsx          # 页面布局容器
│   │   ├── ui/
│   │   │   ├── Button.tsx
│   │   │   ├── Input.tsx
│   │   │   ├── Modal.tsx
│   │   │   ├── Pagination.tsx
│   │   │   └── Loading.tsx
│   │   ├── phone/
│   │   │   ├── PhoneCard.tsx       # 商品卡片
│   │   │   ├── PhoneForm.tsx       # 商品表单
│   │   │   └── PhoneGrid.tsx       # 商品网格
│   │   ├── review/
│   │   │   └── ReviewItem.tsx      # 评论项
│   │   └── common/
│   │       ├── Notification.tsx    # 全局通知
│   │       ├── ConfirmDialog.tsx   # 确认对话框
│   │       └── ProtectedRoute.tsx  # 路由守卫
│   │
│   ├── pages/                      # 页面组件
│   │   ├── auth/
│   │   │   ├── LoginPage.tsx
│   │   │   ├── RegisterPage.tsx
│   │   │   ├── VerifyEmailPage.tsx
│   │   │   └── ResetPasswordPage.tsx
│   │   ├── user/
│   │   │   ├── HomePage.tsx
│   │   │   ├── SearchPage.tsx
│   │   │   ├── PhoneDetailPage.tsx
│   │   │   ├── CheckoutPage.tsx
│   │   │   └── WishlistPage.tsx
│   │   ├── profile/
│   │   │   ├── ProfilePage.tsx
│   │   │   ├── EditProfilePage.tsx
│   │   │   ├── ChangePasswordPage.tsx
│   │   │   ├── ManageListingsPage.tsx
│   │   │   └── SellerReviewsPage.tsx
│   │   └── admin/
│   │       ├── DashboardPage.tsx
│   │       ├── UserManagementPage.tsx
│   │       ├── ListingManagementPage.tsx
│   │       ├── ReviewManagementPage.tsx
│   │       └── SalesLogsPage.tsx
│   │
│   ├── utils/                      # 工具函数
│   │   ├── storage.ts              # localStorage 封装
│   │   ├── format.ts               # 格式化函数
│   │   └── validators.ts           # 验证函数
│   │
│   └── styles/                     # 全局样式
│       └── index.css               # Tailwind 入口
│
├── public/                         # 静态资源
├── index.html
├── package.json
├── tsconfig.json
├── tailwind.config.js
├── vite.config.ts
└── .env.example
```

---

## 三、功能模块映射

### 3.1 认证模块

| Angular 原有功能 | React 实现方案 |
|-----------------|---------------|
| LoginComponent | LoginPage + useAuth hook |
| RegisterComponent | RegisterPage + React Hook Form |
| VerifyEmailComponent | VerifyEmailPage |
| ResetPasswordComponent | ResetPasswordPage |
| AuthGuardService | ProtectedRoute HOC |
| AdminGuardService | AdminRoute HOC |

**状态管理**: 使用 AuthContext 管理用户登录状态、Token 存储

### 3.2 用户页面模块

| Angular 原有功能 | React 实现方案 |
|-----------------|---------------|
| UserHomeComponent | HomePage (热销 + 即将售罄展示) |
| UserSearchComponent | SearchPage (搜索、过滤、排序、分页) |
| UserPhoneDetailComponent | PhoneDetailPage (详情 + 评论) |
| CheckoutComponent | CheckoutPage |
| WishlistComponent | WishlistPage |

### 3.3 用户资料模块

| Angular 原有功能 | React 实现方案 |
|-----------------|---------------|
| UserProfileComponent | ProfilePage |
| EditProfileComponent | EditProfilePage |
| ChangePasswordComponent | ChangePasswordPage |
| ManageListingsComponent | ManageListingsPage |
| ViewSellerReviewComponent | SellerReviewsPage |

### 3.4 管理员模块

| Angular 原有功能 | React 实现方案 |
|-----------------|---------------|
| AdminDashboardComponent | DashboardPage (统计仪表板) |
| AdminUserManagementComponent | UserManagementPage |
| AdminListingManagementComponent | ListingManagementPage |
| AdminReviewManagementComponent | ReviewManagementPage |
| AdminSalesLogsComponent | SalesLogsPage (含导出功能) |

### 3.5 共享组件

| Angular 原有组件 | React 实现 |
|-----------------|-----------|
| PhoneCardComponent | PhoneCard |
| PhoneFormComponent | PhoneForm |
| PaginationComponent | Pagination |
| ReviewItemComponent | ReviewItem |
| ConfirmationDialog | ConfirmDialog |
| QuantityInputDialog | 使用 Modal + Input 组合 |
| MessageComponent | Notification (Toast 风格) |

---

## 四、后端 API 对接

### 4.1 API 基础配置

- **Base URL**: 环境变量 `VITE_API_BASE_URL`（开发默认 `http://localhost:8080/api`）
- **认证**: JWT Bearer Token，localStorage key `auth_token`
- **请求拦截**: 自动附加 Authorization 头
- **响应格式**: 统一 `{ success, message, data }`，类型与后端对齐
- **错误处理**: 401/403 清理 Token 并跳转登录；其他错误弹出通知
- **跨域/代理**: Spring Boot 放行 `http://localhost:5173`；Vite devServer 代理 `/api -> http://localhost:8080/api`；生产同源或经网关转发

### 4.2 API 端点（Spring Boot）

**认证**

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/login` | POST | 登录 |
| `/api/register` | POST | 注册 |
| `/api/verify-email` | GET | 验证邮箱 |
| `/api/send-reset-password-email` | POST | 发送重置密码邮件 |
| `/api/reset-password` | POST | 重置密码 |
| `/api/check-verified` | GET | 查询验证状态 |
| `/api/user-info` | GET | 获取当前用户信息 |

**商品 / 评论**

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/phones` | GET | 商品列表（支持 search/special/sort/page） |
| `/api/phones/{phoneId}` | GET | 商品详情 |
| `/api/phones/by-seller/{sellerId}` | GET | 某卖家商品 |
| `/api/phones/{phoneId}/reviews` | GET | 商品评论分页 |
| `/api/phones/{phoneId}/reviews` | POST | 添加评论 |
| `/api/phones/{phoneId}/reviews/{reviewId}/visibility` | PATCH | 切换评论可见性 |
| `/api/phones/{phoneId}` | DELETE | 删除商品 |
| `/api/phones/{phoneId}/disable` | PUT | 上/下架商品 |
| `/api/phones` | POST | 创建商品 |
| `/api/phones/{phoneId}` | PUT | 更新商品 |
| `/api/upload-image` | POST | 上传图片（multipart/form-data） |
| `/api/phones/reviews/by-seller` | GET | 卖家全部评论 |

**购物车 / 订单**

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/cart` | GET | 获取购物车 |
| `/api/cart/items` | POST | 添加/更新购物车商品 |
| `/api/cart/items/{phoneId}` | PATCH | 更新数量 |
| `/api/cart/items/{phoneId}` | DELETE | 移除商品 |
| `/api/orders` | POST | 结账创建订单 |
| `/api/orders` | GET | 订单列表 |
| `/api/orders/{orderId}` | GET | 订单详情 |

**资料 / 收藏**

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/profile` | GET | 获取个人资料 |
| `/api/profile` | PUT | 更新个人资料 |
| `/api/profile/change-password` | POST | 修改密码 |
| `/api/wishlist` | GET | 收藏夹列表 |
| `/api/wishlist/{phoneId}` | POST | 加入收藏 |
| `/api/wishlist/{phoneId}` | DELETE | 移除收藏 |

**管理员**

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/admin/login` | POST | 管理员登录 |
| `/api/admin/profile` | GET/PUT | 获取/更新管理员资料 |
| `/api/admin/stats` | GET | 仪表盘统计 |
| `/api/admin/users` | GET | 用户列表（search/isDisabled） |
| `/api/admin/users/{userId}` | GET/PUT/DELETE | 用户详情/更新/删除 |
| `/api/admin/users/{userId}/toggle-disabled` | PUT | 启用/停用用户 |
| `/api/admin/phones` | GET | 商品列表（支持搜索/分页） |
| `/api/admin/phones/{phoneId}` | PUT/DELETE | 更新/删除商品 |
| `/api/admin/phones/{phoneId}/toggle-disabled` | PUT | 切换商品上下架 |
| `/api/admin/reviews` | GET | 评论列表（visibility/reviewerId/phoneId/search） |
| `/api/admin/reviews/{phoneId}/{reviewId}/toggle-visibility` | PUT | 切换评论可见性 |
| `/api/admin/reviews/{phoneId}/{reviewId}` | DELETE | 删除评论 |
| `/api/admin/orders` | GET | 订单列表（userId/日期区间） |
| `/api/admin/orders/{orderId}` | GET | 订单详情 |
| `/api/admin/orders/stats` | GET | 销售统计 |
| `/api/admin/logs` | GET | 操作日志 |

---

## 五、路由配置

```
/                           → 重定向到 /home
/home                       → HomePage
/search                     → SearchPage
/phone/:id                  → PhoneDetailPage
/login                      → LoginPage
/register                   → RegisterPage
/verify-email               → VerifyEmailPage
/reset-password             → ResetPasswordPage

# 需要认证的路由
/checkout                   → CheckoutPage (ProtectedRoute)
/wishlist                   → WishlistPage (ProtectedRoute)
/profile                    → ProfilePage (ProtectedRoute)
/profile/edit               → EditProfilePage
/profile/change-password    → ChangePasswordPage
/profile/listings           → ManageListingsPage
/profile/reviews            → SellerReviewsPage

# 管理员路由
/admin                      → 重定向到 /admin/dashboard (AdminRoute)
/admin/dashboard            → DashboardPage
/admin/users                → UserManagementPage
/admin/listings             → ListingManagementPage
/admin/reviews              → ReviewManagementPage
/admin/sales                → SalesLogsPage

/*                          → 重定向到 /home
```

---

## 六、状态管理策略

### 6.1 全局状态 (React Context API)

| Context | 管理内容 |
|---------|---------|
| AuthContext | 当前用户、Token、登录状态、登录/登出方法 |
| CartContext | 购物车数量徽章（详细数据从 API 获取） |
| NotificationContext | 全局消息通知队列 |

**Context 架构**:
- 每个 Context 配套 Provider 和 useXxx hook
- 在 App 根组件中嵌套所有 Provider
- 通过自定义 hook 暴露状态和方法

### 6.2 服务端状态 (TanStack Query)

- 商品列表、详情、搜索结果
- 购物车内容
- 订单列表
- 用户资料
- 管理员数据表

**优势**: 自动缓存、后台刷新、乐观更新、分页支持

---

## 七、UI/UX 设计原则

### 7.1 样式迁移策略

- **保持原有 UI 风格**: 参考 Angular 版本的视觉设计
- **使用 Tailwind 重构**: 将 SCSS 样式转换为 Tailwind 类
- **响应式设计**: 移动端优先，支持多种屏幕尺寸
- **组件一致性**: 统一按钮、输入框、卡片等基础组件样式

### 7.2 交互体验

- 页面加载状态指示
- 表单验证即时反馈
- 操作成功/失败通知
- 确认对话框防止误操作
- 平滑过渡动画

---

## 八、开发阶段规划

### 阶段一：项目初始化
- 创建 Vite + React + TypeScript 项目
- 配置 Tailwind CSS
- 设置项目目录结构
- 配置开发代理和环境变量

### 阶段二：基础架构
- 实现 API 客户端和拦截器
- 创建类型定义
- 实现全局状态 Context
- 创建基础 UI 组件

### 阶段三：认证模块
- 登录/注册页面
- 邮箱验证和密码重置
- 路由守卫实现
- Token 管理

### 阶段四：用户功能
- 首页（热销、即将售罄）
- 搜索页面（过滤、排序、分页）
- 商品详情页
- 购物车和结账
- 收藏夹

### 阶段五：用户资料
- 资料展示和编辑
- 密码修改
- 卖家商品管理
- 卖家评论查看

### 阶段六：管理员后台
- 仪表板统计
- 用户管理
- 商品管理
- 评论管理
- 销售日志和导出

### 阶段七：优化和测试
- 性能优化
- 响应式适配
- 错误处理完善
- 集成测试

---

## 九、关键迁移映射

### Angular → React 架构对照

| Angular 概念 | React 对应 |
|-------------|-----------|
| Services | Custom Hooks + API 模块 |
| BehaviorSubject | React Context + useState |
| Route Guards | ProtectedRoute HOC |
| HttpClient | Axios + TanStack Query |
| Reactive Forms | React Hook Form |
| Angular Material | Headless UI + Tailwind |
| Modules | 目录组织 + 懒加载 |
| Dependency Injection | Hooks + Context |

---

## 十、注意事项

1. **Token 双存储**: 用户和管理员使用不同的 Token key
2. **跨标签页同步**: 监听 storage 事件处理登出同步
3. **管理员实时通知**: 使用轮询或 WebSocket 检测新订单
4. **图片处理**: 支持 URL 和 Base64 两种格式
5. **分页一致性**: 前端分页参数与后端保持一致
6. **错误边界**: 添加 React Error Boundary 处理异常

---

## 十一、交付物

完成后将产出：
- 完整的 React 前端项目
- 与 Spring Boot 后端完全对接
- 功能和 UI 与 Angular 版本保持一致
- 响应式设计，支持移动端
- 完善的类型定义和代码规范
