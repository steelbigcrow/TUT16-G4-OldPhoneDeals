# React + Spring Boot 端到端测试指南（仅 Playwright MCP）

## 1. 目标与约束

本指南用于在本仓库中，使用**唯一测试工具：Playwright MCP**，对 `react-frontend`（React）与 `spring-old-phone-deals`（Spring Boot）进行端到端测试设计与执行。

### 1.1 强约束（必须遵守）

- 仅允许使用 Playwright MCP 能力进行测试执行与取证。
- 不使用其他测试工具（如 Cypress、Selenium、Postman、JMeter、k6 等）。
- 端到端测试必须覆盖“页面行为 + 后端接口联动”两个维度。

### 1.2 本文使用的 Playwright MCP 操作

- `browser_navigate`
- `browser_click`
- `browser_type`
- `browser_fill_form`
- `browser_file_upload`
- `browser_wait_for`
- `browser_snapshot`
- `browser_network_requests`
- `browser_console_messages`
- `browser_run_code`

---

## 2. 与当前项目对齐的测试基线

### 2.1 React 前端关键路由

- 公共：`/home`、`/search`、`/phone/:id`、`/login`、`/register`、`/verify-email`、`/reset-password`
- 用户受保护：`/wishlist`、`/checkout`、`/profile`、`/profile/settings`、`/profile/listings`、`/profile/reviews`
- 管理员：`/admin/login`、`/admin/dashboard`、`/admin/users`、`/admin/phones`、`/admin/reviews`、`/admin/orders`、`/admin/logs`

### 2.2 Spring Boot 关键 API（与前端联动）

- 认证：`/api/auth/*`
- 商品与评论：`/api/phones*`
- 购物车：`/api/cart*`
- 订单：`/api/orders*`
- 用户资料：`/api/profile*`
- 收藏夹：`/api/wishlist*`
- 上传：`/api/upload/image`
- 管理后台：`/api/admin/*`
- E2E 数据重置：`/api/e2e/reset`（仅在 `APP_E2E_ENABLED=true` 时可用）

### 2.3 内置 E2E 测试账号（来自后端重置端点）

调用 `POST /api/e2e/reset` 后可获得固定账号：

- 买家：`e2e-buyer@example.com`
- 卖家：`e2e-seller@example.com`
- 管理员：`e2e-admin@example.com`
- 默认密码：`Password123!`

---

## 3. 环境准备与启动顺序

### 3.1 推荐环境变量（后端）

- `APP_E2E_ENABLED=true`（启用 `/api/e2e/reset`）
- `FRONTEND_URL=http://localhost:5173`
- `JWT_SECRET=<your-secret>`
- `SPRING_PROFILES_ACTIVE=dev`

### 3.2 启动顺序（必须固定）

1. 启动后端：在 `spring-old-phone-deals` 目录运行 `mvn spring-boot:run`。
2. 启动前端：在 `react-frontend` 目录运行 `npm run dev`。
3. 用 Playwright MCP 打开前端页面后，执行数据重置。

### 3.3 仅用 Playwright MCP 执行数据重置

建议操作：

1. `browser_navigate` 到 `http://localhost:5173/home`
2. `browser_run_code` 执行 `fetch('/api/e2e/reset', { method: 'POST' })`
3. `browser_run_code` 校验响应包含 buyer/seller/admin 账号数据
4. `browser_network_requests` 确认 `POST /api/e2e/reset` 返回 `200`

---

## 4. 全通用案例覆盖框架

以下类别是 React + Spring Boot 电商应用的“通用 E2E 全覆盖基线”，本项目应全部覆盖：

1. 公共浏览（首页、搜索、详情、404）
2. 注册/登录/邮箱验证/重置密码
3. 鉴权与角色权限（user/admin）
4. 受保护路由重定向与会话失效
5. 商品查询（搜索/筛选/排序/分页）
6. 收藏夹（增删、去重）
7. 购物车（增改删、库存边界）
8. 结算与订单（成功、失败、重复提交）
9. 评论（发布、可见性、分页）
10. 卖家商品管理（创建、上传、上下架、删除）
11. 管理员用户管理
12. 管理员商品管理
13. 管理员评论管理
14. 管理员订单管理与导出（CSV/JSON）
15. 管理员日志分页
16. 错误与鲁棒性（401/403/404/500、重试、控制台错误）
17. 前后端数据一致性（UI 与 API 返回一致）

---

## 5. Playwright MCP 执行模板（可直接复用）

### 5.1 登录与鉴权模板

1. `browser_navigate` 到 `/login`
2. `browser_fill_form` 填写邮箱/密码
3. `browser_click` 提交
4. `browser_wait_for` 等待首页或受保护页出现
5. `browser_run_code` 检查 localStorage 中用户 token
6. `browser_network_requests` 断言 `POST /api/auth/login` 状态码

