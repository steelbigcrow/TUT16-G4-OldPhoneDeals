# RabbitMQ 集成改造规划

## 1. 改造背景

### 1.1 现状分析

当前 `spring-old-phone-deals` 后端存在以下问题：

**邮件发送服务：**
- `EmailServiceImpl` 使用 `@Async` + 本地线程池（core=2, max=5, queue=100）实现异步邮件发送
- 应用重启时，线程池中未完成的邮件任务会永久丢失
- SMTP 调用失败后没有重试机制，异常被静默吞掉
- `AuthServiceImpl` 中 6 处调用 EmailService，错误处理不一致（部分吞异常、部分抛 RuntimeException）

**订单结账流程：**
- `OrderServiceImpl.checkout()` 在单个 `@Transactional` 中同步执行 7 个步骤
- 库存扣减在循环中逐个调用 `phoneRepository.save()`，N 件商品 = N 次 DB 写入
- 缺少订单确认邮件和卖家通知功能
- 任何一步失败导致整个事务回滚，用户等待时间长

### 1.2 改造目标

| 目标 | 说明 |
|------|------|
| 可靠投递 | 邮件发送通过 MQ 持久化，保证不丢失 |
| 自动重试 | SMTP 失败后自动重试，超过阈值进入死信队列 |
| 响应解耦 | 订单结账的后置操作（邮件、通知、审计）异步化，缩短用户等待（库存扣减保留在同步核心路径） |
| 一致性保障 | 通过同步原子扣库存、消息幂等和补偿重发流程，避免重复消费与超卖 |
| 可扩展性 | 未来新增通知渠道（短信、推送）只需增加消费者 |

---

## 2. 技术选型

### 2.1 选择 RabbitMQ

| 对比维度 | RabbitMQ | Kafka |
|----------|----------|-------|
| 适用场景 | 任务队列、事件通知 | 高吞吐流式处理、日志聚合 |
| Spring 集成 | `spring-boot-starter-amqp` 开箱即用 | 需要额外配置 |
| 运维复杂度 | 低，单节点即可运行 | 高，依赖 ZooKeeper/KRaft |
| 消息模型 | 支持 Direct / Topic / Fanout Exchange | 仅 Topic 分区模型 |
| 本项目需求 | 邮件投递 + 订单事件，消息量中等 | 过度设计 |

结论：RabbitMQ 更适合本项目的规模和需求。

### 2.2 基础设施依赖

- RabbitMQ Server 3.12+（Docker 部署）
- Maven 依赖：`spring-boot-starter-amqp`
- 管理控制台：RabbitMQ Management Plugin（端口 15672）

---

## 3. 消息架构设计

### 3.1 Exchange 与 Queue 拓扑

本次改造定义两个 Topic Exchange，共 4 条队列：

