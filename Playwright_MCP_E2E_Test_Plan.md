# Playwright MCP 端到端（E2E）测试文档（React 前端 + Spring Boot 后端）

文档版本：v1.0  
适用代码基线：`react-frontend/` + `spring-old-phone-deals/`  
更新日期：2026-01-30  

> 提示（Windows/PowerShell）：本文档为 **UTF-8** 编码。如命令行查看出现乱码，请使用 `Get-Content -Encoding UTF8 Playwright_MCP_E2E_Test_Plan.md` 或用支持 UTF-8 的编辑器打开。

> 目标：基于本仓库的 **React（Vite + TS）前端** 与 **Spring Boot（MongoDB + JWT）后端**，给出一套可落地、可扩展、能用于回归/冒烟/全量的 **Playwright MCP E2E 测试完整文档**。  
> 本文档既覆盖 **“自动化端到端测试”**（推荐最终落地为 Playwright Test Runner 代码），也覆盖 **“MCP 驱动的交互式端到端测试”**（通过 Playwright MCP 由 Agent/LLM 执行、复现、验收）。

---

## 0. 术语与约定

- **SUT（System Under Test）**：本次测试的系统对象：React 前端 + Spring Boot API + MongoDB + 静态资源（uploads/images）+ 邮件系统（可选）。
- **E2E（端到端）**：从浏览器 UI 出发，完整走通真实后端，覆盖路由、鉴权、数据读写、错误处理。
- **MCP（Model Context Protocol）**：通过 Playwright MCP 暴露浏览器自动化能力，支持：
  - 用于探索性测试（Exploratory）、回归验收（Acceptance）、Bug 复现脚本化（Repro Script）。
  - 或作为自动化测试的辅助工具（例如录制步骤、定位元素、生成稳定选择器策略）。
- **统一返回体**：后端返回 `ApiResponse<T> = { success: boolean; message?: string; data?: T }`。
- **端口约定（默认）**：
  - React（Vite dev）：`http://localhost:5173`
  - Spring Boot：`http://localhost:8080`
  - React 通过 Vite Proxy 访问 `/api` → 转发到 `8080`（避免 CORS）
- **token 存储（前端 localStorage）**：
  - 用户：`user_auth_token`
  - 管理员：`admin_auth_token`

---

## 1. 测试目标与范围

### 1.1 测试目标（必须达成）

1) 覆盖所有关键用户角色与主流程：  
   - 游客（Guest）
   - 已登录普通用户（Buyer）
   - 卖家（Seller，发布/管理 listing）
   - 管理员（Admin，后台管理用户/商品/评论/订单/日志）
2) 覆盖关键异常路径：校验失败、401/403、库存不足、商品禁用、网络错误、分页越界、上传非法文件等。  
3) 覆盖 “前后端契约是否一致”：UI 能正确展示 `success/message/data` 并在错误时给出可理解提示。  
4) 测试结果可追溯：失败时能产出截图/console/network/后端日志定位问题。

### 1.2 测试范围（按路由）

React 路由（`react-frontend/src/router.tsx`）：

- 公共：
  - `/home`
  - `/search`
  - `/phone/:id`
  - `/login`
  - `/register`
  - `/verify-email`
  - `/reset-password`
  - `/admin/login`
  - `*`（404）
- 用户受保护（需要 `user_auth_token` + `/api/auth/me` 成功）：
  - `/checkout`
  - `/wishlist`
  - `/profile`（子路由：`/profile/settings` `/profile/listings` `/profile/reviews`）
- 管理员受保护（需要 `admin_auth_token` + `/api/admin/profile` 成功）：
  - `/admin`（子路由：`/admin/dashboard` `/admin/users` `/admin/phones` `/admin/reviews` `/admin/orders` `/admin/logs`）

### 1.3 测试范围（按后端 API）

核心 API（详见 `兼容情况.md`）：

- Auth：`/api/auth/*`
- Profile：`/api/profile/*`
- Phones：`/api/phones/*`
- Reviews：`/api/phones/:id/reviews*`
- Wishlist：`/api/wishlist*`
- Cart：`/api/cart*`
- Orders：`/api/orders*`
- Upload：`/api/upload/image`
- Admin：`/api/admin/*`
- E2E 测试专用（仅当 `APP_E2E_ENABLED=true`）：`/api/e2e/reset`

---

## 2. Playwright MCP 测试方法论

### 2.1 为什么要用 Playwright MCP（相对“纯人工测试”）

- 可以把“人工点击步骤”规范化为 **可重复执行** 的测试步骤。
- 可以在功能迭代后快速跑一遍“主流程回归”。
- 可以用浏览器自动化捕获：
  - console errors / network failures
  - 401/403 自动重定向是否正确
  - 页面 loading/empty/error 状态是否完整
  - 下载（CSV）、上传（图片）等高摩擦交互

### 2.2 MCP 执行测试的通用步骤模板（强烈建议）

> MCP 模式下通常做不到“凭空写选择器直接点”，正确方式是：**先 snapshot → 再对 ref 操作**。

1) `browser_navigate` 到目标 URL  
2) `browser_wait_for` 等待关键文字出现（避免盲点等待）  
3) `browser_snapshot` 获取可访问性树（A11y Snapshot）  
4) 在 snapshot 中定位目标元素（按 role/name/label/placeholder 等）  
5) `browser_click` / `browser_type` / `browser_fill_form`  
6) 再次 `browser_snapshot` 或 `browser_wait_for(text)` 做断言  
7) 出错时：
   - `browser_console_messages(level=error)` 获取 console 错误
   - `browser_network_requests(includeStatic=false)` 查看关键 API 是否 4xx/5xx
   - `browser_take_screenshot(fullPage=true)` 留证据

### 2.3 选择器/定位策略（避免脆弱用例）

优先级（从强到弱）：

