# kudos-ms-auth-notification-msg

安全事件 durable outbox 到 `kudos-ms-msg` 的可选适配模块。它不拥有扫描或重试调度，只实现
`IAuthSecurityEventNotificationPublisher`，把 core 已领取并完成租户路由的通知可靠交给消息服务。

## 启用

部署需提供可用的 `IMsgSendApi` Bean，并显式开启：

```yaml
kudos:
  ms:
    auth:
      security-event:
        notification:
          msg:
            enabled: true
            event-type-dict-code: auth_security_event_sla_escalated
            msg-type-dict-code: security_alert
            locale-dict-code: zh-CN # 可选
```

只有同时满足启用开关和 `IMsgSendApi` 可用时才注册 publisher；应用已提供其他 publisher 时自动退让。
部署仍需用自己的调度设施有界调用 `IAuthSecurityEventNotificationDispatcher.dispatchPending`。

## 路由与交付语义

本适配器通过 `supports(destination, channel)` 只声明 `USER` 目标的 `SITE_MESSAGE`、`EMAIL`、`SMS`。core 按渠道
逐个调用，每次调用只投递 `publication.channel`；幂等键仍是 `{notificationId}:{channel}`，与逐渠道拆分前完全一致，
因此已记录的发送在重试时仍然匹配。模板参数固定包含 `notificationId`、`eventId`、`notificationType`、
`escalationLevel`、`dueAt`、`escalatedAt` 和 `routeCode`。

本模块不声明 `TENANT_SECURITY_QUEUE` 目标，也不声明 `EVENT_BUS`、`WORK_ORDER` 等渠道——声明了就等于把这些通知
悄悄吞掉。默认 core 路由把无负责人通知送往 `TENANT_SECURITY_QUEUE + EVENT_BUS`，若部署没有为该渠道注册 publisher，
core 会把该渠道结算为 `DEAD` 并记录 `AUTH_SECURITY_EVENT_NOTIFICATION_CHANNEL_NO_PUBLISHER`，其它渠道不受影响。
这类部署应配置租户路由（含值班表）把通知指向真实响应人，或再注册一个支持安全队列/事件总线的 publisher —— 二者
可以与本模块并存，不需要手写组合器。

`IMsgSendApi` 返回发送 ID 表示消息服务已经接管持久化交付；返回 `null` 或调用异常按可重试失败处理。
路由、字典码配置非法属于永久失败。具体模板和各渠道厂商配置仍由 `kudos-ms-msg` 管理。