```
┌─────────────────────────────────────────────────────────────────┐
│                        RabbitMQ Broker                          │
│                                                                 │
│  ┌──────────────────────┐     ┌───────────────────────────────┐ │
│  │ notification.exchange │     │     order.exchange            │ │
│  │   (Topic Exchange)    │     │     (Topic Exchange)          │ │
│  └──────┬───────┬───────┘     └──────┬──────────┬─────────────┘ │
│         │       │                    │          │               │
│    ┌────▼──┐ ┌──▼─────┐      ┌──────▼───┐ ┌───▼────────────┐  │
│    │ email │ │ email  │      │ order    │ │ order          │  │
│    │ .send │ │ .send  │      │ .post    │ │ .post          │  │
│    │       │ │ .dlq   │      │ .process │ │ .process.dlq   │  │
│    └───────┘ └────────┘      └──────────┘ └────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 队列定义明细

| Exchange | Routing Key | Queue | 用途 | 死信队列 |
|----------|-------------|-------|------|----------|
| `notification.exchange` | `email.send` | `email.send.queue` | 所有类型的邮件发送任务 | `email.send.dlq.queue` |
| `notification.exchange` | `email.send.dlq` | `email.send.dlq.queue` | 邮件发送失败消息堆积与人工处理 | `-` |
| `order.exchange` | `order.post.process` | `order.post.process.queue` | 订单创建后的后置处理 | `order.post.process.dlq.queue` |
| `order.exchange` | `order.post.process.dlq` | `order.post.process.dlq.queue` | 订单后置处理失败消息堆积与人工处理 | `-` |

### 3.3 消息体设计

**邮件消息（EmailMessage）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `messageId` | String(UUID) | 消息唯一 ID（用于幂等与链路追踪） |
| `type` | Enum | 邮件类型：`VERIFICATION` / `PASSWORD_RESET_LINK` / `PASSWORD_RESET_CODE` / `GENERIC` |
| `toEmail` | String | 收件人地址 |
| `userName` | String | 收件人姓名（用于模板渲染） |
| `token` | String | 验证令牌或重置令牌（按类型使用） |
| `subject` | String | 邮件主题（仅 GENERIC 类型使用） |
| `htmlContent` | String | HTML 正文（仅 GENERIC 类型使用） |
| `timestamp` | Instant | 消息创建时间（UTC） |

**订单后置处理消息（OrderPostProcessMessage）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `messageId` | String(UUID) | 消息唯一 ID（用于幂等与链路追踪） |
| `orderId` | String | 订单 ID |
| `userId` | String | 买家用户 ID |
| `items` | List | 订单商品列表（phoneId + quantity） |
| `totalAmount` | Double | 订单总金额（与当前 `Order.totalAmount` 字段保持一致，后续可统一升级为 BigDecimal） |
| `timestamp` | Instant | 消息创建时间（UTC） |

### 3.4 重试与死信策略

| 参数 | 邮件队列 | 订单后置队列 |
|------|----------|-------------|
| 最大重试次数 | 3 次 | 3 次 |
| 重试间隔 | 5s → 15s → 45s（指数退避） | 2s → 6s → 18s（指数退避） |
| 死信路由键 | `email.send.dlq` | `order.post.process.dlq` |
| 主队列 TTL | 不配置短 TTL（避免消费者故障时消息自然过期） | 不配置短 TTL（避免后置任务丢失） |
| DLQ 保留期 | 7 天（便于排障与补偿） | 7 天（便于排障与补偿） |
| 确认模式 | 手动 ACK（消费成功后确认） | 手动 ACK |
| 重回队列策略 | `defaultRequeueRejected=false`（避免失败消息无限重回主队列） | `defaultRequeueRejected=false` |

重试策略通过 Spring AMQP 的 `RetryInterceptor` 实现，配合 `x-dead-letter-exchange` 和 `x-dead-letter-routing-key` 参数声明死信路由。  
邮件与订单队列的退避参数不同，需分别配置 `emailListenerContainerFactory` 与 `orderPostProcessListenerContainerFactory`，不要共用同一个默认 listener factory。

---

## 4. 邮件发送服务改造设计

### 4.1 改造前后对比

**改造前调用链：**

```
AuthServiceImpl ──直接调用──▶ EmailServiceImpl(@Async) ──同步──▶ SMTP Server
                                    │
                                    ▼
                              失败则静默丢失
```

**改造后调用链：**

```
AuthServiceImpl ──发布消息──▶ RabbitMQ (email.send.queue)
                                    │
                                    ▼ 消费者拉取
                             EmailConsumer ──同步──▶ SMTP Server
                                    │
                                    ├── 成功 → ACK
                                    └── 失败 → 重试 → 超限 → DLQ
