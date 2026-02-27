# MQ 单元测试与集成测试规划（基于当前实现）

## 1. 文档目标

本规划用于在现有 RabbitMQ 实现基础上，系统补齐 `单元测试` 与 `集成测试`，重点覆盖：

- 发布端（Producer）消息发布行为；
- 消费端（Consumer）成功、失败、重试、死信（DLQ）行为；
- 业务服务与 MQ 交互边界（下单后置消息、邮件发送消息）；
- 队列/交换机/绑定/监听容器配置正确性；
- 幂等与可恢复能力。

## 2. 当前 MQ 实现现状（2026-02-27）

### 2.1 已实现的 MQ 组件

| 模块 | 当前实现 | 说明 |
| --- | --- | --- |
| `RabbitMQConfig` | 已实现 | 声明 `notification.exchange`、`order.exchange`、主队列、DLQ、Binding、`Jackson2JsonMessageConverter`、`RabbitTemplate`、两个独立 ListenerContainerFactory（手动 ACK + 重试 + 不重回队列）。 |
| `EmailMessageProducer` | 已实现 | 发送到 `notification.exchange` + `email.send`，发布失败抛 `IllegalStateException`。 |
| `OrderMessageProducer` | 已实现 | 发送到 `order.exchange` + `order.post.process`，发布失败抛 `IllegalStateException`。 |
| `EmailConsumer` | 已实现 | `@RabbitListener` 监听 `email.send.queue`，按 `EmailType` 发送邮件，成功 `basicAck`，失败 `basicNack(requeue=false)`。 |
| `OrderPostProcessConsumer` | 已实现 | 监听 `order.post.process.queue`，先做 `messageId` 幂等检查，再更新订单后置状态，失败时标记 `FAILED` 并 `basicNack`。 |
| `EmailServiceImpl` | 已实现 | 不直接发邮件，仅构建 `EmailMessage` 并发布 MQ 消息。 |
| `OrderServiceImpl.checkout` | 已实现 | 下单成功后发布 `OrderPostProcessMessage`，发布失败抛业务异常触发事务回滚。 |

### 2.2 当前测试现状（已执行）

已在本地执行（工作目录：`spring-old-phone-deals`）：

```bash
mvn -q "-Dtest=EmailServiceImplTest,EmailMessageProducerTest,OrderMessageProducerTest,EmailConsumerTest,OrderPostProcessConsumerTest,OrderServiceTest" test
```

结果：**全部通过（20/20）**。  
说明：命令输出中的部分 `ERROR` 日志来自“预期失败路径”的用例（例如模拟 SMTP 或 AMQP 异常），不代表测试失败。

| 测试类 | 类型 | 当前覆盖重点 | 用例数 |
| --- | --- | --- | --- |
| `EmailMessageProducerTest` | 单元 | 正常发布、发布异常抛出 | 2 |
| `OrderMessageProducerTest` | 单元 | 正常发布、发布异常抛出 | 2 |
| `EmailServiceImplTest` | 单元 | 构建并发布不同邮件类型消息、异常透传 | 4 |
| `EmailConsumerTest` | 单元 | 成功发送并 ACK、发送失败 NACK | 2 |
| `OrderPostProcessConsumerTest` | 单元 | 幂等跳过、成功处理、失败 NACK | 3 |
| `OrderServiceTest` | 单元 | 下单发布后置消息、库存失败、发布失败等 | 7 |

### 2.3 覆盖率基线（JaCoCo，当前测试结果）

| 类 | 行覆盖率 |
| --- | --- |
| `EmailMessageProducer` | 100% |
| `OrderMessageProducer` | 100% |
| `EmailServiceImpl` | 78.05% |
| `OrderServiceImpl` | 74.60% |
| `OrderPostProcessConsumer` | 76.92% |
| `EmailConsumer` | 66.67% |

> 备注：`config/**` 在 JaCoCo 中被排除，因此当前无 `RabbitMQConfig` 覆盖率数据。

