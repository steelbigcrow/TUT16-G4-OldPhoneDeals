# React Frontend Plan - React + TypeScript + Tailwind CSS

## 目标

为 OldPhoneDeals 增加一套 **React 纯前端 SPA（Vite）**，对接本仓库已存在的 **Spring Boot REST API**（位于 [`spring-old-phone-deals/`](spring-old-phone-deals/README.md:1)）。

**仓库目录命名约定（已定稿）**：
- React 前端工程目录为 [`react-frontend/`](react-frontend/:1)（由历史目录 `node-frontend/` 真实改名而来）。
- 该前端是 **SPA（Vite 构建）**：**非 SSR / 非 Node 服务端**。目录名 `react-frontend` 仅表示“前端工程”，不代表后端技术栈。

---

## 一、技术栈选择（最佳实践 + 可执行）

| 类别 | 技术选型 | 说明 |
|------|----------|------|
| 框架 | React 18+ | 组件化开发 |
| 语言 | TypeScript | 与后端 DTO/响应契约对齐 |
| 样式 | Tailwind CSS | 快速开发与一致性 |
| 路由 | React Router v6 | 嵌套路由 + 守卫（element + Outlet） |
| 服务端状态 | Axios + TanStack Query | **所有服务端状态统一交给 Query** |
| UI 状态 | Toast/Modal 等 UI Context（可选） | Context 仅保留纯 UI，不承担业务/服务端状态 |
| 表单 | React Hook Form + Zod | 表单与验证 |
| 构建 | Vite | dev proxy 与快速构建 |

---

## 二、目录结构规划（react-frontend）

> 下面是可执行的推荐结构；允许小幅调整，但“服务端状态由 TanStack Query 管理”的原则不可变。

```text
react-frontend/
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   │
│   ├── types/
│   │   ├── api.ts                  # ApiResponse / Page / 例外响应结构
│   │   ├── user.ts
│   │   ├── phone.ts
│   │   ├── order.ts
│   │   └── review.ts
│   │
│   ├── api/
│   │   ├── client.ts               # axios 实例 + token 策略 + 错误拦截
│   │   ├── auth.ts
│   │   ├── phones.ts
│   │   ├── orders.ts
│   │   ├── profile.ts
│   │   ├── wishlist.ts
│   │   └── admin.ts
│   │
│   ├── hooks/
│   │   ├── useAuth.ts              # /api/auth/me
│   │   ├── useAdminAuth.ts         # /api/admin/profile
│   │   └── ...                     # 其他 Query hooks（phones/cart/orders/admin...）
│   │
│   ├── contexts/                   # 仅 UI Context（可选）
│   │   └── NotificationContext.tsx
│   │
│   ├── components/
│   │   ├── layout/
│   │   ├── ui/
│   │   └── common/
│   │       ├── ProtectedRoute.tsx  # v6 守卫（含 loading 决策）
│   │       └── ErrorBoundary.tsx
│   │
│   ├── pages/
│   │   ├── auth/
│   │   ├── user/
│   │   ├── profile/
│   │   └── admin/
│   │
│   └── styles/
│       └── index.css
│
├── public/
├── index.html
├── vite.config.ts
└── .env.example
```

---

## 三、Axios 统一路径规则（消除 /api/api 风险：唯一方案）

**唯一允许方案（定稿）**：

1) `axios.baseURL` 固定为 `'/api'`  
2) 所有 `config.url` **必须**使用以下形式之一（必须以 `/` 开头，且**禁止**再写 `/api` 前缀）：
- 用户侧：`'/auth/...'`、`'/phones/...'`、`'/orders/...'`、`'/profile/...'`、`'/wishlist/...'`、`'/upload/...'`
- 管理侧：`'/admin/...'`

**禁止**：
- `baseURL='/api'` 同时 `url='/api/auth/login'`（会得到 `/api/api/auth/login`）
- `baseURL=''` 同时 `url='/api/auth/login'`（策略不统一，容易被“admin 前缀判断”误判）

---

## 四、双 token 策略（严谨且不依赖 baseURL 字符串拼接）

### 4.1 Token Key（定稿）

- 用户：`localStorage['user_auth_token']`
- 管理员：`localStorage['admin_auth_token']`

两者允许并存，互不覆盖。

