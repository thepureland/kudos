# kudos-ability-distributed-stream

Spring Cloud Stream 之上的消息中间件接入。

| 子模块 | 实现 |
|---|---|
| [`kudos-ability-distributed-stream-common`](kudos-ability-distributed-stream-common/README.md) | 公共层：`@MqProducer` / `@MqConsumer` 注解 + 切面 + 失败消息持久化 |
| [`kudos-ability-distributed-stream-rabbit`](kudos-ability-distributed-stream-rabbit/README.md) | RabbitMQ |
| [`kudos-ability-distributed-stream-kafka`](kudos-ability-distributed-stream-kafka/README.md) | Kafka |
| [`kudos-ability-distributed-stream-rocketmq`](kudos-ability-distributed-stream-rocketmq/README.md) | RocketMQ（含 `RocketMqBatchConsumer` 原生批量拉取） |

业务侧选择具体 broker 后，引入对应的 `stream-<broker>` 实现 + `stream-common`，业务代码
依然按 `@MqProducer` / `@MqConsumer` 注解写。