1) **label + input**：例如 `label: Email`、`id='login_email'` 等（本项目多数表单都提供 label）。  
2) **按钮文本 + role=button**：例如 `Log in / Create account / Apply / Place order`。  
3) **表格列名 + 行内容**：Admin 表格类页面更稳定。  
4) 必要时才引入 `data-testid`（若后续需要在 CI 稳定运行，建议对关键按钮/卡片/弹窗加 testid）。

禁止/避免：

- 依赖 Tailwind class、DOM 层级 nth-child、随机生成的 id。
- `waitForTimeout(…)`（除非用于调试，且不能进入主分支）。

### 2.4 断言策略（E2E 推荐断言）

- URL 断言：是否跳转到预期路由（尤其是 returnUrl 逻辑）。
- 文本断言：标题、empty state、error toast、成功提示、关键字段展示。
- 网络断言：关键 API 返回 200 且 `success=true`；错误时返回 4xx/5xx 且 message 可读。
- 状态断言：localStorage token 是否存在/被清除；列表是否刷新；库存是否变化。

---

## 3. E2E 测试环境与启动方式

### 3.1 最小可用（P0）环境

- MongoDB：本地或容器均可
- Spring Boot：`spring-old-phone-deals`（dev profile）
- React：`react-frontend`（Vite dev server）
- （可选但推荐）邮件沙箱：MailHog 或 Mailtrap（用于完整覆盖“注册/验证/重置密码”）

### 3.2 推荐的 E2E 专用环境变量（隔离数据）

**后端（Spring Boot）建议：**

- `SPRING_PROFILES_ACTIVE=dev`
- `APP_E2E_ENABLED=true`（允许调用 `/api/e2e/reset` 重置数据）
- `MONGODB_URI_DEV=mongodb://localhost:27017/oldphonedeals-e2e`（建议独立库）
- `UPLOAD_DIR=./e2e-uploads`（避免污染本地 dev-uploads）
- 邮件（两种方案二选一）：
  - 方案 A（MailHog 本地 SMTP）：`MAIL_HOST=localhost`, `MAIL_PORT=1025`
  - 方案 B（Mailtrap）：按团队的 Mailtrap 凭证设置 `MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD`

**前端（React）建议：**

- 使用默认 Vite proxy（`react-frontend/vite.config.ts`），无需配置 baseURL
- 需要稳定端口时，可固定 `vite --port 5173`

### 3.3 启动命令（示例）

Windows PowerShell（从仓库根目录）：

```powershell
# 1) 启动 Spring Boot（启用 E2E reset）
cd spring-old-phone-deals
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:APP_E2E_ENABLED = "true"
$env:MONGODB_URI_DEV = "mongodb://localhost:27017/oldphonedeals-e2e"
$env:UPLOAD_DIR = ".\\e2e-uploads"
# 可选邮件：MailHog
$env:MAIL_HOST = "localhost"
$env:MAIL_PORT = "1025"
mvn spring-boot:run
```

另开一个终端启动 React：

```powershell
cd react-frontend
npm install
npm run dev
```

（可选）验证后端 e2e reset 是否可用：

```powershell
curl -Method POST http://localhost:8080/api/e2e/reset
```

---

## 4. 测试数据策略（强制执行，避免用例互相污染）

### 4.1 统一“重置数据”入口（后端已提供）

后端存在测试专用端点（仅当 `APP_E2E_ENABLED=true`）：

- `POST /api/e2e/reset`
  - 清空：orders / cart / phones / users
  - 创建：buyer、seller、以及 1 个 seed phone + seed reviews

**已知 seed 账号（来自 `E2eTestController`）**：

- Buyer：`e2e-buyer@example.com` / `Password123!`（已验证）
- Seller：`e2e-seller@example.com` / `Password123!`（已验证）

**已知 seed 商品：**

- 标题：`E2E Apple iPhone 15 Pro`
- `brand=APPLE`
- `stock=3`
- `salesCount=42`
- 带有 seed reviews（注意：seed reviews 的存在会影响“新增评论”类用例，见后文说明）

> 约定：除非用例明确声明“不重置”，否则每个测试用例/每组测试前必须调用一次 `/api/e2e/reset`。
>
> 注意：`/api/e2e/reset` **只重置 MongoDB 数据**，不会自动清理 `UPLOAD_DIR` 下的上传文件（`/uploads/images/*`）。若 E2E 用例包含上传（如 H01/H02），建议使用独立的 `UPLOAD_DIR=./e2e-uploads` 并在回归/夜跑前清理目录，避免磁盘污染与用例互相影响。  
> 示例（PowerShell，在 `spring-old-phone-deals/` 目录下执行）：
>
> ```powershell
> Remove-Item -Recurse -Force .\\e2e-uploads\\images\\* -ErrorAction SilentlyContinue
> ```

### 4.2 关于 Admin 用例的数据准备（当前代码的现实约束）

`/api/e2e/reset` **不会创建管理员用户**，而 Admin UI 需要 `role="ADMIN"` 才能通过 `/api/admin/login`。

因此 Admin E2E 有两种可落地方案：

**方案 A（推荐，简单且不改代码）：把 seller 提升为 ADMIN（仅限 e2e DB）**

1) 调用 `/api/e2e/reset`  
2) 通过 Mongo 将 `e2e-seller@example.com` 的 role 改为 `ADMIN`（必要时同时 set `isAdmin=true`）  
3) 用该账号登录 `/admin/login` 获取 `admin_auth_token`

示例（mongosh，替换你的 DB 名称）：

```js
db.users.updateOne(
  { email: "e2e-seller@example.com" },
  { $set: { role: "ADMIN", isAdmin: true } }
)
```

**方案 B（更理想，需小改后端）：扩展 `/api/e2e/reset` 直接种 admin**

> 如果团队希望“Admin E2E 全自动且不依赖 DB 操作”，建议新增一个 e2e-only seed admin（不进入生产）。  
> 建议把该项作为后续改造任务：在 `APP_E2E_ENABLED=true` 时，让 reset 同时创建一个 `isVerified=true` 的管理员账号（例如 `e2e-admin@example.com`），从而让 Smoke/CI 能够无人工介入地覆盖 K 类用例。

