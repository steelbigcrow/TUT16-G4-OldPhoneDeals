# Spring Old Phone Deals - 后端服务

这是 Spring Boot 3.x 实现的 Old Phone Deals 后端服务，使用 MongoDB 作为数据库，基于 JWT 进行认证。

## 技术栈概览

- **框架**: Spring Boot 3.2.0
- **JDK**: 17
- **数据库**: MongoDB
- **安全**: Spring Security + JWT
- **构建工具**: Maven
- **主要依赖**:
  - Spring Data MongoDB
  - Spring Boot Validation
  - Spring Boot Mail (SendGrid)
  - Lombok
  - MapStruct
  - jjwt (JWT 库)

## 环境准备

### 前置条件

- JDK 17（确保 `java -version` 为 17）
- MongoDB 4.0+
- Maven 3.6+
- SendGrid API Key（用于发送邮件）

### 1. 克隆并进入项目

```bash
cd spring-old-phone-deals
```

### 2. 配置环境变量

推荐使用 `.env` 文件（不会被提交到 Git）：

```env
# MongoDB 连接
MONGODB_URI=mongodb://localhost:27017/oldphonedeals

# JWT 配置
JWT_SECRET=your-super-secret-jwt-key-min-256-bits

# SendGrid 邮件配置
SENDGRID_API_KEY=your-sendgrid-api-key
FROM_EMAIL=noreply@oldphonedeals.com

# 前端 URL（本地开发时指向 React Vite 服务）
FRONTEND_URL=http://localhost:5173

# 运行环境(development/production)
NODE_ENV=development
```

**注意**: 
- `.env` 已加入 `.gitignore`，不要提交真实密钥。
- 生产环境请使用安全的密钥和独立的配置。

### 3. 构建项目

```bash
mvn clean install
```

### 4. 启动服务

```bash
# 使用 Maven 启动（默认 dev 配置）
mvn spring-boot:run

# 指定 profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或打包后运行
mvn package
java -jar target/spring-old-phone-deals-1.0.0.jar
```

应用默认运行在 `http://localhost:8080`。

### 5. 健康检查

启动后可以通过以下命令快速检查：

```bash
curl http://localhost:8080/api/test
```

## 项目结构

```
src/main/java/com/oldphonedeals/
├── OldPhoneDealsApplication.java  # 启动类
├── config/                        # 配置
│   ├── SecurityConfig.java        # Spring Security 配置
│   ├── CorsConfig.java            # CORS 配置
│   └── ...
├── entity/                        # 实体类（MongoDB 文档）
│   ├── User.java
│   ├── Phone.java
│   └── ...
├── dto/                           # 数据传输对象
│   ├── request/                   # 请求 DTO
│   └── response/                  # 响应 DTO
├── repository/                    # 仓储接口
├── service/                       # 业务逻辑
│   └── impl/                      # 实现类
├── controller/                    # REST 控制器
│   └── admin/                     # 管理员相关接口
├── security/                      # 安全与认证
│   ├── JwtAuthenticationFilter.java
│   └── JwtTokenProvider.java
├── exception/                     # 全局异常处理
├── mapper/                        # MapStruct 映射
└── util/                          # 工具类
```

## 认证与调用约定

所有需要认证的接口使用 JWT，HTTP 头格式如下：

```
Authorization: Bearer <your-jwt-token>
```

### 获取 Token 示例

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

响应示例：

```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": "123",
      "firstName": "John",
      "lastName": "Doe",
      "email": "user@example.com",
      "isAdmin": false
    }
  }
}
```

## 主要 API 概览

> 详细字段请参考代码中的 DTO 和控制器注释（或补充 `API_MAPPING.md`）。下面是按模块划分的主要路径。

### 认证/Auth

- **用户认证**:
  - `POST /api/auth/login` — 登录，返回 JWT 和用户信息
  - `POST /api/auth/register` — 注册并发送验证邮件
  - `POST /api/auth/verify-email` — 验证邮箱
  - `POST /api/auth/request-password-reset` — 请求重置密码验证码
  - `POST /api/auth/verify-reset-code` — 校验重置验证码
  - `POST /api/auth/reset-password` — 重置密码
  - `POST /api/auth/resend-verification` — 重新发送验证邮件
  - `GET  /api/auth/me` — 获取当前登录用户信息

