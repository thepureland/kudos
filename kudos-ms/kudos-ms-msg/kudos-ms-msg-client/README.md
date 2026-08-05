# kudos-ms-msg-client

Msg 服务的 **Feign 客户端代理 + 降级**——其他微服务通过 Kotlin 接口调用远端 msg 服务。
**不依赖 msg-core**，纯远端调用包装。

## 包结构

```
io.kudos.ms.msg.client.
├── template/   {proxy/IMsgTemplateProxy,  fallback/MsgTemplateFallback}
├── instance/   {proxy/IMsgInstanceProxy,  fallback/MsgInstanceFallback}
├── receiver/   {proxy/IMsgReceiveProxy,   fallback/MsgReceiveFallback}
└── send/       {proxy/IMsgSendProxy,      fallback/MsgSendFallback}
```

每个业务领域一对 `Proxy + Fallback`，全部都是 Kotlin `interface` + `open class`，
方便 Spring Cglib 代理。

## Proxy 与 FeignClient name

| Proxy | `@FeignClient(name)` | 实现的契约 | 降级（fallbackFactory） |
|-------|----------------------|------------|------|
| `IMsgTemplateProxy` | `"msg-template"` | `IMsgTemplateApi` | `MsgTemplateFallbackFactory` → `MsgTemplateFallback` |
| `IMsgInstanceProxy` | `"msg-instance"` | `IMsgInstanceApi` | `MsgInstanceFallbackFactory` → `MsgInstanceFallback` |
| `IMsgReceiveProxy` | `"msg-receive"` | `IMsgReceiveApi` | `MsgReceiveFallbackFactory` → `MsgReceiveFallback` |
| `IMsgSendProxy` | `"msg-send"` | `IMsgSendApi` | `MsgSendFallbackFactory` → `MsgSendFallback` |

> ⚠️ FeignClient name **按业务名而非服务名**：`msg-template` / `msg-send` 都指向同一个
> `msg` 微服务实例（注册名 `msg`，见 `SysConsts.ATOMIC_SERVICE_NAME`）。Spring Cloud 的
> 服务发现按 name 解析，这意味着 4 个 proxy 各持一份 LoadBalancer 状态。如果想做精细
> 的熔断隔离（让 send 失败不影响 template 读取），这种拆法 OK；如果将来想合并，需要先
> 检查 Hystrix / Resilience4j 的 instance metrics 是否有依赖。

接口体常态：

```kotlin
@FeignClient(name = "msg-send", fallbackFactory = MsgSendFallbackFactory::class)
interface IMsgSendProxy : IMsgSendApi
```

——`IMsgSendApi` 在 common 已经挂好方法级 `@PostMapping("/api/internal/msg/send/publish")`，
Feign 直接复用，proxy 不需要再写路径。

## Fallback 模式

所有 fallback 都继承 `AbstractFeignFallbackSupport("ComponentName")`
（来自 `kudos-ability-distributed-client-feign`），并通过 `FallbackFactory` 拿到触发降级的
原始异常 `cause`（可区分 4xx / 5xx / unreachable）：

- **读接口**调 `warnRead(method, cause, args)` —— `WARN` 级别落日志 + 返回安全默认值（`null` /
  `emptyList()` / `0` / `false`）
- **写接口**调 `errorWrite(method, cause, args)` —— `ERROR` 级别落日志 + 返回失败标识（`false` /
  `0` / `null`）让调用方决定补偿

实例：

```kotlin
open class MsgSendFallback(
    private val cause: Throwable? = null,
) : AbstractFeignFallbackSupport("MsgSendFallback"), IMsgSendProxy {

    override fun publish(request: MsgPublishRequest): String? {
        errorWrite("publish", cause, request)
        return null         // ← 调用方按 null 判定走重试或入业务侧补偿表
    }
}

@Component
open class MsgSendFallbackFactory : FallbackFactory<IMsgSendProxy> {
    override fun create(cause: Throwable): IMsgSendProxy = MsgSendFallback(cause)
}
```

**核心原则**：fallback **从不抛异常**——异常一旦冒出去，调用方业务流就断了。所有降级
路径都返回"看似成功但内容为空 / null"，把决策权交还调用方。

## 使用姿态

业务侧引入：

```kotlin
implementation(project(":kudos-ms:kudos-ms-msg:kudos-ms-msg-client"))
```

业务侧典型用法：

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

`MsgClientAutoConfiguration` 已提供 `@EnableFeignClients(basePackages = ["io.kudos.ms.msg.client"])`
和 `@ComponentScan`，由 kudos 的 `IComponentInitializer` 扫描机制导入。消费方通常只需
依赖本模块并启用 `@EnableKudos`；若没有走 kudos 装配，可显式：

```kotlin
@EnableFeignClients(basePackages = ["io.kudos.ms.msg.client"])
```

## 与 common 的对齐

- `IMsgReceiverGroupApi` 已有最小查询契约；client 侧同步提供 `IMsgReceiverGroupProxy`
  与 `MsgReceiverGroupFallback`。
- common 里 4 个 `IMsg*Api` 的方法签名变更需要立即同步到 client；编译器会替你检查
  （proxy 实现了 common 接口），所以契约漂移会在编译期暴露——这是 `interface IMsgSendProxy : IMsgSendApi`
  这种"继承式 proxy"的最大优点。

## 测试

- `MsgReceiverGroupFallbackTest` —— fallback 安全默认值的纯 JVM 单测
- `MsgConsumerMarkerTest`（`object`）—— 不是真实测试用例，作用是占住
  `io.kudos.ms.msg.consumer` test classpath 包路径，给上层集成测试（在 consumer 域）
  注入测试上下文时做扫描定位用

proxy 的请求路径 / body 契约没有 WireMock 类测试，有效性靠下游服务的端到端测试覆盖。

## 依赖

- `api(project(":kudos-ms:kudos-ms-msg:kudos-ms-msg-common"))` —— 契约
- `api(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))`
  —— Feign 抽象 + `AbstractFeignFallbackSupport` 基类 + 全局 RequestInterceptor / FallbackFactory
- `testImplementation(project(":kudos-test:kudos-test-container"))`

无 `kudos-ms-msg-core` 依赖——避免循环，client 不能含任何业务实现。

## 已知限制 / 后续工作

- ✅ 已补 `MsgClientAutoConfiguration`，统一扫描 msg-client 下的 Feign proxy 与 fallback
- ✅ 已补 `IMsgReceiverGroupProxy` / `MsgReceiverGroupFallback`
- ✅ **fallback 已记录失败原因**：全部改为 `FallbackFactory` 注入 `cause`，
  `warnRead` / `errorWrite` 日志可区分 `client-error-4xx` / `server-error-5xx` / `unreachable`
- ❗ **返回值仍不区分 4xx vs 5xx**：日志已分类，但调用方拿到的降级返回值（null / 空集）
  仍是同一个，代码层无法据此决定"修参数"还是"稍后重试"；需要差异化时要改契约返回结构
- ❗ **`@FeignClient(name = ...)` 用业务名而非服务名**——4 个 proxy 各自独立做负载均衡
  与熔断；如果 ops 想观测 msg 服务整体调用，需要在 Grafana 上手动 sum 4 个 metric
- ❗ **缺契约测试**：现有单测只覆盖 fallback 默认值；建议至少加一个 `WireMock`
  驱动的契约测试，验证 proxy 拼出来的请求路径 / body 跟 common 接口注解一致