### 4.3 关于“邮箱验证/密码重置”类用例的数据准备

真实后端会发送邮件（dev profile 默认使用 Mailtrap；也可改为 MailHog）。若要 **完全覆盖** 邮件相关 E2E，需要：

- 可观测邮件内容（能拿到 verify token / reset code）
- 或提供 test-only endpoint 查询 token/code（更快，但需要后端改造）

推荐路径（不改后端）：

- 使用 MailHog（SMTP + Web UI + HTTP API）
  - SMTP：`localhost:1025`
  - Web UI：`http://localhost:8025`
  - 测试中通过 MailHog API 拉取最新邮件并解析 token/code

---

## 5. 测试分层与执行策略（建议）

### 5.1 测试套件（Suite）定义

- **Smoke（冒烟，P0）**：10~20 条，5 分钟内跑完，覆盖登录/搜索/加购/下单/后台登录。  
- **Regression（回归，P0+P1）**：覆盖所有核心模块的正向 + 典型异常分支。  
- **Full（全量，P0+P1+P2）**：包含大量边界/安全/并发/兼容性场景，可夜间跑。

**Smoke 推荐用例清单（可直接执行）**：

- **Smoke-Min（最小门禁，推荐 CI）**：`A01` → `A02` → `B01` → `E02` → `G01` → `G04` → `K01` → `K02`  
  - 执行建议：在 Smoke 开始前先跑一次 `P-RESET`；若包含 K02（Admin 登录），先执行 `P-PROMOTE-SELLER-TO-ADMIN`（或已实现“方案 B”自动种 Admin）。
- **Smoke-Plus（扩展门禁，推荐 release 前）**：在 Smoke-Min 基础上追加：`F02`（Wishlist）/ `H01`（上传 + 新建 listing）/ `I05`（Seller 评论可见性）/ `K04`（Admin Users 过滤/分页）/ `N01`（console error 门禁）。

> 提示：若采用“方案 A”将 `e2e-seller@example.com` 提升为 ADMIN，该账号仍然可以作为普通用户登录 `/login` 获取 `user_auth_token`（用于 Seller/Buyer 用例）；同时也可用 `/admin/login` 获取 `admin_auth_token`（用于 K 类用例）。两类 token 在前端按请求路径自动区分，不冲突。

### 5.2 浏览器/视口矩阵（建议）

- 浏览器：
  - Chromium（必须）
  - Firefox（建议）
  - WebKit（建议）
- 视口：
  - Desktop：1280×720（默认）
  - Mobile：390×844（iPhone 12/13 常用）

---

## 6. 覆盖矩阵（功能 × 角色 × 状态）

> 用于检查“是否漏测”。每个功能至少覆盖：Happy path、Unauthorized/Forbidden、空态/异常态。

| 模块 | Guest | Buyer(登录) | Seller(登录) | Admin(登录) | 空态/异常态 | 权限(401/403) |
|---|---|---|---|---|---|---|
| Home | ✅ | ✅ | ✅ | N/A | ✅ | N/A |
| Search/List | ✅ | ✅ | ✅ | N/A | ✅ | N/A |
| Phone Detail | ✅ | ✅ | ✅ | N/A | ✅(404/禁用) | ✅(写操作) |
| Wishlist | ❌（应跳登录） | ✅ | ✅ | N/A | ✅ | ✅ |
| Cart/Checkout | ❌（应跳登录） | ✅ | ✅ | N/A | ✅(空车/库存不足) | ✅ |
| Orders（Profile） | ❌ | ✅ | ✅ | N/A | ✅ | ✅ |
| Profile Settings | ❌ | ✅ | ✅ | N/A | ✅(校验失败) | ✅ |
| Seller Listings | ❌ | ✅（可访问但视为卖家功能） | ✅ | N/A | ✅ | ✅ |
| Seller Reviews | ❌ | ✅ | ✅ | N/A | ✅ | ✅ |
| Admin Dashboard | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Admin Users | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Admin Phones | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Admin Reviews | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Admin Orders/Export | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Admin Logs | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |

---

## 7. 详细测试用例（按模块完全覆盖）

> 说明：  
> - 每条用例都以 **“前置条件 → 操作步骤 → 期望结果”** 描述。  
> - 若需要 MCP 执行，用例步骤应当映射为：navigate → snapshot → click/fill → wait_for → snapshot/assert。  
> - 约定：除“数据/邮件/并发”类用例外，默认每条用例执行前都调用一次 `/api/e2e/reset`。

### 7.1 通用前置步骤（可复用脚本）

**P-RESET：重置数据**

1) `POST http://localhost:8080/api/e2e/reset`  
2) 期望：返回 `success=true` 且 `data.buyer/data.seller/data.phones[0]` 存在

**P-LOGIN-BUYER：登录为 Buyer**

1) 访问 `/login`  
2) Email 输入 `e2e-buyer@example.com`  
3) Password 输入 `Password123!`  
4) 点击 `Log in`  
5) 期望：toast `Logged in`，跳转到 `/home` 或 returnUrl

**P-LOGIN-SELLER：登录为 Seller**

同上，账号改为 `e2e-seller@example.com`

**P-PROMOTE-SELLER-TO-ADMIN：将 Seller 提升为 Admin（仅 e2e DB）**

1) 执行 P-RESET  
2) 在 Mongo 将 `e2e-seller@example.com` 的 `role` 改为 `ADMIN`  
3) 期望：`POST /api/admin/login` 可成功

**P-LOGIN-ADMIN：登录为 Admin（基于上一步）**

1) 访问 `/admin/login`  
2) Email=`e2e-seller@example.com`，Password=`Password123!`  
3) 点击 `Log in`  
4) 期望：toast `Admin logged in`，跳转 `/admin/dashboard`

---

### 7.2 App 启动 & 路由（A 类）

