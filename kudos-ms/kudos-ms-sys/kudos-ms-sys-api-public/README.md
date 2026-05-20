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

## 典型组合

| 组合 | 入口主类 | 暴露的路径 | 适用场景 |
|------|----------|------------|----------|
| **仅 public** | `SysApiWebApplication` | （无业务路径，只剩 `kudos-ability-web-springmvc` 装配的健康检查等） | 验证 Web 装配；通常不直接用于生产 |
| **public + admin** | `SysApiWebApplication` | `/api/admin/sys/**` | 对外管理 API（控制台后端） |
| **public + admin + internal** | 任选一个 application 主类（最终扫描三个模块） | `/api/admin/sys/**` + `/api/internal/sys/**` | 单进程同时承载控制台与 Feign 提供方；不需 Nacos 的本地开发可缺省 internal 中的分布式 ability |
| **internal 单独** | `SysApiProviderApplication` | `/api/internal/sys/**` | 纯 Provider 节点（生产中通常如此分离） |

> 由于本模块的 `SysApiWebAutoConfiguration` 仅扫描 `io.kudos.ms.sys.api.public`（包内只有 `init`），实际"加载哪些控制器"完全由 classpath 上是否含 `api-admin` / `api-internal` 决定——`core` 的 `SysAutoConfiguration` 在 `IComponentInitializer` 编排下会一并启动。

---

## 与 api-admin / api-internal 的对比

| 维度 | api-public | api-admin | api-internal |
|------|------------|-----------|--------------|
| 主类命名 | `SysApiWebApplication` | `SysApiAdminApplication` | `SysApiProviderApplication` |
| 是否含 Controller | **否** | 是（管理端 REST） | 是（**对内 RPC**，路径继承 `common.ISys*Api` 的方法注解） |
| 路径前缀 | （无） | `/api/admin/sys/**` | `/api/internal/sys/**` |
| 额外分布式能力 | 无（仅 core + MVC） | 无 | Nacos discovery / config + interservice 缓存 provider |
| 典型部署 | 与 admin 同进程作为对外 Web | 单独可执行 | 单独可执行（注册到 Nacos） |

---

## 扩展建议

- 若需「纯 Web 网关入口」专用配置（如 CORS、全局 filter、统一异常处理装配），可放在 `io.kudos.ms.sys.api.public` 下并由本模块扫描；**业务接口仍建议放在 api-admin / api-internal**，便于权限与路由前缀统一。
- **不要在本模块加业务 Controller**——一旦 public 自带控制器，"public + admin" 与 "public + internal" 的组合就会暴露不一致的端点集，破坏路径双轨制（`/api/admin/**` vs `/api/internal/**`）的清晰边界。
