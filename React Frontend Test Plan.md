# React Frontend Test Plan

> 目标：为本项目的 `react-frontend/`（React + Vite + TypeScript + TanStack Query + React Router）建立一套可落地的测试规划，覆盖 **单元测试（Unit）**、**集成测试（Integration）**、**端到端测试（E2E）**，并明确优先级、边界与规范，便于团队按迭代逐步补齐。

---

## 1. 背景与范围

### 1.1 项目范围（前端）

- 应用类型：SPA（Vite），路由由 React Router v6 管理。
- 主要依赖：
  - 数据请求与缓存：Axios + TanStack React Query
  - 表单：React Hook Form + Zod
  - UI：Tailwind CSS + 自定义 Notification（Toast）Context

### 1.2 本测试计划覆盖内容

- 覆盖 `react-frontend/src/` 中的：
  - 纯函数/工具函数（utils、auth、api 辅助）
  - API client 适配（错误处理、路径归一化、上传等）
  - hooks（useAuth/usePhones/useCart/useOrders…）
  - components（布局、路由守卫、错误边界、通用组件）
  - pages（auth、profile、home/search/phone detail、user、admin）
- 不在本计划内（但可作为后续扩展）：
  - 视觉回归（截图 diff）
  - 性能压测（Lighthouse / Web Vitals 自动门禁）

---

## 2. 现状盘点（已有测试）

> 现有单测基于 Vitest 运行（`react-frontend/package.json` 中 `"test": "vitest run"`），测试环境为 `jsdom`（`react-frontend/vite.config.ts`）。

### 2.1 本地运行与调试（推荐）

- 安装依赖：`cd react-frontend && npm install`（CI 中建议用 `npm ci`）
- 一次性运行全部测试：`cd react-frontend && npm test`
- Watch 模式：`cd react-frontend && npm run test:watch`
- 只跑单个文件：`cd react-frontend && npm test -- src/auth/tokens.test.ts`
- 按用例名筛选：`cd react-frontend && npm run test:watch -- -t 'ProtectedRoute'`
- 图形界面（项目已包含 `@vitest/ui`）：`cd react-frontend && npx vitest --ui`

> 说明：Vitest 已配置 `globals: true`（`react-frontend/vite.config.ts`），测试里可以不显式 import `describe/it/expect`；当前代码选择显式 import 也没问题，建议在同一项目内保持风格一致即可。

### 2.2 当前已存在的测试（按目录）

- `react-frontend/src/auth/`
  - `tokens.test.ts`
  - `returnUrl.test.ts`
- `react-frontend/src/api/`
  - `client.getApiErrorMessage.test.ts`
  - `normalizePath.test.ts`
  - `upload.test.ts`
- `react-frontend/src/utils/`
  - `images.test.ts`
  - `zodFormErrors.test.ts`
- `react-frontend/src/contexts/`
  - `NotificationContext.test.tsx`
- `react-frontend/src/components/common/`
  - `ProtectedRoute.test.tsx`
- `react-frontend/src/pages/`
  - `auth/LoginPage.test.tsx`
  - `profile/ProfileListingsPage.test.tsx`

### 2.3 总体判断

- 已有测试覆盖了少量关键路径（登录跳转安全、路由守卫、上传创建 listing），但对 **大部分页面/hook/API 模块**仍缺少测试。
- 当前也没有 “覆盖率阈值/CI 门禁”，因此“完整性”无法量化。

---

## 3. 测试分层策略（测试金字塔 / Testing Trophy）

### 3.1 建议权重（经验比例）

- **集成测试（Integration / Component Integration）**：60%
  - 以 “页面/组件 + Router + React Query + Context” 为最小可用集成单元
  - mock 网络与外部依赖，但不 mock 自己的组件实现细节
- **单元测试（Unit）**：30%
  - 覆盖纯函数、规则函数、少量复杂 hook 的边界条件
- **E2E（Playwright/Cypress）**：10%
  - 覆盖关键用户主流程（Happy path + 1~2 条典型异常路径）

> 原因：前端的 bug 更多出现在 “多个模块协同 + 用户交互” 的边界上；单纯测纯函数带来的信心有限，但成本更低，适合作为基础补齐。

