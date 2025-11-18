# Spring Old Phone Deals - 后端服务

基于 Spring Boot 3.x 的二手手机交易平台后端服务，使用 MongoDB 数据库和 JWT 认证。

## 📋 技术栈

- **框架**: Spring Boot 3.2.0
- **JDK**: 17
- **数据库**: MongoDB
- **安全**: Spring Security + JWT
- **构建工具**: Maven
- **其他依赖**:
  - Spring Data MongoDB
  - Spring Boot Validation
  - Spring Boot Mail (SendGrid)
  - Lombok
  - MapStruct
  - jjwt (JWT库)

## 🚀 快速开始

### 前置要求

- JDK 17（项目根目录已配置JDK 17工具链）
- MongoDB 4.0+
- Maven 3.6+
- SendGrid API Key（用于邮件发送）

### 1. 克隆项目

```bash
cd spring-old-phone-deals
```

### 2. 配置环境变量

在项目根目录创建 `.env` 文件（或配置系统环境变量）：

```env
# MongoDB配置
MONGODB_URI=mongodb://localhost:27017/oldphonedeals

# JWT配置
JWT_SECRET=your-super-secret-jwt-key-min-256-bits

# SendGrid邮件配置
SENDGRID_API_KEY=your-sendgrid-api-key
FROM_EMAIL=noreply@oldphonedeals.com

# 前端URL（用于邮件链接）
FRONTEND_URL=http://localhost:4200

# 运行环境（development/production）
NODE_ENV=development
```

**安全提示**: 
- `.env` 文件已添加到 `.gitignore`，请勿提交到版本控制
- 生产环境请使用强密码和安全的密钥

### 3. 安装依赖

```bash
mvn clean install
```

### 4. 运行应用

```bash
# 使用Maven运行
mvn spring-boot:run

# 或者使用开发环境配置
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或者打包后运行
mvn package
java -jar target/spring-old-phone-deals-1.0.0.jar
```

应用将在 `http://localhost:3000` 启动。

### 5. 验证运行

访问测试端点：
```bash
curl http://localhost:3000/api/test
```

## 📁 项目结构

```
src/main/java/com/oldphonedeals/
├── OldPhoneDealsApplication.java  # 启动类
├── config/                        # 配置类
│   ├── SecurityConfig.java        # Spring Security配置
│   ├── CorsConfig.java            # CORS配置
│   └── ...
├── entity/                        # 实体类（MongoDB文档）
│   ├── User.java
│   ├── Phone.java
│   └── ...
├── dto/                           # 数据传输对象
│   ├── request/                   # 请求DTO
│   └── response/                  # 响应DTO
├── repository/                    # 数据访问层
├── service/                       # 业务逻辑层
│   └── impl/                      # 实现类
├── controller/                    # REST控制器
│   └── admin/                     # 管理员控制器
├── security/                      # 安全相关
│   ├── JwtAuthenticationFilter.java
│   └── JwtTokenProvider.java
├── exception/                     # 异常处理
├── mapper/                        # MapStruct映射器
└── util/                          # 工具类
```

## 🔐 API认证

所有需要认证的端点都需要在请求头中携带 JWT Token：

```
Authorization: Bearer <your-jwt-token>
```

### 获取Token

```bash
curl -X POST http://localhost:3000/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

响应：
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

## 📡 API端点

详细的API端点映射请查看 [API_MAPPING.md](./API_MAPPING.md)

### 主要端点分组

- **认证**: `/api/login`, `/api/register`, `/api/verify-email`
- **商品**: `/api/phones`, `/api/phones/{id}`
- **评论**: `/api/phones/{id}/reviews`
- **购物车**: `/api/cart`, `/api/cart/items`
- **订单**: `/api/orders`, `/api/orders/{id}`
- **收藏夹**: `/api/wishlist`
- **用户资料**: `/api/profile`
- **管理员**: `/api/admin/*`

## 🗄️ 数据库

### MongoDB集合

- `users` - 用户信息
- `phones` - 商品信息（包含嵌套的reviews数组）
- `carts` - 购物车（包含嵌套的items数组）
- `orders` - 订单（包含嵌套的items数组和address对象）
- `adminlogs` - 管理员操作日志

### 索引策略

- `users.email` - 唯一索引
- `users.firstName + lastName` - 复合索引
- `phones.seller` - 索引
- `carts.userId` - 唯一索引
- `orders.userId` - 索引

## 🧪 测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=UserServiceTest

# 生成测试报告
mvn test jacoco:report
```

测试覆盖率报告位于 `target/site/jacoco/index.html`

## 📦 打包部署

### 构建JAR包

```bash
mvn clean package -DskipTests
```

生成的JAR文件位于 `target/spring-old-phone-deals-1.0.0.jar`

### Docker部署（可选）

```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/spring-old-phone-deals-1.0.0.jar app.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

构建和运行：
```bash
docker build -t old-phone-deals-backend .
docker run -p 3000:3000 --env-file .env old-phone-deals-backend
```

## 🔧 配置说明

### application.yml

主配置文件，包含以下配置：
- MongoDB连接
- JWT配置（密钥、过期时间）
- 文件上传配置
- 邮件配置
- CORS配置

### application-dev.yml

开发环境配置：
- 启用热重载
- 详细日志输出
- 错误堆栈跟踪

### application-prod.yml

生产环境配置：
- 生产数据库连接
- 最小日志输出
- 安全加固

## 📝 开发规范

### 代码风格

- 使用Lombok减少样板代码
- 使用MapStruct进行DTO映射
- 遵循RESTful API设计规范
- 所有Service方法需要事务管理
- Controller只处理HTTP层，不包含业务逻辑

### 提交规范

```
feat(scope): 添加新功能
fix(scope): 修复Bug
docs(scope): 文档更新
refactor(scope): 重构代码
test(scope): 添加测试
chore(scope): 构建/工具链更新
```

示例：
```
feat(auth): add JWT token refresh endpoint
fix(cart): fix stock validation in checkout
docs(api): update API documentation
```

## 🔒 安全注意事项

1. **密码安全**: 使用BCrypt（12轮）加密存储
2. **JWT安全**: 使用HS512算法，密钥长度至少256位
3. **CORS配置**: 生产环境限制允许的域名
4. **文件上传**: 验证文件类型和大小，防止恶意文件
5. **SQL注入**: 使用Spring Data MongoDB的参数化查询
6. **XSS防护**: 前端使用Angular内置的XSS防护
7. **CSRF防护**: 使用JWT Token，禁用Spring Security的CSRF

## 🐛 常见问题

### Q: JWT Token过期怎么办？
A: 前端需要重新登录获取新Token。后续可以实现Token刷新机制。

### Q: MongoDB连接失败？
A: 检查MongoDB服务是否启动，URI配置是否正确。

### Q: 邮件发送失败？
A: 检查SendGrid API Key是否有效，FROM_EMAIL是否已验证。

### Q: 文件上传报错？
A: 检查上传目录是否存在且有写入权限。

### Q: 跨域问题？
A: 检查CorsConfig配置，确保前端URL在允许列表中。

## 📞 联系方式

- **项目**: Old Phone Deals
- **文档**: 查看 [ARCHITECTURE_DESIGN.md](./ARCHITECTURE_DESIGN.md)
- **API映射**: 查看 [API_MAPPING.md](./API_MAPPING.md)

## 📄 许可证

本项目仅用于学习目的。