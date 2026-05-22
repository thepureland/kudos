# kudos-ability-log-audit-rdb-ktorm

审计日志的 **Ktorm + RDB 落地实现**。把 `kudos-ability-log-audit-common` 的
`SysAuditLogModel` 同步落库到 `sys_audit_log` / `sys_audit_detail_log` 两张表。

## 设计要点

### 事务边界：`REQUIRES_NEW`

```kotlin
@Transactional(propagation = Propagation.REQUIRES_NEW)
override fun submit(sysAuditLogVo: SysAuditLogModel): Boolean { ... }
```

审计动作不能挂在业务事务里：
- **业务事务回滚不应该带走审计记录**——"业务失败了"恰恰是最需要保留的审计信息
- **审计落库失败不应该撞翻业务事务**——`submit` 内部 catch 所有异常返回 `false`，
  上游切面看到 `false` 后可以决定是否兜底（降级写本地文件 / 重试 / 忽略）

`REQUIRES_NEW` 在 Hibernate / JPA 场景下要求 datasource 支持 `getConnection()` 新建连接；
通过 `KudosContextHolder.currentDatabase()` 拿到的 Ktorm `Database` 已经走过
`Database.connectWithSpringSupport`，理论上沿用 Spring 事务管理器的传播能力。

### 双表批量插入

`SysAuditLogModel` 顶层有 `entities: List<SysAuditLogVo>` + `sysAuditDetailLogs: List<SysAuditDetailLogVo>`
两段。本类对两段各跑一次 `Database.batchInsert(...)`，不做一对一 join——业务侧的
`AuditLogTool.createSysAuditLogModel` 已经按一对一关系准备好 id 引用。

### 顶层 `tenantId` / `subSysCode` 兜底

```kotlin
item.set(tenantId, entity.tenantId ?: model.tenantId)
item.set(subSysCode, entity.subSysCode ?: model.subSysCode)
```

`SysAuditLogModel` 与单条 entity 都可能携带 tenantId / subSysCode——切面有时只在
model 顶层填，有时也填到 entity 里。优先 entity，缺失时用 model 兜底。

### Table 用 `Table<Nothing>` 不绑 Entity

`log-audit-common` 的 `SysAuditLogVo` 是普通 POJO（非 Ktorm Entity），所以本模块的
`SysAuditLogTable` / `SysAuditDetailLogTable` 不绑实体：

```kotlin
object SysAuditLogTable : Table<Nothing>(AuditLogSchema.TABLE_AUDIT_LOG) {
    val id = varchar(AuditLogColumn.ID).primaryKey()
    // ...
}
```

`Table<Nothing>` 是 Ktorm 表示"只读取列引用、不要 ktorm 自动绑 Entity proxy"的标准
写法。`batchInsert(SysAuditLogTable) { item { set(column, value) } }` 走纯 DSL 写入，
不经过 `IDbEntity` 抽象——和 `kudos-ability-data-rdb-ktorm` 的 `BaseCrudDao` 路径互不
冲突。

### 不用 `@Primary`

```kotlin
@Bean("rdbKtormAuditService")
@ConditionalOnMissingBean(name = ["rdbKtormAuditService"])
open fun rdbKtormAuditService(): IAuditService = RdbKtormAuditService()
```

`kudos-ability-log-audit-mq.LogAuditMqAutoConfiguration` 的 MQ 版本带了 `@Primary`，
所以同进程内同时引入两个模块时 MQ 版本胜出。本模块的 RDB bean 仍然被注册，业务侧
可以通过 `@Qualifier("rdbKtormAuditService")` 显式取——双写场景（既 MQ 又 DB）需要
业务方在切面外自己组合调用。

## 与 MQ 路径对照

| 维度 | log-audit-mq | log-audit-rdb-ktorm |
|---|---|---|
| `IAuditService` 实现 | `MqAuditService`（AOP 占位 + `@MqProducer`） | `RdbKtormAuditService`（`@Transactional` + `batchInsert`） |
| `@Primary` | ✅ MQ 优先 | ❌（被注册但非 primary） |
| 异步 / 同步 | 异步投递（取决于 MQ producer 切面） | 同步 SQL |
| 失败语义 | 返回 `true` 占位，真实结果取决于 MQ broker | 抓异常返回 `false`，业务可据此降级 |
| 数据可观测性 | 取决于 broker 落地 | 直接 SQL 可查 |
| 部署复杂度 | 需要 stream + broker | 仅需 RDB |

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/LogAuditRdbKtormAutoConfiguration` | 装配入口：注册 `rdbKtormAuditService` bean，受 `kudos.ability.log.audit.rdb.ktorm.enabled` 开关控制 |
| `service/RdbKtormAuditService` | `IAuditService` 的 Ktorm 实现，事务边界 + 失败语义 |
| `table/SysAuditLogTable` | `sys_audit_log` 表的 Ktorm 列引用对象 |
| `table/SysAuditDetailLogTable` | `sys_audit_detail_log` 表的 Ktorm 列引用对象 |

## 配置示例

```yaml
kudos:
  ability:
    log:
      audit:
        rdb:
          ktorm:
            enabled: true     # 默认 true，可关闭
spring:
  flyway:
    enabled: true             # 让 DDL 自动执行（DDL 在 rdb-common 模块）
    locations: classpath:db/migration
```

## 测试覆盖

- `RdbKtormAuditServiceTest`（6）—— H2 in-memory + 真实 Ktorm 写入 / 读取，覆盖：
  主表 + 详情表批量插入、顶层 tenant/subSys 兜底、entity 字段优先、空模型 no-op、
  多 entity batch insert、重复主键异常返回 false 而非外抛。

6/6 测试全绿。

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-rdb:kudos-ability-log-audit-rdb-common"))
api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-rdb:kudos-ability-data-rdb-ktorm"))

testImplementation(project(":kudos-test:kudos-test-common"))
```

`rdb-common` 提供表名 / 列名常量与 DDL；`data-rdb-ktorm` 提供 Ktorm DSL + 数据源上下文。

## 已知限制 / 后续工作

- ❗ 同步 SQL 写入——高 TPS 业务的审计落库会成为业务请求路径上的瓶颈。可考虑：(a)
  本模块加一层 fire-and-forget executor（参考 `MixCache.pushMsgRedis` 的方案），(b)
  改用 `MqAuditService` 通过 MQ 异步落库
- ❗ 没有审计专用数据源——共用业务数据源时，DBA 视角下"审计写"与"业务交易写"混在一起，
  分库时不便。需要时可在 `KudosContextHolder.currentDataSource()` 通过 thread-local
  切换 `audit` 数据源
- ✅ `RdbKtormAuditService` 已有 H2 端到端测试覆盖主表 / 详情表写入、tenant/subSys
  兜底、空模型、批量插入和异常返回 false 语义
- ❗ DDL 写在 `rdb-common` 模块的 `resources/db/migration/`——表 schema 演进时务必新增
  `V<date>N__*.sql`，不要改老版本，否则 flyway checksum 失败
