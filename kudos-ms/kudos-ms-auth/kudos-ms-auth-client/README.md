# kudos-ms-auth-client

## 定位

**其他微服务**（user / sys / msg / 业务）远程调用鉴权（`auth`）原子服务时的**客户端模块**：
仅依赖 **`kudos-ms-auth-common`** 与 interface client 能力模块，**不**依赖 `kudos-ms-auth-core`，
避免将 ORM / 缓存实现等打入调用方 classpath。

传输实现是 **Spring Interface Clients**（`@HttpExchange` + `RestClient`），不再是 OpenFeign，
迁移记录见文末。

每个 **`IAuth*Proxy`** 继承 `common` 中对应的 **`IAuth*Api`**，保证**本地注入 `IAuth*Api`
与远程调用**在方法签名上完全一致——服务端 `core` 实现切换为 client 远程代理时调用方无需改码。

---

## 包结构

在 **`io.kudos.ms.auth.client.<业务模块>`** 下与 **common / core** 的模块名对齐；每个模块内
再分 **`proxy`**（`IAuth*Proxy`）与 **`fallback`**（`Auth*Fallback`）子包，`init/` 下是统一注册入口。

典型形态——proxy 上**没有任何类型级注解**：

```kotlin
interface IAuthRoleProxy : IAuthRoleApi
```

| Proxy | Fallback |
|-------|----------|
| `IAuthRoleProxy` | `AuthRoleFallback` |
| `IAuthzDecisionProxy` | `AuthzDecisionFallback` |

> **`group` 域仍未暴露 Proxy**——`auth-common` 未定义 `IAuthGroupApi`，client / api-internal 也就
> 没有对应的 Proxy / Controller。若后续业务需要"用户组 → 跨服务展示"等能力，按 `role` 模式同步新增：
> `auth-common.group.api.IAuthGroupApi` → `auth-client.group.proxy.IAuthGroupProxy` →
> `auth-api-internal.controller.group.AuthGroupInternalController`。

---

## 部署配置（必需）

两个 proxy 注册在同一个 group `auth` 下（见 `init/AuthClientAutoConfiguration`），目标由部署决定：

```yaml
spring:
  http:
    serviceclient:
      auth:                             # == AuthClientAutoConfiguration.GROUP
        base-url: lb://kudos-ms-auth    # auth api-internal 在注册中心的服务 id
        connect-timeout: 2s
        read-timeout: 5s
```

> ⚠️ **漏配不会在启动期报错**，proxy 照常注册，直到第一次调用才失败。

---

## Fallback 约定

`Auth*Fallback` 继承 `kudos-ability-distributed-client-http` 的 `AbstractHttpFallbackSupport`：

- **读接口**：`warn` 日志 + **安全默认值**（`null` / 空 `Map` / 空 `List` / `false`），
  保证调用方继续执行而不是级联抛错。
- **写接口**：`error` 日志 + **失败值**（`false` / `0` 等），由调用方判断补偿逻辑。

两条硬约束：**必须 `open`**（Spring Cloud 套 CGLIB 代理，Kotlin 类默认 final）、
**不能加 `@Component`**（fallback 实现 `IAuth*Proxy`，同时是 bean 会与 HTTP 代理 bean 争注入）。

---

## Gradle 依赖

```kotlin
dependencies {
    api(project(":kudos-ms:kudos-ms-auth:kudos-ms-auth-common"))
    api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-http"))
    testImplementation(project(":kudos-test:kudos-test-common"))
    testImplementation(project(":kudos-test:kudos-test-container"))
    testImplementation(libs.spring.boot.starter.web)
}
```

- **`kudos-ms-auth-common`**：契约与序列化类型（`IAuth*Api` + 全套 VO），HTTP 路径注解也在这里。
- **`kudos-ability-distributed-client-http`**：interface client 集成 + 上下文透传（traceId / tenantId）
  + 降级基类。

---

## 调用方接入

```kotlin
@Service
class MyBizService(
    private val authRoleProxy: IAuthRoleProxy,
) {
    fun canRead(userId: String, resourceId: String): Boolean =
        authRoleProxy.isUserHasResource(userId, resourceId)
}
```

