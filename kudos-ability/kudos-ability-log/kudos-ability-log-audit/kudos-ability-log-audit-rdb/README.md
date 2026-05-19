# kudos-ability-log-audit-rdb

审计日志 RDB 落地集合——按 ORM 分子模块。

| 子模块 | 实现 |
|---|---|
| [`kudos-ability-log-audit-rdb-common`](kudos-ability-log-audit-rdb-common/README.md) | RDB 共享层（**占位**） |
| [`kudos-ability-log-audit-rdb-ktorm`](kudos-ability-log-audit-rdb-ktorm/README.md) | Ktorm 实现（**占位**） |

**整套 RDB 落地路径当前未实现**——业务侧若需 RDB 落地审计日志，目前需自行实现
`IAuditService` 写表，或换用 `kudos-ability-log-audit-mq` 走 MQ + 下游消费者写 DB。
