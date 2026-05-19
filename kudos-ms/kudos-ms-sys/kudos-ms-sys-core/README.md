# kudos-ms-sys-core

## 定位

系统（`sys`）原子服务的 **领域实现中心**：在 `kudos-ms-sys-common` 契约与 `kudos-ms-sys-sql` 表结构之上，提供 **Ktorm 数据访问、业务服务、多级缓存、领域事件、以及 `ISys*Api` 的 Spring 实现**。本模块不包含 HTTP 控制器——管理控制器在 `kudos-ms-sys-api-admin`，对内 RPC 控制器在 `kudos-ms-sys-api-internal`。

**包结构与 `common` 对齐**：`core` 同样以业务模块为一级目录（`io.kudos.ms.sys.core.<module>`），每个模块内再按分层分子包 `model/`（`table`、`po`）、`dao/`、`service/`（`iservice` + `impl`）、`cache/`、`api/`、`event/`、可选 `support/`。这是历史 README 描述的 "flat 分层" 的实际形态——不是 `core.dao.SysXxxDao` 而是 **`core.<module>.dao.SysXxxDao`**。**字典项** 的契约与 VO 已在 **`common.dict`** 的 `api` / `vo` 叶子包（与字典头同模块），**无** `dictitem` 子包；**规则 IP** 同理在 **`common.accessrule`**，**无** `accessruleip` 子包；实现侧在 **`core.dict`** / **`core.accessrule`** 下同层。

---

## 按业务划分（与 `common` 模块包对齐）

| `common` 下模块名 | 在 `core` 中的作用概述 |
|-------------------|------------------------|
| **accessrule** | **访问规则与 IP 明细**：主表按子系统/租户等配置策略；**IP（或 IP 段）** 挂在规则下，与主表联动；与视图 `VSysAccessRuleWithIp` 联合查询配合。对应 `SysAccessRule*`、`SysAccessRuleIp*` DAO/Service，`AccessRuleIpsBySubSysAndTenantIdCache`、`SysAccessRuleApi`、`SysAccessRuleIpApi` 及规则侧缓存一致性。 |
| **cache** | **业务侧「缓存配置」元数据**（非本 README 中的 `io.kudos.ms.sys.core.cache` 基础设施包）：登记各业务缓存模块、名称等，供运维/控制台管理；对应 `SysCache*`、`SysCacheHashCache`、`SysCacheApi`。 |
| **datasource** | 多租户/多子系统下的数据源定义与查询：与连接信息、作用范围相关；对应 `SysDataSource*`、`SysDataSourceHashCache`、`SysDataSourceApi`。 |
| **dict** | **字典头与字典项**：字典类型（头）的树形结构、编码约束；**字典项**挂接在头下，配合项编码解析。对应 `SysDict*`、`SysDictItem*`、`VSysDictItem*` 视图服务、`SysDictHashCache`、`SysDictItemHashCache`、`SysDictApi`、`SysDictItemApi`，以及 `DictItemCodeFinder` 等支撑。 |
| **domain** | 业务域（`SysDomain`）：用于域隔离、路由或切换上下文的领域标识；对应 `SysDomain*`、`DomainByNameCache`、`SysDomainApi`。 |
| **i18n** | 国际化键值与多语言文案：按原子服务/命名空间等维度管理；对应 `SysI18n*`（实现中多为 `SysI18NService` 命名）、`SysI18nHashCache`、`SysI18nApi`。 |
| **locale** | **语言/区域全局字典**：维护 locale code（如 `zh_CN`）及显示名称；`LocaleByCodeCache` 按 code 单 key 缓存；对应 `SysLocale*` DAO / Service / Api，由 V1.0.0.23 表 + V1.0.0.24 seed 提供数据兜底。**与 `tenant.SysTenantLocale*`（租户↔语言绑定）配合使用，不是同一概念。** |
| **microservice** | 平台注册的微服务元数据（编码、名称等）：对应 `SysMicroService*`、`SysMicroServiceHashCache`、`SysMicroServiceApi`。子系统与微服务的 **绑定关系** 在表结构中单立，由 `SysSubSystemMicroService*` Service/API 实现（契约见 `ISysSubSystemMicroServiceApi`，无独立 `vo` 子包时仍属本能力块）。 |
| **outline** | **大纲（OutLine）**：按 (子系统编码, 租户 ID) 复合键缓存的轮廓数据；键归一化由 `OutLineSystemTenantKey` 统一，缓存为 `OutLineBySystemAndTenantCache`，对应 `SysOutLineDao` / `SysOutLineApi`；数据来源 V1.0.0.22 表 + V1.0.0.25 seed。 |
| **param** | 系统/模块级参数键值：按模块名与参数名定位；对应 `SysParam*`、`ParamByModuleAndNameCache`、`SysParamApi`。 |
| **resource** | 资源与菜单树（含 URL、类型、图标、层级）：支撑控制台路由与权限资源模型；对应 `SysResource*`、`SysResourceHashCache`、`SysResourceApi`（含菜单树构建等）。 |
| **system** | **子系统**（subsystem / system）主数据：与租户、资源、访问规则等按 `subSystemCode` 关联；对应 `SysSystem*`、`SysSystemHashCache`、`SysSystemApi`。 |
| **tenant** | **租户**主数据及其扩展关系：包括租户与 **子系统**、**资源**、**默认语言** 等关联（在 `common.tenant.vo` 内以多种 CacheEntry/表单体现）；对应 `SysTenant*`、`SysTenantSystem*`、`SysTenantResource*`、`SysTenantLocale*` 等 Service 与 DAO、`TenantByIdCache`、`SysTenantSystemHashCache`、`SysTenantApi` / `SysTenantSystemApi` / `SysTenantResourceApi` / `SysTenantLocaleApi` 等。 |

