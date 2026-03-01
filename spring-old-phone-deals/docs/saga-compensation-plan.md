# Saga 自动业务补偿机制规划

## 1. 改造背景

### 1.1 现状分析

当前订单后置处理流程（`OrderPostProcessConsumer`）的失败处理策略为：

- 消费者重试 3 次（指数退避：2s → 6s → 18s）
- 重试耗尽后消息进入 DLQ（`order.post.process.dlq.queue`）
- 同时将订单的 `postProcessStatus` 标记为 `FAILED`，并记录 `postProcessError`
- **后续依赖人工介入**：运维人员需手动查看 DLQ 消息、定位问题、修复后重投消息

这种模式存在以下问题：

| 问题 | 影响 |
|------|------|
| 人工补偿响应慢 | 订单失败后可能数小时甚至数天才被处理，用户体验差 |
| 库存不一致 | 库存已在同步路径中扣减，但订单后置处理失败后库存未回滚，导致"幽灵锁库" |
| 缺乏自动回滚 | 没有自动化的补偿逻辑来恢复库存、通知用户、清理中间状态 |
| 运维负担重 | 每次失败都需要人工判断补偿策略，容易出错 |
| 无补偿审计 | 缺少补偿操作的完整记录，难以追溯和复盘 |

### 1.2 改造目标

| 目标 | 说明 |
|------|------|
| 自动补偿 | 订单后置处理最终失败后，系统自动触发补偿流程（回滚库存、更新订单状态、通知用户） |
| 补偿可追溯 | 每次补偿操作记录完整的审计日志（SagaLog），支持查询和复盘 |
| 补偿幂等 | 补偿操作本身支持幂等，避免重复补偿导致数据错乱 |
| 渐进式降级 | 补偿失败后仍进入 DLQ，但已大幅减少需要人工介入的场景 |
| 可观测性 | 补偿流程的每个步骤都有状态追踪和日志记录 |

---

## 2. Saga 模式选型

### 2.1 编排式（Orchestration）vs 协同式（Choreography）

| 对比维度 | 编排式 Saga | 协同式 Saga |
|----------|------------|------------|
| 协调方式 | 中央编排器（Orchestrator）统一调度各步骤 | 各服务通过事件自行响应，无中央协调 |
| 流程可见性 | 高——编排器持有完整状态机，流程一目了然 | 低——逻辑分散在各消费者中，需跨服务拼凑全貌 |
| 补偿控制 | 编排器按逆序精确触发补偿步骤 | 各服务自行监听失败事件并补偿，顺序难以保证 |
| 复杂度 | 编排器本身有一定复杂度，但整体可控 | 服务少时简单，服务多时事件链路爆炸式增长 |
| 适用场景 | 步骤间有依赖、需要精确补偿顺序 | 步骤完全独立、无顺序依赖 |

### 2.2 选择：编排式 Saga

本项目选择**编排式 Saga**，理由：

1. 订单结账的后置处理步骤之间存在逻辑依赖（如：必须先确认订单状态再发通知）
2. 补偿操作需要严格的逆序执行（先回滚库存，再更新订单状态，最后通知用户）
3. 编排器集中管理状态机，便于调试、监控和审计
4. 当前系统为单体应用（Monolith），编排器可以直接调用本地 Service，无需跨服务通信

---

## 3. 整体架构设计

### 3.1 改造前后对比

**改造前（DLQ + 人工补偿）：**

```
订单后置处理失败
    │
    ▼
重试 3 次（2s → 6s → 18s）
    │
    ▼ 全部失败
┌──────────────────────────────┐
│  1. 消息进入 DLQ              │
│  2. 订单标记 postProcessStatus │
│     = FAILED                  │
│  3. 库存已扣减但未回滚         │
│  4. 等待人工介入               │
└──────────────────────────────┘
```

**改造后（Saga 自动补偿）：**

```
订单后置处理失败
    │
    ▼
重试 3 次（2s → 6s → 18s）
    │
    ▼ 全部失败
┌──────────────────────────────────────────┐
│  发布补偿消息（routingKey=order.compensation） │
└──────────┬───────────────────────────────┘
           │
           ▼ (异步消费)
┌──────────────────────────────────────────┐
│       SagaCompensationOrchestrator       │
│                                          │
│  Step 1: 创建 SagaLog（STARTED）          │
│  Step 2: 回滚库存（逐商品原子加回）        │
│  Step 3: 更新订单状态（COMPENSATED）       │
│  Step 4: 发送补偿通知邮件给用户            │
│  Step 5: 更新 SagaLog（COMPLETED）        │
│                                          │
│  任一步骤失败：                            │
│    → 记录失败步骤到 SagaLog               │
│    → 重试补偿消息                         │
│    → 最终失败进入补偿 DLQ                  │
└──────────────────────────────────────────┘
```