### 3.2 不建议的方向（控制成本与脆弱性）

- 不把 Snapshot 当主要手段（易脆、价值低）
- 不测试实现细节（state、内部函数调用次数）作为主要断言
- 少使用 `data-testid`，优先 `getByRole/getByLabelText` 等可访问性查询

---

## 4. 工具链与推荐库

### 4.1 单元/集成测试（现状：可直接使用）

- Test Runner：Vitest（已在项目中）
- DOM 测试：React Testing Library（已在项目中）
- 交互：`@testing-library/user-event`（已在项目中）
- 环境：`jsdom`（已在 `vite.config.ts` 配置）
- 全局断言增强：`@testing-library/jest-dom/vitest`（已在 `src/test/setup.ts` 引入）
- 交互式调试：`@vitest/ui`（已在项目中，可用 `cd react-frontend && npx vitest --ui`）
- 覆盖率（建议尽快落地）：引入 `@vitest/coverage-v8` 并在 CI 执行 `vitest run --coverage`（落地细节见第 10 节）

### 4.2 网络 mock（建议补充，引入后集成测试更“像真实应用”）

两种路线按测试层级选择（同一条用例尽量不要混用）：

1) **模块级 mock（现状做法）**：`vi.mock('../../api/auth', ...)`
   - 优点：上手快、局部精确
   - 缺点：容易与实现耦合，测试可能绕开 axios client 的逻辑

2) **请求级 mock（推荐）**：MSW（Mock Service Worker）
   - 优点：更接近真实网络层，能覆盖 axios client 拦截器/headers/token 等逻辑
   - 缺点：需要一次性引入与配置

> 推荐：逐步迁移到 MSW，用它写“页面级集成测试”，保留少量模块 mock 给纯单元测试使用。

MSW 落地建议（最小可用配置）：

- 安装依赖：`cd react-frontend && npm i -D msw`
- 目录建议：`react-frontend/src/test/msw/`（例如 `server.ts`、`handlers.ts`）
- 在 `react-frontend/src/test/setup.ts` 中全局启停：
  - `beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))`（避免“漏 mock”导致测试悄悄打到真实网络/走错分支）
  - `afterEach(() => server.resetHandlers())`（避免用例之间互相污染）
  - `afterAll(() => server.close())`
- 单用例覆写：在测试内通过 `server.use(...)` 覆写返回数据，优先把“场景差异”留在测试里而不是堆很多 if/else handler

### 4.3 E2E（建议新增）

- 推荐：Playwright
  - 与 Vite/React 配合好，稳定性与速度较好
  - 适合做真正的浏览器级流程验证（登录、搜索、下单、后台管理等）

Playwright 落地建议（最小可用配置）：

- 安装依赖：`cd react-frontend && npm i -D @playwright/test`
- 安装浏览器：`cd react-frontend && npx playwright install`
- 目录建议：`react-frontend/e2e/` + `react-frontend/playwright.config.ts`
- CI 运行：建议用 `webServer` 启动 `npm run preview`，并设置 `baseURL`（避免直接依赖 dev server 的偶然行为）

---

## 5. 测试目录与命名规范

### 5.1 目录约定

- Unit/Integration：
  - `react-frontend/src/**/__tests__/*.test.ts(x)` 或 `react-frontend/src/**/*.test.ts(x)`
  - 维持“测试靠近代码”的方式，便于维护与发现
- 测试工具：
  - `react-frontend/src/test/`（现已有 `setup.ts`、`testUtils.tsx`）
- 构建隔离：`react-frontend/tsconfig.app.json` 已排除 `src/test/**` 与 `src/**/*.test.*` / `src/**/*.spec.*`，确保测试文件不会影响 `npm run build`
- （建议新增）测试数据工厂：
  - `react-frontend/src/test/factories/`（集中构造 User/Phone/Order 等 DTO）
- （建议新增）E2E：
  - `react-frontend/e2e/`（例如 `auth.spec.ts`、`checkout.spec.ts`）

### 5.2 命名与书写