1. **注册是自动的**：`AuthClientAutoConfiguration` 实现 `IComponentInitializer`，调用方 `@EnableKudos`
   即装配，不需要 `@EnableFeignClients` 之类的扫描注解。
2. **必须配 `base-url`**：见上文。
3. **契约变更**：修改 `IAuth*Api` 时须**同步**检查 `auth-core` 实现、`Auth*Fallback`
   与 `auth-api-internal/*InternalController`——三者签名脱节会让反序列化失败。

---

## 与其他子模块的关系

| 模块 | 关系 |
|------|------|
| **common** | Proxy 继承 `IAuth*Api`，请求 / 响应体为同一套 VO；**HTTP 路径注解也在这里** |
| **core** | 在**服务端**实现 `IAuth*Api`；客户端**不引用** core |
| **api-internal** | 通过 `*InternalController : IAuth*Api` 暴露同一套路径，路由由接口上的 `@HttpExchange` 家族注解提供 |

---

## 从 OpenFeign 迁移（2026-08-19）

| 改动 | 位置 |
|---|---|
| 2 个 `@FeignClient(...)` → 一个 `@ImportHttpServices` + 2 个 `@HttpServiceFallback` | `proxy/` → `init/AuthClientAutoConfiguration` |
| 15 个 `@GetMapping` / `@PostMapping` → `@GetExchange` / `@PostExchange` | `kudos-ms-auth-common` 的 `IAuth*Api` |
| `AbstractFeignFallbackSupport` → `AbstractHttpFallbackSupport`；去掉 `@Component` | 2 个 `fallback/` |

**auth 是四个模块里最能说明问题的一个**：两个 proxy 原本都写 `name = "auth-role"`，
`IAuthzDecisionProxy` 还得额外加 `contextId = "authzDecision"` 才能绕开 Feign 的重名检查——
说明"每个 proxy 一个服务名"这个模型本身就不成立。interface client 下按类型区分，
这个 workaround 直接消失。

**为什么必须动 `-common`**：`HttpServiceProxyFactory` 只认 `@HttpExchange` 家族，不认 `@GetMapping`。
路径注解写在 `-common` 的共享接口上被两端继承，所以 Feign 与 interface client 无法在同一个
`IAuth*Api` 上共存，auth 这条线是一次性切换。

---

## 扩展建议

- 新增远程能力：先在 **common** 定义 `IAuth*Api` 与方法级 `@GetExchange` / `@PostExchange`，
  再在 **core** 实现，最后在本模块新增 `proxy/IAuth*Proxy` 与 `fallback/Auth*Fallback`，
  并在 `AuthClientAutoConfiguration` 的 `types` 与 `@HttpServiceFallback` 两处各加一行。
- 不要在本模块写业务逻辑——`client` 是薄包装，业务实现属于 `core`。

## 测试覆盖

| 测试 | 覆盖 |
|---|---|
| `init/AuthClientContractTest` | 起真实 servlet 容器 + **不带任何自己 mapping 注解**的 mock controller，证明共享契约两端都成立；并钉住两个 proxy 现在是各自独立的 bean（不再需要 `contextId`） |
| 2 个 `*FallbackTest` | 各 fallback 的降级返回值 |

## 已知限制 / 后续工作

- ✅ **已解决：重名 + `contextId` workaround** — 见上文
- ✅ **已解决：熔断 / 超时配置分散** — 现在是一个 `spring.http.serviceclient.auth` 配置块
- ❗ **`base-url` 无启动期校验** — 见上文
- ❗ **`group` 域仍无 Proxy** — 跨服务读用户组归属必须走 admin HTTP 或自建 Proxy
- ❗ **fallback 缺新方法时编译不报错** — 新增 `IAuth*Api` 方法时 fallback 不会被强制 override
- ❗ **Fallback 读写语义混杂** — 单个 fallback 类同时处理读 / 写接口的降级
- ❗ **fallback 拿不到异常** — 当前两个 fallback 用的是契约签名，分不清"对端拒绝"还是"对端不可达"。
  如需区分，可按 `kudos-ms-msg-client` 的做法加 `Throwable` 首参重载
- ❗ **降级无指标** — `AbstractHttpFallbackSupport` 只有日志，没有 Micrometer 埋点
