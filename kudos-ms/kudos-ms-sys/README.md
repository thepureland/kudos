# kudos-ms-sys

**定位**：**系统（`sys`）原子服务**的 Gradle 聚合模块，承载平台级能力：租户与子系统关系、微服务注册、资源与菜单、字典与参数、国际化、数据源、域与缓存配置、访问规则等。通过 **契约（common）→ 数据库脚本（sql）→ 领域实现（core）→ HTTP/Feign 暴露（api-* / client）** 分层组织代码。

**在工程中的角色**：作为 `kudos-ms` 下的 **系统（`sys`）原子服务**（`SysConsts.ATOMIC_SERVICE_NAME = "sys"`），供控制台、网关及其他微服务通过管理端 HTTP 或 Feign 使用。

---

## 构建与运行

工程根使用 Gradle Wrapper（`./gradlew`），无需本地装 Gradle。以下命令均在仓库根执行。

### 单模块构建 / 测试

```bash
# 仅编译某个子模块（不跑测试）
./gradlew :kudos-ms:kudos-ms-sys:kudos-ms-sys-core:assemble

# 跑某个子模块的测试
./gradlew :kudos-ms:kudos-ms-sys:kudos-ms-sys-core:test
./gradlew :kudos-ms:kudos-ms-sys:kudos-ms-sys-api-admin:test

# 跑整个 sys 聚合的测试（依赖会被一并构建）
./gradlew :kudos-ms:kudos-ms-sys:check
```

> 单测使用 H2（默认 file 模式 `~/h2/sys`，或 H2 TCP server），`Flyway` 在每次启动时跑全套迁移；**不需要** 本地 Postgres / Redis 即可跑通 `*-core` 与 `*-api-admin` 的测试。

### 启动可执行进程（开发态）

`api-admin` / `api-public` / `api-internal` 各自带 Spring Boot `main`，通常组合启动：

```bash
# 启动 admin（含 /api/admin/sys/** REST），最常用
./gradlew :kudos-ms:kudos-ms-sys:kudos-ms-sys-api-admin:bootRun

# 启动 internal（含 /api/internal/sys/** RPC + 需要 Nacos）
./gradlew :kudos-ms:kudos-ms-sys:kudos-ms-sys-api-internal:bootRun

# 仅 Web 装配（基本不直接用）
./gradlew :kudos-ms:kudos-ms-sys:kudos-ms-sys-api-public:bootRun
```

`api-admin` 默认 yml 在 `kudos-ms-sys-api-admin/resources/application.yml`（H2 file + `kudos.ability.cache.enabled=true`）。本地无 Redis 时改为 `false`，否则启动会 `RedisConnectionFailureException`。

### 端口与健康检查

- 端口与 actuator 端点由上层可执行模块（如 `*-api-web` 聚合 jar）的 yml 决定；本仓库内 sys 各模块的 yml 没有显式 `server.port`，默认走 Spring Boot `8080`。
- 健康检查：`GET /actuator/health`（前提是上层装配了 actuator；`kudos-ability-web-springmvc` 是否默认引入以该模块文档为准）。

---

## 子模块文档索引

| 子模块 | 说明文档 |
|--------|----------|
| [kudos-ms-sys-common](kudos-ms-sys-common/README.md) | 跨模块共享契约：API 接口、VO、枚举、校验与常量 |
| [kudos-ms-sys-sql](kudos-ms-sys-sql/README.md) | Flyway 迁移脚本（sys 库表与视图） |
| [kudos-ms-sys-core](kudos-ms-sys-core/README.md) | DAO、Service、缓存、API 实现与 Spring 扫描入口 |
| [kudos-ms-sys-api-admin](kudos-ms-sys-api-admin/README.md) | 管理端 REST 控制器（`/api/admin/sys/**`），控制台 / 管理网关使用 |
| [kudos-ms-sys-api-public](kudos-ms-sys-api-public/README.md) | 对外 Web 进程启动入口与自动配置（`sys-api-web`，无业务控制器） |
| [kudos-ms-sys-api-internal](kudos-ms-sys-api-internal/README.md) | **对内 RPC 控制器（`/api/internal/sys/**`）+ Provider 进程入口**，承接 Feign 客户端调用，附带 Nacos discovery/config 装配 |
| [kudos-ms-sys-client](kudos-ms-sys-client/README.md) | Feign 代理与降级，供其他服务远程调用 sys；含 `FeignDictItemCodeFinder`（不部署 sys-core 时的字典码校验回退） |

