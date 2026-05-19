# kudos-ms-auth-api-internal

## 定位

鉴权（`auth`）原子服务的**对内服务 Provider 进程**入口与自动配置。**与 sys-api-internal 的差异**：
本模块**包含一个真实 Controller**——`AuthRoleInternalController`，实现 `IAuthRoleApi` 把角色查询 /
鉴权判断等能力暴露给其他微服务通过 Feign 调用。除此之外**额外引入服务发现、配置中心与跨服务
缓存 Provider** 等能力，面向**集群内部**调用场景。

---

## 入口与自动配置

| 类型 | 类 | 说明 |
|------|----|------|
| 启动类 | `AuthApiProviderApplication` | `@EnableKudos`，`main` 启动 Spring Boot |
| 自动配置 | `AuthApiProviderAutoConfiguration` | `@ComponentScan("io.kudos.ms.auth.api.internal")`，`IComponentInitializer` 组件名 **`kudos-ms-auth-api-internal`** |

`io.kudos.ms.auth.api.internal` 下当前有两个包：

- `init/` —— `Application` + `AutoConfiguration`
- `controller/role/AuthRoleInternalController` —— `implements IAuthRoleApi`，路径完全继承自
  接口方法级注解（`/api/internal/auth/role/...`）

---

## 控制器一览

| 控制器 | 实现接口 | 职责 |
|--------|----------|------|
| `AuthRoleInternalController` | `IAuthRoleApi`（auth-common.role.api） | 把 `core` 的 `AuthRoleApi` Bean 通过 HTTP 暴露给 `auth-client.IAuthRoleProxy`：`getRoleById` / `getRolesByIds` / `getRoleId` / `getRoleUsers` / `getUserIdsByRoleCode` / `getRoleResources` / `hasResource` / `hasRole` / `getUserResourceIds` 等 |

> **路径继承自方法级注解**——本 Controller 类上**没有** `@RequestMapping`，所有路径都在
> `IAuthRoleApi` 的方法级 `@GetMapping("/api/internal/auth/role/...")` 上。这样 Feign 代理与服务端
> 路径自动对齐，新增方法时只需改 `common`。

---

## Gradle 依赖（要点）

在 **`kudos-ms-auth-core`** 与 **`kudos-ability-web-springmvc`** 之外，本模块额外依赖：

| 依赖 | 作用 |
|------|------|
| `kudos-ability-cache-interservice-provider` | 跨服务缓存的 **Provider 侧**能力（其他服务的缓存能在本服务发生写入后被失效） |
| `kudos-ability-distributed-discovery-nacos` | 服务注册与发现（Nacos） |
| `kudos-ability-distributed-config-nacos` | 配置中心（Nacos） |

因此**可执行 fat jar** 若以本模块为入口，通常具备**注册到 Nacos、拉取远程配置、参与服务间
缓存协议**等能力；具体行为以 Kudos 各 ability 模块文档与配置为准。

---

## 依赖关系（概念）

```
kudos-ms-auth-api-internal
    ├── kudos-ms-auth-core
    │       ├── kudos-ms-auth-sql
    │       └── kudos-ms-auth-common
    ├── kudos-ability-web-springmvc
    ├── kudos-ability-cache-interservice-provider
    ├── kudos-ability-distributed-discovery-nacos
    └── kudos-ability-distributed-config-nacos
```

---

## 部署形态

跑两个独立 JVM：
- `api-public` 对外暴露管理端 HTTP（含 `PermittedResourceController`，若组合 admin 还含
  `/api/admin/auth/**`）
- `api-internal` 给其他微服务（如 user / sys / msg / 业务）通过 Feign 调用
  `/api/internal/auth/role/**`

这种分离让"控制台流量"与"服务间流量"在**网络层 / 监控 / SLI** 上能分别治理；同时
api-internal 的 Nacos 依赖也只会出现在内网进程。

---

## 与 api-public 的对比

| 维度 | api-public | api-internal |
|------|------------|--------------|
| 典型场景 | 对外 Web / 网关后管理端 | 对内 Provider / 服务间调用 |
| Nacos / interservice 缓存 | 不强制 | 依赖中默认引入 |
| 源码中的 Controller | `PermittedResourceController`（当前用户视图） | `AuthRoleInternalController`（无状态 RPC） |
| 路径前缀 | `/api/internal/auth/permittedResource/*` + 组合 admin 时的 `/api/admin/auth/**` | `/api/internal/auth/role/*` |

---

## 已知限制

- ❗ **`api-internal` 与 `api-public` 的实际边界由 yml 配置决定**（端口 / actuator 暴露面 /
  网关路由），**代码层未硬分隔**——若上层聚合不当，两个进程可能加载同一个 Controller。
  业务侧需自行约束"哪些路径内部专用"，例如：
  - 网关只把 `/api/admin/**` 转发到 public 进程
  - 内部 Feign 调用走独立的 service registry 标签
- ❗ **`AuthRoleInternalController` 无 `@PreAuthorize`**：依赖 mTLS / 内网网络隔离做边界
  防护。`hasResource` / `getUserResourceIds` 等鉴权关键判定如果被非授信调用方命中，
  返回结果可被用于权限旁路侦察。
- ❗ **`group` 域无 Internal Controller**：当前 group 不对外，所有组操作只能通过 admin
  HTTP。若后续业务需要"跨服务查用户所在组"，需同步补 `IAuthGroupApi`（common）→
  `IAuthGroupProxy`（client）→ `AuthGroupInternalController`（本模块）。

---

## 扩展建议

- 新增 internal 端点：在 `auth-common` 对应 `Api` 接口新增方法（带方法级 `@GetMapping` /
  `@PostMapping`），在 `auth-core` 实现 `*Api` Bean，然后在本模块对应 Controller 重写
  override 即可——不要在 Controller 写业务逻辑。
- 部署为 Provider 节点时，需与 **Feign 客户端**（`kudos-ms-auth-client`）及**网关路由**
  中的服务名（`auth-role`）、上下文路径保持一致。
- 纯本地开发若不需要 Nacos，可优先用 **api-public + api-admin** 组合，避免强依赖注册中心。
