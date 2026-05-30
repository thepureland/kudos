# kudos-ability-log-audit-rdb

审计日志 RDB 落地集合——按 ORM 分子模块。

| 子模块 | 实现 |
|---|---|
| [`kudos-ability-log-audit-rdb-common`](kudos-ability-log-audit-rdb-common/README.md) | RDB 共享层（**占位**） |
| [`kudos-ability-log-audit-rdb-ktorm`](kudos-ability-log-audit-rdb-ktorm/README.md) | Ktorm 实现（PG / MySQL / H2） |
| [`kudos-ability-log-audit-rdb-clickhouse`](kudos-ability-log-audit-rdb-clickhouse/README.md) | ClickHouse 实现（大规模 audit 列存场景） |

业务侧若需异步解耦，仍建议使用 `kudos-ability-log-audit-mq` 走 MQ + 下游消费者写 DB。
**不推荐同时引入 `ktorm` + `clickhouse`** —— 两个 `IAuditService` bean 共存会强制业务侧
加 `@Qualifier`，参考各自 README 的设计要点章节。
