# kudos-ability-log-audit-rdb-common

审计日志 **RDB 落地的 ORM 无关共享层**。承载所有 RDB 实现（当前仅 Ktorm）共用的：
- 表名 / 列名常量（`AuditLogSchema`）
- Flyway DDL 迁移脚本

## 模块入口

| 路径 | 角色 |
|---|---|
| `AuditLogSchema` | `sys_audit_log` / `sys_audit_detail_log` 表名 + 列名常量。表 / 列名只在这一处定义，多种 ORM 适配跟着走 |
| `resources/db/migration/V20260519__create_sys_audit_log.sql` | flyway 迁移脚本：建主表 + 详情表 + 必备索引 |

## 与 `kudos-ability-data-rdb-flyway` 的协作

本模块的 `resources/db/migration/*.sql` 在 jar 里位于 `BOOT-INF/classes/db/migration/`，
默认 Spring Boot flyway autoconfig 会扫这条路径（`spring.flyway.locations=classpath:db/migration`）。
**不需要在业务侧额外配置 location**，只需要业务应用同时引入 flyway 和本模块。

若业务方关闭了默认 location 自定义路径，需要把 `classpath:db/migration` 显式加回。

## 设计要点

### 为什么常量值用下划线命名而不是按 Kotlin 风格

`sys_audit_log` / `entity_id` 直接对应 SQL 标识符，与 [SysAuditLogVo](../../kudos-ability-log-audit-common/src/io/kudos/ability/log/audit/common/entity/SysAuditLogVo.kt)
等 Kotlin 字段的 camelCase 区分明确。`AuditLogSchema` 内的 Kotlin 常量名（`OPERATE_TYPE_ID`）
是按 Kotlin 风格的 const 命名，值（`"operate_type_id"`）是 SQL 风格——读者从代码到 SQL
都能直接看出来对应关系。

### 没有数据访问 SPI 抽象

最初设想引入 `IAuditLogRepository`-类的 ORM 无关接口，但实际上 [IAuditService.submit]
已经是足够的"业务到存储"抽象，再加一层 repository 只是把同样的方法签名往下传一级——
对仅有一种 ORM 实现（Ktorm）的当下没有收益。等真出现第二个 ORM 实现（MyBatis 等）时
再抽。

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-common"))
```

只透传 `log-audit-common`，让下游 `rdb-ktorm` 模块拿到 `IAuditService` / `SysAuditLogModel`
等。无 ORM 依赖——刻意保持 ORM 无关。

## 已知限制 / 后续工作

- ❗ DDL 没有 `IF NOT EXISTS` 之外的迁移策略——重命名列 / 新增列时需要补 `V20260519N__*.sql`
  形式的版本化迁移；不要直接改 `V20260519__create_sys_audit_log.sql`，flyway 会因 checksum
  mismatch 失败
- ❗ DDL 用 ANSI SQL 写——大多数 RDB 兼容，但 PostgreSQL 的 `JSONB` / Oracle 的 `CLOB`
  等数据库专属类型未利用；业务方需要时自行扩展
