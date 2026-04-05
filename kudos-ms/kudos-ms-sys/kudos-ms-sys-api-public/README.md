# kudos-ms-sys-api-public

## 定位

系统（`sys`）原子服务的 **对外 Web 进程壳层**（历史命名 **sys-api-web**）：提供 **可独立运行的 Spring Boot 入口类** 与 **极窄包扫描的自动配置**，本模块源码目录下 **几乎不包含业务 Controller**（Controller 集中在 `kudos-ms-sys-api-admin`）。

设计意图是：将「**启动入口 + Web 栈装配**」与「**具体 REST 定义**」拆开，由上层可执行应用通过 **组合依赖**（`api-public` + `api-admin` + `core` 等）一次性拉入完整功能。

---

## 入口与自动配置

| 类型 | 类 | 说明 |
|------|-----|------|
| 启动类 | `SysApiWebApplication` | `@EnableKudos`，`main` 启动 Spring Boot |
| 自动配置 | `SysApiWebAutoConfiguration` | `@ComponentScan("io.kudos.ms.sys.api.public")`，组件名 **`kudos-ms-sys-api-public`** |

当前 `io.kudos.ms.sys.api.public` 包下仅有 `init` 包，**无 Controller**；扫描仅注册该包内的配置类，避免与 admin 重复。

---

## 依赖关系

```
kudos-ms-sys-api-public
    └── kudos-ms-sys-core
            ├── kudos-ms-sys-sql
            └── kudos-ms-sys-common
```

依赖 **`kudos-ability-web-springmvc`**，与 **api-admin** 同级依赖 `core`，差异在于 **本模块不包含 `api.admin` 控制器**。

---

## 典型使用方式

- **单独依赖 public**：得到入口与 `core` 全量 Bean，但 **没有** `/api/admin/sys/**` 控制器，除非 classpath 上另有 `api-admin`。
- **public + admin**：常见组合，用于对外提供完整系统管理 API。

具体以团队内 **聚合启动模块**（例如 `*-api-web`）的依赖列表为准。

---

## 与 api-admin / api-internal 的对比

| 维度 | api-public | api-admin | api-internal |
|------|------------|-----------|--------------|
| 主类命名 | `SysApiWebApplication` | `SysApiAdminApplication` | `SysApiProviderApplication` |
| 是否含 Controller | 否（本仓库现状） | 是 | 否（本仓库现状） |
| 额外分布式能力 | 无（仅 core + MVC） | 无 | Nacos、interservice 缓存等 |

---

## 扩展建议

- 若需「纯 Web 网关入口」专用配置，可放在 `io.kudos.ms.sys.api.public` 下并由本模块扫描；**业务接口仍建议放在 api-admin**，便于权限与路由前缀统一。