### 3.2 消息队列拓扑扩展

在现有 RabbitMQ 拓扑基础上，新增补偿专用队列：

```
┌─────────────────────────────────────────────────────────────────────┐
│                          RabbitMQ Broker                            │
│                                                                     │
│  ┌──────────────────────┐     ┌───────────────────────────────────┐ │
│  │   order.exchange      │     │   compensation.exchange           │ │
│  │   (Topic Exchange)    │     │   (Topic Exchange) [新增]         │ │
│  └──────┬──────┬─────────┘     └──────┬──────────┬────────────────┘ │
│         │      │                      │          │                  │
│   ┌─────▼──┐ ┌─▼──────────┐   ┌──────▼───┐ ┌───▼──────────────┐   │
│   │ order  │ │ order      │   │ order    │ │ order            │   │
│   │ .post  │ │ .post      │   │ .compen- │ │ .compensation    │   │
│   │ .proc  │ │ .proc.dlq  │   │ sation   │ │ .dlq             │   │
│   └────────┘ └────────────┘   └──────────┘ └──────────────────┘   │
│                                  [新增]         [新增]              │
└─────────────────────────────────────────────────────────────────────┘
```

| Exchange | Routing Key | Queue | 用途 |
|----------|-------------|-------|------|
| `compensation.exchange` [新增] | `order.compensation` | `order.compensation.queue` | 接收补偿触发消息 |
| `compensation.exchange` [新增] | `order.compensation.dlq` | `order.compensation.dlq.queue` | 补偿最终失败的消息堆积 |

### 3.3 补偿消息体设计

**OrderCompensationMessage：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `sagaId` | String(UUID) | Saga 实例唯一 ID，贯穿整个补偿流程 |
| `orderId` | String | 需要补偿的订单 ID |
| `userId` | String | 买家用户 ID |
| `items` | List | 需要回滚库存的商品列表（phoneId + quantity） |
| `totalAmount` | Double | 订单总金额 |
| `reason` | String | 触发补偿的原因（原始失败错误信息） |
| `timestamp` | Instant | 补偿消息创建时间（UTC） |

---

## 4. SagaLog 实体设计

### 4.1 SagaLog 数据模型

`SagaLog` 是补偿流程的核心审计实体，记录每次 Saga 补偿的完整生命周期。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | String | MongoDB 自动生成的文档 ID |
| `sagaId` | String(UUID) | Saga 实例唯一 ID（唯一索引，用于幂等） |
| `orderId` | String | 关联的订单 ID |
| `userId` | String | 关联的用户 ID |
| `status` | SagaStatus 枚举 | 当前状态（见下方状态机） |
| `steps` | List&lt;SagaStep&gt; | 已执行的补偿步骤列表 |
| `reason` | String | 触发补偿的原因 |
| `startedAt` | Instant | Saga 开始时间 |
| `completedAt` | Instant | Saga 完成/失败时间 |

**SagaStep 嵌套对象：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `stepName` | String | 步骤名称（如 `RESTORE_STOCK`、`UPDATE_ORDER`、`SEND_NOTIFICATION`） |
| `status` | StepStatus 枚举 | `SUCCESS` / `FAILED` / `SKIPPED` |
| `executedAt` | Instant | 步骤执行时间 |
| `errorMessage` | String | 失败时的错误信息（成功时为 null） |

### 4.2 SagaStatus 状态机

```
STARTED ──────▶ COMPLETED
   │
   │ (任一步骤失败且重试未耗尽)
   ▼
STARTED ──────▶ RETRYING
                   │
                   ├── 重试成功 ──▶ COMPLETED
                   │
                   └── 重试耗尽 ──▶ FAILED
```

| 状态 | 说明 |
|------|------|
| `STARTED` | Saga 已创建，补偿步骤开始执行 |
| `RETRYING` | 补偿步骤执行失败后进入重试中 |
| `COMPLETED` | 所有补偿步骤执行成功 |
| `FAILED` | 补偿重试耗尽，需人工介入（消息进入补偿 DLQ） |