---

## 依赖关系与路径暴露（概念）

```
                          ┌─────────────────┐
                          │ kudos-ms-sys-   │   ← 契约层：ISys*Api（含 /api/internal/sys/** 方法注解）
                          │ common          │     VO、enums、SysConsts / SysDictTypes
                          └────────┬────────┘
                                   │
                ┌──────────────────┼──────────────────────────────┐
                ▼                  ▼                              ▼
       ┌─────────────────┐  ┌─────────────────┐         ┌─────────────────┐
       │ kudos-ms-sys-   │  │ kudos-ms-sys-   │         │ kudos-ms-sys-   │
       │ sql             │  │ core            │         │ client          │
       │ (Flyway scripts)│  │ (DAO/Service/   │         │ (Feign proxies, │
       └────────┬────────┘  │  Cache/Event/   │         │  fallbacks,     │
                │           │  @Component Api)│         │  FeignDictItem- │
                └──────────►│                 │         │  CodeFinder)    │
                            └────────┬────────┘         └─────────────────┘
                                     │                          ▲
                ┌────────────────────┼────────────────────┐     │ HTTP /api/internal/sys/**
                ▼                    ▼                    ▼     │
       ┌─────────────────┐  ┌────────────────┐  ┌────────────────┐
       │ kudos-ms-sys-   │  │ kudos-ms-sys-  │  │ kudos-ms-sys-  │
       │ api-public      │  │ api-admin      │  │ api-internal   │
       │ (Web 装配,      │  │ /api/admin/    │  │ /api/internal/ │
       │  无业务路径)    │  │  sys/** REST   │  │  sys/** RPC    │
       │                 │  │ (15 controllers)│ │ (18 controllers,│
       │ Spring Boot     │  │                │  │  + Nacos /     │
       │ main:           │  │ main:          │  │  interservice  │
       │ SysApiWebApp    │  │ SysApiAdminApp │  │  cache provider)│
       └─────────────────┘  └────────────────┘  │ main:          │
                                                │ SysApiProviderApp│
                                                └────────────────┘
```

- **client 仅依赖 common**（及 Feign 能力），避免把 `core` 打进调用方。Feign 代理通过 `/api/internal/sys/**` HTTP 路径调用 **api-internal** 的控制器；后者实现 `common.ISys*Api`，三处共享同一份方法签名。
- **api-admin / api-public / api-internal** 均依赖 **core**，差异在 **挂载的控制器集** 与 **额外能力栈**：
  - `api-admin`：管理端 15 个 `*AdminController`（继承 `BaseCrudController`）
  - `api-public`：仅 Web 装配，无业务控制器
  - `api-internal`：12 个模块共 18 个 `*InternalController`（实现 `ISys*Api`，路径来自接口方法注解）+ Nacos discovery/config + interservice cache provider
- **sql → core**：core 的 `api` 依赖 sql 模块只为把 Flyway 资源打进 classpath，迁移在运行时由 `kudos-ability-data-rdb-flyway` 触发。

### 路径双轨制

| 路径前缀 | 模块 | 鉴权预期 | 调用方 |
|---------|------|---------|--------|
| `/api/admin/sys/**` | api-admin | 网关 / 外部过滤器（**模块内零 `@PreAuthorize`**） | 控制台 / 管理网关 |
| `/api/internal/sys/**` | api-internal | 内网可达性约束（同样无 `@PreAuthorize`） | `kudos-ms-sys-client` 的 Feign 代理 |

> `api-internal` 端点在路径注解上仅由 `common.ISys*Api` 接口定义；`api-admin` 路径由控制器自身的 `@RequestMapping` 定义。两条路径**不允许互相借用**，否则 admin / internal 双轨制崩溃。

---

## 命名与约定（跨模块）

