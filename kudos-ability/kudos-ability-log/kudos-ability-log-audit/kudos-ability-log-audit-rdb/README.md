# kudos-ability-log-audit-rdb

审计日志 RDB 落地集合——按 ORM 分子模块。

| 子模块 | 实现 |
|---|---|
| [`kudos-ability-log-audit-rdb-common`](kudos-ability-log-audit-rdb-common/README.md) | RDB 共享层（**占位**） |
| [`kudos-ability-log-audit-rdb-ktorm`](kudos-ability-log-audit-rdb-ktorm/README.md) | Ktorm 实现 |

当前已提供 Ktorm 落地实现：同步写入 `sys_audit_log` / `sys_audit_detail_log` 两张表。
业务侧若需异步解耦，仍建议使用 `kudos-ability-log-audit-mq` 走 MQ + 下游消费者写 DB。
