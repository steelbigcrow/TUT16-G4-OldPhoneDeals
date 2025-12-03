# OldPhoneDeals Spring Backend Improvement Plan

## 缺失/待补齐的功能
- [x] Admin 订单导出与高级过滤：旧版有 `/api/admin/orders/export` (CSV/JSON) 以及 searchTerm/brandFilter 过滤；Spring 版仅支持 userId、起止日期分页。需要在 `/api/admin/orders` 增加 searchTerm/brandFilter，并新增 `/api/admin/orders/export` 支持 CSV/JSON。
- [x] Admin 商品详情：旧版有 GET `/api/admin/phones/:phoneId`；Spring 版只提供列表/更新/禁用/删除，缺单个详情获取。建议新增 GET `/api/admin/phones/{phoneId}` 返回商品+卖家基本信息。
- [] Admin 用户关联资源：旧版有 `/api/admin/users/:userId/phones` 和 `/api/admin/users/:userId/reviews`；Spring 版缺失。需要补充两个端点用于管理员查看指定用户的商品与评论。
- [] Admin 按商品查看评论：旧版有 GET `/api/admin/phones/:phoneId/reviews`；Spring 版只有综合列表、切换可见和删除，缺按商品聚焦的查询。建议新增 GET `/api/admin/reviews/phones/{phoneId}`（或类似路径），支持分页/可见性过滤。
- [] 用户订单历史分页：旧版用户 GET `/api/orders` 支持 page/limit/sort；Spring 版 `/api/orders/user/{userId}` 返回全集。建议增加分页/排序参数并保持权限校验只允许本人访问。
- [] 资料更新对齐旧逻辑：旧版 `/api/update-profile` 允许更新 email/姓名且要求当前密码；Spring 版 `/api/profile/{userId}` 仅改姓名且不校验旧密码。若需保持旧体验，需允许变更邮箱并要求提供当前密码。