### 商品/Phones & 评论/Reviews

- **商品**:
  - `GET  /api/phones` — 商品列表（支持 search/brand/maxPrice/special/sort/page/limit）
  - `GET  /api/phones/{id}` — 商品详情
  - `GET  /api/phones/by-seller/{sellerId}` — 某卖家的所有商品
  - `POST /api/phones` — 创建商品（需登录）
  - `PUT  /api/phones/{id}` — 更新商品（需卖家本人）
  - `DELETE /api/phones/{id}` — 删除商品
  - `PUT  /api/phones/{id}/disable` — 上下架商品

- **评论**:
  - `GET  /api/phones/{phoneId}/reviews` — 获取商品评论（带分页）
  - `POST /api/phones/{phoneId}/reviews` — 新增评论（需登录）
  - `PATCH /api/phones/{phoneId}/reviews/{reviewId}/visibility` — 切换评论可见性（作者/卖家）
  - `GET  /api/phones/reviews/by-seller` — 当前卖家收到的所有评论（需登录）

- **文件上传**:
  - `POST /api/upload/image` — 上传商品图片（multipart/form-data，需登录），返回 `/uploads/...` URL

### 购物车/Cart & 订单/Orders

- **购物车** (`/api/cart`):
  - `GET  /api/cart` — 获取当前用户购物车
  - `POST /api/cart` — 添加/更新商品（body 包含 phoneId, quantity）
  - `PUT  /api/cart/{phoneId}` — 更新购物车中某商品数量
  - `DELETE /api/cart/{phoneId}` — 删除购物车中某商品

- **订单** (`/api/orders`):
  - `POST /api/orders/checkout` — 下单（从购物车创建订单）
  - `GET  /api/orders` — 当前用户订单分页列表
  - `GET  /api/orders/user/{userId}` — 指定用户订单（带权限校验）
  - `GET  /api/orders/{orderId}` — 单个订单详情

### 用户资料/Profile & 收藏/Wishlist

- **个人资料** (`/api/profile`):
  - `GET  /api/profile` — 获取当前用户资料
  - `PUT  /api/profile` — 更新当前用户资料
  - `PUT  /api/profile/change-password` — 修改当前用户密码

- **收藏夹** (`/api/wishlist`):
  - `GET  /api/wishlist` — 获取收藏列表
  - `POST /api/wishlist` — 添加到收藏（body 包含 phoneId）
  - `DELETE /api/wishlist/{phoneId}` — 从收藏移除

### 管理员/Admin

所有管理员接口路径前缀为 `/api/admin`，需要 `ADMIN` 角色。

- **认证 & 基本信息**
  - `POST /api/admin/login` — 管理员登录
  - `GET  /api/admin/profile` — 获取管理员资料
  - `PUT  /api/admin/profile` — 更新管理员资料
  - `GET  /api/admin/stats` — 仪表盘聚合统计

- **用户管理**
  - `GET    /api/admin/users` — 用户列表（分页 + search + isDisabled）
  - `GET    /api/admin/users/{userId}` — 用户详情
  - `PUT    /api/admin/users/{userId}` — 更新用户信息
  - `PUT    /api/admin/users/{userId}/toggle-disabled` — 冻结/解冻用户
  - `DELETE /api/admin/users/{userId}` — 删除用户
  - `GET    /api/admin/users/{userId}/phones` — 指定用户的商品列表
  - `GET    /api/admin/users/{userId}/reviews` — 指定用户的评论列表

- **商品管理**
  - `GET    /api/admin/phones` — 商品列表（分页）
  - `PUT    /api/admin/phones/{phoneId}` — 更新商品
  - `PUT    /api/admin/phones/{phoneId}/toggle-disabled` — 上下架商品
  - `DELETE /api/admin/phones/{phoneId}` — 删除商品

- **评论管理**
  - `GET    /api/admin/reviews` — 评论列表（支持 visibility/reviewerId/phoneId/search/brand）
  - `GET    /api/admin/reviews/phones/{phoneId}` 或 `/api/admin/phones/{phoneId}/reviews` — 某商品的评论
  - `PUT    /api/admin/reviews/{phoneId}/{reviewId}/toggle-visibility` — 切换评论可见性
  - `DELETE /api/admin/reviews/{phoneId}/{reviewId}` — 删除评论

