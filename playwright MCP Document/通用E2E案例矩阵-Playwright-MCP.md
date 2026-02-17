# 通用 E2E 案例矩阵（仅 Playwright MCP）

## 1. 说明

- 本矩阵只使用 Playwright MCP 作为测试工具。
- 每个案例都要求同时验证：前端 UI 行为 + 后端接口响应。
- 场景覆盖 React 路由与 Spring Boot API 的全部通用类别。
- 优先级定义：`P0`（核心阻断）/`P1`（高频重要）/`P2`（增强回归）。

## 2. 通用矩阵（48 个案例）

| 编号 | 场景 | 前置条件 | Playwright MCP 关键步骤 | 关键断言（UI + Network） | 优先级 |
| --- | --- | --- | --- | --- | --- |
| G-01 | 首页加载精选商品 | 执行过 `/api/e2e/reset` | `navigate /home` → `wait_for Best sellers` → `network_requests` | 看到两个精选区块；`GET /api/phones?special=*` 为 200 | P0 |
| G-02 | 搜索关键词返回结果 | 至少存在 1 个 iPhone 商品 | `navigate /search` → `type iPhone` → `click Apply` | 列表刷新；`GET /api/phones?search=iPhone` 返回分页结构 | P0 |
| G-03 | 品牌+价格组合筛选 | 有不同品牌与价格商品 | `navigate /search` → `click brand` → `click price` → `snapshot` | 结果符合筛选；请求包含 `brand`、`maxPrice` 参数 | P1 |
| G-04 | 排序与分页联动 | 搜索页有多页数据 | `navigate /search` → `click sort` → `click Next` | 页码变化；`GET /api/phones` 参数含 `sortBy/sortOrder/page` | P1 |
| G-05 | 商品详情与评论分页 | 存在可访问商品 ID | `navigate /phone/{id}` → `click reviews next` | 详情字段渲染；`GET /api/phones/{id}` 与 `/reviews` 均成功 | P0 |
| G-06 | 未知路由进入 404 | 无 | `navigate /this-route-not-exist` → `snapshot` | 展示 Not Found 页面；无未处理异常 | P2 |
| A-01 | 注册成功 | 邮箱未被占用 | `navigate /register` → `fill_form` → `click submit` | 注册成功提示；`POST /api/auth/register` 为 201 | P0 |
| A-02 | 注册重复邮箱失败 | 已存在邮箱 | `navigate /register` → `fill_form` → `click submit` | 展示失败提示；`POST /api/auth/register` 返回 400/409 | P1 |
| A-03 | 邮箱验证码验证成功 | 已注册且有验证码 | `navigate /verify-email` → `fill_form` → `click verify` | 验证成功提示；`POST /api/auth/verify-email` 为 200 | P0 |
| A-04 | 用户登录成功 | 有有效用户账号 | `navigate /login` → `fill_form` → `click login` | 跳转成功页；`POST /api/auth/login` 为 200 且 token 已存储 | P0 |
| A-05 | 用户登录失败（密码错） | 有有效邮箱 | `navigate /login` → `fill_form wrong pwd` → `click login` | 展示错误提示；`POST /api/auth/login` 为 401/400 | P1 |
| A-06 | 忘记密码发送重置码 | 有有效邮箱 | `navigate /reset-password` → `type email` → `click send` | 提示发送成功；`POST /api/auth/request-password-reset` 为 200 | P1 |
| A-07 | 重置密码成功 | 有验证码 | `navigate /reset-password` → `fill_form code+newPassword` → `click reset` | 重置成功并可登录；`POST /api/auth/reset-password` 为 200 | P0 |
| A-08 | 未登录访问用户受保护路由 | 清空 token | `run_code clear token` → `navigate /wishlist` | 重定向到 `/login?returnUrl=*`；业务接口出现 401/403 | P0 |
| A-09 | 普通用户访问管理员路由受限 | 使用买家账号登录 | `navigate /admin/dashboard` → `wait_for` | 被拒绝/重定向；`GET /api/admin/*` 为 403 | P0 |
| A-10 | 会话失效后自动回登录 | 使用过期或伪造 token | `run_code set fake token` → `navigate /profile` | 自动清 token 并跳登录；`GET /api/profile` 为 401/403 | P0 |
| U-01 | 收藏夹添加成功 | 用户已登录 | `navigate /search` → `click wishlist` → `navigate /wishlist` | 列表出现新增项；`POST /api/wishlist` 为 200 | P1 |
| U-02 | 收藏夹重复添加被拦截 | 同一商品已在收藏夹 | `click wishlist again` → `wait_for error` | 显示去重提示；`POST /api/wishlist` 返回 400 | P1 |
| U-03 | 收藏夹移除成功 | 收藏夹有数据 | `navigate /wishlist` → `click remove` | 条目消失；`DELETE /api/wishlist/{phoneId}` 为 200 | P1 |
| U-04 | 购物车添加成功 | 用户已登录且商品可售 | `navigate /phone/{id}` → `click add to cart` → `navigate /checkout` | 购物车出现商品；`POST /api/cart` 为 200 | P0 |
| U-05 | 购物车数量更新成功 | 购物车已有商品 | `navigate /checkout` → `type quantity` → `wait_for` | 金额刷新；`PUT /api/cart/{phoneId}` 为 200 | P0 |
| U-06 | 超库存更新失败 | 商品库存有限 | `navigate /checkout` → `type large quantity` | 显示库存错误；`PUT /api/cart/{phoneId}` 返回 400 | P1 |
| U-07 | 购物车移除成功 | 购物车已有商品 | `navigate /checkout` → `click remove` | 条目减少；`DELETE /api/cart/{phoneId}` 为 200 | P1 |
| U-08 | 结算下单成功 | 购物车有有效商品 | `navigate /checkout` → `fill_form address` → `click place order` | 展示订单成功；`POST /api/orders/checkout` 为 201 | P0 |
| U-09 | 空购物车结算失败 | 购物车为空 | `navigate /checkout` → `click place order` | 展示失败提示；`POST /api/orders/checkout` 返回 400 | P1 |
| U-10 | 我的订单分页查询 | 至少有 1 笔订单 | `navigate /profile` → `click orders tab` | 列表展示订单；`GET /api/orders?page=*` 为 200 | P1 |
| U-11 | 下单后数据一致性 | 记录下单前库存销量 | `checkout` → `navigate /phone/{id}` → `network_requests` | 库存减少、销量增加；UI 与 `GET /api/phones/{id}` 一致 | P0 |
| U-12 | 订单防重复提交（幂等） | 购物车有数据 | `navigate /checkout` → `double click place order` | 仅产生一笔订单；成功结算请求次数为 1 | P0 |
| S-01 | 卖家上传图片并发布商品 | 使用卖家账号登录 | `navigate /profile/listings` → `file_upload` → `fill_form` → `click create` | 新商品卡片出现；`POST /api/upload/image` + `/api/phones` 成功 | P0 |
| S-02 | 卖家上下架商品 | 卖家有商品 | `navigate /profile/listings` → `click toggle disabled` | 状态标签变化；`PUT /api/phones/{id}/disable` 为 200 | P1 |
| S-03 | 卖家删除商品 | 卖家有商品 | `navigate /profile/listings` → `click delete` | 商品消失；`DELETE /api/phones/{id}` 为 200 | P1 |
| S-04 | 卖家查看评论并分页 | 该卖家商品存在评论 | `navigate /profile/reviews` → `click next` | 评论页码变化；`GET /api/phones/reviews/by-seller` 成功 | P1 |
| S-05 | 卖家切换评论可见性 | 卖家评论列表非空 | `navigate /profile/reviews` → `click toggle visibility` | 可见性状态变化；`PATCH /api/phones/{phoneId}/reviews/{reviewId}/visibility` 为 200 | P1 |
| M-01 | 管理员登录与仪表盘 | 使用管理员账号 | `navigate /admin/login` → `fill_form` → `click login` → `navigate /admin/dashboard` | 仪表盘卡片出现；`POST /api/admin/login`、`GET /api/admin/stats` 成功 | P0 |
| M-02 | 管理员用户查询（搜索/分页） | 管理员已登录 | `navigate /admin/users` → `type search` → `click next` | 结果与分页刷新；`GET /api/admin/users` 参数正确 | P1 |
| M-03 | 管理员冻结用户 | 管理员已登录 | `navigate /admin/users` → `click toggle disabled` | 状态切换；`PUT /api/admin/users/{userId}/toggle-disabled` 为 200 | P0 |
| M-04 | 被冻结用户登录失败 | 先冻结目标用户 | `navigate /login` → `fill_form frozen user` → `click login` | 登录被拒绝；`POST /api/auth/login` 返回 403 | P0 |
| M-05 | 管理员编辑商品信息 | 管理员已登录 | `navigate /admin/phones` → `click edit` → `fill_form` → `click save` | 字段更新成功；`PUT /api/admin/phones/{phoneId}` 为 200 | P1 |
| M-06 | 管理员上下架/删除商品 | 管理员已登录 | `navigate /admin/phones` → `click toggle` 或 `click delete` | 列表状态更新；`PUT/DELETE /api/admin/phones/*` 成功 | P1 |
| M-07 | 管理员评论治理 | 管理员已登录且有评论数据 | `navigate /admin/reviews` → `type search` → `click toggle/delete` | 评论状态变化；`PUT/DELETE /api/admin/reviews/*` 成功 | P1 |
| M-08 | 管理员订单筛选排序 | 管理员已登录 | `navigate /admin/orders` → `fill_form filters` → `click apply` | 列表变化；`GET /api/admin/orders` 参数包含筛选与排序 | P1 |
| M-09 | 管理员导出订单 CSV/JSON | 管理员已登录 | `navigate /admin/orders` → `click export csv/json` | 导出触发成功；`GET /api/admin/orders/export?format=*` 为 200 | P0 |
| M-10 | 管理员日志分页 | 管理员已登录 | `navigate /admin/logs` → `click next` | 日志列表分页正常；`GET /api/admin/logs` 成功 | P2 |
| R-01 | API 500 错误提示与可恢复性 | 构造后端异常输入 | `navigate` → `click submit` → `console_messages` | 页面出现错误提示；控制台无崩溃级未捕获异常 | P1 |
| R-02 | 资源不存在 404 处理 | 使用不存在 phoneId | `navigate /phone/{badId}` | 展示友好错误；`GET /api/phones/{badId}` 返回 404 | P1 |
| R-03 | 上传非法文件类型被拒绝 | 卖家已登录 | `navigate /profile/listings` → `file_upload .exe` | 上传失败提示；`POST /api/upload/image` 返回 400 | P1 |
| R-04 | 上传超大小文件被拒绝 | 卖家已登录 | `navigate /profile/listings` → `file_upload >10MB` | 上传失败提示；`POST /api/upload/image` 返回 413/400 | P2 |
| R-05 | 全链路无严重控制台错误 | 完整跑一轮 P0 场景 | 每个场景后执行 `console_messages(level=error)` | 无新增严重错误堆栈；如有则记录缺陷 | P1 |

## 3. 执行顺序建议

1. 先执行全部 `P0`（发布阻断线）。
2. 再执行 `P1`（主流程高风险回归）。
3. 最后执行 `P2`（边界与增强稳定性）。

## 4. 每条用例的最小证据要求

- 至少 1 份 `browser_snapshot`
- 至少 1 份 `browser_network_requests`
- 至少 1 份 `browser_console_messages`

## 5. 通过标准

- UI 行为符合预期。
- 对应后端接口状态码与响应结构正确。
- 不出现未预期的 5xx、重定向循环或前端未捕获错误。
- 下单、冻结用户、导出、上传等关键动作具备可复核证据。