### 4.2 选择哪个 token（必须避免 baseURL 造成的 prefix 判断失效）

**强制约束**：由于已经统一 `baseURL='/api'`，请求的“最终路径”会变成 `/api/...`，但 axios 拦截器里拿到的 `config.url` 可能是 `'/admin/...'`（推荐）或 `'admin/...'`（不推荐）。

因此要求你在 `react-frontend/src/api/client.ts` 中实现“路径归一化”策略（此处是规范，不是改代码）：

- 输入：`config.url`（必须存在）
- 输出：`normalizedPath`（保证以 `/` 开头，不含查询串）
- 归一化规则：
  - 若 `config.url` 不以 `/` 开头，补上 `/`
  - 去掉 querystring（`?a=b`）与 hash（`#...`）
  - 不要基于 `baseURL` 做 `startsWith('/api/admin')` 这类判断（会让逻辑依赖拼接细节）

**最终判定**（唯一规则）：
- `isAdminRequest = normalizedPath.startsWith('/admin')`
- `isAdminRequest === true` → 使用 `admin_auth_token`
- `isAdminRequest === false` → 使用 `user_auth_token`

> 该规则与第三节的“统一 url 书写”配套：管理端只能以 `'/admin/...'` 开头，天然不会被 `/api` 前缀影响。

### 4.3 401/403 清理规则 + returnUrl（定稿）

**后端错误响应统一来源**：全局异常处理会返回 `ApiResponse.error(message)`（见 [`GlobalExceptionHandler.java`](spring-old-phone-deals/src/main/java/com/oldphonedeals/exception/GlobalExceptionHandler.java:39) 与 [`ApiResponse.java`](spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/response/ApiResponse.java:21)）。

前端响应拦截器收到 `401/403` 时必须按 `normalizedPath` 区分清理哪个 token，并跳转到对应登录页：

- `normalizedPath.startsWith('/admin')`：
  - 清理 `admin_auth_token`
  - 跳转 `/admin/login?returnUrl=<encodeURIComponent(currentPath+search)>`
- 其他路径：
  - 清理 `user_auth_token`
  - 跳转 `/login?returnUrl=...`

**returnUrl 防循环**：
- 若当前已在 `/login` 或 `/admin/login`，不要再追加 returnUrl 指向登录页自身（避免循环重定向）。
- 建议：过滤 `returnUrl`，若 `decodedReturnUrl` 以 `/login` 或 `/admin/login` 开头，则丢弃，回退默认页。

---

## 五、后端响应体 / 分页结构（必须以 Spring Boot 真实实现为准）

### 5.1 统一响应：ApiResponse<T>

后端统一包装类为 [`ApiResponse.java`](spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/response/ApiResponse.java:21)：

```ts
// react-frontend/src/types/api.ts（规范示意）
export type ApiResponse<T> = {
  success: boolean;
  message?: string;
  data?: T;
};
```

- 成功：`success=true`，可能包含 `message`、`data`
- 失败：`success=false`，一定有 `message`（来自异常 handler）

### 5.2 全局异常：message 组织方式（前端必须直接展示 message）

- 验证错误：`"Validation failed: field: msg; field2: msg2"`（见 [`GlobalExceptionHandler.java`](spring-old-phone-deals/src/main/java/com/oldphonedeals/exception/GlobalExceptionHandler.java:180)）
- 其他异常：`ApiResponse.error(ex.getMessage())`（见 [`GlobalExceptionHandler.java`](spring-old-phone-deals/src/main/java/com/oldphonedeals/exception/GlobalExceptionHandler.java:47)）

前端解析错误时：
- 优先取 `response.data.message`
- 兜底：取 `error.message` 或统一提示“网络错误”

### 5.3 管理端分页：PageResponse<T>（响应页码 1 基）

管理员大量列表接口返回 `ApiResponse<PageResponse<T>>`，分页包装为 [`PageResponse.java`](spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/response/PageResponse.java:26)：

- `currentPage`：**1 基**（由 `page.getNumber() + 1` 转换，见 [`PageResponse.java`](spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/response/PageResponse.java:71)）
- `itemsPerPage`：pageSize
- `totalPages` / `totalItems` / `hasNext` / `hasPrevious`

### 5.4 用户订单分页：OrderPageResponse（items + pagination）

