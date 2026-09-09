# OldPhoneDeals — React 前端

OldPhoneDeals 二手手机交易平台的前端 SPA，对接 [`spring-old-phone-deals/`](../spring-old-phone-deals/) 提供的 REST API。

## 技术栈

- **React 19** + TypeScript
- **Vite**（开发服务器与构建）
- **Tailwind CSS**
- **React Router v6**（嵌套路由 + 守卫）
- **Axios + TanStack Query**（服务端状态统一由 Query 管理）
- **React Hook Form + Zod**（表单与校验）
- **Vitest + Testing Library**（单元测试）、**Playwright**（E2E 测试）

## 开发

```bash
npm install
npm run dev
```

开发服务器默认运行在 `http://localhost:5173`。

### 后端代理

`vite.config.ts` 已配置代理，以下路径会转发到后端 `http://localhost:8080`：

- `/api`
- `/uploads`
- `/images`

因此本地开发时需先启动 Spring 后端。

## 可用脚本

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动开发服务器 |
| `npm run build` | 类型检查 + 生产构建 |
| `npm run preview` | 预览生产构建产物 |
| `npm run lint` | ESLint 检查 |
| `npm test` | 运行 Vitest 单元测试 |
| `npm run test:watch` | Vitest 监听模式 |
| `npm run test:e2e` | 运行 Playwright E2E 测试 |

## E2E 测试

E2E 测试依赖后端可用，并需要通过 `/api/e2e/reset` 重置测试数据（需后端设置 `APP_E2E_ENABLED=true`）。

```bash
npm run test:e2e
```

可通过环境变量覆盖目标地址：

- `E2E_APP_BASE_URL`（默认 `http://localhost:5173`）
- `E2E_API_BASE_URL`（默认 `http://localhost:8080/api`）

## 目录结构

```text
src/
├── api/          # Axios 客户端、路径归一化、各模块接口封装
├── auth/         # token 存取与 returnUrl 处理
├── components/   # 通用组件（布局、路由守卫、错误边界）
├── contexts/     # 纯 UI 状态（通知等）
├── hooks/        # TanStack Query hooks
├── pages/        # 页面（auth / home / search / phone / user / profile / admin）
├── types/        # 与后端 DTO 对齐的类型
└── utils/        # 工具函数
```

## 认证约定

- 用户 token：`localStorage['user_auth_token']`
- 管理员 token：`localStorage['admin_auth_token']`

两者可并存。请求时按归一化后的路径前缀判断使用哪个 token：`/admin` 开头用管理员 token，其余用用户 token。
