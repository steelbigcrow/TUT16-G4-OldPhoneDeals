# Next Old Phone Deals

基于 Next.js App Router 的前端骨架工程，用于重构旧手机交易平台的用户端与管理端界面。本项目仅搭建路由、布局和 HTTP 客户端封装，不包含完整业务逻辑实现。

## 目录结构概览

- `src/app/layout.tsx`：全局根布局，默认使用用户端布局。
- `src/app/page.tsx`：根首页（对应原 Angular `/user-home`）。
- 用户端主要路由占位：
  - `src/app/search/page.tsx`：搜索页。
  - `src/app/phone/[id]/page.tsx`：手机详情页。
  - `src/app/login/page.tsx`：登录页。
  - `src/app/register/page.tsx`：注册页。
  - `src/app/verify-email/page.tsx`：邮箱验证页。
  - `src/app/reset-password/page.tsx`：重置密码页。
  - `src/app/checkout/page.tsx`：购物车/结算页。
  - `src/app/wishlist/page.tsx`：心愿单页。
  - 用户个人中心：
    - `src/app/user/profile/page.tsx`：个人中心首页。
    - `src/app/user/profile/edit/page.tsx`：编辑资料。
    - `src/app/user/profile/change-password/page.tsx`：修改密码。
    - `src/app/user/profile/listings/page.tsx`：管理本人挂牌的手机。
    - `src/app/user/profile/reviews/page.tsx`：查看卖家收到的评论。
- 管理端路由与布局：
  - `src/app/admin/layout.tsx`：Admin 专用布局。
  - `src/app/admin/page.tsx`：Admin 仪表盘首页。
  - `src/app/admin/users/page.tsx`：用户管理。
  - `src/app/admin/listings/page.tsx`：挂牌管理。
  - `src/app/admin/reviews/page.tsx`：评论管理。
  - `src/app/admin/orders/page.tsx`：订单/销售日志。
  - `src/app/admin/logs/page.tsx`：管理操作日志。
- 公共布局与基础组件：
  - `src/components/layout/UserLayout.tsx`：用户端布局（含 Header/Footer 包裹）。
  - `src/components/layout/AdminLayout.tsx`：管理端布局（左侧导航 + 顶部栏）。
  - `src/components/common/Header.tsx`：通用头部组件。
  - `src/components/common/Footer.tsx`：通用底部组件。
- HTTP 客户端封装：
  - `src/lib/apiClient.ts`：统一的 `apiGet/apiPost/apiPut/apiPatch/apiDelete` 封装。

所有页面目前仅包含简单的标题与 TODO 文案，后续子任务会在这些页面中补充实际业务逻辑与调用后端 API。

## 环境变量配置

HTTP 客户端使用环境变量 `NEXT_PUBLIC_API_BASE_URL` 作为后端 API 基础 URL，并在代码中提供默认值 `http://localhost:8080/api`。

本仓库提供示例文件：

```bash
cp .env.example .env.local
```

然后根据实际后端地址修改：

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

## 本地启动

在仓库根目录下，进入 Next 前端子项目目录并安装依赖：

```bash
cd next-old-phone-deals
npm install
```

开发模式运行：

```bash
npm run dev
```

构建与生产运行（可选）：

```bash
npm run build
npm start
```

以上脚本均来自 `package.json` 默认配置，无需额外自定义脚本。