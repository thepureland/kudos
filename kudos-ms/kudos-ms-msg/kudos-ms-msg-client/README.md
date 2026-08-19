# kudos-ms-msg-client

Msg 服务的 **客户端代理 + 降级**——其他微服务通过 Kotlin 接口调用远端 msg 服务。
**不依赖 msg-core**，纯远端调用包装。传输实现是 **Spring Interface Clients**
（`@HttpExchange` + `RestClient`），不再是 OpenFeign，迁移记录见文末。

## 包结构

```
io.kudos.ms.msg.client.
├── init/       MsgClientAutoConfiguration（注册入口：group + 全部 proxy + 全部 fallback）
├── template/   {proxy/IMsgTemplateProxy,  fallback/MsgTemplateFallback}
├── instance/   {proxy/IMsgInstanceProxy,  fallback/MsgInstanceFallback}
├── receiver/   {proxy/IMsgReceiveProxy,   fallback/MsgReceiveFallback}
│               {proxy/IMsgReceiverGroupProxy, fallback/MsgReceiverGroupFallback}
└── send/       {proxy/IMsgSendProxy,      fallback/MsgSendFallback}
```

每个业务领域一对 `Proxy + Fallback`，全部是 Kotlin `interface` + `open class`。

## Proxy 清单

5 个 proxy 全部注册在**同一个 group `msg`** 下：

| Proxy | 实现的契约 | 降级 |
|-------|------------|------|
| `IMsgTemplateProxy` | `IMsgTemplateApi` | `MsgTemplateFallback` |
| `IMsgInstanceProxy` | `IMsgInstanceApi` | `MsgInstanceFallback` |
| `IMsgReceiveProxy` | `IMsgReceiveApi` | `MsgReceiveFallback` |
| `IMsgReceiverGroupProxy` | `IMsgReceiverGroupApi` | `MsgReceiverGroupFallback` |
| `IMsgSendProxy` | `IMsgSendApi` | `MsgSendFallback` |

proxy 上**没有任何类型级注解**——路径与 HTTP 方法全部继承自 common 的 `IMsg*Api`
（`@GetExchange` / `@PostExchange`），同一份声明既是客户端契约、也是服务端 request mapping。

```kotlin
interface IMsgSendProxy : IMsgSendApi
```

## 部署配置（必需）

```yaml
spring:
  http:
    serviceclient:
      msg:                            # == MsgClientAutoConfiguration.GROUP
        base-url: lb://kudos-ms-msg   # msg api-internal 在注册中心的服务 id
        connect-timeout: 2s
        read-timeout: 5s
```

> ⚠️ **漏配不会在启动期报错**，proxy 照常注册，直到第一次调用才失败。

## Fallback 模式

所有 fallback 继承 `AbstractHttpFallbackSupport("ComponentName")`（来自
`kudos-ability-distributed-client-http`）：

- **读接口**调 `warnRead(method, cause, args)` —— `WARN` + 安全默认值（`null` / `emptyList()` / `0`）
- **写接口**调 `errorWrite(method, cause, args)` —— `ERROR` + 失败标识（`false` / `0` / `null`）

**核心原则**：fallback **从不抛异常**——异常一旦冒出去，调用方业务流就断了。

### 为什么每个方法写两遍

msg 原来用 `fallbackFactory` 而非 `fallback`，靠 `FallbackFactory<T>.create(cause)` 把触发降级的
异常传给 fallback，日志才能区分 4xx / 5xx / unreachable。`@HttpServiceFallback` 走无参构造，
没有这个入口——但 `CircuitBreakerConfigurerUtils.resolveFallbackMethod` 会**查两次方法**：
先找 `name(Throwable, ...args)`，找不到才用 `name(...args)`。所以带 `Throwable` 首参的重载
就是 `FallbackFactory` 的替代品，**能力没丢，只是换了形状**。

```kotlin
open class MsgSendFallback : AbstractHttpFallbackSupport("MsgSendFallback"), IMsgSendProxy {

    // 真正在降级时被调用的是这个——Spring Cloud 优先解析 Throwable 首参重载
    fun publish(cause: Throwable?, request: MsgPublishRequest): String? {
        errorWrite("publish", cause, request)
        return null
    }

    // 保留契约签名，只为让编译器强制检查"每个契约方法都有 fallback"
    override fun publish(request: MsgPublishRequest): String? = publish(null, request)
}
```

契约签名那份是刻意保留的：去掉它，本类就不再实现 `IMsgSendProxy`，新增契约方法时不会有任何
编译错误，那个方法会**静默地没有降级**。代价是每个方法两份声明。

另外两条硬约束：**必须 `open`**（Spring Cloud 套 CGLIB 代理，Kotlin 类默认 final）、
**不能加 `@Component`**（fallback 实现 `IMsg*Proxy`，同时是 bean 会与 HTTP 代理 bean 争注入）。

## 使用姿态