---

## 架构分层

> **实际包结构是按业务模块分的，不是按层分的**：`core.<module>.<layer>`。下文按 **分层** 描述每层应有的子包，但物理上每层都散落在各 `<module>/` 子目录里。

### 横切层：`io.kudos.ms.sys.core.platform`

承载非领域专属的"原子服务级"装配与工具，是其它分层的依赖前提。

| 子包 | 内容 |
|------|------|
| `platform.init` | **`SysAutoConfiguration`**：`@Configuration` + `@ComponentScan("io.kudos.ms.sys.core")`，`@AutoConfigureAfter(KtormAutoConfiguration::class)`；实现 `IComponentInitializer`，组件名 `kudos-ms-sys-core`，参与 Kudos 启动编排（不依赖 Spring Boot 原生 `META-INF/spring.factories`）。详见 [project_autoconfig_spi](../../../.claude/memory/project_autoconfig_spi.md) 描述的 IComponentInitializer 机制。 |
| `platform.cache` | **`CacheConfigProvider`**：领域缓存的容量 / TTL 集中配置入口。 |
| `platform.service.impl` | **`CrudLogSyncSupport`** 等横切工具：审计日志钩子，被各业务 Service 选择性调用。 |

### 业务模块层：`io.kudos.ms.sys.core.<module>`

每个业务模块（见上表 13 项）内部统一以下子包（按需出现）：