## 3. 关键测试缺口

当前 MQ 测试以 Mockito 单测为主，以下能力仍需补齐：

1. **配置正确性缺口**：缺少 `RabbitMQConfig` 的显式测试（交换机、队列参数、DLQ、监听重试参数）。
2. **消费者分支缺口**：`EmailConsumer` 对 `PASSWORD_RESET_LINK`、`PASSWORD_RESET_CODE`、`null/非法类型` 等分支覆盖不足。
3. **失败处理细节缺口**：`OrderPostProcessConsumer` 对“订单不存在”“更新失败后的补偿状态”缺少精细断言。
4. **集成链路缺口**：缺少真实 RabbitMQ Broker 的端到端测试（当前无 `@SpringBootTest + Testcontainers RabbitMQ`）。
5. **重试与死信缺口**：尚未验证“重试耗尽后进入 DLQ”的真实行为。
6. **事务一致性缺口**：`OrderServiceImpl` 发布失败触发回滚目前仅单测验证异常，未做真实数据库事务层验证。

## 4. 单元测试规划

### 4.1 目标

- 将 MQ 关键类单测提升为“分支完备 + 行为可回归”的最小安全网；
- 重点提升 `EmailConsumer` 与 `OrderPostProcessConsumer` 的异常/边界覆盖；
- 为后续集成测试减少定位成本（先锁定纯业务逻辑正确性）。

### 4.2 新增/补强用例清单

| 优先级 | 测试类（建议） | 重点用例 |
| --- | --- | --- |
| P0 | `RabbitMQConfigTest`（新增） | 断言主队列与 DLQ 参数（`x-dead-letter-exchange`、`x-dead-letter-routing-key`、TTL）；断言 ListenerContainerFactory 为手动 ACK、`defaultRequeueRejected=false`、重试参数正确。 |
| P0 | `EmailConsumerTest`（扩展） | `PASSWORD_RESET_LINK`、`PASSWORD_RESET_CODE`、`GENERIC` 分支；`type=null` 时 NACK；URL 编码行为（email/token）；`basicAck` 失败时兜底 NACK。 |
| P0 | `OrderPostProcessConsumerTest`（扩展） | `orderId` 不存在时应 NACK 并标记失败；`orderRepository.save` 异常时 NACK；重复消息仅 ACK 不重复落库。 |
| P1 | `EmailServiceImplTest`（扩展） | 增加 `sendPasswordResetCodeEmail` 分支；断言 `timestamp` 与 `messageId` 非空。 |
| P1 | `OrderServiceTest`（扩展） | 对发布失败场景增加更细断言（错误信息一致、下游调用边界一致）。 |

### 4.3 目录与命名建议

- 新增：`src/test/java/com/oldphonedeals/config/RabbitMQConfigTest.java`
- 扩展：  
  - `src/test/java/com/oldphonedeals/consumer/EmailConsumerTest.java`  
  - `src/test/java/com/oldphonedeals/consumer/OrderPostProcessConsumerTest.java`  
  - `src/test/java/com/oldphonedeals/service/EmailServiceImplTest.java`  
  - `src/test/java/com/oldphonedeals/service/OrderServiceTest.java`

命名建议遵循 `shouldXxxWhenYyy`，便于 CI 报告快速定位。

## 5. 集成测试规划

### 5.1 目标

- 在真实 RabbitMQ 环境下验证“配置生效 + 消息流转 + 重试/DLQ + 幂等”；
- 覆盖单测无法证明的系统行为（容器、序列化、监听容器、Broker 交互）。

### 5.2 技术方案

1. 使用 `@SpringBootTest` + `Testcontainers` 启动 RabbitMQ（必要时同时启 MongoDB）。
2. 使用 `@DynamicPropertySource` 注入容器地址到 `spring.rabbitmq.*`。
3. 使用 `Awaitility` 等待异步消费完成，避免 `Thread.sleep`。
4. 使用独立命名约定 `*IT`（集成测试）与 `*Test`（单元测试）分层执行。

