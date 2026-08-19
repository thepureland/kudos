# kudos-ms-user-client

User 服务的 **客户端代理 + 降级**——其他微服务通过 Kotlin 接口调用远端 user 服务。
传输实现是 **Spring Interface Clients**（`@HttpExchange` + `RestClient`），不再是 OpenFeign，
迁移记录见文末。

## 代理 / 降级清单

7 个 proxy 全部注册在 **同一个 group `user`** 下（见 `init/UserClientAutoConfiguration`）：

| Proxy（继承 `IUser*Api`） | Fallback |
|---|---|
| `IUserAccountProxy` | `UserAccountFallback` |
| `IUserAccountProtectionProxy` | `UserAccountProtectionFallback` |
| `IUserAccountThirdProxy` | `UserAccountThirdFallback` |
| `IUserContactWayProxy` | `UserContactWayFallback` |
| `IUserOrgProxy` | `UserOrgFallback` |
| `IUserLoginRememberMeProxy` | `UserLoginRememberMeFallback` |
| `IPassportProxy` | `PassportFallback` |

> `kudos-ms-auth` 主要消费 `IPassportProxy`、`IUserAccountProxy`、`IUserAccountThirdProxy`、
> `IUserOrgProxy` 这几条线（登录 / 用户元数据 / 第三方绑定 / 组织归属）。

## 部署配置（必需）

proxy 接口上**不写服务名**，目标由部署决定：

```yaml
spring:
  http:
    serviceclient:
      user:                             # == UserClientAutoConfiguration.GROUP
        base-url: lb://kudos-ms-user    # user api-internal 在注册中心的服务 id
        connect-timeout: 2s
        read-timeout: 5s
```

> ⚠️ **漏配不会在启动期报错**，proxy 照常注册，直到第一次调用才因 URL 无法解析而失败。

## 接口签名约定

- `*Proxy` 接口继承 `user-common` 中的 `IUser*Api`，与 server 端实现同源，避免双份维护。
  HTTP 路径与方法也写在 `IUser*Api` 上（`@GetExchange` / `@PostExchange`），
  **同一份声明既是客户端契约、也是服务端 request mapping**（Spring MVC 7 支持 `@HttpExchange` 映射）。
- `*Fallback` 都继承 `AbstractHttpFallbackSupport(name)`，方法层面默认返回安全值。

### Fallback 的两条硬约束

1. **必须 `open`**：Spring Cloud 给 fallback 套 CGLIB 代理，Kotlin 类默认 final。
2. **不能加 `@Component`**：fallback 实现 `IUser*Proxy`，若同时是 Spring bean，按该类型注入时会与
   HTTP 代理 bean 产生歧义。Feign 时代没这个问题，因为 `@FeignClient` 注册的代理默认带 `@Primary`。

## 业务侧引入

```kotlin
implementation(project(":kudos-ms:kudos-ms-user:kudos-ms-user-client"))
```

```kotlin
@Autowired
private val userAccountProxy: IUserAccountProxy
```

注册是自动的：`UserClientAutoConfiguration` 实现 `IComponentInitializer`，调用方 `@EnableKudos`
即装配，不需要 `@EnableFeignClients` 之类的扫描注解。

## 依赖

- `kudos-ms-user-common`（契约 / VO / 枚举）
- `kudos-ability-distributed-client-http`（interface client 集成、`AbstractHttpFallbackSupport` 在此）

**不依赖 user-core**——纯远端调用包装，保持 client 模块轻量。

## 降级策略要点

- 任何 fallback 返回值**必须能让上游调用方不抛 NPE**：列表返回 `emptyList()`，单对象返回 `null`
  或安全默认值。
- 不在 fallback 里做"重试"——重试属于 group 级配置。

## 从 OpenFeign 迁移（2026-08-19）

| 改动 | 位置 |
|---|---|
| 7 个 `@FeignClient(name=, fallback=)` → 一个 `@ImportHttpServices` + 7 个 `@HttpServiceFallback` | `proxy/` → `init/UserClientAutoConfiguration` |
| 21 个 `@GetMapping` / `@PostMapping` → `@GetExchange` / `@PostExchange` | `kudos-ms-user-common` 的 `IUser*Api` |
| `AbstractFeignFallbackSupport` → `AbstractHttpFallbackSupport`；去掉 `@Component` | 7 个 `fallback/` |

**为什么必须动 `-common`**：`HttpServiceProxyFactory` 只认 `@HttpExchange` 家族，完全不认 Spring MVC
的 `@GetMapping`。路径注解从一开始就写在 `-common` 的共享接口上，被服务端 controller 和客户端 proxy
同时继承，所以这一步不可能只改 client——也因此 **Feign 与 interface client 无法在同一个 `IUser*Api`
上共存**，user 这条线是一次性切换。

## 已知限制 / 后续工作

- ✅ **已解决：多 client name 没有统一开关** — 原来 7 个 `user-*` name 各自配熔断 / 超时，
  现在收成一个 `spring.http.serviceclient.user` 配置块
- ❗ **`base-url` 无启动期校验** — 见上文
- ❗ **Fallback 静默吞错** — 业务侧从返回值无法区分"远端真返回空"和"服务不可达"；
  `AbstractHttpFallbackSupport` 只有日志、没有 Micrometer 埋点
- ❗ **`PassportFallback.login` 借用 `LOCKED` 表示降级**
  （`src/io/kudos/ms/user/client/passport/fallback/PassportFallback.kt`）：
  `PassportLoginStatusEnum.LOCKED` 的语义是"错误次数超限被锁定"，降级时返回它会让终端用户被误导为
  "账号被锁"。建议在 `PassportLoginStatusEnum` 新增 `SERVICE_UNAVAILABLE` 后改写（枚举属公共契约，
  未直接修改）。
- ❗ **`PassportFallback.login` 降级后无重试** — 上游业务（auth 网关）需自行做指数退避
- ❗ **fallback 缺新方法时编译不报错** — 新增 `IUser*Api` 方法时 fallback 不会被强制 override
- ❗ **7 个 `@HttpServiceFallback` 与 `types` 列表需手工同步** — 新增 proxy 要在
  `UserClientAutoConfiguration` 两处各加一行，漏了不报错
- ❗ **`AbstractHttpFallbackSupport` 的组件名字符串硬编码** — 各 Fallback 在 ctor 里塞自己的类名，
  重命名时易遗漏；可改为 `this::class.simpleName`
- ❗ **降级日志级别不对称** — `PassportFallback` 用 `errorWrite`、`UserAccountFallback` 用 `warnRead`；
  读 / 写语义区分合理，但同为"远端不可达"事件，监控告警需同时盯两个级别

## 测试覆盖

| 测试 | 覆盖 |
|---|---|
| `init/UserClientContractTest` | 起真实 servlet 容器 + **不带任何自己 mapping 注解**的 mock controller，证明共享契约上的 `@GetExchange` / `@PostExchange` 同时作为服务端路由与客户端契约生效；覆盖单参 / 双参 GET、`@RequestBody` + Map 返回的 POST、List 返回、null 返回 |
| 7 个 `*FallbackTest` | 各 fallback 的降级返回值 |