| 子包 | 内容 | 典型类型 |
|------|------|---------|
| `model.table` | Ktorm `Table<*>` / `BaseTable<*>` 表对象 | `SysTenants`、`SysResources`、`VSysDictItems`（视图也在此层） |
| `model.po` | 持久化 / 视图行对象 | `SysTenant`、`VSysDictItem` |
| `dao` | Ktorm DAO；与表一一对应（含视图 DAO 前缀 `VSys*`） | `SysTenantDao`、`VSysAccessRuleWithIpDao` |
| `service.iservice` | 服务接口 | `ISysTenantService`、`IVSysAccessRuleIpService` |
| `service.impl` | 业务实现（`open class`，参见下文 Kotlin 风格） | `SysTenantServiceImpl` |
| `cache` | 领域缓存处理器（多级 Caffeine + Redis），与 Service 通过事件解耦 | `TenantByIdCache`、`SysDictHashCache`、`LocaleByCodeCache`、`OutLineBySystemAndTenantCache`、`AccessRuleIpsBySubSysAndTenantIdCache` |
| `event` | 领域事件定义（`Sys*Inserted/Updated/Deleted/BatchDeleted`），供 `cache` 层用 `@TransactionalEventListener(AFTER_COMMIT)` 订阅 | `TenantEvents.kt`、`AccessRuleIpEvents.kt`、`DictEvents.kt`、`DictItemEvents.kt`（同一模块可拆多个事件文件） |
| `api` | `@Component` 形态的 `Sys*Api`，实现 `common` 中 `ISys*Api`，方法签名与路径与接口完全一致，供 **同进程注入** 与 **`api-internal` 控制器复用** | `SysTenantApi`、`SysOutLineApi` |
| `support`（可选） | 模块内辅助类，避免 Service 膨胀 | `dict/support/DictItemCodeFinder`、`dict/support/SysDictTypesStartupValidator`（启动期校验 `SysDictTypes` 声明的字典都真实存在） |

> **`core.cache` 不存在为顶层基础设施目录**——`cache` 仅作为各业务模块的子包出现。`common.cache` 模块对应的是"缓存配置元数据"业务表（`SysCache*`），与缓存基础设施名字撞车但是两回事。

---

## Gradle 依赖（要点）

| 依赖 | 作用 |
|------|------|
| `kudos-ms-sys-sql` | 引入 Flyway 脚本资源 |
| `kudos-ms-sys-common` | 契约与 VO |
| `kudos-ability-data-rdb-ktorm` | ORM 与数据访问 |
| `kudos-ability-data-rdb-flyway` | 迁移执行 |
| `kudos-ability-cache-local-caffeine` / `*-remote-redis` | 多级缓存 |
| `kudos-ability-cache-common` | 缓存抽象 |

测试依赖 **H2 / PostgreSQL**、`kudos-test-rdb`，用于 DAO 与 Service 的集成测试。

---

## 测试

- 源码测试位于 `test-src`，测试数据 SQL 位于 **`test-resources/sql/h2/<业务模块>/<分层>/`**：`<业务模块>` 与 `core` / `common` 中的模块名对齐（如 `dict`、`accessrule`、`tenant`；**缓存配置** 元数据相关为 `sys_cache`）；`<分层>` 为 `cache`、`dao`、`service` 之一，文件名为对应 `*Test.sql`（与测试类名一致）。`SqlTestBase` 会在 `sql/h2` 下递归解析。
- 覆盖范围：每个 `cache` / `dao` / `service` 实现类应有对应 `*Test`，新增类应连带补测试 SQL fixture。
- **测试不 mock 数据库**——使用真实 H2 + Flyway 跑全套迁移；这是项目级原则。

---

## 配置键速查

`core` 模块本身不在 `@Value` / `@ConfigurationProperties` 上声明配置，关键键由依赖的 ability 模块消费。运行 `core` 测试或基于 `core` 启动 sys 进程时，最少要在 `application.yml` 提供：

| 键 | 用途 | 典型值（测试 / 本地） |
|----|------|---------------------|
| `spring.datasource.dynamic.primary` | 多数据源默认源名 | `h2`（本地 file） / `h2-tcp`（H2 TCP server） |
| `spring.datasource.dynamic.datasource.<name>.url/username/password/driver-class-name` | 各数据源连接信息 | `jdbc:h2:~/h2/sys;DATABASE_TO_LOWER=TRUE;` |
| `spring.datasource.dynamic.hikari.*` | Hikari 连接池调优 | 见 `kudos-ms-sys-api-admin/resources/application.yml` 模板 |
| `kudos.ability.flyway.datasource-config.sys` | 把 `sys` 库的 Flyway location 绑定到上面哪个数据源名 | `h2` |
| `kudos.ability.cache.enabled` | 是否启用 Redis 远程缓存；**本地无 Redis 时设为 `false`**，否则启动会 `RedisConnectionFailureException` | `true`（默认） |
| `logging.level.org.springframework.core.io.support.SpringFactoriesLoader` | 调 `DEBUG` 时能在控制台看到 ability/auto-config 装配链——排查 `IComponentInitializer` 顺序问题时常用 | `DEBUG`（按需） |