### 4.3 OrderPostProcessStatus 枚举扩展

在现有枚举基础上新增 `COMPENSATING`、`COMPENSATED` 状态：

| 状态 | 说明 | 是否已有 |
|------|------|----------|
| `PENDING` | 后置处理等待中 | 已有 |
| `SUCCESS` | 后置处理成功 | 已有 |
| `FAILED` | 后置处理失败（等待补偿） | 已有 |
| `COMPENSATING` [新增] | 补偿进行中 | 新增 |
| `COMPENSATED` [新增] | 补偿完成，库存已回滚 | 新增 |

---

## 5. 补偿编排器详细设计

### 5.1 触发时机

当 `OrderPostProcessConsumer` 重试耗尽后，不再仅仅将消息 NACK 到 DLQ，而是：

1. 将订单 `postProcessStatus` 标记为 `COMPENSATING`
2. 构建 `OrderCompensationMessage`，发布到 `compensation.exchange`（routingKey=`order.compensation`）
3. 原始消息仍然 NACK（不 requeue），保留 DLQ 中的原始失败记录用于审计

### 5.2 编排器执行流程

`SagaCompensationOrchestrator` 监听 `order.compensation.queue`，按以下顺序执行补偿步骤：

```
收到 OrderCompensationMessage
    │
    ▼
Step 1: 幂等检查
    │  查询 SagaLog 是否已存在该 sagaId
    │  ├── 已存在且 COMPLETED → 直接 ACK，跳过
    │  └── 不存在或 STARTED / RETRYING / FAILED → 继续执行
    │
    ▼
Step 2: 创建/更新 SagaLog（首次为 STARTED，重试为 RETRYING）
    │
    ▼
Step 3: 回滚库存（RESTORE_STOCK）
    │  遍历 items，逐商品调用 PhoneStockRepository
    │  原子增加 stock、减少 salesCount
    │  ├── 成功 → 记录 step SUCCESS
    │  └── 失败 → 记录 step FAILED，抛异常触发重试
    │
    ▼
Step 4: 更新订单状态（UPDATE_ORDER）
    │  将 Order.postProcessStatus 设为 COMPENSATED
    │  将 Order.checkoutStatus 设为 FAILED
    │  记录 checkoutError = "Order compensated: " + reason
    │  ├── 成功 → 记录 step SUCCESS
    │  └── 失败 → 记录 step FAILED，抛异常触发重试
    │
    ▼
Step 5: 发送补偿通知（SEND_NOTIFICATION）
    │  通过 EmailMessageProducer 发布补偿通知邮件
    │  告知用户订单已取消、库存已释放
    │  ├── 成功 → 记录 step SUCCESS
    │  └── 失败 → 记录 step SKIPPED（保留 errorMessage，继续）
    │
    ▼
Step 6: 完成 SagaLog（status = COMPLETED）
    │
    ▼
ACK 消息
```

### 5.3 补偿步骤的关键性分级

| 步骤 | 关键性 | 失败处理策略 |
|------|--------|-------------|
| `RESTORE_STOCK` | 关键 | 失败则抛异常，触发消息重试 |
| `UPDATE_ORDER` | 关键 | 失败则抛异常，触发消息重试 |
| `SEND_NOTIFICATION` | 非关键 | 失败则标记 SKIPPED，不阻塞整体补偿完成 |

设计理由：库存回滚和订单状态更新是数据一致性的核心保障，必须成功；通知邮件属于"尽力而为"，即使发送失败也不影响数据正确性（邮件本身已有独立的 MQ 重试机制）。

### 5.4 库存回滚的幂等设计

库存回滚操作必须幂等，防止重试时重复加回库存：

- 编排器在执行 `RESTORE_STOCK` 前，先检查 SagaLog 中该步骤是否已标记为 `SUCCESS`
- 若已成功，直接跳过该步骤（不重复写入步骤记录）
- 库存回滚通过 `PhoneStockRepository` 新增的 `increaseStockAndDecreaseSales()` 方法实现
- 该方法使用 MongoDB 的原子 `$inc` 操作，与现有 `decreaseStockAndIncreaseSales()` 对称

### 5.5 补偿重试与 DLQ 策略

