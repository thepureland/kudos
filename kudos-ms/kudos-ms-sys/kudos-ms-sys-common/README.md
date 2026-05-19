# kudos-ms-sys-common

## 定位

系统（`sys`）原子服务的**共享契约层**：不包含持久化、缓存实现或 HTTP 控制器，只提供跨 `core`、`client`、各 `api-*` 模块复用的 **类型与接口**。依赖面刻意保持精简（仅 `kudos-context` 等基础能力），便于其他微服务仅引用本模块即可编译通过 Feign 接口与 DTO。

---

## 包结构（`io.kudos.ms.sys.common`）

**第一层按业务模块划分**，模块名与历史 `vo` 下的一级目录一致：

`accessrule`、`cache`、`datasource`、`dict`、`domain`、`i18n`、`locale`、`microservice`、`outline`、`param`、`resource`、`system`、`tenant`

**字典头** 与 **字典项** 同属 **`dict`**：`ISysDictApi` / `ISysDictItemApi` 均在 **`dict.api`**；相关 VO 在 **`dict.vo`**（含 `SysDictItem*` 等），**无** `dictitem` 子包。**访问规则 IP** 与主规则同属 **`accessrule`**（`accessrule.api` / `accessrule.vo`），**无** `accessruleip` 子包。**租户语言**（`SysTenantLocaleApi`）和**语言/区域主数据**（`SysLocaleApi`）是 **两个独立模块**：前者属 `tenant`（租户与语言的绑定），后者属 `locale`（全局语言字典）。

**第二层**在各模块下按类型分为四类子包（若该模块暂无某类内容可省略目录）：

| 子包名 | 内容 |
|--------|------|
| **`api`** | 该模块对应的 `ISys*Api` 契约（若有） |
| **`consts`** | 该模块专用常量（若有；绝大多数模块当前无独立 consts） |
| **`enums`** | 错误码枚举 `*ErrorCodeEnum`、领域枚举（如 `ResourceTypeEnum`）等 |
| **`vo`** | 值对象：`vo/request`、`vo/response` 及根下的 `*CacheEntry` 等 |

**跨模块、不属于单一业务域**的契约放在 **`io.kudos.ms.sys.common.platform`** 下：

| 子包 | 说明 |
|------|------|
| **`platform.consts`** | 全局常量，如 **`SysConsts`**（含 `ATOMIC_SERVICE_NAME = "sys"`、`DEFAULT_SUB_SYSTEM_CODE`）、**`SysDictTypes`**（启动期由 `core.dict.support.SysDictTypesStartupValidator` 校验真实存在） |

> 早期版本另含 `platform.validation`（`DictCodeValidator` 等）。当前实现将字典编码校验下沉到 `core.dict.support`，`common` 不再承载校验逻辑，仅保留常量契约。

示例（租户模块）：

- `io.kudos.ms.sys.common.tenant.api` — `ISysTenantApi`、`ISysTenantSystemApi`、`ISysTenantResourceApi`、`ISysTenantLocaleApi`
- `io.kudos.ms.sys.common.tenant.enums` — `SysTenantErrorCodeEnum`
- `io.kudos.ms.sys.common.tenant.vo` — `request` / `response` 及 `SysTenantCacheEntry` 等

各模块 `ISys*Api` 与模块对应关系与迁移前一致，仅包路径由 `common.api` / `common.vo.*` / `common.enums.*` 调整为 **`common.<模块>.api|enums|vo`**。

---

## 接口一览（`ISys*Api`）

接口方法以**同步、面向缓存或领域服务**为主；具体语义见各接口 KDoc 与 `core` 实现。

| 接口 | 模块包 | 职责概要 |
|------|--------|----------|
| `ISysTenantApi`、`ISysTenantSystemApi`、`ISysTenantResourceApi`、`ISysTenantLocaleApi` | `tenant.api` | 租户及租户-子系统 / 资源 / 语言绑定 |
| `ISysSystemApi` | `system.api` | 子系统 |
| `ISysMicroServiceApi`、`ISysSubSystemMicroServiceApi` | `microservice.api` | 微服务及子系统-微服务绑定 |
| `ISysResourceApi` | `resource.api` | 资源 / 菜单树 |
| `ISysDictApi` / `ISysDictItemApi` | `dict.api`（并列两个接口） | 字典与字典项 |
| `ISysParamApi` | `param.api` | 系统参数 |
| `ISysI18nApi` | `i18n.api` | 国际化（实现侧类名为 `SysI18NService` 的历史命名仍保留） |
| `ISysDomainApi` | `domain.api` | 业务域 |
| `ISysDataSourceApi` | `datasource.api` | 数据源 |
| `ISysCacheApi` | `cache.api` | 缓存配置元数据（**当前空接口**：保留作为契约占位，运维 / 控制台读写走 `api-admin` 下的 `/api/admin/sys/cache`，没有 internal RPC 端点；新增 RPC 方法前对应的 `Sys*InternalController` 也未生成） |
| `ISysAccessRuleApi` / `ISysAccessRuleIpApi` | `accessrule.api`（并列两个接口） | 访问规则及 IP |
| `ISysLocaleApi` | `locale.api` | 语言/区域全局字典（与 `tenant.SysTenantLocaleApi` 配合使用） |
| `ISysOutLineApi` | `outline.api` | 大纲数据，按 (子系统, 租户) 缓存 |

每个 `ISys*Api` 方法上以 `@GetMapping` / `@PostMapping` 标注 **`/api/internal/sys/...`** 路径——同一个接口同时承担：
1. **同进程注入**：`core` 中 `Sys*Api`（`@Component`）实现该接口；
2. **Feign 远程调用**：`client` 中 `ISys*Proxy : ISys*Api` 沿用方法签名与路径；
3. **HTTP 暴露**：`api-internal` 中 `Sys*InternalController` 实现该接口，由 Spring MVC 根据方法级注解暴露同一 `/api/internal/sys/...` 路径。

三者共享同一份方法签名，是 sys 服务"本地 / 远程 / HTTP 一致性"的关键。

---

## `vo` 命名约定

- **Row**：列表/表格行  
- **Detail**：只读详情  
- **Edit**：表单回填  
- **`*CacheEntry`**：缓存条目；树/联合视图见各模块 `vo` 下具体类名  

---

## 依赖关系

- **直接依赖**：`kudos-context`（及 Gradle 传递依赖）。
- **被依赖方**：`kudos-ms-sys-core`、`kudos-ms-sys-client`；其他微服务也可仅依赖本模块以使用 `ISys*Api` 与 VO。

---

## 与其他子模块的关系

| 模块 | 关系 |
|------|------|
| **core** | 实现 `ISys*Api`，并大量使用各模块 `vo` / `enums` |
| **client** | `ISys*Proxy : ISys*Api`，序列化同一套 VO |
| **api-admin** | Controller 入参出参直接使用本模块各模块 `vo` |

---

## 扩展建议

- 新增契约：在对应业务模块下增加或扩展 **`api` / `vo` / `enums`**；若确属横切能力，再考虑放入 **`platform`**。
- 保持本模块**无数据库类型**、无 ORM / Ktorm 依赖。`api` 接口仅依赖 `kudos-context`、Spring Web 注解与少量基础类型。
- 新增 `ISys*Api` 方法时，**必须** 标注 `@GetMapping` / `@PostMapping("/api/internal/sys/<module>/<methodName>")` —— `api-internal` 控制器仅依靠这些注解暴露 HTTP，Feign 客户端也依靠它们路由。漏标的方法在 `client` 一侧会直接 404。