- **默认子系统编码**：`SysConsts.DEFAULT_SUB_SYSTEM_CODE`（`default-sub-system`），与资源、菜单、访问规则等按子系统维度隔离的数据一致。
- **领域 API 三身一体**：`common` 中 `ISys*Api` 在方法上标注 `@*Mapping("/api/internal/sys/...")`，由 **三处** 同时使用：
  - `core` 的 `Sys*Api`（`@Component`）—— 同进程注入实现
  - `api-internal` 的 `Sys*InternalController`（`@RestController`）—— 暴露 HTTP
  - `client` 的 `ISys*Proxy`（`@FeignClient`）—— 远程调用

  三者的方法签名、参数顺序、URL 路径在编译期通过同一接口绑定；fallback 是唯一不参与编译对齐的处，新增方法时需手动补 override。
- **路径前缀双轨制**：管理路径 `/api/admin/sys/**`（仅 `api-admin`） vs 内部 RPC 路径 `/api/internal/sys/**`（仅 `api-internal`，且来自 `common` 接口的方法注解）。这两条路径有不同的鉴权与可达性预期，新增端点时不要混淆。
- **业务模块清单**（与 `common` / `core` 一级目录对齐）：`accessrule`、`cache`、`datasource`、`dict`、`domain`、`i18n`、`locale`、`microservice`、`outline`、`param`、`resource`、`system`、`tenant`，共 13 个。
- **包结构对齐原则**：`common` / `core` / `client` / `api-admin` / `api-internal` 在 `<module>/` 一级目录上完全一致，便于跨模块对照阅读。`api-public` 不含业务子目录，只有 `init`。

## 已知限制 / 后续工作

- ❗ **api-public 进程几乎无价值** — 当前 `api-public` 不挂任何业务控制器，纯 Web 装配壳；
  若没有业务方在外层补 controller，启动它只占进程不提供能力。建议合并到 `api-admin` 或干脆删除
- ❗ **fallback 与 server 端不参与编译对齐** — `*Proxy` 与 `*InternalController` 通过 `ISys*Api`
  绑定，但 `*Fallback` 是 `open class extends AbstractFeignFallbackSupport`，**新增接口方法**
  需手动在 fallback 补 override；漏写时 Feign 调用降级路径走默认抛错而非业务安全返回值
- ❗ **`/api/admin/sys/**` 零 `@PreAuthorize`** — 模块层无任何鉴权；上游网关漏配 = 13 个业务模块的
  CRUD 全暴露
- ❗ **缓存依赖 `sys.sys_cache` 表配置** — `CacheConfigProvider` 启动从 DB 加载缓存配置 + 懒加载
  cacheConfigs；DB 未就绪或表为空时缓存装配静默失败（没有醒目报错）
- ❗ **dict 没有版本号** — `sys_dict_item` 改动后下游字典枚举类需要 `BeanKit` 重新映射；
  缓存版本失效靠 `DictKey.FEIGN_CACHE_PREFIX` 等 keyVersion，无法精确到单条 dict
- ❗ **`SysConsts.DEFAULT_SUB_SYSTEM_CODE` 硬编码** — 业务定制子系统码时需全工程 grep 替换；
  没有配置式接入点

## 改进建议（自动分析 2026-06-11）

> 以下为对 7 个子模块的深度审查中**未直接修改**的发现，按分析维度归类；每条注明文件路径与理由。
> 与上方"已知限制"重复的条目（api-public 空壳、零鉴权、CacheConfigProvider 静默、dict 无版本等）不再重复列出。

### 1. Kotlin 写法

- `kudos-ms-sys-core/src/io/kudos/ms/sys/core/accessrule/service/impl/SysAccessRuleIpService.kt`（51-52 行）：
  `sysAccessRuleHashCache` 用 `@Resource` 字段注入且类型写成内联全限定名，与同类其余依赖的构造器注入风格不一致；
  建议改为构造器参数并正常 import（涉及 Spring 装配时序，未直接改）。
- `kudos-ms-sys-core/src/io/kudos/ms/sys/core/dict/cache/SysDictItemHashCache.kt`（`getDictItemsByIds`）：
  `associateBy + mapNotNull + toMap` 三段链可用 `buildMap` 一次完成；本次仅移除了无副作用的 `.map { it }`。

### 2. 功能缺陷 / 值得补充

