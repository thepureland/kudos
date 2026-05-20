# kudos-ms-auth-api-public

## 定位

鉴权（`auth`）原子服务的**对外 Web 进程**入口与自动配置。**与 sys-api-public 的差异**：本模块
**包含一个真实 Controller**——`PermittedResourceController`，对外暴露"当前登录用户可见菜单 /
资源"的能力（`IPermittedResource` 的实现端）。除此之外的管理端 REST 仍由
`kudos-ms-auth-api-admin` 承担，本模块通过组合依赖一并装载。

---

## 入口与自动配置

| 类型 | 类 | 说明 |
|------|----|------|
| 启动类 | `AuthApiWebApplication` | `@EnableKudos`，`main` 启动 Spring Boot |
| 自动配置 | `AuthApiWebAutoConfiguration` | `@ComponentScan("io.kudos.ms.auth.api.public")`，`IComponentInitializer` 组件名 **`kudos-ms-auth-api-public`** |

`io.kudos.ms.auth.api.public` 下当前有两个包：

- `init/` —— `Application` + `AutoConfiguration`
- `controller/platform/PermittedResourceController` —— `implements IPermittedResource`，
  路径完全继承自接口方法级注解（典型为 `/api/internal/auth/permittedResource/...`）

---

## 控制器一览

| 控制器 | 实现接口 | 职责 |
|--------|----------|------|
| `PermittedResourceController` | `IPermittedResource`（auth-common.platform.api） | `getMenusForCurrentUser` —— 拿当前请求上下文里的 `userId`，按"用户 → 组 → 角色 → 资源"链路拼出菜单树（`List<MenuTreeNode>`） |

> 之所以放在 `api-public` 而非 `api-internal`：**当前用户视图**是 Web 进程内的请求级 API
> （要从 `RequestContext` 取 `userId` / `tenantId`），不是无状态的 RPC——放在 internal 用 Feign
> 调会丢上下文。

---

## 启动

```bash
java -jar auth-api-public-<version>.jar
```

或本仓库本地运行：

```bash
./gradlew :kudos-ms:kudos-ms-auth:kudos-ms-auth-api-public:bootRun
```

---

## Gradle 依赖

```kotlin
dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-core"))
    api(project(":kudos-ability:kudos-ability-web:kudos-ability-web-springmvc"))
    testImplementation(project(":kudos-test:kudos-test-container"))
}
```

依赖 **`core`** 间接拉入 `auth-sql` / `auth-common` 与所有缓存 / DAO / Service Bean；
**`web-springmvc`** 拉入 Filter / Interceptor / 异常映射 / 序列化等 Web 栈。
**不强制依赖 `api-admin`**——若需要同时对外暴露 `/api/admin/auth/**`，由上层聚合
（如 `*-api-web`）一并引入 `api-admin`。

---

## 依赖关系（概念）

```
kudos-ms-auth-api-public
    ├── kudos-ms-auth-core
    │       ├── kudos-ms-auth-sql
    │       └── kudos-ms-auth-common
    └── kudos-ability-web-springmvc
```

---

## 与 api-admin / api-internal 的对比

| 维度 | api-public | api-admin | api-internal |
|------|------------|-----------|--------------|
| 主类 | `AuthApiWebApplication` | `AuthApiAdminApplication` | `AuthApiProviderApplication` |
| 是否含 Controller | 是（`PermittedResourceController`，1 个） | 是（`AuthRoleAdminController` / `AuthGroupAdminController`） | 是（`AuthRoleInternalController`，1 个） |
| 额外分布式能力 | 无（仅 core + MVC） | 无 | Nacos + interservice 缓存 |
| 暴露面 | 当前用户视图 + 管理 HTTP（由 admin 组合时） | 管理 REST `/api/admin/auth/**` | 服务间 Feign `/api/internal/auth/**` |
| 适用场景 | 控制台前端 / 网关后用户路径 | 控制台前端 | 微服务间调用 |

---

## 扩展建议

- 若需"纯 Web 网关入口"专用配置（如自定义 Filter），可放在 `io.kudos.ms.auth.api.public`
  下并由本模块扫描；**业务管理接口仍建议放在 api-admin**，便于权限与路由前缀统一。
- 新增"当前用户视图"型接口（同样需要 RequestContext）：在 `controller/platform/` 下扩展，
  在 `auth-common.platform.api` 加对应接口契约，保持 controller 类只做"取上下文 + 委托
  `core` 实现"。

## 已知限制 / 后续工作

- ❗ **路径 `/api/internal/auth/permittedResource/...` 名义上是 internal 但实际在 public 暴露** —
  这是有意为之（需取 RequestContext），但与"internal 仅集群内可达"约定冲突；运维容易误以为
  本路径不需外网鉴权
- ❗ **`getMenusForCurrentUser` 缺少 `@PreAuthorize`** — 只检查"有 userId"，不校验"会话有效"；
  绕过 session filter 直接构造 RequestContext 可越权拿菜单
- ❗ **菜单树没有缓存** — 每次请求都跑"用户 → 组 → 角色 → 资源"完整链路；高并发场景应在
  `IPermittedResource` 实现侧加 user 级缓存（与 `ResourceIdsByUserIdCache` 配合）
- ❗ **未提供 admin 视角下的"看他人菜单"** — admin 排查权限问题时无法通过 API 看到指定用户的
  菜单视图；需要业务方自建 admin 接口走 service 层
- ❗ **`AuthApiWebApplication` 单独运行无 admin 路径** — 与 sys-api-public 类似，
  本模块单独 boot 仅得 `permittedResource` 一个端点；管理用户角色等仍需 admin 进程