```

### 4.2 涉及的文件与改动说明

| 文件 | 改动类型 | 说明 |
|------|----------|------|
| `config/RabbitMQConfig.java` | 新增 | 声明 Exchange、Queue、Binding、死信队列、JSON 序列化器 |
| `dto/message/EmailMessage.java` | 新增 | 邮件消息 DTO，包含 type/toEmail/userName/token 等字段 |
| `enums/EmailType.java` | 新增 | 邮件类型枚举 |
| `producer/EmailMessageProducer.java` | 新增 | 消息生产者，封装 `RabbitTemplate.convertAndSend()` |
| `consumer/EmailConsumer.java` | 新增 | 消息消费者，监听 `email.send.queue`，调用 SMTP 发送 |
| `service/impl/EmailServiceImpl.java` | 修改 | 移除 `@Async`，改为调用 `EmailMessageProducer` 发布消息 |
| `config/AsyncConfig.java` | 修改 | 移除邮件线程池（不再需要） |
| `service/impl/AuthServiceImpl.java` | 修改 | 收敛重复 try-catch，按业务场景统一邮件投递失败策略（记录告警或抛业务异常） |

### 4.3 设计要点

**生产者端（EmailServiceImpl 改造）：**

- `EmailServiceImpl` 不再直接调用 `JavaMailSender`，而是构造 `EmailMessage` 对象并通过 `EmailMessageProducer` 发布到 MQ
- 移除所有 `@Async` 注解——异步性由 MQ 天然提供
- `EmailService` 接口签名保持不变，调用方（AuthServiceImpl）无需感知底层变化
- HTML 模板渲染逻辑（`buildVerificationEmailContent` 等）迁移到消费者端，生产者只传递模板参数
- 生产者启用 Publisher Confirm + Return（`confirm-type=correlated`、`mandatory=true`），确保消息未入队时可感知并处理

**消费者端（EmailConsumer）：**

- 使用 `@RabbitListener` 监听 `email.send.queue`
- 根据 `EmailMessage.type` 分发到对应的模板渲染 + SMTP 发送逻辑
- 采用手动 ACK 模式：SMTP 发送成功后才确认消息
- 消费失败时抛出异常，触发 Spring AMQP 的重试拦截器

**AuthServiceImpl 简化：**

- 移除 6 处重复的 try-catch 模式，统一异常处理风格
- 按业务场景定义失败语义：注册/通知类可记录告警并返回成功，关键链路（如必须立即反馈的操作）抛业务异常
- 发布到 MQ 通常是毫秒级操作，但需配置连接与发送超时，避免 RabbitMQ 不可用时拖慢请求

---

## 5. 订单结账流程改造设计

### 5.1 改造前后对比

**改造前（全同步）：**

```
用户请求 checkout
    │
    ▼
┌─────────────────────────────────────────────┐
│            单个 @Transactional               │
│                                             │
│  1. 获取购物车                               │
│  2. 验证库存（循环每件商品）                   │
│  3. 计算总价                                 │
│  4. 创建订单对象                              │
│  5. 保存订单到 DB                             │
│  6. 循环扣减库存 + 更新销量（N 次 DB 写入）     │
│  7. 清空购物车                               │
│                                             │
└─────────────────────────────────────────────┘
    │
    ▼
  返回响应（用户等待全部完成）
```

**改造后（核心同步 + 后置异步）：**

```
用户请求 checkout
    │
    ▼
┌──────────────────────────────┐
│      同步事务（核心路径）       │
│                              │
│  1. 获取购物车                │
│  2. 验证库存                  │
│  3. 原子扣减库存 + 更新销量     │
│  4. 计算总价                  │
│  5. 创建并保存订单             │
│  6. 清空购物车                │
│                              │
└──────────┬───────────────────┘
           │
    ┌──────▼──────┐
    │ 发布消息到   │
    │ order.post  │
    │ .process    │
    │ .queue      │
    └──────┬──────┘
           │
    返回响应（用户无需等待后置操作）
           │
           ▼ (异步消费)
