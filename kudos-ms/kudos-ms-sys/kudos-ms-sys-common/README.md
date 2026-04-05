# kudos-ms-sys-common

## 定位

系统（`sys`）原子服务的**共享契约层**：不包含持久化、缓存实现或 HTTP 控制器，只提供跨 `core`、`client`、各 `api-*` 模块复用的 **类型与接口**。依赖面刻意保持精简（仅 `kudos-context` 等基础能力），便于其他微服务仅引用本模块即可编译通过 Feign 接口与 DTO。

---

## 包结构（`io.kudos.ms.sys.common`）

**第一层按业务模块划分**，模块名与历史 `vo` 下的一级目录一致：

`accessrule`、`cache`、`datasource`、`dict`、`domain`、`i18n`、`microservice`、`param`、`resource`、`system`、`tenant`

**字典头** 与 **字典项** 同属 **`dict`**：`ISysDictApi` / `ISysDictItemApi` 均在 **`dict.api`**；相关 VO 在 **`dict.vo`**（含 `SysDictItem*` 等），**无** `dictitem` 子包。**访问规则 IP** 与主规则同属 **`accessrule`**（`accessrule.api` / `accessrule.vo`），**无** `accessruleip` 子包。

**第二层**在各模块下按类型分为四类子包（若该模块暂无某类内容可省略目录）：

| 子包名 | 内容 |
|--------|------|
| **`api`** | 该模块对应的 `ISys*Api` 契约（若有） |
| **`consts`** | 该模块专用常量（若有；绝大多数模块当前无独立 consts） |
| **`enums`** | 错误码枚举 `*ErrorCodeEnum`、领域枚举（如 `ResourceTypeEnum`）等 |
| **`vo`** | 值对象：`vo/request`、`vo/response` 及根下的 `*CacheEntry` 等 |

**跨模块、不属于单一业务域**的契约放在 **`io.kudos.ms.sys.common.platform`** 下，同样按类型分子包：

| 子包 | 说明 |
|------|------|
| **`platform.consts`** | 全局常量，如 **`SysConsts`**、**`SysDictTypes`** |
| **`platform.validation`** | 跨模块校验器，如 **`DictCodeValidator`** 及 **`DictCodeConstraintValidatorProvider`** |

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
| `ISysTenantApi` 等 | `tenant.api` | 租户及租户-子系统/资源/语言 |
| `ISysSystemApi` | `system.api` | 子系统 |
| `ISysMicroServiceApi`、`ISysSubSystemMicroServiceApi` | `microservice.api` | 微服务及子系统-微服务绑定 |
| `ISysResourceApi` | `resource.api` | 资源/菜单树 |
| `ISysDictApi` / `ISysDictItemApi` | `dict.api`（并列两个接口） | 字典与字典项 |
| `ISysParamApi` | `param.api` | 系统参数 |
| `ISysI18nApi` | `i18n.api` | 国际化 |
| `ISysDomainApi` | `domain.api` | 业务域 |
| `ISysDataSourceApi` | `datasource.api` | 数据源 |
| `ISysCacheApi` | `cache.api` | 缓存配置元数据 |
| `ISysAccessRuleApi` / `ISysAccessRuleIpApi` | `accessrule.api`（并列两个接口） | 访问规则及 IP |

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
- 保持本模块**无数据库类型**；与校验、Spring 相关的少数类以现有 `platform` 为准。