**缓存 TTL / 策略不来自 yml**：`core.platform.cache.CacheConfigProvider` 实现 `ICacheConfigProvider`，在 `@DependsOn("dataSource")` 之后从 **`sys_cache` 表** 读取 active 行，按 `strategyDictCode`（`SINGLE_LOCAL` / `REMOTE` / `LOCAL_REMOTE`）+ `hash` 标志分发到各 cache manager。这意味着：

- 调整某个 cache 的容量 / TTL / 是否走 Redis 不需要改代码，改 `/api/admin/sys/cache` 即可；
- 但 **首次启动**（DB 空表）时 `CacheConfigProvider` 返回空——必须靠 Flyway seed / 运维预置 `sys_cache` 行；
- 缓存名是 cache 类内部的字符串常量（`CACHE_NAME`），与 `sys_cache.name` 必须一致，否则配置查不到默认走 ability 内置 fallback。

> sys 服务在 Nacos / Spring Cloud 上的具体键（如 `spring.cloud.nacos.discovery.server-addr`）由 `kudos-ability-distributed-discovery-nacos` 定义，详见 `api-internal` 的 ability 依赖文档。

---

## 与其他子模块的关系

| 模块 | 关系 |
|------|------|
| **api-admin** | 注入 `ISys*Service`，暴露 `/api/admin/sys/**` REST |
| **api-internal** | 注入 `Sys*Api`（`@Component`），通过实现 `ISys*Api` 接口同时复用方法级 `@*Mapping`，把 `/api/internal/sys/**` 暴露给 Feign 客户端 |
| **api-public** | 仅承载启动入口与 Web 栈装配；运行时随可执行应用一起加载 `core` 的 Bean |
| **client** | **不**依赖 `core`；远程调用走 Feign，服务端仍由本模块提供实现 |

---

## 扩展建议

- 新增业务表：先在 **sql** 模块增加迁移，再在 **model → dao → service → api** 逐层补齐。
- 对外暴露的新能力：先在 **common** 扩展 `ISys*Api` 与 VO，再在 **core** 实现并编写测试。

---

## 缓存一致性模型

本模块缓存全部走"`@TransactionalEventListener(AFTER_COMMIT)` 订阅业务事件"模式：

- 单条变更 → `Sys*Inserted/Updated/Deleted` 事件
- 批量删除 → `Sys*BatchDeleted`，**附 (id + 维度键) snapshot**：AFTER_COMMIT 时行已删，
  cache key 必须提前 snapshot
- 维度变更 → `Sys*Updated.dimensionChanged + beforeSystemCode/beforeTenantId`：同时刷新
  新旧两个缓存槽位（如 [AccessRuleIpsBySubSysAndTenantIdCache.onParentUpdated]）

### 事件清单

每个业务模块有一个 `sealed interface Sys*Event` 与四种 `data class`：`Inserted` / `Updated` / `Deleted` / `BatchDeleted`。Service 在事务内 `applicationEventPublisher.publishEvent(...)`，`*Cache` 用 `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` 订阅。

