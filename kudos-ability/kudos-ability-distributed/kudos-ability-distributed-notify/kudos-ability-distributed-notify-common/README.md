# kudos-ability-distributed-notify-common

跨节点通知 SPI + 装配。具体投递实现在 `kudos-ability-distributed-notify-mq`。

## 设计要点

| 文件 | 角色 |
|---|---|
| `api/INotifyProducer` | 发送方 SPI（`BEAN_NAME = "notifyMqProducer"`） |
| `api/INotifyListener` | 消费方 SPI——业务侧实现并注册 bean |
| `model/NotifyMessageVo<T>` | 消息载体（notifyType + Serializable messageBody） |
| `support/NotifyTool` | 业务侧调用入口；`@Autowired` 即可，无生产者就 warn 并返回 false（或按配置 fail-fast） |
| `support/NotifyListenerBeanPostProcessor` | 启动期自动登记 listener 到 `NotifyListenerItem` |
| `support/NotifyListenerItem` | `(namespace, type) → listener` 全局注册表 |
| `init/NotifyCommonAutoConfiguration` | 装配入口 |
| `init/properties/NotifyCommonProperties` | `failOnMissingProducer` + `listenerNamespace` + `fallbackToDefaultNamespace` 配置 |

## 配置

```yaml
kudos:
  ability:
    distributed:
      notify:
        listener-namespace: ${spring.application.name}   # 缺省按 app name
        fail-on-missing-producer: false                  # true 时启动期校验生产者必须存在
        mq:
          rethrow-consumer-exception: true               # notify-mq 消费失败时交回 binder 重试 / DLQ
```

## 已知限制

- ✅ `NotifyListenerItem.notifyListenerMap` 已改为两级 `ConcurrentHashMap`，空白 namespace
  回落 default namespace，并补单测锁住 namespace 隔离
- ✅ `NotifyMessageVo.notifyType` 已从 `lateinit var` 改成默认空串，producer / consumer
  可统一用 `isBlank()` 防御未设置类型的消息，并补构造器单测
- ℹ️ `INotifyListener` 仍保持最小 SPI，不在 common 层定义重试 / 失败补偿契约；投递语义由具体
  producer 实现决定。`notify-mq` 已支持
  `kudos.ability.distributed.notify.mq.rethrow-consumer-exception=true`，消费失败可交回 MQ binder
  重试或进入 DLQ；业务 listener 仍必须按 at-least-once 语义自行幂等

## 依赖

```kotlin
api(project(":kudos-context"))
```

## 改进建议（自动分析 2026-06-11）

- 【功能】`model/NotifyMessageVo.kt`：消息没有 messageId / 发送时间戳 / 来源节点字段——
  listener 被要求"自行幂等"（at-least-once），但缺少可作为幂等键的字段支撑；排障时也无法
  追溯消息来源与延迟。建议增加可选的 `messageId`（默认 UUID）与 `sendTime` 字段（向后兼容）。
- 【功能】`support/NotifyListenerItem.kt`：每个 `(namespace, notifyType)` 只支持单个 listener，
  `put` 重复注册时静默覆盖；若两个 bean 声明相同 notifyType，后装配者悄悄"赢"，难以排查。
  建议覆盖时打 WARN 日志，或支持同 type 多 listener 多播。
- 【对外接口】`api/INotifyProducer.kt`：`BEAN_NAME = "notifyMqProducer"` 常量定义在 common 层
  却带有 "Mq" 实现色彩；若未来出现 redis pub/sub 等其他实现，命名会产生误导。仅文档层面注意，
  不建议现在改名（破坏兼容）。
