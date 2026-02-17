# P0执行清单（仅 Playwright MCP）

## 0. 本轮执行状态（2026-02-11）

说明：本状态基于当前仓库一次实际 MCP 执行结果（仅使用 Playwright MCP，无脚本）。

| 案例 | 状态 | 备注 |
| --- | --- | --- |
| G-01 | ✅ 已完成 | 首页精选加载成功；`GET /api/phones?special=*` = 200 |
| G-02 | ✅ 已完成 | 搜索 iPhone 成功；`GET /api/phones?search=iPhone` = 200 |
| G-05 | ✅ 已完成（部分） | 详情与评论接口成功；评论翻页深测未覆盖 |
| A-01 | ✅ 已完成 | 注册成功；`POST /api/auth/register` 成功 |
| A-03 | ⏸ 阻塞 | 缺少可消费验证码通道，未完成验证闭环 |
| A-07 | ⏸ 阻塞 | 仅完成重置请求入口验证，未完成 reset 提交闭环 |
| A-04 | ✅ 已完成 | 登录成功；`POST /api/auth/login` = 200 |
| A-08 | ✅ 已完成 | 未登录访问受保护页重定向到 `login?returnUrl=*` |
| A-09 | ✅ 已完成 | 普通用户访问后台受限（重定向到管理员登录） |
| A-10 | ✅ 已完成 | 退出后访问 `/profile` 会回登录页 |
| U-04 | ✅ 已完成 | 加入购物车成功；`POST /api/cart` = 200 |
| U-05 | ✅ 已完成 | 更新数量成功；`PUT /api/cart/{id}` = 200 |
| U-08 | ✅ 已完成 | 下单成功；`POST /api/orders/checkout` = 201 |
| U-11 | ⏸ 阻塞（部分） | 订单后可见订单数据；库存/销量差量对比未完整闭环 |
| U-12 | ✅ 已完成 | 结算按钮双击仅产生 1 次成功下单请求（POST /api/orders/checkout=201） |
| S-01 | ✅ 已完成 | 卖家上传与发布成功；POST /api/upload/image=200，POST /api/phones=201 |
| M-01 | ❌ 失败 | 管理员登录返回 `401`（`/api/admin/login`） |
| M-03 | ⏸ 阻塞 | 依赖 M-01 成功后执行 |
| M-04 | ⏸ 阻塞 | 依赖 M-03 冻结用户后执行 |
| M-09 | ⏸ 阻塞 | 依赖 M-01 管理后台登录成功 |

证据文件：`e2e-home.png`、`e2e-admin-login-401.png`。

## 1. 范围与原则

- 目标：将 `通用E2E案例矩阵-Playwright-MCP.md` 中全部 `P0` 案例落成可执行发布阻断清单。
- 工具约束：仅使用 Playwright MCP（`browser_snapshot` / `browser_network_requests` / `browser_console_messages` 等）。
- 阻断原则：任一 `P0` 失败、关键证据缺失或出现未豁免严重错误，即阻断发布。

## 2. P0批次总览

| 批次 | 批次名称 | 案例数 | 执行顺序（案例编号） |
| --- | --- | --- | --- |
| Batch-1 | 前置准备 | 6 | G-01 → G-02 → G-05 → A-01 → A-03 → A-07 |
| Batch-2 | 用户主链路 | 9 | A-04 → A-08 → A-09 → A-10 → U-04 → U-05 → U-08 → U-11 → U-12 |
| Batch-3 | 卖家主链路 | 1 | S-01 |
| Batch-4 | 管理员主链路 | 4 | M-01 → M-03 → M-04 → M-09 |
| Batch-5 | 稳定性检查 | 20（复核） | G-01 → G-02 → G-05 → A-01 → A-03 → A-07 → A-04 → A-08 → A-09 → A-10 → U-04 → U-05 → U-08 → U-11 → U-12 → S-01 → M-01 → M-03 → M-04 → M-09 |

## 3. 分批执行细则

### Batch-1 前置准备

- 目标：验证站点基础可用性、商品详情可访问、账号注册/验证/重置能力正常。
- 入口账号：`游客（未登录）`、`新注册买家账号（buyer_new）`。
- 依赖数据：已执行 `/api/e2e/reset`；至少 1 个可访问 `phoneId`；可接收验证码邮箱。
- 执行顺序（案例编号）：`G-01` → `G-02` → `G-05` → `A-01` → `A-03` → `A-07`。
- 通过门槛：6/6 通过；每例均具备 `snapshot + network + console(error级)` 最小证据。
- 失败阻断规则：任一案例失败即阻断；若账号类（`A-01/A-03/A-07`）失败，后续批次停止执行并回滚到数据重置。

### Batch-2 用户主链路

- 目标：验证买家从登录到下单、鉴权边界与下单一致性/幂等全链路。
- 入口账号：`买家账号（buyer_primary）`（已验证）；`普通买家账号（buyer_guard）`（用于权限校验）。
- 依赖数据：至少 1 个可售商品（有库存）；购物车可写；可访问结算页；可查询订单。
- 执行顺序（案例编号）：`A-04` → `A-08` → `A-09` → `A-10` → `U-04` → `U-05` → `U-08` → `U-11` → `U-12`。
- 通过门槛：9/9 通过；`U-08/U-11/U-12` 必须同时满足 UI 成功与网络断言一致。
- 失败阻断规则：任一鉴权失败（`A-08/A-09/A-10`）或任一下单相关失败（`U-08/U-11/U-12`）立即阻断发布。

### Batch-3 卖家主链路

