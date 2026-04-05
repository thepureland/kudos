# kudos-ms-sys-api-internal

## 定位

系统（`sys`）原子服务的 **对内服务 Provider 壳层**（历史命名 **sys-api-provider**）：提供 **可独立运行的 Spring Boot 入口** 与 **自动配置**，与 `api-public` 类似 **本模块几乎不包含业务 Controller**，但 **额外引入服务发现、配置中心与跨服务缓存 Provider** 等能力，面向 **集群内部** 调用场景（与其它微服务、注册中心协同）。

---

## 入口与自动配置

| 类型 | 类 | 说明 |
|------|-----|------|
| 启动类 | `SysApiProviderApplication` | `@EnableKudos`，`main` 启动 Spring Boot |
| 自动配置 | `SysApiProviderAutoConfiguration` | `@ComponentScan("io.kudos.ms.sys.api.internal")`，组件名 **`kudos-ms-sys-api-internal`** |

当前 `io.kudos.ms.sys.api.internal` 包下仅有 `init`，**无 Controller**；实际 REST 仍由 **`kudos-ms-sys-api-admin`**（若在同一进程引入）或网关路由提供。

---

## Gradle 依赖（要点）

在 **`kudos-ms-sys-core`** 与 **`kudos-ability-web-springmvc`** 之外，本模块额外依赖：

| 依赖 | 作用 |
|------|------|
| `kudos-ability-cache-interservice-provider` | 跨服务缓存的 Provider 侧能力 |
| `kudos-ability-distributed-discovery-nacos` | 服务注册与发现（Nacos） |
| `kudos-ability-distributed-config-nacos` | 配置中心（Nacos） |

因此 **可执行 fat jar** 若使用本模块作为入口，通常具备 **注册到 Nacos、拉取远程配置、参与服务间缓存协议** 等能力；具体行为以 Kudos 各 ability 模块文档与配置为准。

---

## 依赖关系（概念）

```
kudos-ms-sys-api-internal
    ├── kudos-ms-sys-core
    │       ├── kudos-ms-sys-sql
    │       └── kudos-ms-sys-common
    ├── kudos-ability-web-springmvc
    ├── kudos-ability-cache-interservice-provider
    ├── kudos-ability-distributed-discovery-nacos
    └── kudos-ability-distributed-config-nacos
```

---

## 与 api-public 的对比

| 维度 | api-public | api-internal |
|------|------------|--------------|
| 典型场景 | 对外 Web、网关后的管理 API | 对内 Provider、注册中心、服务间协作 |
| Nacos / interservice 缓存 | 不强制 | 依赖中默认引入 |
| 源码中的 Controller | 无（本仓库现状） | 无（本仓库现状） |

---

## 扩展建议

- 部署为 Provider 节点时，需与 **Feign 客户端**（`kudos-ms-sys-client`）及 **网关路由** 中的服务名、上下文路径保持一致。
- 纯本地开发若不需要 Nacos，可优先使用 **api-public + api-admin** 组合，避免强依赖注册中心。
