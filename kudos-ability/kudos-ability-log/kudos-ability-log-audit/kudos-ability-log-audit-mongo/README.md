# kudos-ability-log-audit-mongo

`IAuditService` 的 MongoDB 后端实现。

业务工程把本模块作为依赖加入即可获得：

1. **`MongoAuditService` 自动装配**：实现 [`IAuditService`](../kudos-ability-log-audit-common/src/io/kudos/ability/log/audit/common/api/IAuditService.kt)，
   与 RDB / MQ backend 接口完全一致——切换后端只换依赖，业务侧的 `@Audit` 注解 / aspect 不动。
2. **嵌套子文档持久化**：每条 audit 主记录 + 其匹配的 detail 作为一个 Mongo document 写入
   `sys_audit_log` collection（不分两表）。
3. **`@ConditionalOnMissingBean(IAuditService::class)`**：若工程已声明其它 backend
  （`RdbKtormAuditService` / `MqAuditService` / 自定义），本模块自动让位，不会重复注册。

## 设计要点

### 为什么单 collection + 嵌套 detail

`SysAuditLogVo` ↔ `SysAuditDetailLogVo` 是严格 1:1 关系（每个 entity 一条主 + 一条 detail）。
Mongo 强项就是嵌套文档——把 detail 直接嵌入主文档而不是分两 collection 写入有三个好处：

1. **避免跨 collection 一致性问题**：standalone Mongo 没事务，分写两 collection 必有"主写
   成功 detail 写失败"的可能；嵌入即原子。
2. **避免 N+1 读**：查 audit 列表时不必为每条记录再去 detail collection 拉一次。
3. **建模直观**：1:1 关系本就该嵌套。

soul 的 Mongo backend 形式上也定义了 `SysAuditDetailLog @Document(collection = ...)` 类，
但 `AuditLogMongoService` 从来没真正往那个 collection 写——属于 dead code。本模块直接清掉
那个伪 collection 声明，把 `SysAuditDetailLogDocument` 做成纯子节点 POKO。

### 为什么不加 `@Transactional`

soul 的 `MongoAuditService.submit` 和 `AuditLogMongoService.saveAuditLog` 都标 `@Transactional`，
但在 standalone Mongo（最常见部署形态）下事务不生效——加上去只会让读者误以为有跨文档原子性。

- **standalone Mongo**：不支持事务。`@Transactional` silently inert。
- **replica-set Mongo**：支持，但 `repository.insert(docs)` 已经是单批次原子写，
  跨多 docs 的事务买回来的额外一致性对 audit 不是 load-bearing。

明确不加注解 + README 说清楚比"加了但没用"安全。

### 为什么 errors return false（而不是抛异常）

跟 `RdbKtormAuditService` 一致。`IAuditService` 是 audit aspect 调用的，**audit 链路异常
绝对不能打断业务**——业务方法已经执行完了，aspect 失败时唯一合理的行为是记一条 error log
+ 返回 false，让 aspect 决定要不要重试或回退到本地文件。

### `operateIp` 为什么是 `Long?` 而不是 `BigInteger?`

