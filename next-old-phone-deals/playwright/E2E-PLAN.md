# next-old-phone-deals Playwright E2E 规划

## 目标与范围
- 为 Next.js 前端建立可维护的端到端测试套件，覆盖买家/卖家/管理员关键路径。
- 运行时由 Spring 后端提供真实 API，使用后端已有的测试环境（含 mongodb-memory-server），前端不再增加同类依赖。
- 测试前通过 `/api/e2e/reset` 清空并注入基线数据，保证用例相互独立、可重复。

## 目录规划（前端仓库）
- `playwright/tests/`：所有 E2E 用例根目录（已存在）。
  - `auth/`：登录、注册、会话过期、重设密码等认证流。
  - `catalog/`：列表/搜索/筛选、详情页展示、图片切换等浏览体验。
  - `buyer/`：购物车、结算、支付占位、评价、心愿单等买家操作。
  - `seller/`：上架/编辑/下架、库存调整、订单处理（如前端暴露）。
  - `admin/`：仪表盘、商品禁用/解禁、用户封禁、审核类操作。
  - `helpers/`：通用 fixtures（页面模型、测试账号、API 客户端）、选择器常量、数据生成器。
  - 可选：`shared-state/` 用于存放 `storageState/*.json`（如需预登录快照）。
- `playwright/` 其他文件：
  - `playwright.config.ts`：统一入口；Web 服务器编排、baseURL、截屏/trace 策略。
  - `test-results/、playwright-report/`：默认报告与产物输出位置（运行后生成）。

## 运行时/环境约定
- 后端：启动 `spring-old-phone-deals` 的测试环境，打开 E2E 开关并复用后端已安装的 `mongodb-memory-server`，不在前端重复安装。
  - 建议命令（供 Playwright `webServer` 使用）：`mvn spring-boot:run -Dspring-boot.run.profiles=test -Dspring-boot.run.arguments="--server.port=8080 --app.e2e.enabled=true"`，必要时补充 `MONGODB_URI`/`JWT_SECRET`。
  - `APP_E2E_ENABLED=true` 后 `/api/e2e/reset` 可用；默认返回的账号：`e2e-buyer@example.com`、`e2e-seller@example.com`，密码 `Password123!`，并含一台示例手机。
- 前端：`npm run dev -- --hostname 127.0.0.1 --port 4200`，环境变量 `NEXT_PUBLIC_API_BASE_URL=http://127.0.0.1:8080/api`。
- Playwright：Node 18+，Chromium 项目优先；可使用 `PLAYWRIGHT_JAVA_HOME`/`JAVA_HOME` 指向仓库自带 JDK17。
- 资源清理：每个测试文件的 `beforeAll`/`beforeEach` 通过 `helpers` 中的 API 客户端调用 `/api/e2e/reset`，不要在前端创建新的 Mongo 内存实例。

## 用例分组与覆盖要点
- P0（优先落地）：登录/退出、目录浏览与搜索、商品详情、加入购物车、结算成功、管理员禁用商品后前端状态更新。
- P1：买家评价与隐藏、心愿单同步、卖家上架/调整库存、管理员查看仪表盘指标。
- P2：异常与边界：登录失败提示、库存见顶不可加入、无权限访问时的重定向、表单校验、网络错误兜底。

## 执行方式
- 本地运行（无探索 UI）：`npm run e2e`（自动按 `playwright.config.ts` 启动前后端并回收）。
- 调试模式：`npm run e2e:ui`，在 Playwright UI 中挑选用例、查看 trace。
- 如需复用已启动服务，设置 `CI=true` 或手动注释 `webServer` 的 `reuseExistingServer`=true，避免反复拉起进程。

## 后续落地步骤
- 在 `helpers` 内实现基础设施：`apiClient`（封装 `/api/e2e/reset` + 登录）、`roles.ts`（买家/卖家/管理员账号常量）、`page-models`（登录、目录、购物车、管理员仪表盘）。
- 按分组补充首批用例：`auth/login.spec.ts`、`catalog/browse.spec.ts`、`buyer/checkout.spec.ts`、`admin/toggle-phone.spec.ts`。
- 在 README/CI 脚本补充运行提示与依赖说明，强调无需在前端安装 Mongo 内存依赖。 