### 5.3 集成测试用例矩阵

| 优先级 | 测试类（建议） | 验证目标 | 通过标准 |
| --- | --- | --- | --- |
| P0 | `EmailMqFlowIT`（新增） | 生产 `EmailMessage` 后可被 `EmailConsumer` 消费并 ACK | 监听成功执行，消息不在主队列堆积。 |
| P0 | `OrderPostProcessMqFlowIT`（新增） | 生产订单后置消息后，订单状态由 `PENDING` 更新为 `SUCCESS` | `Order` 状态正确、`ProcessedMessage` 写入成功。 |
| P0 | `EmailDlqIT`（新增） | 人为制造邮件发送失败，验证重试后进入 `email.send.dlq.queue` | 重试耗尽后主队列无残留，DLQ 有对应消息。 |
| P0 | `OrderPostProcessDlqIT`（新增） | 人为制造订单后置处理失败，验证进入 `order.post.process.dlq.queue` | DLQ 收到消息，订单状态为 `FAILED`。 |
| P1 | `OrderPostProcessIdempotentIT`（新增） | 重复发送同一 `messageId`，验证幂等 | `ProcessedMessage` 仅一条有效记录。 |
| P1 | `RabbitTopologyIT`（新增） | 验证交换机/队列/绑定在应用启动后可被被动声明查询 | 所有关键拓扑存在且参数匹配。 |

### 5.4 集成测试前置改造

| 类型 | 计划项 |
| --- | --- |
| 依赖 | 在 `pom.xml` 增加 `org.testcontainers:junit-jupiter`、`org.testcontainers:rabbitmq`（如需数据库联动可加 Mongo 容器依赖）、`org.awaitility:awaitility`（test scope）。 |
| 执行层 | 建议引入 `maven-failsafe-plugin` 跑 `*IT`，避免与单测混跑导致反馈慢。 |
| 配置层 | 测试专用较短重试退避（例如 100ms 起步），缩短 DLQ 场景执行时间并减少 flaky。 |
| 可观测性 | 必要时在测试 profile 下增加可断言指标（例如消费计数器或关键日志标记）。 |

## 6. 分阶段落地计划

### 阶段 A（1 天）- 单元测试补齐

- 新增 `RabbitMQConfigTest`；
- 扩展两个 Consumer 单测分支；
- 扩展 Email/Order Service 与 MQ 边界断言；
- 目标：MQ 相关单测全部通过，Consumer 行覆盖率显著提升（建议目标：`EmailConsumer` >= 85%，`OrderPostProcessConsumer` >= 85%）。

### 阶段 B（1-2 天）- 集成主链路

- 引入 Testcontainers 基础设施；
- 落地 `EmailMqFlowIT`、`OrderPostProcessMqFlowIT`；
- 目标：在真实 Broker 下验证核心成功路径。

### 阶段 C（1 天）- 失败与恢复能力

- 落地 `EmailDlqIT`、`OrderPostProcessDlqIT`、`OrderPostProcessIdempotentIT`；
- 目标：验证重试、DLQ、幂等三大可靠性能力。

### 阶段 D（0.5 天）- CI 接入与门禁

- CI 分层执行：`unit` 必跑、`integration` 可按分支策略或夜间全跑；
- 设置门禁：MQ 相关测试失败即阻断合并。

## 7. 验收标准

满足以下条件即可认为本轮 MQ 测试体系建设完成：

1. 单元测试：MQ 相关测试全部通过且新增关键分支覆盖。
2. 集成测试：至少 4 个 P0 场景（成功链路 + DLQ）稳定通过。
3. 幂等与失败恢复：重复消息与重试耗尽行为可被自动化验证。
4. CI 可执行：在标准 CI 节点（支持 Docker）可稳定跑完 `*IT`。
5. 文档化：测试命令、依赖、运行前置条件写入 `docs` 并可复现。