用户订单分页 DTO 为 [`OrderPageResponse.java`](spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/response/order/OrderPageResponse.java:17)：

```ts
export type OrderPageResponse<Order> = {
  items: Order[];
  pagination: {
    currentPage: number; // 1-based
    pageSize: number;
    totalPages: number;
    totalItems: number;
  };
};
```

### 5.5 手机列表 GET /api/phones：Map 结构（例外：data 是 Map，不是 PageResponse）

`GET /api/phones` 在 Controller 内返回 `ApiResponse.success(responseMap, ...)`（见 [`PhoneController.getAllPhones()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/PhoneController.java:206)），其中 `responseMap` 的 keys 在服务层明确为（见 [`PhoneServiceImpl.getPhones()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/service/impl/PhoneServiceImpl.java:282)）：

- `phones`: `PhoneListItemResponse[]`
- `currentPage`: number（1 基）
- `totalPages`: number
- `total`: number（总条目数）

因此前端类型建议：

```ts
export type PhonesListData<TPhone> = {
  phones: TPhone[];
  currentPage: number; // 1-based
  totalPages: number;
  total: number;
};
```

### 5.6 评论分页：ReviewPageResponse

评论分页响应为 [`ReviewPageResponse.java`](spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/response/phone/ReviewPageResponse.java:17)：

- `reviews`: ReviewResponse[]
- `totalReviews`: number
- `currentPage`: 1 基
- `totalPages`: number

### 5.7 例外响应：管理员查看单机评论不走 ApiResponse<T>

管理员“按手机查看评论”接口返回 `PhoneReviewListResponse`（不在 `ApiResponse<T>` wrapper 内，见 [`AdminController.getPhoneReviews()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/AdminController.java:283)）。

该接口前端必须单独建类型与解析逻辑，不能复用 `ApiResponse<T>`。

---

## 六、分页：请求 0 基 vs 响应 1 基（写清转换规则）

后端存在两类分页“请求 page”口径：

### 6.1 管理端列表：请求 page 多为 0 基，但响应 currentPage 永远 1 基

例如（请求 0 基）：
- `GET /api/admin/users?page=0&pageSize=10`（见 [`AdminController.getAllUsers()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/AdminController.java:96)）
- `GET /api/admin/phones?page=0&pageSize=10`（见 [`AdminController.getAllPhones()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/AdminController.java:208)）
- `GET /api/admin/reviews?page=0&pageSize=10`（见 [`AdminController.getAllReviews()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/AdminController.java:263)）
- `GET /api/admin/orders?page=0&pageSize=10`（见 [`AdminController.getAllOrders()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/AdminController.java:337)）
- `GET /api/admin/logs?page=0&pageSize=10`（见 [`AdminController.getAllLogs()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/AdminController.java:417)）

**前端变量命名建议（避免混淆）**：
- 管理端请求用：`pageIndex`（0-based）
- 响应展示用：`currentPage`（1-based，来自后端）

转换公式：
- 请求：`pageIndex` 直接发给后端 `page`
- UI 展示：用后端 `currentPage`（无需再 +1）
- 若 UI 组件需要 0 基：`pageIndexFromResponse = currentPage - 1`

### 6.2 管理端“按用户查看 phones/reviews”：请求 page 是 1 基（controller 内会 -1）

例如：
- `GET /api/admin/users/{userId}/phones?page=1&limit=10`（controller 内 `page - 1`，见 [`AdminController.getUserPhones()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/AdminController.java:164)）
- `GET /api/admin/users/{userId}/reviews?page=1&limit=10`（同理，见 [`AdminController.getUserReviews()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/AdminController.java:184)）

因此这两类接口前端建议用：
- `pageNumber`（1-based）用于请求参数 `page`
- 响应依旧用 `PageResponse.currentPage`（1-based）

### 6.3 用户侧 phones/reviews/orders：请求 page 多为 1 基

