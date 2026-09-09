# OldPhoneDeals E-Commerce Platform

OldPhoneDeals 是一个二手手机买卖的全栈电商平台，支持用户注册与邮箱验证、密码重置、商品发布与搜索、评论、购物车、下单、收藏夹，以及一套完整的管理员后台（用户 / 商品 / 评论 / 订单 / 操作日志）。

当前技术栈：**Spring Boot 3 + MongoDB 后端 + React SPA 前端**。

> 历史说明：项目最初为 MEAN（MongoDB + Express + Angular + Node.js）实现，现已迁移到 Spring Boot + React。旧的 Express 后端（`server/`）与 Angular 前端（`AngularOldPhoneDeals/`）已从代码库中移除。

## 仓库结构

```text
.
├── spring-old-phone-deals/   # 后端：Spring Boot 3.2 + MongoDB + JWT + RabbitMQ
│   ├── src/main/java/com/oldphonedeals/
│   │   ├── controller/       # REST 控制器（用户侧）
│   │   ├── service/          # 业务逻辑与实现
│   │   ├── repository/       # Spring Data MongoDB 仓储
│   │   ├── entity/           # MongoDB 文档实体
│   │   ├── dto/              # 请求 / 响应 DTO
│   │   ├── security/         # JWT 认证与授权
│   │   ├── consumer/         # RabbitMQ 消费者与 Saga 编排
│   │   ├── producer/         # RabbitMQ 生产者
│   │   └── exception/        # 全局异常处理
│   ├── src/test/             # 单元测试 + Testcontainers 集成 / E2E 测试
│   └── docs/                 # MQ 集成、MQ 测试、Saga 补偿规划文档
├── react-frontend/           # 前端：React 19 + TypeScript + Vite + Tailwind
│   └── src/
│       ├── api/              # Axios 客户端与接口封装
│       ├── hooks/            # TanStack Query hooks
│       ├── pages/            # 页面（auth / user / profile / admin）
│       ├── components/       # 通用组件与路由守卫
│       └── types/            # 与后端 DTO 对齐的类型定义
└── MongoDB Database Structure.md  # 数据库结构说明
```

## 技术栈

### 后端（`spring-old-phone-deals/`）

- **框架**：Spring Boot 3.2，JDK 21
- **数据库**：MongoDB
- **安全**：Spring Security + JWT（HS512）
- **消息队列**：RabbitMQ（邮件异步投递、订单后置处理、Saga 自动补偿）
- **邮件**：SendGrid SMTP
- **构建**：Maven
- **测试**：JUnit 5 + Testcontainers（MongoDB / RabbitMQ）+ Awaitility，JaCoCo 覆盖率

### 前端（`react-frontend/`）

- **框架**：React 19 + TypeScript
- **构建**：Vite
- **样式**：Tailwind CSS
- **路由**：React Router v6（嵌套路由 + 守卫）
- **服务端状态**：Axios + TanStack Query
- **表单校验**：React Hook Form + Zod
- **测试**：Vitest + Testing Library，Playwright E2E

## 环境准备

- **JDK 21**
- **Maven 3.6+**
- **Node.js 18+**
- **MongoDB**（本地实例，默认端口 27017）
- **RabbitMQ**（本地实例，默认端口 5672）
- **SendGrid 账号**（用于发送验证邮件 / 重置密码邮件）

## 启动后端

1. 进入后端目录并配置环境变量：

    ```bash
    cd spring-old-phone-deals
    cp .env.example .env
    ```

    编辑 `.env`，至少填写以下项：

    ```env
    MONGODB_URI_DEV=mongodb://localhost:27017/oldphonedeals-dev
    JWT_SECRET=your-super-secret-jwt-key-min-256-bits
    SENDGRID_API_KEY=your-sendgrid-api-key
    FROM_EMAIL=noreply@oldphonedeals.com
    FRONTEND_URL=http://localhost:5173
    RABBITMQ_HOST=localhost
    RABBITMQ_PORT=5672
    ```

2. 启动服务：

    ```bash
    mvn spring-boot:run
    ```

    应用默认运行在 `http://localhost:8080`。

3. 健康检查：

    ```bash
    curl http://localhost:8080/api/test
    ```

## 启动前端

```bash
cd react-frontend
npm install
npm run dev
```

打开 `http://localhost:5173`。Vite 已配置代理，将 `/api`、`/uploads`、`/images` 转发到后端 `http://localhost:8080`。

## 测试

### 后端

```bash
cd spring-old-phone-deals

mvn test                      # 单元测试
mvn test jacoco:report        # 生成覆盖率报告（target/site/jacoco/index.html）
```

集成测试与 E2E 测试基于 Testcontainers，需要本地可用的 Docker 环境。

### 前端

```bash
cd react-frontend

npm test          # Vitest 单元测试
npm run test:e2e  # Playwright E2E（需先启动后端）
npm run lint      # ESLint
```

## 主要功能

### 用户侧

- 注册、邮箱验证、登录、重置密码
- 商品浏览、搜索（品牌 / 价格 / 排序 / 分页）、商品详情
- 评论查看与发布、评论可见性切换
- 购物车、结账下单（幂等，防重复下单）
- 收藏夹
- 个人资料：修改资料、修改密码、管理在售商品、查看卖家评论

### 管理侧

- 管理员登录与仪表盘统计
- 用户管理（列表、详情、更新、冻结 / 解冻、删除、关联商品与评论）
- 商品管理（列表、更新、上下架、删除）
- 评论管理（列表、按商品查看、切换可见性、删除）
- 订单管理（列表、详情、销售统计、CSV / JSON 导出）
- 操作日志查询

## 可靠性设计

后端在基础功能之外，针对电商场景的关键一致性问题做了以下设计：

- **结账幂等**：前端生成 `idempotencyKey`，服务端做重放一致性校验，避免重复下单。
- **库存乐观锁**：`Phone` 使用 `@Version` 乐观锁，更新与上下架强制版本校验，冲突返回 409。
- **异步消息投递**：邮件发送与订单后置处理通过 RabbitMQ 持久化，支持重试与死信队列（DLQ）。
- **Saga 自动补偿**：订单后置处理最终失败时自动触发补偿（回滚库存、更新订单状态、通知用户），补偿过程记录 `SagaLog` 审计，补偿失败降级进入补偿 DLQ。

详细设计见 `spring-old-phone-deals/docs/`。

## 文档

- [MongoDB Database Structure.md](./MongoDB%20Database%20Structure.md) —— 数据库集合、字段与索引说明
- [spring-old-phone-deals/README.md](./spring-old-phone-deals/README.md) —— 后端技术栈与 API 概览
- [spring-old-phone-deals/docs/mq-integration-plan.md](./spring-old-phone-deals/docs/mq-integration-plan.md) —— RabbitMQ 集成改造规划
- [spring-old-phone-deals/docs/mq-test-plan.md](./spring-old-phone-deals/docs/mq-test-plan.md) —— MQ 测试体系规划
- [spring-old-phone-deals/docs/saga-compensation-plan.md](./spring-old-phone-deals/docs/saga-compensation-plan.md) —— Saga 自动补偿机制规划

## 作者

- Group 4 – COMP5347 / COMP4347
