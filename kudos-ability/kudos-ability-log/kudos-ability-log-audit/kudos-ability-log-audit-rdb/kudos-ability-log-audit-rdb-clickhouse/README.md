# kudos-ability-log-audit-rdb-clickhouse

`IAuditService` 的 ClickHouse 后端实现。

业务工程把本模块作为依赖加入即可获得：

1. **`RdbClickhouseAuditService` 自动装配**：实现 [`IAuditService`](../../kudos-ability-log-audit-common/src/io/kudos/ability/log/audit/common/api/IAuditService.kt)，
   接口与 RDB / Mongo / MQ backend 完全一致——切换后端只换依赖，业务侧 `@Audit` / `@WebAudit`
   注解与切面不动
2. **简化版 schema**（去掉 `ON CLUSTER` / `ReplicatedMergeTree` / `Distributed` / `View` / `TTL`）
3. **ktorm DSL 跑批量 INSERT**：使用标准 SQL `INSERT ... VALUES (?,?,...)`, ClickHouse JDBC 完美支持
4. **总开关 `kudos.ability.log.audit.rdb.clickhouse.enabled`**（默认 true）

## 设计要点

### 为什么不依赖 `kudos-ability-log-audit-rdb-ktorm`

`audit-rdb-ktorm` 模块带 `RdbKtormAuditService` 默认 bean。如果本模块依赖它会导致：
- 业务工程同时拿到 ktorm + ClickHouse 两个 `IAuditService` bean
- `@Autowired IAuditService` 不带 qualifier 时 `NoUniqueBeanDefinitionException`
- 默认 ktorm bean 没有 `@Primary`，必须业务侧加 qualifier

为避免这个 ergonomic landmine，本模块**只依赖 `audit-rdb-common`**（拿 schema 列名常数）+
`data-rdb-ktorm`（拿 ktorm DSL 但不带 audit Service bean），自建 Table 对象副本（~50 行复制）。
ClickHouse-only 部署的常见场景下，业务侧 `@Autowired IAuditService` 直接拿到唯一的
`clickhouseAuditService`，无需 qualifier。

### 为什么 schema 大幅简化

soul 的生产版 DDL 假设 ClickHouse 集群 + ZooKeeper：

```sql
CREATE TABLE local_sys_audit_log ON CLUSTER dp_cluster
    ENGINE = ReplicatedMergeTree
CREATE TABLE sys_audit_log ON CLUSTER dp_cluster
    AS local_sys_audit_log
    ENGINE = Distributed(...)
```

这些是**部署层面**的关注点，不属于 audit 模块的职责：

| 简化项 | 原因 |
|---|---|
| `ON CLUSTER` | 集群名是部署配置，模块不应硬编码 |
| `ReplicatedMergeTree` | 要求 ZooKeeper；单测/小规模部署不需要 |
| `Distributed` 覆盖表 | 分片策略是部署决策 |
| `View v_sys_audit_log` | 查询模板（带窗口函数）属业务报表层，不属 audit 写入路径 |
| `TTL ... + 63 days` | 数据保留策略；模块不替业务做这个决定 |

需要这些能力的工程在自己工程里覆盖 DDL；本模块只提供单节点 `MergeTree` 的最简可用形态。

### 为什么不加 `@Transactional`

ClickHouse 不支持 RDBMS 语义的多语句事务：
- `INSERT` 写入 MergeTree 后会在 part-merge 时刻可见；不存在 rollback
- `BEGIN/COMMIT` 在协议上被接受但对数据写入无 rollback 语义
- 加 `@Transactional` 会让代码看起来原子，但实际不是 —— 这比"没加 + 我们知道不原子"更糟

跟 `RdbKtormAuditService` 的 `REQUIRES_NEW` 不同 —— 那是 RDBMS 里有意义的事务隔离边界，
ClickHouse 这边无对应概念。

### Bean 命名 `clickhouseAuditService` + `ConditionalOnMissingBean(name=...)`

跟 `audit-rdb-ktorm` 的 `rdbKtormAuditService` 同模式：
- 用 bean 名而非类型作为 `ConditionalOnMissingBean` 维度
- 不加 `@Primary` —— 多 backend 共存时由 MQ 通过 `@Primary` 赢；其它用 qualifier
- 业务侧 `@Resource("clickhouseAuditService")` 可直接拿到本实现

### 为什么 service 不持有 `Database` 字段

跟 `RdbKtormAuditService` 一致 —— 每次 `submit` 内调用 `KudosContextHolder.currentDatabase()`：

```kotlin
val database = KudosContextHolder.currentDatabase()
database.batchInsert(SysAuditLogTable) { ... }
```