- `GET /api/phones?page=1&limit=12`（服务层会 `page - 1`，见 [`PhoneServiceImpl.getPhones()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/service/impl/PhoneServiceImpl.java:282)）
- `GET /api/phones/{phoneId}/reviews?page=1&limit=10`（分页响应 `ReviewPageResponse.currentPage` 为 1 基，见 [`ReviewPageResponse.java`](spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/response/phone/ReviewPageResponse.java:17)）
- `GET /api/orders?page=1&pageSize=10`（响应 `OrderPageResponse.pagination.currentPage` 1 基，见 [`OrderPageResponse.java`](spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/response/order/OrderPageResponse.java:37)）

---

## 七、图片上传：只保存 URL（以 data.fileUrl 为准）+ Vite 代理 /uploads

### 7.1 上传接口与返回

上传端点：
- `POST /api/upload/image`（multipart/form-data，字段名 `file`，需要登录，见 [`FileUploadController.uploadImage()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/FileUploadController.java:83)）

后端返回 `ApiResponse<FileUploadResponse>`，前端只关心：
- `data.fileUrl`（示例为 `/uploads/images/<uuid>.<ext>`，见 [`FileUploadController.java`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/FileUploadController.java:25)）

**前端表单字段仅保存 URL 字符串**：
- phone.image: string（值必须来自 `data.fileUrl`）
- 不保存 Base64，不保存本地文件对象

### 7.2 Vite dev 代理（必须补齐 /uploads）

开发环境为确保 `<img src="/uploads/...">` 可访问，建议配置代理（示例）：

```ts
// react-frontend/vite.config.ts（规范示意）
export default {
  server: {
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/uploads': { target: 'http://localhost:8080', changeOrigin: true }
    }
  }
};
```

### 7.3 后端存储路径“images/images”配置歧义（仅文档说明，不改后端）

当前默认上传目录为 `file.upload.dir = "./uploads/images"`（见 [`FileStorageProperties.java`](spring-old-phone-deals/src/main/java/com/oldphonedeals/config/FileStorageProperties.java:20)），而上传时 `subDirectory = "images"`（见 [`FileUploadController.uploadImage()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/FileUploadController.java:83)），存储实现会拼接为 `Paths.get(dir, subDirectory)`（见 [`FileStorageServiceImpl.storeFile()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/service/impl/FileStorageServiceImpl.java:48)）。

这会导致实际落盘目录可能变成 `./uploads/images/images/...`。  
前端不应猜测文件系统路径，必须以 `data.fileUrl` 为准。

---

## 八、创建商品 seller 字段必填：前端必须从 /api/auth/me 取值

后端创建商品请求 DTO 中 `seller` 字段为必填（`@NotBlank`，见 [`PhoneCreateRequest.seller`](spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/request/phone/PhoneCreateRequest.java:19)）。

而 Controller 创建商品时会从 JWT 取当前用户 ID（见 [`PhoneController.createPhone()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/PhoneController.java:54)），并在服务层验证卖家存在（见 [`PhoneServiceImpl.createPhone()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/service/impl/PhoneServiceImpl.java:76)）。

**因此前端创建商品必须：**
1) 先用 `GET /api/auth/me` 获取当前用户信息（用于拿到当前用户 id）
2) 提交 `POST /api/phones` 时：
   - `seller = me.id`（或等价 userId 字段；以 `/api/auth/me` 返回字段为准）
   - 其余字段按 DTO 填写
3) 不允许提交空 seller（否则会触发 `MethodArgumentNotValidException`，返回 400 + `Validation failed: ...`，见 [`GlobalExceptionHandler.handleValidationException()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/exception/GlobalExceptionHandler.java:180)）

---

## 九、管理端更新商品：不支持改图片（必须在 UI 上写清）

管理员更新商品请求 DTO 为 [`UpdatePhoneRequest`](spring-old-phone-deals/src/main/java/com/oldphonedeals/dto/request/admin/UpdatePhoneRequest.java:1)，字段仅包含：
- `title`
- `brand`
- `price`
- `stock`
- `isDisabled`

管理员更新接口为 `PUT /api/admin/phones/{phoneId}`（见 [`AdminController.updatePhone()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/controller/AdminController.java:221)）。

**结论（定稿）**：
- 管理端商品编辑 UI **不得提供图片上传/图片 URL 修改**能力（后端不支持该字段）。
- 若需要改图片，只能走“用户侧卖家更新商品”（`PUT /api/phones/{phoneId}`）的流程（该接口支持 image 更新，见 [`PhoneServiceImpl.updatePhone()`](spring-old-phone-deals/src/main/java/com/oldphonedeals/service/impl/PhoneServiceImpl.java:116)）。

