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

---

## 控制器一览

| 控制器 | 基础路径（示例） | 职责概要 |
|--------|------------------|----------|
| `SysTenantAdminController` | `/api/admin/sys/tenant` | 租户 CRUD、按子系统查租户、启用状态等 |
| `SysSystemAdminController` | `/api/admin/sys/system` | 子系统 |
| `SysMicroServiceAdminController` | `/api/admin/sys/microService` | 微服务 |
| `SysResourceAdminController` | `/api/admin/sys/resource` | 资源、菜单树等 |
| `SysDictAdminController` / `SysDictItemAdminController` | `/api/admin/sys/dict` 等 | 字典与字典项 |
| `SysParamAdminController` | `/api/admin/sys/param` | 系统参数 |
| `SysI18NAdminController` | `/api/admin/sys/i18n` | 国际化 |
| `SysDomainAdminController` | `/api/admin/sys/domain` | 业务域 |
| `SysDataSourceAdminController` | `/api/admin/sys/dataSource` | 数据源 |
| `SysCacheAdminController` | `/api/admin/sys/cache` | 缓存配置 |
| `SysAccessRuleAdminController` / `SysAccessRuleIpAdminController` | `/api/admin/sys/accessRule` 等 | 访问规则及 IP |

具体路径以各 `@RequestMapping` 为准；部分 Controller 在基类路由上还有子路径（如 `getTenantsBySubSystemCode`）。

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

- **api-admin**：承载 **管理 REST**。
- **api-public**：通常提供 **对外 Web 进程** 的 `main` 与极薄扫描包；可执行应用往往 **同时依赖 admin + core + public**，从而在网关后对外暴露 `/api/admin/sys/**`。
- **api-internal**：提供 **对内 Provider** 形态与 Nacos 等栈；是否挂载同一套 Controller 取决于上层聚合模块的依赖组合。

部署拓扑以实际可执行模块（如 gateway、ams-*-api-web）的 `build.gradle.kts` 为准。

---

## 扩展建议

- 新增管理接口：优先在 **core** 完成 `ISys*Service` 能力，再在本模块新增 `*AdminController`，保持 VO 仅来自 **common**。