- 文件：`Thing.test.ts` / `Thing.test.tsx`
- `describe('模块/组件名', ...)`
- `it('行为描述（用户视角）', ...)`
  - 好例子：`it('shows validation error when password is too short', ...)`
  - 避免：`it('sets state to X', ...)`

### 5.3 推荐写法（减少样板与脆弱性）

- Provider 包装：对需要 Router/React Query/Context 的页面测试，优先使用 `react-frontend/src/test/testUtils.tsx` 的 `renderWithProviders`
- React Query：每个用例使用独立的 `QueryClient`（避免缓存/重试导致用例互相污染）；必要时通过 `renderWithProviders(..., { queryClient })` 显式传入
- 异步断言：优先用 `findBy...` / `waitFor`，避免手写 `setTimeout`；只有在 timer 相关逻辑（toast 自动关闭等）才使用 `vi.useFakeTimers()`

---

## 6. Unit Test 规划（纯函数/规则/边界）

### 6.1 auth（优先级：P0）

- `src/auth/tokens.ts`
  - 根据路径选择 token（user/admin）
  - 清理 token 的逻辑（按路径清理正确的 key）
  - localStorage 不可用/异常时的兜底（建议补充）
- `src/auth/returnUrl.ts`
  - returnUrl 的安全校验：拒绝外域、拒绝双斜杠、拒绝 login loop

### 6.2 api client（优先级：P0）

- `src/api/normalizePath.ts`
  - 去除 query/hash、补前导 `/`、URL 解析行为
- `src/api/client.ts`
  - `getApiErrorMessage()` 的错误提取策略
  - token 注入到 header 的规则（user/admin 分支，已实现，建议补充测试）
  - 401/403 时清 token + redirect（已实现，建议补充测试）
- `src/api/upload.ts`
  - FormData 是否按约定传递字段名、URL 是否正确

### 6.3 utils（优先级：P1）

- `src/utils/images.ts`
  - 空值、绝对 URL、相对路径归一化
- `src/utils/zodFormErrors.ts`
  - Zod issue 到 react-hook-form `setError` 的映射
- （建议补充）任何包含复杂业务规则的工具函数：
  - 价格/库存显示规则
  - 分页参数序列化/解析

---

## 7. Integration Test 规划（组件/页面级）

> 目标：把真实用户交互、路由跳转、React Query 状态、UI 提示串起来测。原则是 **mock 网络，但尽量少 mock 业务组件内部实现**。

### 7.1 通用基础能力（优先级：P0）

- 路由守卫 `ProtectedRoute`
  - 未登录跳转到 `/login?returnUrl=...`
  - loading 状态显示 fallback
  - 登录态成功后允许访问子路由
- 全局通知 `NotificationContext`
  - success/error toast 的展示、关闭、自动消失
- `ErrorBoundary`
  - 子组件抛错时显示 fallback UI（建议补）

### 7.2 Auth 流程页面（优先级：P0）

- `LoginPage`
  - 成功：保存 token、跳转到安全 returnUrl
  - 失败：显示错误 toast/错误文案（建议补）
  - 表单校验：邮箱格式、密码空值（建议补）
- `RegisterPage`
  - 成功：提示“请验证邮箱”或跳转到登录页
  - 失败：重复邮箱/弱密码等错误提示
- `VerifyEmailPage`
  - token 参数缺失/错误时提示
  - 成功时提示并引导登录
- `ResetPasswordPage`
  - 申请重置：成功提示/失败提示
  - 真正重置：token 有效/无效分支

### 7.3 浏览与搜索（优先级：P0 / P1）

- `HomePage`
  - 加载态（skeleton/spinner）、空态（无商品）、列表态
  - 点击某个 `PhoneCard` 进入详情页
- `SearchPage`
  - 关键过滤器：品牌、价格上限、排序
  - 输入 query 后触发请求与结果更新
  - 失败态：API 错误提示
- `PhoneDetailPage`
  - 展示 phone 信息
  - reviews 分页加载（如有）
  - 加入 wishlist/cart 的交互（如有）

### 7.4 Wishlist / Cart / Checkout（优先级：P0）

- `WishlistPage`
  - 未登录访问：被路由守卫拦截
  - 已登录：展示 wishlist、移除 item、空态