`SysAuditLogVo.operateIp` 是 `Long?`（kudos 当前承诺只覆盖 IPv4 范围）。**如果未来切换到
IPv6**，把本 Document 的 `operateIp` 改成 `BigInteger?` 即可——上游 `kudos-ability-data-docdb-mongo`
已经注册了 `BigIntegerConverters`，会自动以 String 形式持久化保住精度。无需新增配置。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/LogAuditMongoAutoConfiguration` | 装配入口 + `@EnableMongoRepositories` 限定本模块 package |
| `service/MongoAuditService` | `IAuditService` 实现：model → documents → `repository.insert` |
| `repository/SysAuditLogRepository` | `MongoRepository<SysAuditLogDocument, String>` 标准 CRUD |
| `entity/SysAuditLogDocument` | `@Document(collection = "sys_audit_log")` 主文档 |
| `entity/SysAuditDetailLogDocument` | 嵌入子文档（无独立 collection） |
| `service/MongoAuditLogReadOnlyService` | `IAuditLogReadOnlyService` 实现（findById / findDetailById / pagingSearch） |

## 使用示例

```kotlin
// build.gradle.kts
dependencies {
    api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-mongo"))
}
```

```yaml
spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: my-audit-db
```

业务侧的 `@Audit` / `@WebAudit` 注解照常使用——aspect 会拿到 Mongo backend 的 `IAuditService` bean。

## 与其它 audit backend 的搭配

模块装配的 bean 名是 `mongoAuditService` / `mongoAuditLogReadOnlyService`，跟 `audit-rdb-ktorm`
的 `rdbKtormAuditService` / `audit-rdb-clickhouse` 的 `clickhouseAuditService` 同模式
（`@Bean(name=...)` + `@ConditionalOnMissingBean(name=...)`）。**所以多 backend 共存合法**：

| 依赖组合 | 行为 |
|---|---|
| 仅依赖 `kudos-ability-log-audit-mongo` | `@Resource IAuditService` 直接拿到 Mongo impl（上下文里唯一） |
| `mongo` + `rdb-ktorm` | 两个 `IAuditService` 同时注册。业务侧用 `@Resource("mongoAuditService")` 或 `@Resource("rdbKtormAuditService")` 显式指定，或自己加一个 `@Primary` 决定默认 |
| `mongo` + `mq` | MQ 装的 `mqAuditService` 带 `@Primary` 默认赢；想读 Mongo 库时用 `@Resource("mongoAuditLogReadOnlyService")` |
| 读端 + 任意写端 backend | 同上 —— 读端独立路由：`@Resource("mongoAuditLogReadOnlyService")` 拿 Mongo 读，`@Resource("rdbKtormAuditLogReadOnlyService")` 拿 ktorm 读，互不干扰 |

## 读端：`MongoAuditLogReadOnlyService`

写入跟 ktorm 一样有读端对称 `IAuditLogReadOnlyService` 实现。三个方法：

| 方法 | 行为 |
|---|---|
| `findById(id)` | 查主文档；null 时返回 null（不抛） |
| `findDetailById(auditId)` | 查主文档 id=auditId，取嵌入 `detail` 字段并映射到 `SysAuditDetailLogVo`；主文档不存在 / 主文档无 detail 都返回 null |
| `pagingSearch(query, pageNo, pageSize)` | 用 `Criteria` AND 组合 `AuditLogQuery` 非空字段；默认按 `operateTime DESC` 排；执行一次 `count()` + 一次 `find` |

`_Like` 类过滤（`operatorLike` / `moduleCodeLike` / `descriptionLike`）翻译成 case-insensitive
substring regex，调用方输入经过 `Pattern.quote` 转义——业务侧填入 `.` / `*` 等正则元字符
按字面字符匹配，不被当作通配符或正则注入向量。

时间窗口 `[operateTimeFrom, operateTimeTo)` 闭/开区间跟 ktorm 一致。

## 测试覆盖

- `MongoAuditServiceTest` (6) —— 与真实 Mongo 7 (testcontainer) 集成；覆盖：
  - submit 一个完整 model 后 collection 里出现对应 docs，字段 1:1
  - tenantId / subSysCode 优先 entity 自己 → 回退 model 顶层
  - detail 嵌入正确（按 `auditId === SysAuditLogVo.id` 匹配）
  - empty entities list 返回 true（no-op success，不算失败）
  - 没有匹配 detail 的 entity 也能正常入库（`detail` 字段为 null）
- `MongoAuditLogReadOnlyServiceTest` (15) —— 读端覆盖：
  - `findById` 命中 / null
  - `findDetailById` 嵌入子文档读取 / 主文档无 detail / 主文档不存在
  - `pagingSearch` 各 query 字段过滤（tenantId / operatorLike / descriptionLike）
  - 默认 `operate_time DESC` 排序
  - 时间窗口 `[from, to)` 闭/开区间
  - 多 page 切片正确
  - `pageNo < 1` / `pageSize < 1` 被钳位到 1
  - `descriptionLike = ""` 视同无过滤
  - regex metachar 被转义（`.` 不当作通配符）

未覆盖：`@ConditionalOnMissingBean` 让位行为（属于 Spring 自身机制，重复测试性价比低）。

## 已知限制 / 后续工作

- ❗ 无内置查询接口；admin 控制台目前不查询 Mongo audit 记录，需要时业务自加 finder
- ❗ 单 Mongo 数据源；多源路由依赖 `kudos-ability-data-docdb-mongo` 的 follow-up
- ❗ 默认 collection 名 `sys_audit_log` 不可配；需要时改 `@Document(collection = ...)` 注解

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-log:kudos-ability-log-audit:kudos-ability-log-audit-common"))
api(project(":kudos-ability:kudos-ability-data:kudos-ability-data-docdb:kudos-ability-data-docdb-mongo"))

testImplementation(project(":kudos-test:kudos-test-common"))
testImplementation(project(":kudos-test:kudos-test-container"))
```