┌──────────────────────────────┐
│    OrderPostProcessConsumer   │
│                              │
│  A. 发送订单确认邮件（可选）   │
│  B. 发送卖家通知（可选）       │
│  C. 更新后置处理状态/审计日志   │
│                              │
└──────────────────────────────┘
```

### 5.2 涉及的文件与改动说明

| 文件 | 改动类型 | 说明 |
|------|----------|------|
| `dto/message/OrderPostProcessMessage.java` | 新增 | 订单后置处理消息 DTO |
| `producer/OrderMessageProducer.java` | 新增 | 订单消息生产者 |
| `consumer/OrderPostProcessConsumer.java` | 新增 | 订单后置处理消费者（幂等判重 + 通知发送 + 状态更新） |
| `repository/custom/PhoneStockRepository.java` | 新增 | 原子扣库存仓储接口（条件更新：`stock >= quantity`） |
| `repository/custom/impl/PhoneStockRepositoryImpl.java` | 新增 | 基于 `MongoTemplate` 的原子扣库存实现 |
| `entity/ProcessedMessage.java` | 新增 | 消息幂等记录（`messageId` 唯一索引） |
| `repository/ProcessedMessageRepository.java` | 新增 | 幂等记录读写 |
| `config/RabbitMQConfig.java` | 修改 | 追加 order.exchange 和相关队列声明 |
| `service/impl/OrderServiceImpl.java` | 修改 | checkout() 保留同步扣库存，后置操作异步化 |
| `entity/Order.java` | 修改 | 增加后置处理状态字段，支撑补偿与审计 |

### 5.3 设计要点

**同步核心路径（OrderServiceImpl.checkout 改造）：**

- 获取购物车、校验商品可售后，执行同步原子扣库存（`stock >= quantity` 条件更新）并同步更新 `salesCount`
- 原子扣库存由自定义仓储（`MongoTemplate` + `findAndModify/updateFirst`）实现，不依赖逐条 `phoneRepository.save()`
- 若任一商品扣减失败（并发售罄），立即抛出业务异常，订单不创建
- 在库存扣减成功后再创建订单，订单初始状态设为 `postProcessStatus=PENDING`
- 购物车清空仍在同步事务中完成，防止用户重复提交
- 订单保存成功后，通过 `OrderMessageProducer` 发布 `OrderPostProcessMessage` 到 MQ，并通过 publisher confirm 确认已被 broker 接收
- 若发布失败，订单保持 `PENDING` 并记录 `postProcessError`，由补偿任务定时重发（避免“DB 成功但 MQ 丢事件”）
- 返回 `OrderResponse` 给用户时，库存已被同步扣减，不会出现“下单成功但后续扣库失败”

**异步后置处理（OrderPostProcessConsumer）：**

- 监听 `order.post.process.queue`，接收 `OrderPostProcessMessage`
- 先基于 `messageId` 做幂等判重（`processed_messages` 集合唯一索引），重复消息直接 ACK
- 执行通知任务：发送订单确认邮件（构造 `EmailMessage` 并发布到 `email.send.queue`）和卖家通知（可选）
- 完成后更新 `Order.postProcessStatus=SUCCESS`；失败时记录错误并按重试策略处理
- 采用手动 ACK：仅在后置任务和状态更新成功后确认消息

**一致性保障：**

- 库存扣减位于同步核心路径，避免“已返回成功但库存未锁定”的一致性问题
- 消息体包含 `messageId`，消费者必须幂等处理，避免重复发送通知
- 生产者启用 Confirm/Return，消费者使用手动 ACK + 有限重试，避免“无限重试风暴”
- 若发布失败或重试耗尽，订单保留 `PENDING/FAILED` 状态并触发补偿重发；必要时落入 DLQ 供人工处理

---

## 6. 配置设计

### 6.1 application.yml 新增配置项

各环境（dev / prod / test）需新增 RabbitMQ 连接配置：

| 配置项 | dev 环境 | prod 环境 |
|--------|----------|-----------|
| `spring.rabbitmq.host` | `localhost` | `${RABBITMQ_HOST}` |
| `spring.rabbitmq.port` | `5672` | `${RABBITMQ_PORT:5672}` |
| `spring.rabbitmq.virtual-host` | `/` | `${RABBITMQ_VHOST:/}` |
| `spring.rabbitmq.username` | `guest` | `${RABBITMQ_USERNAME}` |
| `spring.rabbitmq.password` | `guest` | `${RABBITMQ_PASSWORD}` |
| `spring.rabbitmq.publisher-confirm-type` | `correlated` | `correlated` |
| `spring.rabbitmq.publisher-returns` | `true` | `true` |
| `spring.rabbitmq.template.mandatory` | `true` | `true` |
| `spring.rabbitmq.listener.simple.acknowledge-mode` | `manual` | `manual` |
| `spring.rabbitmq.listener.simple.default-requeue-rejected` | `false` | `false` |
| `spring.rabbitmq.listener.simple.retry.enabled` | `true` | `true` |
| `spring.rabbitmq.listener.simple.retry.max-attempts` | `3` | `3` |
| `spring.rabbitmq.listener.simple.retry.initial-interval` | `5000` | `5000` |
| `spring.rabbitmq.listener.simple.retry.max-interval` | `45000` | `45000` |
| `spring.rabbitmq.listener.simple.retry.multiplier` | `3.0` | `3.0` |

说明：以上为全局默认值。若邮件队列与订单队列使用不同退避参数（如 5s 与 2s 起步），需在 `RabbitMQConfig` 中为不同 listener 配置独立的 `RetryInterceptor` 或 `ListenerContainerFactory`。

补充：为保证 `@Transactional` 的真实原子性，MongoDB 需运行在副本集模式（Replica Set）。若开发环境为单机非副本集，本项目会降级为无资源事务管理器，应开启“发布失败补偿重发”与“库存扣减失败回滚补偿”保护流程。

### 6.2 .env 新增变量

`.env.example` 中需补充：

```
# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_VHOST=/
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

