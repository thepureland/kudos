# kudos-ability-log-audit

审计日志能力——`@Audit` / `@WebAudit` 注解 + 切面 + 落地后端。

| 子模块 | 角色 |
|---|---|
| [`kudos-ability-log-audit-common`](kudos-ability-log-audit-common/README.md) | 注解 + 切面 + 上下文 + `IAuditService` SPI |
| [`kudos-ability-log-audit-mq`](kudos-ability-log-audit-mq/README.md) | MQ 投递实现 |
| [`kudos-ability-log-audit-rdb`](kudos-ability-log-audit-rdb/README.md) | RDB 落地实现集合（含 ktorm 子模块） |
| [`kudos-ability-log-audit-mongo`](kudos-ability-log-audit-mongo/README.md) | MongoDB 落地实现（单 collection + 嵌入 detail） |

业务侧默认引入 `audit-common`（方法注解），按需加 `audit-mq`（异步投递）或 `audit-rdb-ktorm`
（同步入库）。两者都存在时 MQ `@Primary` 优先生效——见 audit-mq README。
