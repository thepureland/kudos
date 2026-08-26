# kudos-ms-auth-notification-eventbus

可选模块：把认证安全事件通知的 `EVENT_BUS` 渠道接到 `kudos-ability-distributed-stream`。

`kudos-ms-auth-core` 只保留协议中立的 `IAuthSecurityEventNotificationPublisher`，不依赖 Kafka、RabbitMQ 或
RocketMQ；具体 broker 由部署引入对应的 `stream-<broker>` 实现决定，本模块只面向 Spring Cloud Stream 的
producer binding。

## 启用

```yaml
kudos:
  ms:
    auth:
      security-event:
        notification:
          event-bus:
            enabled: true
            binding-name: authSecurityEventNotification-out-0
```

需要同时具备 `StreamProducerHelper` Bean（即引入 `stream-common` 加某个 broker 实现），并在
`spring.cloud.stream.bindings` 中配置好该 binding。部署仍需用自己的调度设施有界调用
`IAuthSecurityEventNotificationDispatcher.dispatchPending`。

本模块可与 [`kudos-ms-auth-notification-msg`](../kudos-ms-auth-notification-msg/README.md) **同时启用**：
core 按渠道选择 publisher，站内信/邮件/短信走 msg 适配器，事件总线走本模块，无需手写组合发布器。

## 路由与渠道声明

只声明 `EVENT_BUS` 渠道，`USER` 与 `TENANT_SECURITY_QUEUE` 两种目标都接受——两者都可能合理地由总线承载。
不声明 `WORK_ORDER` 及站内信类渠道：声明了就等于把本适配器无法投递的通知悄悄吞掉。

这补上了默认路由的最后一块：core 默认把无负责人事件路由为 `TENANT_SECURITY_QUEUE + EVENT_BUS`，此前没有任何
publisher 认领该渠道。

## 消息载荷

`AuthSecurityEventBusNotificationMessage` 只包含标识与路由决策：`idempotencyKey`、`notificationId`、
`tenantId`、`eventId`、`notificationType`、`escalationLevel`、`attemptCount`、`dueAt`、`escalatedAt`、
`routeCode`、`destination`、`channel`、`recipientUserIds`。

**不包含**凭证材料、subject 指纹、风险来源与状态码、收件人联系方式。需要事件证据的消费者应通过租户隔离的管理
API 回查，那里仍然执行权限校验；把证据放到 topic 上等于交给所有能访问总线的人。

交付语义为至少一次，消费者必须按 `idempotencyKey`（即 `{notificationId}:EVENT_BUS`）去重。

## ⚠️ 交付语义的真实边界

Spring Cloud Stream 的 `StreamBridge` 是异步的：`sendMessage` 返回 `true` 只表示消息进入了本地 producer 队列，
**不代表 broker 已确认**。真正的 flush 失败由 stream 的 error channel 异步捕获，并由
`kudos-ability-distributed-stream` 落到 `sys_mq_fail_msg`。

也就是说责任确实发生了转移，但转移到的是**那份账本**，而不是 auth outbox——后者此时已把该渠道结算为
`DELIVERED`。因此：

- 部署**必须**启用 stream 的失败消息持久化，否则 broker 故障会变成静默丢失的通知；
- 本模块无法代替部署检测这一点，所以在此明确写出，而不是暗示它是安全的。

## 失败语义

| 情况 | 语义 |
|---|---|
| `sendMessage` 返回 `false`（binding 缺失、生产端背压、发送被拒） | 可重试 `AUTH_SECURITY_EVENT_NOTIFICATION_BUS_NOT_ACCEPTED` |
| 调用抛异常 | 可重试 `AUTH_SECURITY_EVENT_NOTIFICATION_BUS_UNAVAILABLE` |
| `binding-name` 配置非法 | 永久 `AUTH_SECURITY_EVENT_NOTIFICATION_BUS_CONFIG_INVALID` |
| 收到非 `EVENT_BUS` 渠道的 publication | 永久 `AUTH_SECURITY_EVENT_NOTIFICATION_BUS_CHANNEL_UNSUPPORTED` |

`sendMessage` 的返回值无法区分"binding 从未配置"与"暂时背压"，因此统一按可重试处理：背压会自行恢复，而始终
缺失的 binding 会表现为该通知耗尽尝试次数后进入死信列表，管理员能在那里看到稳定错误码。