- 目标：验证卖家核心发布能力（上传图片并创建商品）。
- 入口账号：`卖家账号（seller_primary）`。
- 依赖数据：可用图片文件（合法类型与大小）；卖家具备发布权限。
- 执行顺序（案例编号）：`S-01`。
- 通过门槛：1/1 通过；`POST /api/upload/image` 与 `POST /api/phones` 均成功。
- 失败阻断规则：`S-01` 失败即阻断发布（卖家供给链不可用）。

### Batch-4 管理员主链路

- 目标：验证后台登录、用户治理、封禁生效、订单导出能力。
- 入口账号：`管理员账号（admin_primary）`；`被冻结目标账号（buyer_frozen_target）`。
- 依赖数据：后台统计可读取；存在可冻结用户；存在可导出订单数据。
- 执行顺序（案例编号）：`M-01` → `M-03` → `M-04` → `M-09`。
- 通过门槛：4/4 通过；`M-03` 与 `M-04` 需形成闭环证据（冻结动作成功且登录被拒）。
- 失败阻断规则：任一后台鉴权/治理/导出失败即阻断；`M-03` 成功但 `M-04` 未生效按阻断处理。

### Batch-5 稳定性检查

- 目标：对前四批全部 `P0` 执行结果做统一稳定性复核，确认无严重前端错误与异常网络模式。
- 入口账号：复用 `buyer_primary`、`seller_primary`、`admin_primary` 与必要游客态。
- 依赖数据：前四批执行证据已归档；可按原顺序重放或抽检关键节点。
- 执行顺序（案例编号）：按 `Batch-1` → `Batch-4` 已定义顺序复核全部 20 个 `P0`。
- 通过门槛：20/20 案例证据齐全；`console_messages(level=error)` 无新增严重错误堆栈；关键 API 无未预期 `5xx`。
- 失败阻断规则：发现任一新增严重控制台错误、关键请求连续失败、证据链断裂（缺任一最小证据）即阻断发布。

## 4. P0案例最小证据与建议耗时

| 批次 | 案例编号 | 最小证据（每例至少三类） | 建议耗时 |
| --- | --- | --- | --- |
| 前置准备 | G-01 | `snapshot`：`/home`精选区块；`network`：`GET /api/phones?special=*`=200；`console`：error级为空 | 4 分钟 |
| 前置准备 | G-02 | `snapshot`：搜索结果页；`network`：`GET /api/phones?search=iPhone`=200；`console`：error级为空 | 4 分钟 |
| 前置准备 | G-05 | `snapshot`：详情与评论分页；`network`：`GET /api/phones/{id}` 与 `/reviews`=200；`console`：error级为空 | 5 分钟 |
| 前置准备 | A-01 | `snapshot`：注册成功提示；`network`：`POST /api/auth/register`=201；`console`：error级为空 | 6 分钟 |
| 前置准备 | A-03 | `snapshot`：验证成功提示；`network`：`POST /api/auth/verify-email`=200；`console`：error级为空 | 5 分钟 |
| 前置准备 | A-07 | `snapshot`：重置成功提示；`network`：`POST /api/auth/reset-password`=200；`console`：error级为空 | 6 分钟 |
| 用户主链路 | A-04 | `snapshot`：登录后落地页；`network`：`POST /api/auth/login`=200；`console`：error级为空 | 4 分钟 |
| 用户主链路 | A-08 | `snapshot`：跳转到 `/login?returnUrl=*`；`network`：受保护接口401/403；`console`：error级为空 | 3 分钟 |
| 用户主链路 | A-09 | `snapshot`：管理员页拒绝或重定向；`network`：`GET /api/admin/*`=403；`console`：error级为空 | 3 分钟 |
| 用户主链路 | A-10 | `snapshot`：会话失效后回登录；`network`：`GET /api/profile`=401/403；`console`：error级为空 | 4 分钟 |
| 用户主链路 | U-04 | `snapshot`：购物车出现商品；`network`：`POST /api/cart`=200；`console`：error级为空 | 5 分钟 |
| 用户主链路 | U-05 | `snapshot`：数量与金额刷新；`network`：`PUT /api/cart/{phoneId}`=200；`console`：error级为空 | 4 分钟 |
| 用户主链路 | U-08 | `snapshot`：订单成功页；`network`：`POST /api/orders/checkout`=201；`console`：error级为空 | 6 分钟 |
| 用户主链路 | U-11 | `snapshot`：商品页库存/销量变化；`network`：`GET /api/phones/{id}` 与 UI 一致；`console`：error级为空 | 6 分钟 |
| 用户主链路 | U-12 | `snapshot`：仅一次成功下单反馈；`network`：成功结算请求计数=1；`console`：error级为空 | 5 分钟 |
| 卖家主链路 | S-01 | `snapshot`：新商品卡片出现；`network`：`POST /api/upload/image` + `POST /api/phones` 成功；`console`：error级为空 | 8 分钟 |
| 管理员主链路 | M-01 | `snapshot`：后台仪表盘卡片；`network`：`POST /api/admin/login` + `GET /api/admin/stats` 成功；`console`：error级为空 | 5 分钟 |
| 管理员主链路 | M-03 | `snapshot`：用户状态切换；`network`：`PUT /api/admin/users/{userId}/toggle-disabled`=200；`console`：error级为空 | 4 分钟 |
| 管理员主链路 | M-04 | `snapshot`：被冻结用户登录失败提示；`network`：`POST /api/auth/login`=403；`console`：error级为空 | 4 分钟 |
| 管理员主链路 | M-09 | `snapshot`：导出触发反馈；`network`：`GET /api/admin/orders/export?format=*`=200；`console`：error级为空 | 5 分钟 |

## 5. 执行与出结论规则

- 执行模式：严格按批次顺序串行执行，不跨批并发。
- 重试策略：单案例最多重试 1 次；若两次失败，直接按阻断处理。
- 发布结论：仅当 5 个批次全部达到通过门槛，方可给出 “P0 放行”。
