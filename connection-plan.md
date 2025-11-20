# 新前端（next-old-phone-deals）与新后端（spring-old-phone-deals）连通修复规划

## 目标
- 让 Next 前端的所有 HTTP 调用能够命中 Spring Boot 后端的实际端点，并正确解析返回格式。
- 统一环境变量和端口配置，避免 profile 不同导致的地址错位。
- 为后续端到端（含 Playwright）验证提供可运行的配置说明。

## 当前主要问题
1. **返回结构不匹配**：后端所有接口都包裹在 `ApiResponse { success, message, data }` 中，前端如 `src/lib/phoneCatalogApi.ts` 直接当作裸数据读取，会拿到 `undefined` 并抛错。
2. **端点路径不一致**：
   - 管理员仪表盘：前端调用 `/api/admin/dashboard-stats`，后端提供 `/api/admin/stats`。
   - 卖家上下架：前端调用 `/api/phones/{id}/disable`，后端没有对应路由，仅有管理员专用 `/api/admin/phones/{id}/toggle-disabled`。
3. **分页字段不一致**：前端 `PageResponse` 期望 `total/pageSize`，后端返回 `totalItems/itemsPerPage` 且 `currentPage` 为 1 基，导致列表统计/分页显示异常。
4. **端口/环境变量可能错位**：Next 默认 `http://localhost:8080/api`，但如果 Spring 以 `dev` profile 启动会监听 3000，未同步会断连。
5. **E2E 重置开关默认关闭**：Playwright 调用 `/api/e2e/reset`，但后端需 `app.e2e.enabled=true` 才放通。

## 拟定修改方案
1. **统一响应解析**
   - 在 `src/lib/apiClient.ts` 增加对 `ApiResponse` 的解包辅助，或在各 API 模块对包装层进行安全解包，确保前端取到 `data`。
   - 对已经假设裸数据的模块（如 `phoneCatalogApi`, 以及涉及列表/分页的 admin API）批量调整为读取 `data`。

2. **对齐端点路径**
   - 管理员仪表盘：将前端请求改为 `/api/admin/stats`，或增加后端映射别名 `/dashboard-stats`（择其一，推荐改前端以避免重复端点）。
   - 卖家上下架：补齐后端 `PUT /api/phones/{phoneId}/disable`（卖家权限），与现有前端保持一致；管理员路径继续保留现有 `toggle-disabled`。

3. **分页模型对齐**
   - 前端 `PageResponse` 类型改为接受后端字段（`totalItems`, `itemsPerPage`，`currentPage` 为 1 基）并在加载处做兼容映射，避免显示 0 或页码错乱。
   - 视需要在 UI 层保留向后兼容字段名，降低改动面。

4. **配置/环境变量**
   - 文档中明确：后端若启用 `dev` profile（端口 3000），需在 Next `.env.local` 设置 `NEXT_PUBLIC_API_BASE_URL=http://localhost:3000/api` 与 `NEXT_PUBLIC_FILES_BASE_URL=http://localhost:3000`；若使用默认 8080 则无需改。
   - 添加 `app.e2e.enabled=true` 的示例（本地/测试环境）以放通 Playwright 的 `/api/e2e/reset`。

5. **验证计划**
   - 手动逐项：登录/注册、首页手机列表、搜索、详情页下单流程、心愿单/购物车操作、管理端仪表盘及用户/商品列表分页。
   - 若启用 Playwright：在后端配置 E2E 开关、启动 3000/8080 后运行 `npm run test:e2e`（或项目内现有脚本），确认 `/api/e2e/reset` 正常返回。

## 输出物与影响面
- 受影响文件：`next-old-phone-deals/src/lib/apiClient.ts` 及各 API 模块、`next-old-phone-deals/src/types/admin.ts`，可能新增后端控制器方法（PhoneController 卖家 disable）。
- 配置文件：`.env.local` 示例说明（前端）、`application.yml`/`application-dev.yml`（后端 E2E 开关示例）。

## 后续优先级（建议顺序）
1) 响应解包 + 端点路径对齐（避免直接报错/404）。  
2) 分页字段映射（恢复管理端列表正确分页）。  
3) 卖家上下架接口补齐（防止前端按钮失败）。  
4) 环境变量/E2E 开关文档化。   
