# kudos-ability-distributed-notify

跨节点通知（如 cache 失效广播 / 配置变更 / 状态同步）。

| 子模块 | 实现 |
|---|---|
| [`kudos-ability-distributed-notify-common`](kudos-ability-distributed-notify-common/README.md) | SPI：`INotifyProducer` / `INotifyListener` + `NotifyTool` |
| [`kudos-ability-distributed-notify-mq`](kudos-ability-distributed-notify-mq/README.md) | MQ 投递实现（基于 spring-cloud-stream） |