- ✅ 已修复（2026-06-11）`kudos-ms-sys-core/src/io/kudos/ms/sys/core/accessrule/service/impl/SysAccessRuleIpService.kt`（`checkIpAccess`）：
  原判定**不区分黑白名单**。方案：`SysAccessRuleIpCacheEntry` 新增 `accessRuleTypeDictCode`（缓存映射同步携带），
  判定抽成纯函数 `decideIpAccess`：命中黑名单=拒绝、存在白名单规则时仅白名单命中=放行、无有效规则=默认放行；
  配套纯逻辑单测 `SysAccessRuleIpAccessDecisionTest`。注意：旧版序列化的缓存条目无类型字段（视为不参与黑白分流，
  绝不误杀），升级后建议对 `SYS_ACCESS_RULE_IPS_BY_SYSTEM_CODE_AND_TENANT_ID` 执行一次 reloadAll 以携带新字段。
  剩余：`WHITELIST_BLACKLIST`（dict code 4）类型的条目级黑白标记数据模型上仍无法表达，目前整体按白名单保守处理。
- `kudos-ms-sys-core/src/io/kudos/ms/sys/core/dict/service/impl/SysDictItemService.kt`（`recursionFindAllParentId`）与
  `kudos-ms-sys-core/src/io/kudos/ms/sys/core/resource/service/impl/SysResourceService.kt`（`collectParentIds`）：
  父链递归/循环**无环保护**，脏数据形成 parentId 环时会栈溢出或死循环；建议加已访问集合或深度上限。
- 字典 / 参数仅按 `atomicServiceCode` 隔离，**没有 tenantId 维度**；多租户定制字典/参数（覆盖平台默认值）目前无法表达，
  与本服务自身承载的 tenant 模块能力不对称。

### 3. 安全性

- ✅ 已修复（2026-06-11）`kudos-ms-sys-common/src/io/kudos/ms/sys/common/datasource/vo/response/SysDataSourceRow.kt` / `SysDataSourceDetail.kt` / `SysDataSourceEdit.kt`：
  原响应 VO 直接回显 `password`。方案：`SysDataSourceAdminController` 在出口统一脱敏（pagingSearch / getDetail / getEdit /
  listByTenantId / listBySubSystemCode 非空密码替换为固定掩码 `SysDataSourceConsts.PASSWORD_MASK`），
  编辑链路同步处理 —— `SysDataSourceService.update` 收到空/掩码密码时回填库中原密文（即"不修改密码"，改密走 `resetPassword`）；
  单测 `SysDataSourcePasswordMaskingTest`（api-admin）+ `SysDataSourcePasswordPolicyTest`（core）。
  剩余：内部 RPC（`ISysDataSourceApi` 缓存条目）仍携带密文密码，属建连必需，依赖 internal 路径隔离。
- ✅ 已修复（2026-06-11）`kudos-ms-sys-api-admin/src/io/kudos/ms/sys/api/admin/controller/cache/SysCacheAdminController.kt`（`/management/getValueJson`）：
  原可按 key 导出**任意缓存值的 JSON**。方案：`SysCacheService.getValueJson` 内置敏感缓存名黑名单
  （含 `SYS_DATA_SOURCE__HASH`），命中时抛 `CACHE_VALUE_EXPORT_FORBIDDEN` 拒绝导出；
  单测 `SysCacheSensitiveValueGuardTest`。新缓存若承载敏感数据需同步加入 `SENSITIVE_VALUE_CACHE_NAMES`。
- `kudos-ms-sys-api-admin/src/io/kudos/ms/sys/api/admin/controller/datasource/SysDataSourceAdminController.kt`（`testConnection`）：
  接受任意 JDBC url 即时建连，存在内网探测（SSRF 类）与恶意驱动参数面；建议限制驱动/host 白名单。
- 各 `*AdminController` 继承的 `batchDelete`：ids 集合**无数量上限**，一次请求可携带数万 id；建议统一加 size 上限。

### 4. 测试覆盖

- `kudos-ms-sys-api-admin/test-src` 仅 1 个启动冒烟测试，15 个 Controller 的参数绑定 / 必填性 / 返回结构无 MockMvc 回归。
- `kudos-ms-sys-api-internal`、`kudos-ms-sys-client`（19 个 fallback 的安全返回值）、`kudos-ms-sys-common`
  （`IIpStringToBigDecimalSupport` / `IIpBigDecimalToStringSupport` 的 IPv4/IPv6 转换边界）均无任何测试源集。
