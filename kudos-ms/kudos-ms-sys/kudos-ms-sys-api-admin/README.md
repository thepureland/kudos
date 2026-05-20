# kudos-ms-sys-api-admin

## 定位

系统（`sys`）原子服务的 **管理端 HTTP API** 层：在 `kudos-ms-sys-core` 之上提供 **面向控制台 / 管理网关** 的 Spring MVC 控制器，路径统一落在 **`/api/admin/sys/...`** 下。与 `api-public`、`api-internal` 的区别在于：本模块包含 **具体 Controller 类**，而不仅是启动入口。

---

## 入口与自动配置

| 类型 | 类 | 说明 |
|------|-----|------|
| 启动类 | `SysApiAdminApplication` | `@EnableKudos`，可作为独立 Spring Boot `main` 运行 |
| 自动配置 | `SysApiAdminAutoConfiguration` | `@ComponentScan("io.kudos.ms.sys.api.admin")`，`IComponentInitializer` 组件名 **`kudos-ms-sys-api-admin`** |

依赖 **`kudos-ability-web-springmvc`**，继承工程内通用 Controller 基类（如 `BaseCrudController`）实现标准 CRUD 与扩展接口。

**包结构**：在 **`io.kudos.ms.sys.api.admin.controller`** 下再按业务模块分子包 **`...controller.<模块>`**（与 `common` / `core` 模块名对齐），例如 `...admin.controller.dict.SysDictAdminController`；启动与扫描仍在 **`...api.admin.init`**。

---

## 控制器一览

| 控制器 | 基础路径 | 职责概要 |
|--------|----------|----------|
| `SysTenantAdminController` | `/api/admin/sys/tenant` | 租户 CRUD、按子系统查租户、启用状态等 |
| `SysSystemAdminController` | `/api/admin/sys/system` | 子系统 |
| `SysMicroServiceAdminController` | `/api/admin/sys/microService` | 微服务 |
| `SysResourceAdminController` | `/api/admin/sys/resource` | 资源、菜单树等 |
| `SysDictAdminController` | `/api/admin/sys/dict` | 字典头 |
| `SysDictItemAdminController` | `/api/admin/sys/dictItem` | 字典项（独立路径，与字典头分开） |
| `SysParamAdminController` | `/api/admin/sys/param` | 系统参数 |
| `SysI18NAdminController` | `/api/admin/sys/i18n` | 国际化 |
| `SysLocaleAdminController` | `/api/admin/sys/locale` | 语言/区域全局字典 |
| `SysDomainAdminController` | `/api/admin/sys/domain` | 业务域 |
| `SysDataSourceAdminController` | `/api/admin/sys/dataSource` | 数据源 |
| `SysCacheAdminController` | `/api/admin/sys/cache` | 缓存配置元数据 |
| `SysOutLineAdminController` | `/api/admin/sys/outLine` | 大纲数据 |
| `SysAccessRuleAdminController` | `/api/admin/sys/accessRule` | 访问规则 |
| `SysAccessRuleIpAdminController` | `/api/admin/sys/accessRuleIp` | 访问规则 IP（独立路径） |

路径首段为 `camelCase`（如 `microService`、`dataSource`、`outLine`、`accessRule` / `accessRuleIp`、`dictItem`），与 `api-internal` 的 `/api/internal/sys/<module>` 命名一致。

控制器统一继承自 `kudos-ability-web-springmvc` 中的 `BaseCrudController`（或其变体），开箱获得列表 / 详情 / 新增 / 修改 / 删除 / 批删等通用端点；模块特有动作（如租户的 `getTenantsBySubSystemCode`）以子路径附加。

---

## 依赖关系

```
kudos-ms-sys-api-admin
    └── kudos-ms-sys-core
            ├── kudos-ms-sys-sql
            └── kudos-ms-sys-common
```

---

## 与 api-public / api-internal 的配合

| 模块 | 路径前缀 | 用途 |
|------|----------|------|
| **api-admin** | `/api/admin/sys/**` | 管理端 REST，控制台 / 管理网关使用 |
| **api-internal** | `/api/internal/sys/**` | 内部 RPC，**Feign 客户端** 与服务网格内部使用；控制器实现 `common.ISys*Api` 接口，复用方法级 `@*Mapping` |
| **api-public** | （无业务路径） | 仅提供启动入口与 Web 栈装配；本身不挂控制器 |

可执行应用通常 **同时依赖 `api-public` + `api-admin` + `api-internal` + `core`**，从而既对外暴露 `/api/admin/sys/**`，又对内暴露 `/api/internal/sys/**`。部署拓扑（哪个进程挂哪些路径）由具体可执行模块的 `build.gradle.kts` 决定。

---

## 扩展建议

- 新增管理接口：优先在 **core** 完成 `ISys*Service` 能力，再在本模块新增 `*AdminController`，保持 VO 仅来自 **common**。
- **不要**把 `/api/admin/**` 端点放到 `common.ISys*Api` 接口里。`common` 中接口的 `@*Mapping` 仅用于 `/api/internal/**` 路径，是 admin / internal 双轨制的契约边界。
- 管理路径首段使用 `camelCase`（与现有保持一致），不要混用 `kebab-case` 或 `snake_case`。