- **A01（P0）首页可访问**  
  - 前置：后端/前端已启动  
  - 步骤：访问 `/home`  
  - 期望：页面包含 `Home` 相关标题/内容，无 console error

- **A01-1（P1）Home - Best sellers / Sold out soon 区块可加载**  
  - 前置：P-RESET（确保至少存在 1 个 phone）  
  - 步骤：访问 `/home`  
  - 期望：
    - 页面出现区块标题 `Best sellers` 与 `Sold out soon`  
    - 两个区块的卡片列表能渲染（至少包含 seed phone 或为空态但不报错）  
  - 额外断言（可选）：抓取 network，确认发出
    - `GET /api/phones?special=bestSellers`
    - `GET /api/phones?special=soldOutSoon`

- **A02（P0）Search 页面可访问**  
  - 步骤：访问 `/search`  
  - 期望：出现 `Search` 标题；加载完成后出现 Total 与分页信息或 `No results.`

- **A03（P0）Phone detail 404**  
  - 前置：P-RESET  
  - 步骤：访问 `/phone/not-a-real-id`  
  - 期望：显示错误区域（红色框）或 message 表明 not found

- **A04（P0）未知路由 404 页面**  
  - 步骤：访问 `/some/unknown/path`  
  - 期望：显示 `NotFoundPage` 内容（可断言 `Not Found` 或 404 文案）

---

### 7.3 用户认证（B/C 类）

- **B01（P0）登录成功（已验证用户）**  
  - 前置：P-RESET  
  - 步骤：执行 P-LOGIN-BUYER  
  - 期望：localStorage 出现 `user_auth_token`；访问 `/profile` 成功进入（不跳转）

- **B02（P0）登录失败（错误密码）**  
  - 前置：P-RESET  
  - 步骤：`/login` 输入正确 email + 错误 password → `Log in`  
  - 期望：toast/错误提示为后端 message（`Invalid email or password`）

- **B03（P1）访问受保护页面会跳转登录并携带 returnUrl**  
  - 前置：P-RESET，确保未登录（清空 localStorage）  
  - 步骤：直接访问 `/wishlist`  
  - 期望：跳转到 `/login?returnUrl=%2Fwishlist`  
  - 再步骤：完成登录  
  - 再期望：登录后跳回 `/wishlist`

- **B04（P1）token 无效/过期 → 自动清 token + 跳转登录**  
  - 前置：P-RESET；先登录成功；然后将 localStorage 的 `user_auth_token` 改成无效字符串  
  - 步骤：访问 `/profile`（会触发 `/api/profile` 或 `/api/auth/me`）  
  - 期望：被重定向到 `/login?returnUrl=...`；原 token 被清除

- **C01（P1）注册成功 → 跳转 verify-email（email 预填）**  
  - 前置：可用邮件沙箱（MailHog/Mailtrap），确保 backend mail 配置可用  
  - 步骤：访问 `/register` 填写合法信息 → `Create account`  
  - 期望：toast `Registered. Please verify your email.`；跳到 `/verify-email?email=...` 且 Email 输入框预填

- **C02（P1）注册失败：重复邮箱**  
  - 前置：P-RESET（buyer 已存在）  
  - 步骤：用 `e2e-buyer@example.com` 再次注册  
  - 期望：提示 `Account with that email already exists`

- **C03（P2）verify-email 输入非法 token**  
  - 前置：已有未验证用户（通常来自注册）  
  - 步骤：在 `/verify-email` 输入 email + token=`wrong` → Verify  
  - 期望：错误提示 `Invalid or expired verification token`

> 说明：若要 **完全自动化** 覆盖 verify-email，需要在测试中从邮件系统中抓取 token。参见第 8 章“邮件类用例落地方案”。

---

### 7.4 搜索/列表/详情（D/E 类）

- **E01（P0）Search 默认列表加载成功**  
  - 前置：P-RESET  
  - 步骤：访问 `/search`  
  - 期望：Total > 0 或至少包含 seed phone；页面无错误

- **E02（P0）Search 关键词过滤**  
  - 前置：P-RESET  
  - 步骤：Search 输入 `iPhone` → Apply  
  - 期望：结果包含 `E2E Apple iPhone 15 Pro`

- **E03（P1）Brand filter 生效**  
  - 前置：P-RESET  
  - 步骤：Brand 选择 `APPLE` → Apply  
  - 期望：结果品牌都为 APPLE（至少 seed phone 仍在）

- **E04（P1）maxPrice filter 生效**  
  - 前置：P-RESET  
  - 步骤：Max price 填 `10` → Apply  
  - 期望：显示 `No results.`

- **E05（P1）Sort 生效（price asc/desc）**  
  - 前置：P-RESET（最好准备多条 phone；可由 seller 创建更多 listing）  
  - 步骤：Sort 选择 `Price: low to high`  
  - 期望：列表价格单调不降（抽查前 3 条即可）

- **E06（P1）分页边界：第一页 Previous 禁用**  
  - 前置：P-RESET  
  - 步骤：访问 `/search?page=1`  
  - 期望：Previous button disabled

- **E07（P0）进入详情页展示字段完整**  
  - 前置：P-RESET  
  - 步骤：在 `/search` 点击 seed phone → 进入 `/phone/:id`  
  - 期望：出现 `Phone detail` 标题；展示 title/brand/price/stock/rating；图片 `img` 存在且 `src` 非空（不强制外链 100% 可加载，避免把外网稳定性引入 E2E）

---

### 7.5 Wishlist（F 类）

- **F01（P0）未登录点击 Wishlist → 跳登录并回跳**  
  - 前置：P-RESET，未登录  
  - 步骤：`/search` 点击某个卡片的 `Wishlist`  
  - 期望：提示 `Please log in to use wishlist`，并跳到 `/login?returnUrl=...`；登录后回到 search

