# kudos-ability

横切能力（capability）集合。按主题拆分的可插拔模块，业务侧按需引入。所有能力遵循
**common 契约 → 具体实现**双层结构，业务代码统一面向 SPI 编程，更换底层实现（如把 RabbitMQ
换 Kafka）只需切依赖。

## 主题分组

| 主题 | 子目录 | 内容 |
|---|---|---|
| **缓存** | [`kudos-ability-cache`](kudos-ability-cache/) | 多级缓存框架 + 本地 Caffeine / 远程 Redis / 跨服务广播 |
| **通信** | [`kudos-ability-comm`](kudos-ability-comm/) | 邮件 / SMS（阿里云、AWS） / WebSocket |
| **数据存储** | [`kudos-ability-data`](kudos-ability-data/) | RDB（JDBC / Ktorm / Flyway） + 内存 DB（Redis） |
| **分布式** | [`kudos-ability-distributed`](kudos-ability-distributed/) | Feign 客户端 / Nacos 配置中心 / Nacos 服务发现 / Redisson 锁 / MQ 通知 / Spring Cloud Stream / Seata 事务 |
| **文件存储** | [`kudos-ability-file`](kudos-ability-file/) | 本地 / MinIO，含图像压缩 |
| **审计日志** | [`kudos-ability-log`](kudos-ability-log/) | 审计日志框架 + MQ / RDB 落地 |
| **桌面 UI** | [`kudos-ability-ui`](kudos-ability-ui/) | JavaFX 控件库（与 web 服务体系正交） |
| **Web** | [`kudos-ability-web`](kudos-ability-web/) | Spring MVC / Ktor 服务端 |

## 命名约定

- `*-common` —— 共享契约 / SPI / 抽象类，不依赖具体技术栈
- `*-<impl>` —— 具体实现（caffeine / redis / minio / rabbit / ...），依赖 common
- `*-local` / `*-remote` —— 按部署形态分类（本地 vs 跨进程）

## 装配机制

所有 `*-AutoConfiguration` 类实现 `IComponentInitializer`（kudos-context 提供的 SPI），
由 `ComponentInitializerSelector` 扫描 classpath 自动装配。`@AutoConfigureAfter` /
`@AutoConfigureBefore` 由 `ComponentInitializationDispatcher` 处理（**非** Spring Boot 默认
SPI 的 no-op）——重要：写新模块时这两个注解是有效的。

## 选用建议

- 桌面应用：`ui-javafx`
- 单体服务（HTTP + DB + 缓存）：`web-springmvc` + `data-rdb-ktorm` + `cache-local-caffeine`
- 微服务：加上 `distributed-client-feign` + `distributed-config-nacos` + `distributed-discovery-nacos`
- 需要事务：再加 `distributed-tx-seata`
- 需要分布式锁：`distributed-lock-redisson`
- 需要异步消息：`distributed-stream-{rabbit,kafka,rocketmq}` 三选一
- 需要审计日志：`log-audit-mq`（异步投递）或 `log-audit-rdb-ktorm`（同步入表）
- 文件上传：`file-local`（开发期）/ `file-minio`（生产）
- 通信：`comm-email` + `comm-sms-aws` / `comm-sms-aliyun`