### 6.3 Docker Compose（开发环境）

在项目根目录的 `docker-compose.yml` 中新增 RabbitMQ 服务：

- 镜像：`rabbitmq:3.12-management`
- 端口映射：`5672:5672`（AMQP）、`15672:15672`（管理控制台）
- 持久化：挂载 volume 到 `/var/lib/rabbitmq`

---

## 7. 新增文件清单与包结构

### 7.1 新增文件

```
com.oldphonedeals/
├── config/
│   └── RabbitMQConfig.java              # Exchange、Queue、Binding、MessageConverter 声明
├── enums/
│   └── EmailType.java                   # VERIFICATION / PASSWORD_RESET_LINK / PASSWORD_RESET_CODE / GENERIC
├── dto/
│   └── message/
│       ├── EmailMessage.java            # 邮件消息体
│       └── OrderPostProcessMessage.java # 订单后置处理消息体
├── entity/
│   └── ProcessedMessage.java            # 消息幂等记录（messageId 唯一）
├── producer/
│   ├── EmailMessageProducer.java        # 邮件消息生产者
│   └── OrderMessageProducer.java        # 订单消息生产者
├── repository/
│   ├── ProcessedMessageRepository.java  # 幂等记录仓储
│   └── custom/
│       ├── PhoneStockRepository.java    # 原子扣库存接口
│       └── impl/
│           └── PhoneStockRepositoryImpl.java # MongoTemplate 原子扣库存实现
└── consumer/
    ├── EmailConsumer.java               # 邮件消费者（@RabbitListener）
    └── OrderPostProcessConsumer.java    # 订单后置处理消费者（@RabbitListener）
```

### 7.2 修改文件

| 文件 | 改动概述 |
|------|----------|
| `pom.xml` | 新增 `spring-boot-starter-amqp` 依赖 |
| `config/AsyncConfig.java` | 移除邮件专用线程池配置（若无其他 @Async 用途可整体移除） |
| `service/impl/EmailServiceImpl.java` | 移除 `@Async`，内部改为构造消息对象并调用 Producer 发布 |
| `service/impl/AuthServiceImpl.java` | 收敛重复 try-catch，按业务场景统一投递失败处理策略 |
| `service/impl/OrderServiceImpl.java` | checkout() 改为“同步原子扣库存 + 发布后置处理消息” |
| `repository/PhoneRepository.java` | 保留查询接口；库存扣减改由自定义仓储实现 |
| `entity/Order.java` | 新增后置处理状态字段（如 `postProcessStatus` / `postProcessError`）便于补偿追踪 |
| `application.yml` | 新增 RabbitMQ 连接配置 |
| `application-dev.yml` | 新增 dev 环境 RabbitMQ 配置 |
| `application-prod.yml` | 新增 prod 环境 RabbitMQ 配置 |
| `src/test/resources/application-test.yml` | 新增 test 环境 RabbitMQ 配置（可使用 Testcontainers） |
| `.env.example` | 补充 RABBITMQ_* 环境变量说明 |