- **F02（P0）加入 Wishlist 成功（已登录）**  
  - 前置：P-RESET + P-LOGIN-BUYER  
  - 步骤：`/phone/:id` 点击 `Wishlist`  
  - 期望：toast `Added to wishlist`；进入 `/wishlist` 能看到该 phone

- **F03（P1）重复加入 Wishlist → 后端拒绝并提示**  
  - 前置：P-RESET + P-LOGIN-BUYER；先加入一次  
  - 步骤：再次点击 `Wishlist`  
  - 期望：toast 显示 `Phone already in wishlist`

- **F04（P0）Wishlist 移除成功**  
  - 前置：P-RESET + P-LOGIN-BUYER；Wishlist 中已有商品  
  - 步骤：进入 `/wishlist` 点击 `Remove`  
  - 期望：toast `Removed from wishlist`；列表不再包含该项/或 empty state

- **F05（P1）禁用商品不应出现在 Wishlist 列表**  
  - 前置：P-RESET；Buyer 加入 Wishlist；Seller/管理员将该 phone 禁用  
  - 步骤：刷新 `/wishlist`  
  - 期望：该 phone 不显示（后端会过滤 isDisabled）

---

### 7.6 Cart & Checkout（G 类）

- **G01（P0）加入购物车成功（从 Search）**  
  - 前置：P-RESET + P-LOGIN-BUYER  
  - 步骤：`/search` 点击 `Add to cart`  
  - 期望：toast `Added to cart`；进入 `/checkout` 能看到该商品行

- **G02（P1）在 Checkout 修改数量成功**  
  - 前置：P-RESET + P-LOGIN-BUYER；购物车有 seed phone  
  - 步骤：`/checkout` 将 Qty 改为 2  
  - 期望：不报错；刷新页面后数量仍为 2（服务端持久化）

- **G03（P1）库存不足：数量 > stock → 显示错误**  
  - 前置：P-RESET + P-LOGIN-BUYER；seed phone stock=3  
  - 步骤：`/checkout` 将 Qty 改为 999  
  - 期望：toast/错误提示包含 `Insufficient stock`

- **G04（P0）Checkout 成功下单并清空购物车**  
  - 前置：P-RESET + P-LOGIN-BUYER；购物车至少 1 项  
  - 步骤：填写 Shipping address（Street/City/State/Zip/Country）→ `Place order`  
  - 期望：toast `Checkout successful`；出现绿色 `Order placed` 区块与 `Order ID`；购物车清空（刷新后 empty）

- **G05（P1）空购物车不可下单**  
  - 前置：P-RESET + P-LOGIN-BUYER  
  - 步骤：进入 `/checkout`（应为空）  
  - 期望：`Place order` disabled；如强行触发请求，后端返回 `Cart is empty`

- **G06（P2）并发抢购：最后库存竞争**  
  - 前置：P-RESET，将某 phone stock 改为 1（可通过管理员编辑或 DB 直接修改）  
  - 步骤：
    1) Buyer A 与 Buyer B（两个浏览器上下文/两个隐身窗口）分别登录：A 用 `e2e-buyer@example.com`，B 可用 `e2e-seller@example.com`（作为第二个买家账号即可）  
    2) 两边同时把该 phone 加入 cart Qty=1  
    3) 同时点击 `Place order`  
  - 期望：一个成功、另一个失败（`Insufficient stock ...`）；最终库存为 0；订单数=1

---

### 7.7 Seller：发布与管理 Listing（H 类）

- **H01（P0）Seller 创建 listing（含上传）成功**  
  - 前置：P-RESET + P-LOGIN-SELLER  
  - 步骤：
    1) 进入 `/profile/listings`  
    2) Upload image 选择合法图片（jpg/png/gif，<10MB）  
    3) 填 Title/Brand/Stock/Price → `Create listing`  
  - 期望：toast `Image uploaded` + `Listing created`；新 listing 出现在 `Your listings`

- **H02（P1）上传非法类型文件应失败**  
  - 前置：P-RESET + P-LOGIN-SELLER  
  - 步骤：Upload image 选择 `.pdf` 或 `.txt`  
  - 期望：toast/错误提示包含 `File extension ... is not allowed` 或 `File type ... is not allowed`

- **H03（P1）Stock=0 的 listing 可创建但不可被成功购买（库存不足）**  
  - 前置：P-RESET + P-LOGIN-SELLER；创建 stock=0 的 listing  
  - 步骤：Buyer 尝试加入 cart 并 checkout  
  - 期望：加入 cart 或 checkout 失败（`Insufficient stock`）

- **H04（P0）Seller Toggle disabled 后商品从 Search 消失**  
  - 前置：P-RESET；保证存在可见 phone（seed phone 或新建 phone）  
  - 步骤：
    1) Seller 登录 `/profile/listings`  
    2) 对某 listing 点击 `Toggle disabled` 使其 Disabled  
    3) 作为 Guest 打开 `/search` 查找该 phone  
  - 期望：该 phone 不出现在列表中

- **H05（P1）Seller 删除 listing 后从 Wishlist/Cart 中移除**  
  - 前置：P-RESET；Buyer 将某 phone 加入 wishlist 与 cart；Seller 删除该 phone  
  - 步骤：Buyer 刷新 `/wishlist` 和 `/checkout`  
  - 期望：该 phone 不再存在于 wishlist/cart；详情页访问应报 not found

---

### 7.8 Reviews（I 类）

- **I01（P0）Phone detail 显示评论列表与分页**  
  - 前置：P-RESET（seed phone 有 reviews）  
  - 步骤：进入 `/phone/:id`  
  - 期望：出现 `Reviews` 区块；分页展示 `Page 1 / N`；Previous 禁用

- **I02（P1）新增评论成功（需选择“无既有评论的 phone + 非 seller 用户”）**  
  - 前置：
    1) P-RESET  
    2) Seller 创建一个新 listing（无评论）  
    3) Buyer 登录并进入该新 listing 的详情页  
  - 步骤：填写 rating/comment → Submit  
  - 期望：toast `Review submitted`；评论出现在列表中