### 5.2 受保护路由模板

1. 清理 token（`browser_run_code`）
2. `browser_navigate` 到 `/wishlist` 或 `/profile`
3. `browser_wait_for` 等待跳转到 `/login?returnUrl=...`
4. `browser_snapshot` 留存重定向证据
5. `browser_network_requests` 校验 `/api/auth/me` 或业务请求返回 `401/403`

### 5.3 搜索/筛选/分页模板

1. `browser_navigate` 到 `/search`
2. `browser_type` 输入关键字并提交
3. `browser_click` 选择品牌/价格/排序
4. `browser_click` 切换分页
5. `browser_network_requests` 校验 `GET /api/phones` 参数变化
6. `browser_snapshot` 校验结果区、页码与总数

### 5.4 收藏夹与购物车模板

1. 登录后进入 `/search` 或 `/phone/:id`
2. `browser_click` 加入收藏或购物车
3. `browser_navigate` 到 `/wishlist` 或 `/checkout`
4. `browser_click` 修改数量/移除项目
5. `browser_network_requests` 校验 `/api/wishlist`、`/api/cart` 请求链路
6. `browser_snapshot` 校验 UI 状态变化

### 5.5 结算与订单模板

1. `browser_navigate` 到 `/checkout`
2. `browser_fill_form` 填收货地址
3. `browser_click` 提交订单
4. `browser_wait_for` 等待成功提示
5. `browser_network_requests` 校验 `POST /api/orders/checkout` 为 `201`
6. `browser_run_code` 提取订单号并用于后续查询

### 5.6 上传与卖家发帖模板

1. 登录卖家账号进入 `/profile/listings`
2. `browser_file_upload` 上传图片
3. `browser_fill_form` 填写标题/品牌/库存/价格
4. `browser_click` 创建商品
5. `browser_network_requests` 校验 `POST /api/upload/image` 与 `POST /api/phones`
6. `browser_snapshot` 校验卡片渲染与状态标签

### 5.7 管理员治理模板

1. 管理员登录后进入 `/admin/*`
2. `browser_type` + `browser_click` 做筛选搜索
3. `browser_click` 执行冻结用户、上下架商品、切换评论可见性等操作
4. `browser_network_requests` 校验 `/api/admin/*` 请求与状态码
5. `browser_snapshot` 校验列表状态变更

### 5.8 错误处理模板

1. 构造非法输入或无权限请求
2. `browser_click` 触发请求
3. `browser_wait_for` 等待错误提示
4. `browser_console_messages` 检查是否有未处理异常
5. `browser_network_requests` 校验 `4xx/5xx` 与错误消息
6. `browser_snapshot` 留存错误态界面

---

## 6. 分层执行策略

### 6.1 Smoke（冒烟，P0）

最小闭环：

- 首页加载
- 用户登录
- 搜索结果出现
- 加购物车并可进入结算页
- 管理员可登录并进入仪表盘

### 6.2 Main Flow（主流程，P0/P1）

- 注册/验证/登录
- 购物车到下单
- 卖家发布与管理商品
- 评论与可见性
- 管理员用户/商品/评论/订单主操作

### 6.3 Regression（回归，P0/P1/P2）

- 覆盖文档中的完整通用案例矩阵
- 重点复跑历史缺陷场景（会话失效、重复提交、导出失败等）

---

## 7. 断言与证据标准

每条 E2E 用例至少产出三类证据：

1. UI 证据：`browser_snapshot`
2. 网络证据：`browser_network_requests`
3. 前端运行证据：`browser_console_messages`

建议每条用例记录：

- 用例编号
- 前置数据（尤其是调用 `/api/e2e/reset` 的时间）
- 操作序列（MCP 命令级）
- 关键断言（UI + Network）
- 结果（Pass/Fail）
- 缺陷链接（如有）

---

## 8. 常见问题与修复建议

### 8.1 用例偶发失败

- 先检查 `browser_wait_for` 条件是否过弱
- 再检查 `browser_network_requests` 是否出现请求重试/超时
- 最后检查测试数据是否未重置

### 8.2 401/403 频发

- 检查 token 是否写入正确（user/admin）
- 检查前端是否触发了自动清 token 与登录重定向
- 检查请求路径是否命中错误角色 API

### 8.3 上传/导出不稳定

- 上传关注文件类型和大小边界
- 导出关注 `Content-Disposition` 与下载文件名
- 保留 `browser_network_requests` 作为定位依据

---

## 9. 与案例矩阵的关系

本指南给出方法与模板；具体可执行清单见：

- `playwright MCP Document/通用E2E案例矩阵-Playwright-MCP.md`

该矩阵已覆盖 React 前端与 Spring Boot 后端联动的全部通用类别，并严格约束为“只使用 Playwright MCP”。