| 模块 | 事件 sealed 根 | 文件 | 维度键 / 注意点 |
|------|--------------|------|----------------|
| `accessrule` | `SysAccessRuleEvent` | `accessrule/event/AccessRuleEvents.kt` | Inserted/Updated/Deleted 携带 `(systemCode, tenantId)`；Updated 额外有 `before*` + `dimensionChanged`；BatchDeleted 携带 `dimensions: List<Pair<String, String?>>` |
| `accessrule` (IP) | `SysAccessRuleIpEvent` | `accessrule/event/AccessRuleIpEvents.kt` | 子事件按 **父规则维度** `parentSystemCode/parentTenantId` 携带；Insert/Update 携带 `active` 标志 |
| `cache` | `SysCacheEvent` | `cache/event/SysCacheEvents.kt` | 纯按 id |
| `datasource` | `SysDataSourceEvent` | `datasource/event/DataSourceEvents.kt` | 纯按 id |
| `dict` | `SysDictEvent` | `dict/event/DictEvents.kt` | 纯按 id |
| `dict` (item) | `SysDictItemEvent` | `dict/event/DictItemEvents.kt` | Deleted 含父字典 id 等附加字段（见源码） |
| `domain` | `SysDomainEvent` | `domain/event/DomainEvents.kt` | Deleted 含 `name`（按名查缓存 `DomainByNameCache`） |
| `i18n` | `SysI18nEvent` | `i18n/event/I18nEvents.kt` | 纯按 id |
| `locale` | `SysLocaleEvent` | `locale/event/LocaleEvents.kt` | Deleted 含 `code`（按 code 缓存 `LocaleByCodeCache`） |
| `microservice` | `SysMicroServiceEvent` | `microservice/event/MicroServiceEvents.kt` | 纯按 id |
| `outline` | `SysOutLineEvent` | `outline/event/OutLineEvents.kt` | Deleted 含 `(systemCode, tenantId)` 维度键 |
| `param` | `SysParamEvent` | `param/event/ParamEvents.kt` | Deleted 含 `(moduleName, paramName)` |
| `resource` | `SysResourceEvent` | `resource/event/ResourceEvents.kt` | 纯按 id |
| `system` | `SysSystemEvent` | `system/event/SystemEvents.kt` | 纯按 id |
| `tenant` | `SysTenantEvent` | `tenant/event/TenantEvents.kt` | 纯按 id |
| `tenant` (system 绑定) | `SysTenantSystemEvent` | `tenant/event/TenantSystemEvents.kt` | 维度键 `(tenantId, subSystemCode)`，由 `SysTenantSystemHashCache` 订阅 |

**模板**（accessrule 是最完整范例，其他模块按需裁剪）：

```kotlin
sealed interface SysXxxEvent { val id: String }

data class SysXxxInserted(override val id: String, /* 维度键... */) : SysXxxEvent
data class SysXxxUpdated(
    override val id: String,
    /* 当前维度键 */
    /* before* 维度键（如有维度迁移可能）*/
) : SysXxxEvent {
    val dimensionChanged: Boolean get() = /* before != current */
}
data class SysXxxDeleted(override val id: String, /* 维度键 snapshot */) : SysXxxEvent
data class SysXxxBatchDeleted(
    val ids: Collection<String>,
    val dimensions: List<Pair<...>>,   // AFTER_COMMIT 时行已删，必须提前 snapshot
) : SysXxxEvent { override val id get() = ids.first() }
```

**为何 BatchDeleted 一定要带 dimensions**：`AFTER_COMMIT` 触发时 DB 行已被删除，订阅方反查必空——所以 Service 必须在删除前把维度键 snapshot 进事件载荷。漏带 = 缓存脏数据。新加按维度键聚合的缓存时，必须同步检查对应 BatchDeleted 是否携带了所需维度。

**复合键归一化**：`tenantId` 可为 null（平台级）/ 空白 / 具体值。
- 访问规则 IP：`AccessRuleTenantKey.compositeKey(systemCode, tenantId)` → `systemCode::tenantId`
- 大纲：`OutLineSystemTenantKey.compositeKey(systemCode, tenantId)` 同形态