- **I03（P1）重复评论应失败**  
  - 前置：I02 已提交过一次  
  - 步骤：再次提交  
  - 期望：提示 `You have already reviewed this phone`

- **I04（P1）Seller 不能评论自己的商品**  
  - 前置：P-RESET + P-LOGIN-SELLER  
  - 步骤：进入自己商品详情页 → Submit review  
  - 期望：提示 `You cannot review your own phone`

- **I05（P0）Seller 在 Profile/Reviews 切换评论可见性**  
  - 前置：P-RESET + P-LOGIN-SELLER（seed phone 有 reviews）  
  - 步骤：进入 `/profile/reviews` → 对任意行点击 `Toggle visibility`  
  - 期望：toast 显示 `Review is now hidden/visible`；状态列变化

- **I06（P1）隐藏评论对游客不可见**  
  - 前置：完成 I05 把某 review 设为 Hidden  
  - 步骤：以 Guest 打开该 phone detail 的 reviews 列表  
  - 期望：Hidden 评论不显示；Seller 自己仍能在 `/profile/reviews` 看到

---

### 7.9 Profile Settings（J 类，用户资料）

- **J01（P0）Profile 首页展示账户信息**  
  - 前置：P-RESET + P-LOGIN-BUYER  
  - 步骤：访问 `/profile`  
  - 期望：显示 name/email/email verified/created 等字段

- **J02（P1）仅修改名字成功（不改 email，不要求 currentPassword）**  
  - 前置：P-RESET + P-LOGIN-BUYER  
  - 步骤：进入 `/profile/settings` 改 First/Last name → `Save changes`  
  - 期望：toast `Profile updated`；返回 profile 页面看到新名字

- **J03（P1）修改 email 必须输入 currentPassword**  
  - 前置：P-RESET + P-LOGIN-BUYER  
  - 步骤：将 Email 改为新地址，但 currentPassword 留空 → Save  
  - 期望：提示 `Current password is required to change email`

- **J04（P1）修改密码成功**  
  - 前置：P-RESET + P-LOGIN-BUYER  
  - 步骤：在 Change password 区域输入 currentPassword + newPassword(>=6) → Change password  
  - 期望：toast `Password changed`；之后用新密码能登录

---

### 7.10 Admin（K 类，后台管理全覆盖）

> Admin 用例默认使用“方案 A：提升 seller 为 ADMIN”。

- **K01（P0）未登录访问 /admin/dashboard → 跳 /admin/login**  
  - 前置：清空 localStorage  
  - 步骤：访问 `/admin/dashboard`  
  - 期望：跳转到 `/admin/login?returnUrl=%2Fadmin%2Fdashboard`

- **K02（P0）Admin 登录成功**  
  - 前置：P-PROMOTE-SELLER-TO-ADMIN  
  - 步骤：执行 P-LOGIN-ADMIN  
  - 期望：进入 dashboard，页面无 401/403 重定向

- **K03（P1）非管理员账号尝试 admin login → Forbidden**  
  - 前置：P-RESET  
  - 步骤：用 buyer 在 `/admin/login` 登录  
  - 期望：提示 `Access denied. Admin privileges required.`

- **K04（P0）Admin Users：搜索 + 状态过滤 + 分页**  
  - 前置：P-PROMOTE-SELLER-TO-ADMIN + P-LOGIN-ADMIN  
  - 步骤：
    1) 进入 `/admin/users`  
    2) Search 输入 `e2e-buyer` → Apply  
    3) Disabled filter 切换 Active/Disabled  
    4) Next/Previous 分页  
  - 期望：过滤生效；无用户时显示 `No users found.`

- **K05（P0）Admin 禁用用户后，该用户无法正常登录**  
  - 前置：P-PROMOTE-SELLER-TO-ADMIN + P-LOGIN-ADMIN  
  - 步骤：
    1) `/admin/users` 找到 buyer → `Toggle disabled`（使其 Disabled）  
    2) 清除用户 token → 去 `/login` 用 buyer 登录  
  - 期望：登录失败提示 `Account has been disabled...`（或同类 message）  
  - 再步骤：管理员再 toggle 回 Active → 用户可登录

- **K06（P0）Admin Phones：禁用商品后 Search 不可见**  
  - 前置：P-PROMOTE-SELLER-TO-ADMIN + P-LOGIN-ADMIN  
  - 步骤：`/admin/phones` 对 seed phone 点击 `Toggle disabled` → 变 Disabled  
  - 期望：Guest 在 `/search` 找不到该 phone；Buyer 也无法加入 wishlist/cart（后端会拒绝）

- **K07（P1）Admin Phones：编辑商品字段并保存**  
  - 前置：同上  
  - 步骤：点击 `Edit` → 修改 Title/Price/Stock/Disabled → Save  
  - 期望：列表字段更新；在 `/phone/:id` 可见更新后的 title/price/stock

- **K08（P1）Admin Reviews：可见性切换影响前台展示**  
  - 前置：seed phone 有 review；P-PROMOTE-SELLER-TO-ADMIN + P-LOGIN-ADMIN  
  - 步骤：
    1) `/admin/reviews` 找到某条 review → `Toggle visibility`  
    2) 以 Guest 访问 `/phone/:id` 的 Reviews 列表  
  - 期望：Hidden review 不展示；Visible 时展示