- `CheckoutPage`
  - 未登录访问：被路由守卫拦截
  - 已登录：展示购物车、更新数量、下单成功/失败提示
  - 下单后跳转或清空状态（按实现）

### 7.5 Profile（优先级：P0）

- `ProfileHomePage` / `ProfileLayoutPage`
  - layout 是否正确渲染 Outlet
  - 导航入口是否可用
- `ProfileListingsPage`
  - 获取 seller phones 的加载/空态/列表态
  - 上传图片后自动填充 imageUrl（已覆盖，可补异常分支）
  - 创建 listing 成功后刷新列表/提示
  - 创建 listing 失败时 toast/错误文案
- `ProfileSettingsPage`
  - 读取 profile 并回填表单
  - 更新 profile 成功/失败提示

### 7.6 Admin（优先级：P1）

- `AdminLoginPage`
  - 登录成功保存 admin token、跳转 dashboard
  - 登录失败提示
- `AdminLayoutPage` + admin routes
  - 未登录访问：跳转 `/admin/login?returnUrl=...`
- Admin 管理页（users/phones/orders/reviews/logs）
  - 列表加载/分页/筛选
  - 典型操作：禁用用户、下架商品、修改库存等（按实现）

---

## 8. Hook Test 规划（介于 unit 与 integration 之间）

> hooks 往往绑定 React Query 与 API 模块，建议优先用集成测试间接覆盖；只有当 hook 逻辑复杂、分支多、容易出错时，才单独写 hook test。

建议覆盖的 hooks（优先级由业务关键度决定）：

- `src/hooks/useAuth.ts`（P0）
  - token 存在/不存在分支
  - me 请求成功/失败分支
- `src/hooks/useAdminAuth.ts`（P1）
- `src/hooks/usePhones.ts`（P0/P1）
  - 搜索参数变化时 queryKey 是否稳定/正确
- `src/hooks/useCart.ts`（P0）
  - add/remove/update/checkout 成功后 invalidate 对应 query
- `src/hooks/useWishlist.ts`（P0）
  - add/remove 后刷新
- `src/hooks/useUploadImage.ts`（P0）
  - upload 成功后返回 fileUrl 给页面使用（或被 mutation 使用）

Hook 测试建议做法：

- 若引入 MSW：使用真实 `apiClient` 发请求，由 MSW 返回响应，从而覆盖拦截器/token 行为。
- 使用 `QueryClientProvider` 包裹（可复用 `src/test/testUtils.tsx` 的创建逻辑）。

---

## 9. E2E Test 规划（Playwright）

### 9.1 运行模式（建议）

E2E 通常两种模式：

1) **Against real backend（推荐）**
   - 启动后端（Spring）使用 test profile + test DB（或内存 DB/隔离库）
   - 启动前端（Vite preview 或 dev server）
   - Playwright 执行浏览器流程

2) **Against mocked backend（成本更低）**
   - 前端依旧真实运行，但后端用 mock server（MSW/node）替代
   - 适合 UI/路由/表单流程验证，但对契约与鉴权的信心更低

> 本项目建议：关键主流程用真实后端跑（P0），其余用 mocked backend（P1）。

### 9.2 P0 E2E（必须覆盖的主流程）

- 用户登录与退出
  - 输入账号密码 -> 登录成功 -> 导航到受保护页面（wishlist/profile）
  - returnUrl 逻辑验证（从受保护页面被带回）
- 搜索与查看详情
  - 打开 Home/Search -> 搜索关键词/筛选 -> 打开 Phone Detail
- Wishlist（登录后）
  - 从详情页加入 wishlist -> 在 wishlist 页面能看到 -> 移除成功
- Checkout（登录后）
  - 添加商品到 cart（如有）-> 进入 checkout -> 下单成功 -> 订单列表可见（若有）
- Seller 发布 listing（登录后）
  - Profile Listings -> 上传图片 -> 创建 listing -> 列表出现新商品

### 9.3 P1 E2E（可选但推荐）

- 注册 + 邮箱验证（若可在测试环境中模拟）
- Reset password 流程
- Admin 登录与关键管理操作（禁用用户/下架商品/调整库存）
- 404/错误页面与错误提示一致性