null / 空白都映射到空串 —— `@Cacheable` SpEL 和 `doReload` 必须保持一致；新增按 (subSystem,
tenant) 维度的缓存时，**复用现有 `*Key` 工具**，不要在 SpEL 里手写 `?:''` 拼串。

**Cache 基类三选一**（来自 `kudos-ability-cache-common`）：

| 基类 | 用途 |
|------|------|
| `AbstractByIdCacheHandler<ID, V, DAO>` | 按主键单条缓存（`TenantByIdCache`、`DomainByNameCache`） |
| `AbstractKeyValueCacheHandler<K, V>` | 任意 key→value，复合键场景（`AccessRuleIpsBySubSysAndTenantIdCache`、`OutLineBySystemAndTenantCache`、`LocaleByCodeCache`） |
| `AbstractHashCacheHandler<F, V>` | Redis Hash 形态的"全量分组"缓存（`SysDictHashCache`、`SysI18nHashCache` 等） |

**典型形态**：

```kotlin
open class TenantByIdCache : AbstractByIdCacheHandler<String, SysTenantCacheEntry, SysTenantDao>() {
    fun getTenantById(id: String): SysTenantCacheEntry? =
        getSelf<TenantByIdCache>().doReload(id)        // 自调用必须走代理

    @Cacheable(cacheNames = [CACHE_NAME], key = "#id", unless = "#result == null")
    override fun doReload(id: String): SysTenantCacheEntry? = /* DAO 查询 */

    @TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
    open fun onUpdated(event: SysTenantUpdated) {
        getSelf<TenantByIdCache>().evict(event.id)
    }
}
```

- **`getSelf<XxxCache>()`** 拿到 Spring CGLIB 代理对象——同类内部直接调 `this.foo()` 会绕过代理，`@Cacheable` / `@CacheEvict` 不生效。Kotlin + Spring AOP 的固有坑。
- **`fallbackExecution = true`** 让事件在无事务上下文（如直接 publish）时也执行，避免漏失效。

## 已知限制 / 安全考量

- ❗ **零 `@PreAuthorize`**：admin / internal 控制器全部依赖网关 / 外部鉴权过滤器做访问
  控制。`sys-api-admin` 下的 tenant / system / accessrule / dict / param 写入端点若网关
  挂了，可被未授权用户直接调用
- ❗ **`SysParam` 是否敏感无标志位**：参数表既存普通配置也可能存敏感数据（密钥 / 凭证），
  当前没有 `secret=true` 字段或加密标志——调用方需自行甄别
- ❗ **`SysAccessRuleIp` IP 段格式不强校验**：`ip_start` / `ip_end` 是文本，无 CIDR / IPv4
  范围校验。错误配置（如 ipStart > ipEnd / 非法 IP 字符）会在运行时让所有匹配失败
- ❗ **字典 / 资源 / 菜单树深度不限**：`SysResource` / `SysDict` 都是树，递归展开未做深度
  上限。异常配置（自引用 / 极深嵌套）会让 cache reload 跑很久
- ❗ **跨服务调用未做并发限流**：sys-core 自身不是 client 消费方，但其暴露的 batch endpoint
  （`getResourcesByIds` 等）若被 client 滥用没有限流
- ❗ **`SysI18N` 多语言文案大小无上限**：单条文案过大会撑爆 redis hash entry。建议在
  form 校验加 maxLength
- ❗ **审计日志接入不一致**：部分 Service 调用 `AuditLogTool`，部分没接。租户 / 子系统等
  关键变更建议统一审计

## Kotlin 风格

- 一律 `open class` + Spring CGLIB 代理——`@Transactional` / `@Cacheable` 切面要求方法 `open`
- DAO 通过 ctor 注入；Service 大部分通过 `@Resource` 注入避免循环依赖
  （Cache 之间 / Service 之间互引时常见）
- Cache 类用 `getSelf<XxxCache>()` 拿 Spring 代理对象——让 `@Cacheable` 在同类内部调用
  也能生效（绕过 self-invocation 不走代理的问题）