- **K09（P1）Admin Orders：筛选/排序/分页 + 导出 CSV**  
  - 前置：
    1) P-RESET + Buyer 下单一次（G04）  
    2) P-PROMOTE-SELLER-TO-ADMIN + P-LOGIN-ADMIN  
  - 步骤：
    1) `/admin/orders` 看到订单  
    2) 尝试 Search/Brand/Start date/End date/Sort 组合过滤（至少覆盖 2~3 种组合）  
    3) 点击 `Export CSV` 并校验导出成功（见下方“校验方式”）  
    4) 点击 `Export JSON` 并校验导出成功（见下方“校验方式”）  
  - 期望：
    - 列表能显示订单、金额、时间、用户  
    - 下载成功且文件名正确（前端固定为 `orders_export.csv` / `orders_export.json`）

  **校验方式（按落地形态二选一）**：

  - **Playwright Test Runner（推荐）**：使用下载事件断言文件名为 `orders_export.csv` / `orders_export.json`，并读取文件内容（非空、且 CSV/JSON 结构合理）。
  - **MCP 交互式（不方便落盘读文件）**：抓 network / 直接 fetch 校验返回：
    - network 中应出现 `GET /api/admin/orders/export?format=csv...` 与 `GET /api/admin/orders/export?format=json...`
    - status=200，且响应体 size > 0
    - `Content-Type` 应分别为 `text/csv` 与 `application/json`  

  > 注意：后端 `Content-Disposition` 的文件名可能是 `orders.csv` / `orders.json`（服务端真实文件名），但前端通过 `download` 属性覆盖为 `orders_export.*`（浏览器最终下载名）。校验时请选择“UI 下载名”或“网络响应头”其中一种作为标准，避免出现“看起来不一致”的误报。

- **K10（P1）Admin Logs：操作后能看到日志记录**  
  - 前置：完成 K05/K06/K07 任意管理操作  
  - 步骤：进入 `/admin/logs`  
  - 期望：出现对应操作的日志条目（时间、操作类型、目标等字段）

- **K11（P1）Admin Reviews：删除评论**  
  - 前置：seed phone 有 review；P-PROMOTE-SELLER-TO-ADMIN + P-LOGIN-ADMIN  
  - 步骤：`/admin/reviews` 找到任意一条 review → 点击 `Delete` 并确认  
  - 期望：toast `Review deleted`；列表刷新后该 review 不再出现；前台详情页不再显示该评论

- **K12（P1）Admin Phones：删除商品**  
  - 前置：P-PROMOTE-SELLER-TO-ADMIN + P-LOGIN-ADMIN（可使用 seed phone）  
  - 步骤：`/admin/phones` 对某 phone 点击 `Delete` 并确认  
  - 期望：
    - toast `Phone deleted`（或同类提示）  
    - `/search` 与 `/phone/:id` 不再能访问到该 phone  
    - 若该 phone 曾存在于 cart/wishlist，则对应列表中不再包含该项

---

### 7.11 非功能性/质量保障（N 类：可用性/安全/兼容/性能）

> 这类用例通常不改变业务数据，适合挂在 nightly 或 release 前跑。

- **N01（P1）Console error 门禁（关键页面不应产生 error）**  
  - 前置：P-RESET  
  - 步骤：依次访问 `/home` `/search` `/phone/:id` `/login` `/register`  
  - 期望：console `error` 为空（允许少量第三方 warning，但不允许脚本错误）

- **N02（P2）基本可访问性（A11y）冒烟**  
  - 前置：P-RESET  
  - 步骤：
    1) 打开 `/login`，用 snapshot 检查 Email/Password 都能通过 label 定位  
    2) 打开 `/checkout`，检查 address 表单每个字段都有 label  
  - 期望：表单字段可通过 label/role 被识别（避免“自动化不可测/用户不可用”）

- **N03（P2）响应式布局：移动端主要流程可用**  
  - 前置：P-RESET + P-LOGIN-BUYER  
  - 步骤：设置 viewport=390×844，完成一次 `/search` → `/phone/:id` → `Add to cart` → `/checkout`  
  - 期望：按钮可点击、表单可填写、不会出现关键元素不可见/无法滚动

- **N04（P2）XSS 基础防护：评论内容应被当作纯文本渲染**  
  - 前置：P-RESET；Seller 创建新 phone；Buyer 对该 phone 添加评论，comment 包含 `<script>alert(1)</script>`  
  - 步骤：打开详情页查看评论区  
  - 期望：页面不弹窗；评论以文本形式显示（React 默认转义应满足）

- **N05（P2）上传安全：路径穿越文件名应被拒绝**  
  - 前置：P-RESET + P-LOGIN-SELLER  
  - 步骤：尝试上传一个“文件名包含 .. ”的文件（或模拟请求）  
  - 期望：后端返回 `File name contains invalid path sequence`（或同类 message）


---

## 8. 邮件类用例落地方案（完全覆盖必读）

### 8.1 目标

要把以下用例纳入 “可自动化 E2E”：

- 注册后邮件验证（verify token）
- 忘记密码：请求 reset code（6 位数字）
- 通过 reset code 重置密码

### 8.2 方案一：MailHog（推荐，低侵入）

1) 启动 MailHog（任意方式，示例为 Docker）：

```bash
docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

2) Spring Boot dev 环境设置：

- `MAIL_HOST=localhost`
- `MAIL_PORT=1025`

3) E2E 测试步骤：

- 执行注册/请求重置
- 通过 MailHog HTTP API 拉取最新邮件正文（**推荐在浏览器外执行**，避免页面内 `fetch('http://localhost:8025/...')` 可能遇到 CORS/混合内容限制）
- 解析 verifyUrl 或 resetCode
- 回填到 `/verify-email` 或 `/reset-password` 页面并提交

> MailHog API 常用入口：`GET http://localhost:8025/api/v2/messages`（拉取邮件列表）。实际解析字段结构可能随版本略有差异；建议在本地先用 `curl`/`Invoke-RestMethod` 看一次返回，再固化解析逻辑到测试 helper 中。

### 8.3 方案二：测试专用 endpoint（更快，但需要后端改造）

建议只在 `APP_E2E_ENABLED=true` 时暴露：

- `GET /api/e2e/user/{email}/verify-token`
- `GET /api/e2e/user/{email}/reset-code`

优点：E2E 不依赖外部邮件系统；更稳定、速度更快。  
缺点：需要后端额外代码与安全防护（仅 e2e 环境启用）。

---

## 9. 稳定性与质量门禁（防止 E2E 变“脆弱负担”）