| 参数 | 值 |
|------|-----|
| 最大重试次数 | 3 次（不含首次消费） |
| 重试间隔 | 3s → 9s → 27s（指数退避） |
| 死信路由键 | `order.compensation.dlq` |
| DLQ 保留期 | 7 天 |
| 确认模式 | 手动 ACK |

补偿消息的重试策略比订单后置处理更宽松（起步间隔更长），因为补偿本身不影响用户实时体验，但需要更高的成功率。

---

## 6. 涉及的文件与改动说明

### 6.1 新增文件

```
com.oldphonedeals/
├── entity/
│   └── SagaLog.java                          # Saga 审计日志实体（含嵌套 SagaStep）
├── enums/
│   ├── SagaStatus.java                       # STARTED / RETRYING / COMPLETED / FAILED
│   └── StepStatus.java                       # SUCCESS / FAILED / SKIPPED
├── dto/
│   └── message/
│       └── OrderCompensationMessage.java     # 补偿消息体
├── repository/
│   └── SagaLogRepository.java                # SagaLog 读写仓储
├── producer/
│   └── CompensationMessageProducer.java      # 补偿消息生产者
├── consumer/
│   └── SagaCompensationOrchestrator.java     # 补偿编排器（@RabbitListener）
└── service/
    ├── SagaCompensationService.java          # 补偿业务逻辑接口
    └── impl/
        └── SagaCompensationServiceImpl.java  # 补偿业务逻辑实现
```

### 6.2 修改文件

| 文件 | 改动类型 | 说明 |
|------|----------|------|
| `config/RabbitMQConfig.java` | 修改 | 新增 `compensation.exchange`、`order.compensation.queue`、`order.compensation.dlq.queue` 及对应 Binding；新增 `compensationListenerContainerFactory` |
| `consumer/OrderPostProcessConsumer.java` | 修改 | 重试耗尽时不再仅 NACK，改为先发布补偿消息再 NACK |
| `enums/OrderPostProcessStatus.java` | 修改 | 新增 `COMPENSATING`、`COMPENSATED` 枚举值 |
| `repository/custom/PhoneStockRepository.java` | 修改 | 新增 `increaseStockAndDecreaseSales()` 方法声明 |
| `repository/custom/impl/PhoneStockRepositoryImpl.java` | 修改 | 实现库存回滚的原子操作 |

---

## 7. 测试策略

### 7.1 单元测试

单元测试使用 Mockito 隔离外部依赖，验证各组件的纯业务逻辑。

#### 7.1.1 新增测试类

| 测试类 | 测试对象 | 重点用例 |
|--------|----------|----------|
| `CompensationMessageProducerTest` | `CompensationMessageProducer` | 正常发布补偿消息；发布异常时抛出 `IllegalStateException` |
| `SagaCompensationOrchestratorTest` | `SagaCompensationOrchestrator` | 幂等跳过已完成的 Saga；正常执行全部补偿步骤并 ACK；关键步骤失败时抛异常触发重试；非关键步骤失败时标记 SKIPPED 继续 |
| `SagaCompensationServiceImplTest` | `SagaCompensationServiceImpl` | 库存回滚成功；库存回滚失败时抛异常并记录失败原因；订单状态更新为 COMPENSATED；通知发送失败不阻塞流程 |

#### 7.1.2 扩展现有测试类

| 测试类 | 新增用例 |
|--------|----------|
| `OrderPostProcessConsumerTest` | 重试耗尽时应发布补偿消息并 NACK；补偿消息发布失败时仍 NACK（降级到 DLQ） |
| `OrderServiceTest` | 订单状态为 `COMPENSATED` 时不在用户订单列表中展示 |

#### 7.1.3 单元测试目录结构

```
src/test/java/com/oldphonedeals/
├── producer/
│   └── CompensationMessageProducerTest.java    [新增]
├── consumer/
│   ├── SagaCompensationOrchestratorTest.java   [新增]
│   └── OrderPostProcessConsumerTest.java       [扩展]
├── service/
│   ├── SagaCompensationServiceImplTest.java    [新增]
│   └── OrderServiceTest.java                   [扩展]
```

### 7.2 E2E 测试

E2E 测试与现有集成测试的核心区别：**集成测试通过 `@MockBean` 隔离数据层，仅验证 MQ 消费逻辑；E2E 测试使用真实 MongoDB + 真实 RabbitMQ（均通过 Testcontainers 启动），验证从 REST API 到数据库状态变更的完整链路，无任何 Mock。**

