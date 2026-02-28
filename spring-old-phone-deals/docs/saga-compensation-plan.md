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

### 7.2 集成测试

集成测试使用 `@SpringBootTest` + Testcontainers（RabbitMQ + MongoDB）验证真实环境下的补偿流程。

#### 7.2.1 集成测试用例矩阵

| 优先级 | 测试类 | 验证目标 | 通过标准 |
|--------|--------|----------|----------|
| P0 | `SagaCompensationFlowIT` | 订单后置处理失败后，补偿消息被消费，库存回滚，订单状态变为 `COMPENSATED` | 库存恢复到下单前数值；订单 `postProcessStatus = COMPENSATED`；SagaLog `status = COMPLETED` |
| P0 | `SagaCompensationIdempotentIT` | 重复发送相同 `sagaId` 的补偿消息 | SagaLog 仅一条记录；库存不被重复加回 |
| P0 | `SagaCompensationDlqIT` | 补偿编排器持续失败，重试耗尽 | 补偿消息进入 `order.compensation.dlq.queue`；SagaLog `status = FAILED` |
| P1 | `SagaStockRestoreIT` | 多商品订单的库存逐一回滚 | 每件商品的 stock 和 salesCount 均正确恢复 |
| P1 | `SagaNotificationSkipIT` | 邮件发送失败时补偿仍然完成 | SagaLog 中 `SEND_NOTIFICATION` 步骤为 `SKIPPED`；整体 `status = COMPLETED` |

#### 7.2.2 集成测试目录结构

```
src/test/java/com/oldphonedeals/integration/
└── saga/
    ├── AbstractSagaIT.java                  # 基类：启动 RabbitMQ + MongoDB 容器
    ├── SagaCompensationFlowIT.java          # 补偿主链路
    ├── SagaCompensationIdempotentIT.java    # 补偿幂等
    ├── SagaCompensationDlqIT.java           # 补偿 DLQ
    ├── SagaStockRestoreIT.java              # 多商品库存回滚
    └── SagaNotificationSkipIT.java          # 通知跳过
```

#### 7.2.3 集成测试技术要点

- 复用现有 `AbstractRabbitMqIT` 基类模式，扩展为同时启动 RabbitMQ + MongoDB 容器
- 使用 `@DynamicPropertySource` 注入容器地址
- 使用 `Awaitility` 等待异步补偿完成，避免 `Thread.sleep`
- 测试 profile 下使用较短重试退避（100ms 起步），缩短测试执行时间

### 7.3 E2E 测试（Playwright）

E2E 测试通过 Playwright 从用户视角验证补偿流程的端到端行为，复用现有 `react-frontend/e2e/` 目录和测试模式。

#### 7.3.1 前置条件

- 后端需提供 E2E 测试专用端点（扩展现有 `/api/e2e/reset`），支持：
  - 重置测试数据（用户、商品、订单）
  - 模拟订单后置处理失败（如通过配置开关强制消费者抛异常）
  - 查询 SagaLog 状态（供断言使用）
- 前端需在订单详情页展示补偿状态（如 `COMPENSATED`）

#### 7.3.2 E2E 测试用例

| 测试用例 | 验证目标 | 关键断言 |
|----------|----------|----------|
| 订单补偿后库存恢复 | 下单后模拟后置处理失败，验证商品库存自动恢复 | 商品详情页库存数量回到下单前；订单不在用户订单列表中展示 |
| 补偿后用户收到通知 | 补偿完成后用户收到订单取消通知 | 通过 API 断言补偿通知邮件已发布到 MQ |
| 补偿状态可查询 | 管理员可通过 API 查询 SagaLog | SagaLog 返回正确的 `sagaId`、`status`、`steps` |

#### 7.3.3 E2E 测试文件

```
react-frontend/e2e/
└── saga-compensation.spec.ts    [新增]
```

#### 7.3.4 E2E 测试流程示例

以"订单补偿后库存恢复"为例，测试流程如下：

```
1. 调用 /api/e2e/reset 重置测试数据
2. 登录买家账号，获取 token
3. 通过 API 记录商品初始库存
4. 调用 /api/e2e/saga/enable-failure 开启后置处理强制失败模式
5. 添加商品到购物车
6. 执行结账（checkout）
7. 等待补偿流程完成（轮询 /api/e2e/saga/status/{orderId}）
8. 断言：商品库存恢复到初始值
9. 断言：订单不在用户订单列表中
10. 调用 /api/e2e/saga/disable-failure 关闭强制失败模式
```

#### 7.3.5 E2E 测试辅助端点（后端新增）

为支持 E2E 测试，需在后端新增以下测试专用端点（仅在 `e2e` profile 下启用）：

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/e2e/saga/enable-failure` | POST | 开启订单后置处理强制失败模式 |
| `/api/e2e/saga/disable-failure` | POST | 关闭强制失败模式 |
| `/api/e2e/saga/status/{orderId}` | GET | 查询指定订单的 SagaLog 状态 |

---

## 8. 实施步骤

分三个阶段实施，每个阶段独立可交付、可验证。

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

### 阶段三：E2E 测试 + 辅助端点

| 步骤 | 内容 |
|------|------|
| 1 | 新增 E2E 测试辅助 Controller（`E2eSagaController`），提供强制失败开关和 SagaLog 查询端点 |
| 2 | 改造 `OrderPostProcessConsumer`，支持通过配置开关强制抛异常（仅 `e2e` profile） |
| 3 | 新增 Playwright E2E 测试文件 `saga-compensation.spec.ts` |
| 4 | 编写"订单补偿后库存恢复"E2E 用例 |
| 5 | 编写"补偿状态可查询"E2E 用例 |

验收标准：Playwright E2E 测试在完整环境（前端 + 后端 + RabbitMQ + MongoDB）下稳定通过。

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
7. **E2E 测试**：Playwright 测试在完整环境下稳定通过
8. **向后兼容**：现有订单流程（正常成功路径）不受影响