### 9.1 稳定性规则（必须）

- 所有用例使用 `/api/e2e/reset` 做数据隔离（或等价机制）。
- 用 `wait_for(text)` 等待关键 UI，而不是 sleep。
- 对下载/上传类测试：
  - 上传：必须测试非法类型/过大文件（验证后端校验）
  - 下载：必须验证文件名/类型/非空内容
- 对 401/403：必须验证 token 被清除且跳转到正确 login 页。

### 9.2 失败证据（必须）

每条失败用例至少保留：

- fullPage screenshot
- console errors
- network requests（只保留 API 类即可）
- 后端日志（本项目 `spring-old-phone-deals/logs/` 已输出文件）

---

## 10. 缺陷报告模板（建议）

- 标题：`[E2E][模块] 用例ID - 现象简述`
- 环境：OS/浏览器版本/前端 commit/后端 commit/Mongo DB 名称
- 前置数据：是否执行 `/api/e2e/reset`；是否提升 admin
- 复现步骤：粘贴用例步骤
- 实际结果：截图 + console/network 摘要
- 期望结果：应展示的 UI/消息/状态
- 初步定位：
  - 前端：页面/组件/接口文件
  - 后端：controller/service/repository

---

## 11. 附录：与实现强绑定的“关键点清单”

### 11.1 关键路由与鉴权机制

- 用户受保护：`/checkout /wishlist /profile/*` 依赖 `/api/auth/me`
- 管理员受保护：`/admin/*` 依赖 `/api/admin/profile`
- Axios 拦截器遇到 401/403 会：
  - 清掉对应 token（按请求路径判断 user/admin）
  - `window.location.assign()` 跳到登录页，并附带 returnUrl

### 11.2 文件上传约束（后端校验）

- 允许扩展名：`jpg/jpeg/png/gif`
- MIME：`image/jpeg`, `image/png`, `image/gif`
- 最大：10MB
- 端点：`POST /api/upload/image`（multipart `file` 字段，需认证）

### 11.3 订单/库存约束（后端逻辑）

- cart 为空不能 checkout：`Cart is empty`
- phone 禁用不能 checkout：`Phone ... is not available`
- 数量超过库存：`Insufficient stock ...`
- checkout 成功会：
  - 创建订单
  - 扣减库存 + 增加 salesCount
  - 清空购物车

---

## 12. 推荐的落地目录结构（如后续要写自动化代码）

> 本节不要求立即实现，但作为把“测试文档 → 自动化代码”落地的建议结构。

```
react-frontend/
  e2e/
    fixtures/
      sample-image.jpg
    helpers/
      auth.ts
      e2eReset.ts
      mailhog.ts
    specs/
      smoke.spec.ts
      auth.spec.ts
      search.spec.ts
      checkout.spec.ts
      seller.spec.ts
      admin.spec.ts
```

---

## 13. 附录：Playwright MCP 指令速查（把“文档步骤”落到 MCP 操作）

> 说明：不同客户端对 MCP 的封装可能略有差异；本仓库在 Codex/Playwright MCP 场景下常见能力如下：  
> **核心思路**：`navigate → snapshot → 通过 ref 操作 → wait_for/snapshot 断言 → 失败采证`。

### 13.1 常用指令（按使用频率）

- `browser_navigate(url)`：打开页面（例如 `http://localhost:5173/home`）。
- `browser_snapshot()`：获取可访问性树（用于定位元素 ref）。
- `browser_click(ref, element=描述)`：点击元素。
- `browser_type(ref, text, submit?)`：输入文本（可选回车提交）。
- `browser_fill_form(fields=[...])`：批量填写表单（适合登录/地址表单）。
- `browser_wait_for(text=...)`：等待页面出现关键文字（替代 sleep）。
- `browser_press_key(key)`：键盘操作（Esc、Enter、ArrowDown 等）。
- `browser_take_screenshot(fullPage=true)`：截图留证。
- `browser_console_messages(level="error")`：抓 console error。
- `browser_network_requests(includeStatic=false)`：抓 API 请求（看 4xx/5xx）。
- `browser_evaluate(function="() => ...")`：执行 JS（清 localStorage、打点、触发 fetch）。
- `browser_tabs(action="new|select|close")`：多标签管理。
- `browser_close()`：结束会话。

### 13.2 MCP 执行“重置数据”的两种方式

**方式 A：浏览器外（推荐，最稳定）**

- 直接在测试框架或命令行调用：`POST http://localhost:8080/api/e2e/reset`

**方式 B：浏览器内（通过 evaluate 触发 fetch）**

> 适用于“只用 MCP、不方便跑 curl”的场景。

```js
await fetch('/api/e2e/reset', { method: 'POST' })
```

### 13.3 MCP 执行“免 UI 登录”的技巧（加速回归）

> 目的：多数用例不需要重复测登录 UI，可直接拿 token 写入 localStorage。  
> 注意：仍应保留少量“真实登录 UI”用例（B01/K02）作为冒烟门禁。

**用户 token（写入 `user_auth_token`）**

```js
const res = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'e2e-buyer@example.com', password: 'Password123!' }),
})
const json = await res.json()
localStorage.setItem('user_auth_token', json?.data?.token ?? '')
location.reload()
```

**管理员 token（写入 `admin_auth_token`）**

```js
const res = await fetch('/api/admin/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'e2e-seller@example.com', password: 'Password123!' }),
})
const json = await res.json()
localStorage.setItem('admin_auth_token', json?.data?.token ?? '')
location.reload()
```

### 13.4 MCP 执行“断言 token 是否被清除”（验证 401/403 行为）

```js
localStorage.getItem('user_auth_token')
localStorage.getItem('admin_auth_token')
```

配合步骤：

1) 先写入一个明显无效 token：`localStorage.setItem('user_auth_token', 'bad')`
2) 打开 `/profile`（触发鉴权）
3) 断言：跳转到 `/login?...` 且 token 被清除
