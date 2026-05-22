# kudos-ability-log-audit-mq

把审计日志投递到消息队列的 [IAuditService] 实现，配合 `kudos-ability-distributed-stream-*`
模块走 spring-cloud-stream 发到 RabbitMQ / Kafka / RocketMQ 等。

## 设计要点

### `MqAuditService.submit` 的 "假实现"

```kotlin
class MqAuditService : IAuditService {
    @MqProducer(topic = "LOG_AUDIT_TOPIC", bindingName = "logAudit-out-0")
    override fun submit(sysAuditLogVo: SysAuditLogModel): Boolean = true
}
```

方法体里**就一行 `= true`**——这不是 stub，是 AOP 驱动设计：
- 真正的 MQ 发送由 `@MqProducer` 注解触发的 AOP 切面拦截 `sysAuditLogVo` 参数完成
- 切面按 `topic` + `bindingName` 路由到 spring-cloud-stream 的对应 binding（`logAudit-out-0`）
- `return true` 只是占位返回值，没有意义；实际发送结果由切面决定

**陷阱**：如果应用没引入 `kudos-ability-distributed-stream-*` 的 MQ producer 切面，本方法
实际就是 no-op——**审计日志静默丢失**。生产部署要求：
1. 装 `kudos-ability-distributed-stream-mq-rabbit`（或 kafka / rocketmq 实现）
2. 在 yml 配好 binding 的 `destination`、`group`、`content-type`

### `@Primary` 优先级约定

```kotlin
@Bean
@Primary
open fun mqAuditService(): IAuditService = MqAuditService()
```

业务侧同时引入 `log-audit-mq` 和 `log-audit-rdb-*` 时，`@Autowired IAuditService` 默认拿到
**MQ 版本**——MQ 异步投递、不阻塞业务路径，是默认首选。

想反过来（RDB 优先 / 二者都用）的业务方需要：
- 覆盖 `mqAuditService` bean 移除 `@Primary`
- 或者在注入点用 `@Qualifier` 精确选择
- 或者业务自定义一个 `MultiAuditService` 同时分发

### binding 配置

```yaml
spring:
  cloud:
    stream:
      bindings:
        logAudit-out-0:
          destination: LOG_AUDIT_TOPIC
          group: logAudit
```

`destination` 是 MQ broker 上的 topic / exchange / stream 名（每家叫法不同）。`group`
让多实例 consumer 走"competing consumer"模式，避免一条审计被多次消费。

## 模块入口

| 路径 | 角色 |
|---|---|
| `beans/MqAuditService` | `@MqProducer` 注解 + `IAuditService` 实现 |
| `init/LogAuditMqAutoConfiguration` | 装配入口，注册 `@Primary` 的 `mqAuditService` bean |

## 测试覆盖

- `AuditMqTest`（4）—— 跑通 `@Audit` + `LogAuditAspect` + `MqAuditService` +
  `@MqProducer` 切面的完整链路（spring-flyway 关掉避免无关初始化），**但没断言消息真发出去了**——
  目前的测试本质是"compile + wire-up smoke test"；同时锁定 `MqAuditService` 是当前
  `@Primary IAuditService`、`submit` 上的 `topic` / `bindingName` 元数据和占位返回值

## 已知限制 / 后续工作

- ❗ **测试只验证链路不抛错，不验证消息送达**——MQ 集成期间的真正测试应当配 testcontainer
  起一个 broker、在消费端验证收到了 `SysAuditLogModel`。目前缺这层
- ❗ `MqAuditService.submit` 的 "假实现 + `@MqProducer` 切面" 模式对新人不友好——
  阅读代码看不出 MQ 发送在哪里发生。源码 kdoc 已经说明，但前提是有人阅读
- ❗ 如果切面所在的 stream 模块未装入，本模块**没有任何编译期 / 装配期信号**告诉开发者
  MQ 路径失效；审计日志会静默丢。需要在切面层加 "no aspect present" warn 日志
- ❗ 没有死信 / 失败重试机制——`@MqProducer` 抛异常时切面默认行为依赖具体实现；本模块
  layer 看不到也管不了
- ❗ 当前 binding 名 `logAudit-out-0` 硬编码在 `@MqProducer` 注解里——业务方需要换名只能
  通过覆盖整个 `MqAuditService` bean
- ✅ 已删除未被任何测试引用的 `TestModuleEnum` 遗留辅助类

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-common"))
api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-stream:kudos-ability-distributed-stream-common"))

testImplementation(project(":kudos-test:kudos-test-common"))
```

注意：`distributed-stream-common` 提供 `@MqProducer` 注解定义；**具体切面实现（rabbit / kafka）
仍需业务方自行添加 implementation 依赖**。