- **订单管理 & 日志**
  - `GET  /api/admin/orders` — 订单列表（userId/时间范围/searchTerm/brandFilter）
  - `GET  /api/admin/orders/{orderId}` — 订单详情
  - `GET  /api/admin/orders/stats` — 销售统计
  - `GET  /api/admin/orders/export` — 导出订单（csv/json）
  - `GET  /api/admin/logs` — 管理员操作日志

## 数据库概览

### MongoDB 集合

- `users` — 用户信息
- `phones` — 商品信息（内嵌 reviews）
- `carts` — 购物车（关联用户和商品）
- `orders` — 订单（含订单项与地址）
- `adminlogs` — 管理员操作日志

### 常用索引

- `users.email` — 用户邮箱唯一索引
- `users.firstName + lastName` — 组合文本搜索
- `phones.seller` — 按卖家查询
- `carts.userId` — 用户购物车查找
- `orders.userId` — 用户订单查找

## 测试与覆盖率

```bash
# 运行全部测试
mvn test

# 仅运行某个测试类
mvn test -Dtest=UserServiceTest

# 生成 Jacoco 覆盖率报告
mvn test jacoco:report
```

报告输出在 `target/site/jacoco/index.html`。

## 构建与部署

### 构建可执行 JAR

```bash
mvn clean package -DskipTests
```

生成的 JAR 位于 `target/spring-old-phone-deals-1.0.0.jar`。

### Docker 示例（可选）

```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/spring-old-phone-deals-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

构建并运行容器：

```bash
docker build -t old-phone-deals-backend .
docker run -p 8080:8080 --env-file .env old-phone-deals-backend
```

## 配置说明

### application.yml

集中管理：
- MongoDB 连接
- JWT 配置（密钥、过期时间）
- 邮件服务
- 日志
- CORS 允许的前端 URL

### application-dev.yml

开发环境特定配置：
- 本地端口（8080）
- 开发日志级别
- 本地上传目录

### application-prod.yml

生产环境推荐：
- 独立 MongoDB
- 安全的 JWT 密钥
- 日志与监控配置

## 开发规范

### 代码风格

- 使用 Lombok 简化样板代码
- 使用 MapStruct 进行 DTO 映射
- Controller 仅处理 HTTP 协议与输入输出，业务逻辑放在 Service
- Service 层保持单一职责

### Commit 信息

```text
feat(scope): 新功能说明
fix(scope): Bug 修复
docs(scope): 文档更新
refactor(scope): 重构（无功能变化）
test(scope): 测试相关
chore(scope): 构建/依赖/脚手架
```

示例：

```text
feat(auth): add JWT token refresh endpoint
fix(cart): fix stock validation in checkout
docs(api): update API documentation
```

## 安全注意事项

1. **密码存储**: 使用 BCrypt（至少 12 轮）
2. **JWT**: 使用 HS512 或更强算法，并保证密钥长度足够
3. **CORS**: 限制允许来源为前端真实域名
4. **错误信息**: 对外隐藏敏感异常细节
5. **数据库**: 使用最小权限账户连接 MongoDB
6. **XSS**: 前端模板默认转义，后端对富文本严格过滤
7. **CSRF**: 使用 JWT，无需表单 CSRF Token，但要防止跨站请求泄露 Token

## 常见问题 FAQ

### Q: JWT Token 无效怎么办？
A: 检查时间是否过期、签名密钥是否一致、Authorization 头是否正确。

### Q: MongoDB 连接失败？
A: 确认 MongoDB 已启动，URI 正确，并且账号密码无误（如有）。

### Q: 邮件发送失败？
A: 检查 SendGrid API Key 和 FROM_EMAIL，查看日志中的错误信息。

### Q: CORS 报错？
A: 确认 `frontend.url` 或 `FRONTEND_URL` 环境变量与前端实际访问 URL 一致。

## 相关文档

- **系统名称**: Old Phone Deals
- **架构设计**: 详见 [ARCHITECTURE_DESIGN.md](./ARCHITECTURE_DESIGN.md)
- **API 详情**: 可补充 [API_MAPPING.md](./API_MAPPING.md)

## 维护说明

欢迎在本项目基础上继续扩展功能，保持与前端（Angular 或 React 版本）API 一致。