---

## 8. 测试策略

### 8.1 单元测试

| 测试对象 | 测试方法 |
|----------|----------|
| `EmailMessageProducer` | Mock `RabbitTemplate`，验证 `convertAndSend()` 被调用且参数正确 |
| `OrderMessageProducer` | Mock `RabbitTemplate`，验证消息体序列化正确，并验证 confirm/nack 回调处理 |
| `EmailConsumer` | Mock `JavaMailSender`，验证不同 EmailType 分发到正确的模板渲染逻辑 |
| `OrderPostProcessConsumer` | Mock `ProcessedMessageRepository`/`OrderRepository`，验证幂等判重、后置通知与状态更新 |
| `EmailServiceImpl`（改造后） | 验证调用 Producer 而非直接调用 SMTP |
| `OrderServiceImpl.checkout()`（改造后） | 验证同步原子扣库存成功后才创建订单并发布消息；发布失败时写入待补偿状态 |

### 8.2 集成测试

| 测试场景 | 方法 |
|----------|------|
| 邮件队列端到端 | 使用 Testcontainers 启动 RabbitMQ 容器，发布邮件消息后验证消费者收到并处理 |
| 订单队列端到端 | 下单后验证库存已同步扣减；发布后置消息后验证订单状态由 `PENDING` 变更为 `SUCCESS` |
| 死信队列验证 | 模拟消费者持续失败，验证消息在重试耗尽后进入 DLQ |
| 发布确认验证 | 模拟 exchange/route 不存在，验证生产者能感知 Nack/Return 并触发错误处理 |
| 消息幂等性 | 重复发送相同后置消息，验证通知不会重复发送且状态不被重复更新 |

---

## 9. 实施步骤

分两个阶段实施，每个阶段独立可交付、可验证。

### 阶段一：基础设施 + 邮件服务改造

| 步骤 | 内容 |
|------|------|
| 1 | `pom.xml` 新增 `spring-boot-starter-amqp` 依赖 |
| 2 | 新增 `RabbitMQConfig.java`，声明 notification.exchange、email.send.queue、死信队列与 listener 重试策略 |
| 3 | 新增 `EmailType` 枚举和 `EmailMessage` 消息 DTO |
| 4 | 新增 `EmailMessageProducer`，封装消息发布逻辑 |
| 5 | 新增 `EmailConsumer`，实现邮件消费（模板渲染 + SMTP 发送） |
| 6 | 改造 `EmailServiceImpl`：移除 `@Async`，改为调用 Producer |
| 7 | 改造 `AuthServiceImpl`：收敛 try-catch 并统一投递失败策略 |
| 8 | 更新配置文件（application.yml 各环境 + .env.example） |
| 9 | 编写单元测试和集成测试 |

验收标准：注册、密码重置等流程的邮件通过 RabbitMQ 异步发送，RabbitMQ Management 控制台可观察到消息流转。

### 阶段二：订单结账流程改造

| 步骤 | 内容 |
|------|------|
| 1 | 新增 `OrderPostProcessMessage` 消息 DTO |
| 2 | 新增 `OrderMessageProducer`，封装订单消息发布 |
| 3 | 新增 `PhoneStockRepository`（MongoTemplate 原子扣库存）与 `OrderPostProcessConsumer`（幂等判重 + 后置通知） |
| 4 | 更新 `RabbitMQConfig.java`，追加 order.exchange、相关队列及独立 listener 重试策略 |
| 5 | 改造 `OrderServiceImpl.checkout()`：同步扣库存、记录 `postProcessStatus` 并发布后置消息 |
| 6 | 新增补偿重发机制（扫描 `PENDING/FAILED` 订单重发）并编写单元/集成测试 |

验收标准：结账接口在库存同步扣减后返回，后置通知通过 MQ 异步完成，失败消息可在状态字段与 DLQ 中定位并补偿。
