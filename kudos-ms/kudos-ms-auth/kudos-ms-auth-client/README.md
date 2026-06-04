# kudos-ms-auth-client

## 定位

**其他微服务**（user / sys / msg / 业务）通过 **OpenFeign** 远程调用鉴权（`auth`）原子服务时
的**客户端模块**：仅依赖 **`kudos-ms-auth-common`** 与 Feign 能力模块，**不**依赖
`kudos-ms-auth-core`，避免将 ORM / 缓存实现等打入调用方 classpath。

每个 **`IAuth*Proxy`** 继承 `common` 中对应的 **`IAuth*Api`**，保证**本地注入 `IAuth*Api`
与远程 Feign**在方法签名上完全一致——服务端 `core` 实现切换为 client 远程代理时调用方无需改码。

---

## 包结构

在 **`io.kudos.ms.auth.client.<业务模块>`** 下与 **common / core** 的模块名对齐；每个模块内
再分 **`proxy`**（`IAuth*Proxy`，Feign）与 **`fallback`**（`Auth*Fallback`）子包。

典型形态：

```kotlin
@FeignClient(name = "auth-role", fallback = AuthRoleFallback::class)
interface IAuthRoleProxy : IAuthRoleApi
```

`name` 为注册中心中的**服务名**（或网关 / 配置中心的映射名），需与**提供 auth 能力的进程**
在注册中心中的名称一致。

| Proxy | Feign `name` | Fallback |
|-------|-------------|----------|
| `IAuthRoleProxy` | `auth-role` | `AuthRoleFallback` |

> **当前只暴露 `role` 一个 Proxy**——`group` 域目前没有跨服务调用方，因此 `auth-common` 未
> 定义 `IAuthGroupApi`，client / api-internal 也就没有对应的 Proxy / Controller。若后续业务
> 需要"用户组 → 跨服务展示"等能力，按 `role` 模式同步新增：
> `auth-common.group.api.IAuthGroupApi` → `auth-client.group.proxy.IAuthGroupProxy` →
> `auth-api-internal.controller.group.AuthGroupInternalController`。

---

## Fallback 约定

`Auth*Fallback` 继承自 `kudos-ability-distributed-client-feign` 的 `AbstractFeignFallbackSupport`：

- **读接口**：记录 `warn` 日志 + 返回**安全默认值**（`null` / 空 `Map` / 空 `List` / `false`），
  保证调用方继续执行而不是级联抛错。
- **写接口**：记录 `error` 日志 + 返回**失败值**（写入 `false` / `0` 等），由调用方判断后续
  补偿逻辑。

具体回填行为以 `AuthRoleFallback` 实现为准。

---

## Gradle 依赖

```kotlin
dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-common"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
    testImplementation(project(":kudos-test:kudos-test-container"))
}
```

- **`kudos-ms-auth-common`**：契约与序列化类型（`IAuthRoleApi` + 全套 VO）。
- **`kudos-ability-distributed-client-feign`**：Feign 注解 + 上下文透传（traceId / tenantId）
  + 降级基类（`AbstractFeignFallbackSupport`）。

---

## 调用方接入

```kotlin
@Configuration
@EnableFeignClients(basePackages = ["io.kudos.ms.auth.client"])
class FeignConfig

@Service
class MyBizService(
    private val authRoleProxy: IAuthRoleProxy,
) {
    fun canRead(userId: String, resourceId: String): Boolean =
        authRoleProxy.getUserResourceIds(userId).contains(resourceId)
}
```

1. **启用 Feign 扫描**：调用方 Spring Boot 应用需能扫描到 `io.kudos.ms.auth.client`（含各模块
   下的 `proxy`；或通过 `@EnableFeignClients(basePackages = ...)` 指定）。
2. **服务发现**：`name = "auth-role"` 需与 Nacos / Eureka 等中的实例一致；本地开发可用固定 URL
   配置（视 Spring Cloud 版本而定）。
3. **契约变更**：修改 `IAuthRoleApi` 时须**同步**检查 `auth-core` 实现、`AuthRoleFallback`
   与 `auth-api-internal/AuthRoleInternalController`——三者签名脱节会让 Feign 反序列化失败。

---

## 与其他子模块的关系

| 模块 | 关系 |
|------|------|
| **common** | Proxy 继承 `IAuth*Api`，请求 / 响应体为同一套 VO |
| **core** | 在**服务端**实现 `IAuth*Api`；客户端**不引用** core |
| **api-internal** | 通过 `*InternalController : IAuth*Api` 在服务端暴露同一套路径，Feign 调用最终落到这里 |

---

## 扩展建议

- 新增远程能力：先在 **common** 定义 `IAuth*Api` 与方法级 `@GetMapping`/`@PostMapping`，
  再在 **core** 实现，最后在本模块对应业务目录下新增 `proxy/IAuth*Proxy` 与
  `fallback/Auth*Fallback`，保持四者（common / core / client.proxy / client.fallback）一一对应。
- 不要在本模块写业务逻辑——`client` 是"远端调用对端"的薄包装，所有业务实现都属于 `core`。

## 已知限制 / 后续工作

- ❗ **只有 `IAuthRoleProxy` 一个 Proxy** — `group` 域目前不开 Feign，跨服务读用户组归属
  必须走 admin HTTP 或自建 Proxy
- ❗ **`AuthRoleFallback` 缺新方法时编译不报错** — 新增 `IAuthRoleApi` 方法时 fallback 不会被
  强制 override，漏写会让降级路径走默认抛错
- ❗ **Fallback 读写语义混杂** — 单个 fallback 类同时处理读 / 写接口的降级；不同语义的方法
  共享同一类，重构时容易混淆
- ❗ **熔断阈值默认值无文档** — `auth-role` Feign name 的熔断 / 超时配置依赖
  `kudos-ability-distributed-client-feign` 的全局配置，没有给业务方提供"默认值是什么"的速查
- ❗ **fallback 不区分 4xx vs 5xx** — 业务侧拿到 fallback 返回值时分不清是"对端拒绝"还是
  "对端不可达"；监控埋点需在 `AbstractFeignFallbackSupport` 内统一
