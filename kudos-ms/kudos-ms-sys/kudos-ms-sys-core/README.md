# kudos-ms-sys-core

## 定位

系统（`sys`）原子服务的 **领域实现中心**：在 `kudos-ms-sys-common` 契约与 `kudos-ms-sys-sql` 表结构之上，提供 **Ktorm 数据访问、业务服务、多级缓存、以及 `ISys*Api` 的 Spring 实现**。不包含 HTTP 控制器（控制器在 `kudos-ms-sys-api-admin`）。

`core` **不按** `vo` 同名建包，业务能力分布在 `model` / `dao` / `service` / `cache` / `api` 等通用分层中；下表按 **`kudos-ms-sys-common` 中 `io.kudos.ms.sys.common.<模块>` 的业务模块名**归纳各块在 `core` 中的职责（与 `ISys*Api`、`Sys*Service`、`Sys*Dao`、`*HashCache` 等对应）。**字典项** 的契约与 VO 已在 **`common.dict`** 的 `api` / `vo` 叶子包（与字典头同模块），**无** `dictitem` 子包；**规则 IP** 同理在 **`common.accessrule`**，**无** `accessruleip` 子包；实现侧在 **`core.dict`** / **`core.accessrule`** 下同层。

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
| **microservice** | 平台注册的微服务元数据（编码、名称等）：对应 `SysMicroService*`、`SysMicroServiceHashCache`、`SysMicroServiceApi`。子系统与微服务的 **绑定关系** 在表结构中单立，由 `SysSubSystemMicroService*` Service/API 实现（契约见 `ISysSubSystemMicroServiceApi`，无独立 `vo` 子包时仍属本能力块）。 |
| **param** | 系统/模块级参数键值：按模块名与参数名定位；对应 `SysParam*`、`ParamByModuleAndNameCache`、`SysParamApi`。 |
| **resource** | 资源与菜单树（含 URL、类型、图标、层级）：支撑控制台路由与权限资源模型；对应 `SysResource*`、`SysResourceHashCache`、`SysResourceApi`（含菜单树构建等）。 |
| **system** | **子系统**（subsystem / system）主数据：与租户、资源、访问规则等按 `subSystemCode` 关联；对应 `SysSystem*`、`SysSystemHashCache`、`SysSystemApi`。 |
| **tenant** | **租户**主数据及其扩展关系：包括租户与 **子系统**、**资源**、**默认语言** 等关联（在 `common.tenant.vo` 内以多种 CacheEntry/表单体现）；对应 `SysTenant*`、`SysTenantSystem*`、`SysTenantResource*`、`SysTenantLocale*` 等 Service 与 DAO、`TenantByIdCache`、`SysTenantSystemHashCache`、`SysTenantApi` / `SysTenantSystemApi` / `SysTenantResourceApi` / `SysTenantLocaleApi` 等。 |

---

## 架构分层

### `io.kudos.ms.sys.core.init`

- **`SysAutoConfiguration`**  
  - `@Configuration` + `@ComponentScan("io.kudos.ms.sys.core")`  
  - `@AutoConfigureAfter(KtormAutoConfiguration::class)`，保证数据源与 Ktorm 就绪后再装配本域 Bean。  
  - 实现 `IComponentInitializer`，组件名为 **`kudos-ms-sys-core`**，供 Kudos 统一启动编排识别。

### `io.kudos.ms.sys.core.model`

| 子包 | 内容 |
|------|------|
| `table` | Ktorm 表对象（如 `SysSystems`、`SysTenants`、`SysResources` 等） |
| `po` | 持久化实体 / 行对象（与表或视图对应） |

### `io.kudos.ms.sys.core.dao`

数据访问对象，与表一一对应，例如：`SysTenantDao`、`SysResourceDao`、`SysDictDao`、`SysAccessRuleDao`、`VSysAccessRuleWithIpDao`（视图查询）等。复杂列表/条件查询在 Service 层组合。

### `io.kudos.ms.sys.core.service`

| 子包 | 内容 |
|------|------|
| `iservice` | 服务接口（`ISys*Service` 及只读/视图专用接口如 `IVSysAccessRuleIpService`） |
| `impl` | 业务实现：租户、资源、字典、参数、I18N、数据源、域、缓存、微服务、子系统绑定、访问规则与 IP、租户扩展（资源/语言/子系统关系）等 |

部分实现依赖 **本地 Caffeine + 远程 Redis** 缓存能力（见 `build.gradle.kts`），与 `cache` 包协同。

### `io.kudos.ms.sys.core.cache`

> 注意：此处 `core.cache` 指 **缓存基础设施与领域缓存处理器**；与上表中 **`vo.cache` 所指的「缓存配置」业务表**（`SysCache*`）不是同一概念。

领域缓存的 **装载与一致性** 逻辑，例如：

- `SysResourceHashCache`、`SysTenantSystemHashCache`、`TenantByIdCache`
- `SysDictHashCache`、`SysDictItemHashCache`、`SysI18nHashCache`
- `ParamByModuleAndNameCache`、`DomainByNameCache`、`SysCacheHashCache`
- `AccessRuleIpsBySubSysAndTenantIdCache` 等

与 Service 配合，在增删改后同步或失效缓存。

### `io.kudos.ms.sys.core.api`

- **`Sys*Api`**（`@Component`）：实现 `common` 中对应的 **`ISys*Api`**，委托给相应 `ISys*Service`，供 **同进程内** 其他 Bean 直接注入调用（与 Feign 远程调用并存）。

### `io.kudos.ms.sys.core.support`

- 如 **`DictItemCodeFinder`** 等辅助逻辑，减轻 Service 重复代码。

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
- 覆盖缓存、DAO、Service 等（以仓库内实际测试类为准）。

---

## 与其他子模块的关系

| 模块 | 关系 |
|------|------|
| **api-admin** | 注入 `ISys*Service`，暴露 REST |
| **api-public / api-internal** | 运行时随可执行应用一起加载 `core` 的 Bean |
| **client** | **不**依赖 `core`；远程调用走 Feign，服务端仍由本模块提供实现 |

---

## 扩展建议

- 新增业务表：先在 **sql** 模块增加迁移，再在 **model → dao → service → api** 逐层补齐。
- 对外暴露的新能力：先在 **common** 扩展 `ISys*Api` 与 VO，再在 **core** 实现并编写测试。
