# kudos-ability-log-audit

审计日志能力 —— `@Audit` / `@WebAudit` 注解 + 切面 + 落地后端 + 读端查询。

| 子模块 | 角色 |
|---|---|
| [`kudos-ability-log-audit-common`](kudos-ability-log-audit-common/README.md) | 注解 + 切面 + 上下文 + `IAuditService` / `IAuditLogReadOnlyService` / `IMonitorService` SPI |
| [`kudos-ability-log-audit-mq`](kudos-ability-log-audit-mq/README.md) | MQ 投递实现（异步，仅写端） |
| [`kudos-ability-log-audit-rdb`](kudos-ability-log-audit-rdb/README.md) | RDB 落地集合（含 ktorm 与 ClickHouse 子模块） |
| [`kudos-ability-log-audit-mongo`](kudos-ability-log-audit-mongo/README.md) | MongoDB 落地实现（单 collection + 嵌入 detail） |

## 后端选型决策树

```
┌─ 同步入库 + 业务 RDB 共用？ ──→ kudos-ability-log-audit-rdb-ktorm   (PG/MySQL/H2)
│
├─ 异步解耦 + 下游消费端落 DB？ → kudos-ability-log-audit-mq        (Kafka/RocketMQ 等)
│
├─ 大量写入 + 时序按列扫描查询？ → kudos-ability-log-audit-rdb-clickhouse
│   • 单租户日均 100 万+ audit 行
│   • 主要查询模式 "tenant X 在时间窗 [A, B) 范围内的操作"
│
└─ 嵌套文档 + 灵活字段 + 中小规模？ → kudos-ability-log-audit-mongo
    • detail 字段经常变动（避免 schema migration）
    • 查询绕过强 schema，业务模型偏文档式
```

## SPI 与 backend impl 对照

| SPI                              | ktorm                            | MQ          | Mongo                            | ClickHouse                          |
|---|---|---|---|---|
| `IAuditService` (写)              | `rdbKtormAuditService`           | `mqAuditService` (`@Primary`) | `mongoAuditService`              | `clickhouseAuditService`            |
| `IAuditLogReadOnlyService` (读)   | `rdbKtormAuditLogReadOnlyService`| —           | `mongoAuditLogReadOnlyService`   | `clickhouseAuditLogReadOnlyService` |
| `IMonitorService` (ERR 升级)      | —                                | `mqMonitorService` | —                                | —                                   |

**SPI 默认装配规则**：
- 写端 `IAuditService`：每个 backend 模块的 AutoConfig 用 `@Bean(name=...)` + `@ConditionalOnMissingBean(name=...)`。
  **MQ 模块特殊**：标 `@Primary` 默认赢；其它三个 backend 不标 `@Primary`，业务侧用 qualifier 取
- 读端 `IAuditLogReadOnlyService`：每个 backend 用同样的 named-bean 模式。**无 `@Primary`**，
  业务侧必须用 qualifier 显式选
- ERR 升级 `IMonitorService`：MQ 在场用 MQ 推；否则 `audit-common` 兜底的 `LoggingMonitorService` 用 SLF4J 打 ERROR

## 多 backend 共存场景

业务侧可以同时引多个 backend，**通过 bean name qualifier 区分**：

### 场景 A：MQ 投递 + Mongo 消费落库

```kotlin
// 业务工程 build.gradle.kts
dependencies {
    api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-mq"))   // 业务进程 @Primary
    api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-mongo")) // 消费端进程
}

// 业务方法 ↓ @Audit 切面默认走 @Primary 的 mqAuditService → MQ topic
@Audit(opType = OperationTypeEnum.UPDATE, moduleCode = "USER")
fun updateUser(...) { ... }

// 消费端进程消费 MQ 消息后调用 mongoAuditService 落 Mongo
@Resource("mongoAuditService") private lateinit var mongoSink: IAuditService
```

### 场景 B：迁移窗口期，ktorm + ClickHouse 双写双读

```kotlin
@Service
class DualAuditService(
    @Resource("rdbKtormAuditService") private val ktorm: IAuditService,
    @Resource("clickhouseAuditService") private val clickhouse: IAuditService,
) : IAuditService {
    override fun submit(model: SysAuditLogModel): Boolean {
        val a = ktorm.submit(model)
        val b = clickhouse.submit(model)
        return a && b
    }
}

@Bean @Primary fun primaryAuditService(dual: DualAuditService): IAuditService = dual

// 读端按需选择
@Resource("rdbKtormAuditLogReadOnlyService") val pgReader
@Resource("clickhouseAuditLogReadOnlyService") val chReader
```

### 场景 C：读写分离 —— Mongo 读 + ClickHouse 写

```kotlin
@Resource("clickhouseAuditService") val writer: IAuditService           // 高吞吐写
@Resource("mongoAuditLogReadOnlyService") val reader: IAuditLogReadOnlyService  // 嵌套结构查询
```

⚠️ 这种组合要求业务侧自己处理 ClickHouse → Mongo 的数据同步（CDC / 双写 / ETL）；
模块不内置同步链路。

## 不推荐的搭配

- **同进程同时 ktorm + ClickHouse 写端**：两个 RDB-ish backend 写同样的数据，磁盘成本翻倍且没有
  读端协同机制。要么选其一，要么用场景 B 的 dual-write 加 `@Primary` 显式包装
- **MQ 不带消费端**：MQ 发送但下游没人消费 = audit 数据进 message broker 但永远不落库。
  确保消费端有 `IAuditService` impl（ktorm/Mongo/ClickHouse 其一）

## 业务最简引入

```kotlin
// 默认 + 同步 RDB 入库
api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-common"))
api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-rdb:kudos-ability-log-audit-rdb-ktorm"))
```

`@Audit` / `@WebAudit` 注解打到方法上，切面把执行记录通过 `IAuditService.submit` 同步入库到
`sys_audit_log` / `sys_audit_detail_log`。管理后台查询走 `IAuditLogReadOnlyService.pagingSearch`。
