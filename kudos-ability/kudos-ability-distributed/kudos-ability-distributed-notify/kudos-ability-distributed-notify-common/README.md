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
```

## 已知限制

- ✅ `NotifyListenerItem.notifyListenerMap` 已改为两级 `ConcurrentHashMap`，空白 namespace
  回落 default namespace，并补单测锁住 namespace 隔离
- ✅ `NotifyMessageVo.notifyType` 已从 `lateinit var` 改成默认空串，producer / consumer
  可统一用 `isBlank()` 防御未设置类型的消息，并补构造器单测
- ❗ `INotifyListener` 没有重试 / 失败补偿契约——投递语义全看 producer 实现（MQ at-least-once
  / fire-and-forget），业务侧必须自行做幂等

## 依赖

```kotlin
api(project(":kudos-context"))
```
