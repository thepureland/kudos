# kudos-ms-sys-common

## 定位

系统（`sys`）原子服务的**共享契约层**：不包含持久化、缓存实现或 HTTP 控制器，只提供跨 `core`、`client`、各 `api-*` 模块复用的 **类型与接口**。依赖面刻意保持精简（仅 `kudos-context` 等基础能力），便于其他微服务仅引用本模块即可编译通过 Feign 接口与 DTO。

---

## 包结构概览

### `io.kudos.ms.sys.common.consts`

- **`SysConsts`**：原子服务名、默认子系统编码等全局常量。

### `io.kudos.ms.sys.common.api`

领域对外 API 接口（`ISys*Api`），供 **进程内** 由 `kudos-ms-sys-core` 的 `Sys*Api` 实现，供 **进程间** 由 `kudos-ms-sys-client` 的 `ISys*Proxy`（Feign）继承同一契约。

| 接口 | 职责概要 |
|------|----------|
| `ISysTenantApi` | 租户缓存查询、按子系统列举租户等 |
| `ISysTenantSystemApi` | 租户与子系统关联 |
| `ISysTenantResourceApi` | 租户与资源关联 |
| `ISysTenantLocaleApi` | 租户默认语言等 |
| `ISysSystemApi` | 子系统（system）相关 |
| `ISysSubSystemMicroServiceApi` | 子系统与微服务绑定 |
| `ISysMicroServiceApi` | 微服务元数据 |
| `ISysResourceApi` | 资源/菜单树等 |
| `ISysDictApi` / `ISysDictItemApi` | 字典与字典项 |
| `ISysParamApi` | 系统参数 |
| `ISysI18nApi` | 国际化文案 |
| `ISysDomainApi` | 业务域 |
| `ISysDataSourceApi` | 数据源 |
| `ISysCacheApi` | 缓存配置 |
| `ISysAccessRuleApi` / `ISysAccessRuleIpApi` | 访问规则及 IP 明细 |

接口方法以**同步、面向缓存或领域服务**为主；具体语义见各接口 KDoc 与 `core` 实现。

### `io.kudos.ms.sys.common.vo`

按业务域划分的 **值对象**，典型子包约定：

| 子包 | 内容 |
|------|------|
| `request` | 查询、创建、更新表单与查询条件（`Query` / `FormCreate` / `FormUpdate` / `ISys*FormBase`） |
| `response` | 列表行、详情、编辑回显（`Row` / `Detail` / `Edit`） |
| 根下或并列 | **缓存条目**（`*CacheEntry`）、树节点（如 `BaseMenuTreeNode`）、联合视图行（如带 IP 的访问规则） |

命名上 **Row** 偏列表与表格，**Detail** 偏只读详情，**Edit** 偏表单回填；与控制台前端页面字段一般对应。

### `io.kudos.ms.sys.common.enums`

- 各子目录下的 **错误码枚举**（`*ErrorCodeEnum`），与业务异常或统一返回码配合。
- 领域枚举，如 `ResourceTypeEnum`、`AccessRuleTypeEnum` 等。

### `io.kudos.ms.sys.common.validation`

- 如 **`DictCodeValidator`**：与字典编码相关的校验逻辑，供表单或 Bean 校验使用。

---

## 依赖关系

- **直接依赖**：`kudos-context`（及 Gradle 传递依赖）。
- **被依赖方**：`kudos-ms-sys-core`、`kudos-ms-sys-client`；其他微服务也可仅依赖本模块以使用 `ISys*Api` 类型与 VO。

---

## 与其他子模块的关系

| 模块 | 关系 |
|------|------|
| **core** | 实现 `ISys*Api`，并大量使用 `vo` / `enums` |
| **client** | `ISys*Proxy : ISys*Api`，序列化同一套 VO |
| **api-admin** | Controller 入参出参直接使用本模块 `vo` |

---

## 扩展建议

- 新增跨服务契约时：优先在 `api` 增加或扩展 `ISys*Api`，在 `vo` 下按域新增 `request`/`response`，避免在 `core` 中定义对外 DTO。
- 保持本模块**无 Spring 注解、无数据库类型**，以维持可移植性。