```kotlin
implementation(project(":kudos-ms:kudos-ms-msg:kudos-ms-msg-client"))
```

```kotlin
@Autowired private val msgSendProxy: IMsgSendProxy

val sendId = msgSendProxy.publish(MsgPublishRequest(
    tenantId = ...,
    eventTypeDictCode = "user.welcome",
    msgTypeDictCode = "system",
    receiverIds = setOf(userId),
    params = mapOf("nickname" to "K"),
    publishMethod = MsgPublishMethodEnum.EMAIL,
))
if (sendId == null) {
    // 远端不通 / 模板缺失 / 入参非法——业务侧自决重试 or 入补偿
}
```

`MsgClientAutoConfiguration` 实现 `IComponentInitializer`，消费方 `@EnableKudos` 即自动装配，
不需要 `@EnableFeignClients` 之类的扫描注解（interface client 直接在 `types` 里点名接口，
没有"扫描"这一步）。

## 与 common 的对齐

- common 里 5 个 `IMsg*Api` 的方法签名变更需要立即同步到 client；编译器会替你检查
  （proxy 继承 common 接口），契约漂移在编译期暴露——这是"继承式 proxy"的最大优点。
- fallback 同理，但仅限**契约签名那一份**；`Throwable` 首参重载不受编译器约束，
  新增方法时要记得两份都补。

## 从 OpenFeign 迁移（2026-08-19）

| 改动 | 位置 |
|---|---|
| 5 个 `@FeignClient(name=, fallbackFactory=)` + `@EnableFeignClients` → 一个 `@ImportHttpServices` + 5 个 `@HttpServiceFallback` | `proxy/` → `init/MsgClientAutoConfiguration` |
| 10 个 `@GetMapping` / `@PostMapping` → `@GetExchange` / `@PostExchange` | `kudos-ms-msg-common` 的 `IMsg*Api` |
| 删除 5 个 `Msg*FallbackFactory`；fallback 改为 `Throwable` 首参重载 | 5 个 `fallback/` |
| `AbstractFeignFallbackSupport` → `AbstractHttpFallbackSupport`；去掉 `@Component` | 5 个 `fallback/` |

**为什么必须动 `-common`**：`HttpServiceProxyFactory` 只认 `@HttpExchange` 家族，不认 `@GetMapping`。
路径注解写在 `-common` 的共享接口上被两端继承，所以 Feign 与 interface client 无法在同一个
`IMsg*Api` 上共存，msg 这条线是一次性切换。

## 测试

| 测试 | 覆盖 |
|---|---|
| `init/MsgClientContractTest` | 起真实 servlet 容器 + **不带任何自己 mapping 注解**的 mock controller，证明共享契约两端都成立；覆盖 GET + List、GET + 原始 int、以及 **`@PostExchange` 全部参数都是 `@RequestParam`（空 body）** 这种 msg 独有的形状 |
| 5 个 `*FallbackTest` | 各 fallback 的安全默认值，含 `Throwable` 首参重载 |
| `MsgConsumerMarkerTest`（`object`） | 不是真实用例，占住 `io.kudos.ms.msg.consumer` test classpath 包路径供上层集成测试扫描定位 |

## 依赖

- `api(project(":kudos-ms:kudos-ms-msg:kudos-ms-msg-common"))` —— 契约
- `api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-http"))`
  —— interface client 集成 + `AbstractHttpFallbackSupport` 基类 + 全局上下文透传拦截器
- 测试另需 `kudos-test-common` / `kudos-test-container` / `spring-boot-starter-web`

无 `kudos-ms-msg-core` 依赖——避免循环，client 不能含任何业务实现。

## 已知限制 / 后续工作

- ✅ **已解决：`@FeignClient(name = ...)` 用业务名而非服务名** — 原来 5 个 proxy 各自独立做负载
  均衡与熔断，ops 想观测 msg 整体调用得手动 sum 5 个 metric；现在一个 group 一套配置
- ✅ **降级仍能记录失败原因** — 迁移后靠 `Throwable` 首参重载保住，日志仍可区分
  `client-error-4xx` / `server-error-5xx` / `unreachable`
- ❗ **`base-url` 无启动期校验** — 见上文
- ❗ **返回值仍不区分 4xx vs 5xx** — 日志已分类，但调用方拿到的降级返回值（null / 空集）仍是同一个，
  代码层无法据此决定"修参数"还是"稍后重试"；需要差异化时要改契约返回结构
- ❗ **`Throwable` 首参重载不受编译器约束** — 新增契约方法时，契约签名那份漏了会编译失败，
  但重载那份漏了只会让该方法的降级日志退化成 `unknown`
- ❗ **5 个 `@HttpServiceFallback` 与 `types` 列表需手工同步** — 新增 proxy 要在
  `MsgClientAutoConfiguration` 两处各加一行，漏了不报错
- ❗ **降级无指标** — `AbstractHttpFallbackSupport` 只有日志，没有 Micrometer 埋点