- `kudos-ms-sys-core` 覆盖良好（60 个测试类），但 `VSysDictItemService` 无专门测试类。

### 5. 可扩展性

- `kudos-ms-sys-api-admin/src/io/kudos/ms/sys/api/admin/controller/auditLog/SysAuditLogAdminController.kt`：
  `@Resource(name = "rdbKtormAuditLogReadOnlyService")` 魔法 bean 名硬编码，切换审计后端（ClickHouse/Mongo）需改代码；建议配置化 bean 名或 `@Qualifier` 常量。
- `kudos-ms-sys-api-admin/.../auditLog/AuditLogPagingRequest.kt`：UI 日期格式 `"yyyy-MM-dd HH:mm:ss"` 硬编码于 companion；前端改版需改代码。
- `checkIpAccess` 的黑白名单判定策略不可插拔，建议抽 SPI（2026-06-11 已把判定收敛为纯函数 `decideIpAccess`，语义缺陷已修复，SPI 化仍待做）。

### 6. 可观测性

- `kudos-ms-sys-core/src/io/kudos/ms/sys/core/accessrule/cache/AccessRuleIpsBySubSysAndTenantIdCache.kt`（`getAccessRuleIps`）：
  "数据库无该维度规则"打 `warn`，但"未配置规则"是常态场景，会持续刷 warn 日志；建议降为 `debug`。
- 各 service 的批量变更（如 `SysAccessRuleIpService.deleteByRuleId`）只有 `debug` 日志、无操作者上下文；
  建议高危写操作接入 `kudos-ability-log-audit` 审计切面。

### 7. 可维护性

- `kudos-ms-sys-core/src/io/kudos/ms/sys/core/accessrule/dao/SysAccessRuleIpDao.kt`：
  52 行遗留裸 `//TODO`（排序列解析的双表 fallback 逻辑待收敛）；184-197 行整段注释掉的 `mapRowToRecord` 死代码应删除。
- `kudos-ms-sys-core/src/io/kudos/ms/sys/core/i18n/service/impl/SysI18NService.kt`：
  类名 `SysI18NService`（大写 N）与 `ISysI18nService` / `SysI18nHashCache`（小写 n）大小写不一致，IDE 检索易漏；属公开类名，未直接改。
- 13 个 service 的 "insert/update/delete + 发事件" 样板仍各自重复（`completeCrud*` 只统一了日志部分），可上提为带事件钩子的模板基类。

### 8. 对外接口（API contract）

- `kudos-ms-sys-core/.../dict/service/impl/SysDictService.kt`（`dictCacheKey`）+ `common/.../dict/api/ISysDictApi.kt`：
  `batchGetActiveDictItems(Map)` 返回 Map 的 key 是**翻转后的** `(atomicServiceCode, dictType)`，与入参 Pair `(dictType, atomicServiceCode)` 顺序相反 ——
  调用方用原入参 Pair 查返回 Map 必然 miss，是 contract 级认知陷阱（HTTP 适配端点的 key 同样是 `"asCode|dictType"`，其 KDoc 本次已修正）。
  建议下一个 breaking 版本统一为 key = 入参 Pair。
- `kudos-ms-sys-api-admin/.../cache/SysCacheAdminController.kt`：`/management/reload`、`/management/reloadAll` 是**有副作用**的操作却映射为 GET，
  违背 HTTP 语义（网关/审计通常对 GET 放宽）；建议改 POST（涉及前端联动，未直接改）。
- admin / internal / public 三层划分本身清晰合理；internal 路径由 `common` 接口注解单点定义的"三身一体"机制值得保持。

### 9. 文档

- `kudos-ms-sys-sql/resources/sql/sys/h2/V1.0.0.24__seed_sys_locale_cache.sql`、`V1.0.0.25__seed_sys_out_line_cache.sql`：
  种子脚本无头部注释说明"该行 `sys_cache` 配置与哪个 `*Cache` Handler（`CACHE_NAME` 常量）对应、删除会导致缓存静默失效"；建议补 2-3 行注释。
- 控制器层个别 KDoc 与实现不符（`SysDictAdminController.getDict` 返回类型、dict 批量 HTTP 适配端点的 key 顺序）—— 本次已直接修正。