---

## 十、路由与守卫：React Router v6（避免闪烁/多次重定向）

### 10.1 路由清单（建议）

- `/` → `/home`
- `/home` / `/search` / `/phone/:id`
- `/login` / `/register` / `/verify-email` / `/reset-password`
- `/admin/login`
- 用户受保护：`/checkout` `/wishlist` `/profile/...`
- 管理受保护：`/admin/*`（除 `/admin/login`）
- `*` → 404 页面（必须落地）

### 10.2 ProtectedRoute 必须是“loading 决策”而不是“只看 token”

仅检查 localStorage token 会导致：
- 页面首次加载时闪烁（先渲染受保护页→再重定向）
- token 过期时出现多次重定向

**定稿策略**（用户与管理员各一套）：

- 第一步：读 token（快速判断是否“可能已登录”）
- 第二步：若存在 token，必须发起 `me/profile` 查询（TanStack Query）：
  - 用户：`GET /api/auth/me`
  - 管理：`GET /api/admin/profile`
- 在 Query `isLoading`/`isFetching` 时，ProtectedRoute 必须渲染 Loading（不要 Navigate）
- Query 成功：放行
- Query 失败（401/403 或其他错误）：
  - 401/403：按第四节清 token + 跳登录页（带 returnUrl）
  - 非 401/403：建议展示“系统错误页/重试”而不是强制登出（避免把服务器故障当未登录）

---

## 十一、状态管理（唯一原则：Axios + TanStack Query 管理服务端状态）

### 11.1 原则（必须统一）

- **服务端状态**（me/profile、phones、reviews、cart、orders、admin lists...）：
  - 全部通过 TanStack Query：`useQuery` / `useMutation` + `invalidateQueries`
- **禁止**：
  - 用 Context 当 Auth/Cart 的唯一真相
  - 在多个地方重复缓存同一份服务端数据
- Context（若使用）只保留纯 UI：toast、modal、主题等

### 11.2 推荐 Query Keys（最小可执行集合）

- 用户：`['auth', 'me']`（`GET /api/auth/me`）
- 管理：`['admin', 'profile']`（`GET /api/admin/profile`）
- phones 列表：`['phones', { search, brand, page, limit, sortBy, sortOrder, special, maxPrice }]`
- phone 详情：`['phones', 'detail', phoneId]`
- reviews：`['reviews', phoneId, { page, limit }]`
- wishlist：`['wishlist']`
- orders：`['orders', { page, pageSize }]`
- admin users：`['admin', 'users', { pageIndex, pageSize, search, isDisabled }]`
- admin phones：`['admin', 'phones', { pageIndex, pageSize }]`
- admin reviews：`['admin', 'reviews', { pageIndex, pageSize, ...filters }]`

---

## 十二、开发阶段规划（简版）

1) 初始化 `react-frontend/`（Vite + React + TS + Tailwind）
2) 落地 Axios client（第三/四节规则）
3) 落地 types（第五节规则：ApiResponse / PageResponse / Map 结构 / 例外响应）
4) 落地 Router + ProtectedRoute（第十节 loading 决策）
5) 用户模块（phones/reviews/cart/orders/profile/wishlist）
6) 管理模块（users/phones/reviews/orders/logs）
7) 统一错误处理与 UX 打磨（Toast + ErrorBoundary + retry）

---

## 十三、注意事项（最终约束清单）

1) **路径规则唯一**：`baseURL='/api'` 且 url 仅用 `'/auth' '/phones' '/admin' ...`（见本文件第三节）
2) **双 token 严谨**：按 `normalizedPath.startsWith('/admin')` 判定 token（见本文件第四节）
3) **分页不可混用**：管理端 pageIndex(0) / 响应 currentPage(1) 分离（见本文件第六节）
4) **上传只存 URL**：只使用 `data.fileUrl`，并代理 `/uploads`（见本文件第七节）
5) **seller 必填**：创建 phone 必须从 `/api/auth/me` 填入 seller（见本文件第八节）
6) **管理端不支持改图**：UpdatePhoneRequest 无 image（见本文件第九节）
7) **守卫必须 loading 决策**：避免闪烁与重定向风暴（见本文件第十节）
8) **服务端状态只用 Query**：Context 仅 UI（见本文件第十一节）