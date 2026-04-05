# kudos-ms-sys

**定位**：**系统（`sys`）原子服务**的 Gradle 聚合模块，承载平台级能力：租户与子系统关系、微服务注册、资源与菜单、字典与参数、国际化、数据源、域与缓存配置、访问规则等。通过 **契约（common）→ 数据库脚本（sql）→ 领域实现（core）→ HTTP/Feign 暴露（api-* / client）** 分层组织代码。

**在工程中的角色**：作为 `kudos-ms` 下的 **系统（`sys`）原子服务**（`SysConsts.ATOMIC_SERVICE_NAME = "sys"`），供控制台、网关及其他微服务通过管理端 HTTP 或 Feign 使用。

---

## 子模块文档索引

| 子模块 | 说明文档 |
|--------|----------|
| [kudos-ms-sys-common](kudos-ms-sys-common/README.md) | 跨模块共享契约：API 接口、VO、枚举、校验与常量 |
| [kudos-ms-sys-sql](kudos-ms-sys-sql/README.md) | Flyway 迁移脚本（sys 库表与视图） |
| [kudos-ms-sys-core](kudos-ms-sys-core/README.md) | DAO、Service、缓存、API 实现与 Spring 扫描入口 |
| [kudos-ms-sys-api-admin](kudos-ms-sys-api-admin/README.md) | 管理端 REST 控制器（`/api/admin/sys/...`） |
| [kudos-ms-sys-api-public](kudos-ms-sys-api-public/README.md) | 对外 Web 进程启动入口与自动配置（`sys-api-web`） |
| [kudos-ms-sys-api-internal](kudos-ms-sys-api-internal/README.md) | 对内 Provider 进程启动入口与自动配置（`sys-api-provider`） |
| [kudos-ms-sys-client](kudos-ms-sys-client/README.md) | Feign 代理与降级，供其他服务远程调用 sys |

---

## 依赖关系（概念）

```
                    ┌─────────────────┐
                    │ kudos-ms-sys-   │
                    │ common          │
                    └────────┬────────┘
                             │
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   │
┌─────────────────┐  ┌─────────────────┐          │
│ kudos-ms-sys-   │  │ kudos-ms-sys-   │          │
│ sql             │  │ client          │          │
└────────┬────────┘  └────────┬────────┘          │
         │                    │ only common       │
         └──────────┬─────────┘                   │
                    ▼                             │
            ┌───────────────┐                     │
            │ kudos-ms-sys- │                     │
            │ core          │◄────────────────────┘
            └───────┬───────┘
                    │
    ┌───────────────┼───────────────┐
    ▼               ▼               ▼
┌────────┐   ┌────────────┐   ┌────────────┐
│ api-   │   │ api-       │   │ api-       │
│ admin  │   │ public     │   │ internal   │
└────────┘   └────────────┘   └────────────┘
```

- **api-admin / api-public / api-internal** 均依赖 **core**；三者差异主要在 **打包形态与依赖的能力栈**（见各子模块 README）。
- **client** 仅依赖 **common**（及 Feign 能力），避免把 `core` 打进调用方。

---

## 命名与约定（跨模块）

- **默认子系统编码**：`SysConsts.DEFAULT_SUB_SYSTEM_CODE`（`default-sub-system`），与资源、菜单、访问规则等按子系统维度隔离的数据一致。
- **领域 API**：`common` 中 `ISys*Api` 由 `core` 中 `Sys*Api` 实现；`client` 中 `ISys*Proxy` 继承同一接口并通过 Feign 调用远程服务。

具体类名与边界以各子模块源码为准。
