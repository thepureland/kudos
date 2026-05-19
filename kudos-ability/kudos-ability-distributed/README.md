# kudos-ability-distributed

分布式能力主题——微服务架构所需的横切能力。

| 子目录 | 内容 |
|---|---|
| [`kudos-ability-distributed-client`](kudos-ability-distributed-client/README.md) | Feign 客户端封装 |
| [`kudos-ability-distributed-config`](kudos-ability-distributed-config/README.md) | 配置中心（Nacos） |
| [`kudos-ability-distributed-discovery`](kudos-ability-distributed-discovery/README.md) | 服务发现（Nacos） + 客户端负载均衡 |
| [`kudos-ability-distributed-lock`](kudos-ability-distributed-lock/README.md) | 分布式锁（Redisson） |
| [`kudos-ability-distributed-notify`](kudos-ability-distributed-notify/README.md) | 跨节点通知（MQ） |
| [`kudos-ability-distributed-stream`](kudos-ability-distributed-stream/README.md) | spring-cloud-stream MQ 接入（Rabbit / Kafka / RocketMQ） |
| [`kudos-ability-distributed-tx`](kudos-ability-distributed-tx/README.md) | 分布式事务（Seata） |

业务侧典型微服务套餐：`client-feign` + `config-nacos` + `discovery-nacos` + 一种 `stream-<broker>`。
需要事务再加 `tx-seata`；需要锁加 `lock-redisson`。