#### 7.2.1 E2E 测试基类

新增 `AbstractSagaE2E` 基类，同时启动 MongoDB 和 RabbitMQ 两个容器，加载完整 Spring 上下文（`@SpringBootTest(webEnvironment = RANDOM_PORT)`），提供：

- `TestRestTemplate`：模拟前端发起 HTTP 请求
- `MongoTemplate`：直接读写 MongoDB 验证数据状态
- `RabbitTemplate`：检查队列消息数量与内容
- 测试数据工厂方法：预置 Phone、User、Order 等基础数据到真实 MongoDB
- `@BeforeEach` 清理：每个用例执行前清空 `orders`、`saga_logs`、`phones` 集合及 MQ 队列，确保用例隔离

#### 7.2.2 E2E 测试用例

| 测试类 | 场景 | 验证要点 |
|--------|------|----------|
| `SagaCompensationE2E` | 完整补偿链路 | 预置订单和库存数据到 MongoDB → 模拟后置处理失败触发补偿消息 → 等待异步补偿完成 → 从 MongoDB 验证：库存精确回滚（`stock` 加回、`salesCount` 减少）、订单 `postProcessStatus` = `COMPENSATED`、`checkoutStatus` = `FAILED`、`saga_logs` 集合存在对应记录且 `status` = `COMPLETED`、所有步骤均为 `SUCCESS`（`SEND_NOTIFICATION` 允许 `SKIPPED`） |
| `SagaIdempotentE2E` | 幂等保障 | 用相同 `sagaId` 连续发布两条补偿消息 → 等待消费完成 → 从 MongoDB 验证：`saga_logs` 中仅一条记录、库存仅回滚一次（对比补偿前后的 `stock` 差值等于订单商品数量，而非两倍） |
| `SagaMultiItemStockE2E` | 多商品库存精确回滚 | 预置含 3 种不同商品的订单（各商品购买数量不同） → 触发补偿 → 从 MongoDB 逐一验证每种商品的 `stock` 和 `salesCount` 均精确回滚到下单前的值 |
| `SagaDlqFallbackE2E` | 补偿降级兜底 | 在 MongoDB 中制造导致补偿必然失败的数据状态（如删除订单文档使 `UPDATE_ORDER` 步骤无法执行） → 等待补偿重试耗尽 → 验证：补偿消息最终进入 `order.compensation.dlq.queue`、`saga_logs` 中记录 `status` = `FAILED` 且包含失败步骤的 `errorMessage` |
| `SagaNormalFlowUnaffectedE2E` | 正常路径不受影响 | 在补偿机制已部署的环境下，执行一次正常的订单结账后置处理 → 验证：`postProcessStatus` = `SUCCESS`、库存正确扣减、`saga_logs` 集合无新增记录（证明正常路径不会误触发补偿） |

#### 7.2.3 E2E 测试目录结构

```
src/test/java/com/oldphonedeals/
└── e2e/
    ├── AbstractSagaE2E.java                    [新增]
    ├── SagaCompensationE2E.java                [新增]
    ├── SagaIdempotentE2E.java                  [新增]
    ├── SagaMultiItemStockE2E.java              [新增]
    ├── SagaDlqFallbackE2E.java                 [新增]
    └── SagaNormalFlowUnaffectedE2E.java         [新增]
```

#### 7.2.4 E2E 测试关键约束

| 约束 | 说明 |
|------|------|
| 无 Mock | 全链路使用真实组件，不允许 `@MockBean`；若需模拟外部服务（如邮件），通过 `@TestConfiguration` 注入测试替身 |
| 异步等待 | 使用 Awaitility 等待异步流程，超时上限 30 秒（补偿链路含重试，需要比集成测试更长的等待窗口） |
| 数据隔离 | 每个用例独立预置和清理数据，禁止跨用例共享状态 |
| 容器复用 | MongoDB 和 RabbitMQ 容器在同一测试类内复用（`static @Container`），跨测试类通过 `@DirtiesContext` 隔离 |
| CI 兼容 | 标记 `@Tag("e2e")`，CI 流水线可通过 `-Dgroups=e2e` 单独执行或排除 |

---

## 8. 实施步骤

分四个阶段实施，每个阶段独立可交付、可验证。

### 阶段一：基础设施 + 核心补偿逻辑

