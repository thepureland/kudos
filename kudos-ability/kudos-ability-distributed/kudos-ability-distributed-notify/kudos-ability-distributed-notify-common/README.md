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
| `init/properties/NotifyCommonProperties` | `failOnMissingProducer` + `listenerNamespace` 配置 |

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

- ❗ `NotifyListenerItem.notifyListenerMap` 用普通 `mutableMapOf`——只在装配期写、运行期读，
  依赖 Spring 装配完成的 happens-before。**业务侧若 runtime 动态注册 listener 需自行同步**
- ❗ `NotifyMessageVo.notifyType` 是 `lateinit var`——Java 序列化跨进程时如果发送方用了无
  notifyType 的构造器，反序列化端读 notifyType 会抛 `UninitializedPropertyAccessException`。
  消费端应 try-catch 或先用反射检查
- ❗ `INotifyListener` 没有重试 / 失败补偿契约——投递语义全看 producer 实现（MQ at-least-once
  / fire-and-forget），业务侧必须自行做幂等

## 依赖

```kotlin
api(project(":kudos-context"))
```