### 9.4 E2E 稳定性规范

- 只用用户可见的选择器（role/label/text），必要时给关键按钮加稳定的 `data-testid`
- 避免 `waitForTimeout`，用 Playwright 的自动等待与明确断言
- 每条用例独立准备数据（或清理），避免依赖执行顺序

---

## 10. 覆盖率与质量门禁（建议）

### 10.1 如何开启覆盖率（落地步骤）

- 安装 provider（Vitest v3 需要单独安装）：`cd react-frontend && npm i -D @vitest/coverage-v8`
- 增加脚本（建议）：在 `react-frontend/package.json` 增加 `"test:coverage": "vitest run --coverage"`
- 在 `react-frontend/vite.config.ts` 中配置 `test.coverage`（建议先用全局阈值，后续再按目录细化）：

```ts
export default defineConfig({
  test: {
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov', 'html'],
      reportsDirectory: 'coverage',
      thresholds: {
        statements: 60,
        lines: 60,
        branches: 50,
        functions: 60,
      },
      exclude: ['src/test/**', '**/*.d.ts'],
    },
  },
})
```

- CI 门禁：PR 触发跑 `npm run test:coverage`，让 Vitest coverage thresholds 直接作为 fail 条件（比“只看报告”更可靠）
- 产物处理：`coverage/` 建议作为 CI artifact（便于回溯），但不要提交到 git

> 备注：Vitest coverage 阈值在 v3 位于 `test.coverage.thresholds`，不要按旧写法把 lines/branches 写在 `coverage` 顶层。

### 10.2 覆盖率指标（建议起点）

> 覆盖率不是最终目标，但可以作为“避免完全没测”的最低门槛。

- lines/statements：先设 60% 起步，逐步提高到 75%+
- branches：先不强制或设 50% 起步（前端分支多、提升需要时间）
- 只对 `src/` 统计，排除 `src/test/`、类型声明与入口文件

### 10.3 PR 合并门禁（建议）

- 新增/修改功能必须包含：
  - 至少 1 条集成测试覆盖核心路径
  - 若新增纯函数规则，则补对应 unit test
- 修复 bug 必须包含：
  - 能复现 bug 的测试（先红后绿）

---

## 11. 迭代落地路线（推荐执行顺序）

### Phase 1（P0：提升核心信心）

- 补齐 Auth + ProtectedRoute + Notification + ErrorBoundary 的集成测试
- 补齐 Wishlist/Checkout/ProfileListings 的异常分支
- 为 hooks 增加最小必要覆盖（优先 useAuth/useCart/useWishlist/useUploadImage）

### Phase 2（P1：扩展覆盖面）

- Search/Home/Detail 三大浏览链路的集成测试全面化（loading/error/empty/success）
- Admin login + 1~2 个关键管理页面的基础流程测试

### Phase 3（P0/P1：引入 E2E）

- 引入 Playwright（或 Cypress）并落地 5 条 P0 E2E
- 形成 CI 流水线：unit+integration on PR、E2E on merge/nightly

---

## 12. 附录：推荐测试用例清单（按模块）

> 用例写法建议：每条用例对应一个 “用户可感知行为”。

### Auth

- Unit：tokens、returnUrl、安全跳转
- Integration：Login/Register/Verify/Reset 的成功/失败/校验
- E2E：登录成功 + returnUrl，退出/过期 token 处理

### Phones（Home/Search/Detail）

- Unit：筛选参数归一化（如存在）
- Integration：列表加载、筛选、分页、详情页展示与错误态
- E2E：搜索 -> 打开详情

### Wishlist

- Integration：未登录拦截；已登录展示/移除/空态
- E2E：加入 -> 查看 -> 移除

### Cart / Checkout / Orders

- Integration：更新数量、下单成功/失败、订单列表刷新
- E2E：下单主流程（可基于固定测试数据）

### Profile（Seller）

- Integration：profile 获取、settings 更新、listings 上传并创建
- E2E：上传创建 listing 主流程

### Admin

- Integration：admin token 分支、管理页列表基础功能
- E2E：admin 登录 + 1 个关键管理操作