| 步骤 | 内容 |
|------|------|
| 1 | 新增 `SagaStatus`、`StepStatus` 枚举 |
| 2 | 扩展 `OrderPostProcessStatus` 枚举，新增 `COMPENSATING`、`COMPENSATED` |
| 3 | 新增 `SagaLog` 实体（含嵌套 `SagaStep`）和 `SagaLogRepository` |
| 4 | 新增 `OrderCompensationMessage` 消息 DTO |
| 5 | 新增 `CompensationMessageProducer`，封装补偿消息发布 |
| 6 | 更新 `RabbitMQConfig`，声明 `compensation.exchange`、补偿队列、DLQ 及 `compensationListenerContainerFactory` |
| 7 | 扩展 `PhoneStockRepository`，新增 `increaseStockAndDecreaseSales()` 方法 |
| 8 | 新增 `SagaCompensationService` 接口及 `SagaCompensationServiceImpl` 实现 |
| 9 | 新增 `SagaCompensationOrchestrator`（补偿编排器消费者） |
| 10 | 改造 `OrderPostProcessConsumer`，重试耗尽时发布补偿消息 |

验收标准：订单后置处理失败后，补偿消息自动发布并被编排器消费，库存回滚、订单状态更新为 `COMPENSATED`，SagaLog 记录完整。

### 阶段二：单元测试 + 集成测试

| 步骤 | 内容 |
|------|------|
| 1 | 新增 `CompensationMessageProducerTest` 单元测试 |
| 2 | 新增 `SagaCompensationOrchestratorTest` 单元测试 |
| 3 | 新增 `SagaCompensationServiceImplTest` 单元测试 |
| 4 | 扩展 `OrderPostProcessConsumerTest`，覆盖补偿触发场景 |
| 5 | 新增 `AbstractSagaIT` 集成测试基类 |
| 6 | 新增 `SagaCompensationFlowIT` 集成测试（补偿主链路） |
| 7 | 新增 `SagaCompensationIdempotentIT` 集成测试（幂等） |
| 8 | 新增 `SagaCompensationDlqIT` 集成测试（补偿 DLQ） |
| 9 | 新增 `SagaStockRestoreIT`、`SagaNotificationSkipIT` 集成测试 |

验收标准：所有单元测试通过；集成测试在 Testcontainers 环境下稳定通过。

### 阶段三：E2E 测试

| 步骤 | 内容 |
|------|------|
| 1 | 新增 `AbstractSagaE2E` 基类（同时启动 MongoDB + RabbitMQ 容器，提供数据预置与清理工具） |
| 2 | 新增 `SagaCompensationE2E`（完整补偿链路验证） |
| 3 | 新增 `SagaIdempotentE2E`（幂等保障验证） |
| 4 | 新增 `SagaMultiItemStockE2E`（多商品库存精确回滚验证） |
| 5 | 新增 `SagaDlqFallbackE2E`（补偿降级兜底验证） |
| 6 | 新增 `SagaNormalFlowUnaffectedE2E`（正常路径不受影响验证） |

验收标准：5 个 E2E 测试场景在 Testcontainers（MongoDB + RabbitMQ）环境下稳定通过；无任何 `@MockBean`；CI 可通过 `-Dgroups=e2e` 独立执行。

---

## 9. 分支策略

本功能基于 `react-frontend` 分支创建独立的功能分支：

```
main
 └── react-frontend
      └── feature/saga-compensation    ← 当前分支（已创建）
```

开发完成后合并回 `react-frontend`，最终随 `react-frontend` 合并到 `main`。

---

## 10. 验收标准总览

满足以下全部条件即可认为 Saga 自动补偿机制建设完成：

1. **功能正确性**：订单后置处理失败后，系统自动触发补偿，库存回滚、订单状态更新为 `COMPENSATED`
2. **幂等保障**：重复补偿消息不会导致库存重复加回或 SagaLog 重复记录
3. **审计完整**：每次补偿在 `saga_logs` 集合中有完整的步骤记录
4. **降级兜底**：补偿本身失败后消息进入补偿 DLQ，SagaLog 标记为 `FAILED`
5. **单元测试**：所有新增和扩展的单元测试通过
6. **集成测试**：5 个集成测试场景在 Testcontainers 环境下稳定通过
7. **E2E 测试**：5 个 E2E 测试场景在 Testcontainers（MongoDB + RabbitMQ）环境下稳定通过，全链路无 Mock
8. **向后兼容**：现有订单流程（正常成功路径）不受影响