kudos 的 dynamic-datasource 层把不同租户路由到不同物理 ClickHouse 实例。如果 service 在构造
时缓存 `Database` 字段，所有写入都会固定到 bean 创建瞬间绑定的 DS，跨租户多源路由失效。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/LogAuditRdbClickhouseAutoConfiguration` | 装配入口（`@AutoConfigureAfter(KtormAutoConfiguration)`） |
| `service/RdbClickhouseAuditService` | `IAuditService` 实现，批量 INSERT 主表 + 详情表 |
| `table/SysAuditLogTable` | ktorm Table 元数据；列名复用 `AuditLogSchema.AuditLogColumn` |
| `table/SysAuditDetailLogTable` | 同上，详情表 |
| `resources/db/V1.0.0__create_sys_audit_log_clickhouse.sql` | 简化版 ClickHouse DDL |

## 配置示例

```yaml
# 业务工程的 application.yml
spring:
  datasource:
    dynamic:
      datasource:
        # 把 audit 路由到独立的 ClickHouse data source
        audit_clickhouse:
          driver-class-name: com.clickhouse.jdbc.ClickHouseDriver
          url: jdbc:clickhouse://localhost:8123/audit_db
          username: default
          password: ${CLICKHOUSE_PASSWORD}

kudos:
  ability:
    log:
      audit:
        rdb:
          clickhouse:
            enabled: true        # 默认 true
```

## ktorm + ClickHouse 兼容性说明

- **写端用 ktorm DSL**：`database.insert(table) { set(col, value) }` 翻译成标准 INSERT，兼容
- **读端用 raw JDBC**：ktorm 没有 ClickHouse 方言插件，`select(specific_column)` / `count()` /
  `totalRecordsInAllPages` / `.limit(offset, size)` 全部抛 `translate(...) must not be null`。
  `RdbClickhouseAuditLogReadOnlyService` 借 `Database.useConnection { conn -> ... }` 直发
  `PreparedStatement` —— ClickHouse SQL 标准简单（`SELECT ... WHERE ... ORDER BY ...
  LIMIT N OFFSET M`），手写也清晰可控
- **时间字段 `DateTime64` 用字符串绑参**：`ps.setTimestamp` 在 ClickHouse JDBC 上会做 TZ 平移，
  跟我们 INSERT 时的字符串字面值不一致；统一用 `yyyy-MM-dd HH:mm:ss` 字符串 `ps.setString`,
  让 ClickHouse 服务端自己做 String→DateTime64 转换
- **不支持**：`UPDATE` / `DELETE`（ClickHouse 22.x 后 `ALTER TABLE ... UPDATE` 延迟生效）；
  audit 业务不需要

## 模块入口（含读端）

| 路径 | 角色 |
|---|---|
| `service/RdbClickhouseAuditService` | `IAuditService` 写端（forEach insert 循环） |
| `service/RdbClickhouseAuditLogReadOnlyService` | `IAuditLogReadOnlyService` 读端（raw JDBC + 动态 WHERE） |
| `table/SysAuditLogTable` / `SysAuditDetailLogTable` | ktorm Table 元数据（仅写端用） |
| `init/LogAuditRdbClickhouseAutoConfiguration` | 装配 `clickhouseAuditService` + `clickhouseAuditLogReadOnlyService` bean |
| `resources/db/V1.0.0__create_sys_audit_log_clickhouse.sql` | 简化 ClickHouse DDL |

## 测试覆盖

- `RdbClickhouseAuditServiceTest` (7) —— 写端：
  - submit 一个完整 model 后主表 + 详情表都写入正确行数
  - tenantId / subSysCode 优先 entity 自己 → 回退 model 顶层
  - 空 model 返回 true（no-op success）
  - 仅 entity 无 detail 也能正常写主表
  - 仅 detail 无 entity 也能正常写详情表（少见但合法）
- `RdbClickhouseAuditLogReadOnlyServiceTest` (14) —— 读端：
  - `findById` 命中 / null
  - `findDetailById` 命中 / audit 存在但无 detail / audit 不存在
  - `pagingSearch` 空表返回空 page
  - 无过滤时按 `operate_time DESC` 排
  - `tenantId` exact / `descriptionLike` substring / `descriptionLike = ""` 视同无过滤
  - 时间窗口 `[from, to)` 闭/开
  - 多 page 切片正确
  - `pageNo < 1` / `pageSize < 1` 被钳位到 1
  - 多字段组合 AND（tenantId + operatorId + descriptionLike）

未覆盖：`@Primary` / qualifier 解析（属 Spring 自身机制）；
ClickHouse 集群部署（不属模块职责）。

## 已知限制 / 后续工作

- ❗ 单节点 schema（`MergeTree`，非 `Replicated`）；集群部署需业务覆盖 DDL
- ❗ 无 Monitor / MonitorRecord service —— soul 的 ClickHouse 模块带这两个，本 MVP 不含
- ❗ 与 `audit-rdb-ktorm` 共存时业务侧需 qualifier；不推荐同时引入两个 RDB backend
- ❗ ClickHouse JDBC `0.9.6` 驱动 jar 较大（~10MB），不打瘦应用镜像

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-rdb:kudos-ability-log-audit-rdb-common"))
api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))
api(libs.clickhouse.jdbc)

testImplementation(project(":kudos-test:kudos-test-common"))
testImplementation(project(":kudos-test:kudos-test-container"))
```
