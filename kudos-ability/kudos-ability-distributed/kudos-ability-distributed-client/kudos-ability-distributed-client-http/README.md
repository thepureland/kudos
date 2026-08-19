# kudos-ability-distributed-client-http

Spring Interface Clients（`@HttpExchange` + `RestClient`）客户端封装：kudos 上下文的跨服务自动传递
+ 统一降级。**全仓唯一的跨服务客户端实现** —— `kudos-ability-distributed-client-feign` 与
`spring-cloud-starter-openfeign` 依赖已于 2026-08-19 删除。

下文保留大量"与 Feign 的对应关系"，不是因为 Feign 还在，而是因为迁移中踩到的坑几乎都源于两者语义
差异；写下来是为了让后来者不必重新踩一遍。

## 为什么是它

Spring Cloud OpenFeign 官方已标记 feature-complete（只修 bug 不加特性），并建议迁移到 Spring HTTP
Service Clients。此前挡路的两个能力缺口，在当前依赖栈（Spring Boot 4.1.0 / Spring Cloud 2025.1.2，
即 `spring-cloud-commons` 5.0.2）里都已经补齐：

| 缺口 | 补齐方式 | 提供者 |
|---|---|---|
| 服务发现 / 负载均衡 | group 的 base-url 写 `lb://<service-id>` | `LoadBalancerRestClientHttpServiceGroupConfigurer` |
| 降级（`@FeignClient(fallback=)`） | `@HttpServiceFallback` | `CircuitBreakerRestClientHttpServiceGroupConfigurer` |

## 与 Feign 的对应关系

| client-feign | 本模块 |
|---|---|
| `@FeignClient(name = "sys")` | 接口标 `@HttpExchange`，注册用 `@ImportHttpServices(group = "sys", types = [...])`，服务 id 写在 group 的 `base-url` |
| `RequestInterceptor` / `GlobalHeaderRequestInterceptor` | `ClientHttpRequestInterceptor` / `KudosContextRequestInterceptor` |
| `IFeignRequestContextProcess(RequestTemplate, ctx)` | `IHttpRequestContextProcess(HttpRequest, ctx)` |
| `@FeignClient(fallback = X)` | `@HttpServiceFallback(value = X, service = [...], group = "...")` |
| `GlobalFeignFallBackFactory` | 无对应物，也不需要——见下 |
| `FeignFallbackStatusResolver` | `HttpFallbackStatusResolver` |
| `AbstractFeignFallbackSupport` | `AbstractHttpFallbackSupport` |
| `FeignContextSignature` | `HttpContextSignature`（**头名与取值完全相同**） |

**为什么没有 `GlobalFeignFallBackFactory` 的对应物**：Feign 自己没有熔断器，所以需要一个手写工厂把异常
翻译成 `HttpResult`。interface client 这条路上每次调用都过 Spring Cloud CircuitBreaker，降级 bean 由
`@HttpServiceFallback` 按服务指定，工厂这层就没有存在意义了。从 Feign 那边留下来的是两个纯逻辑件——
`AbstractHttpFallbackSupport`（日志约定）和 `HttpFallbackStatusResolver`（异常 → 状态码分类），
任何 `@HttpServiceFallback` bean 都能直接用。

## 用法

### 1. 定义接口

```kotlin
@HttpExchange("/api/dict")
interface ISysDictClient {
    @GetExchange("/{code}")
    fun byCode(@PathVariable code: String): DictItem?
}
```

注意接口上**没有**服务 id——部署形态不写进契约。

### 2. 注册 group

```kotlin
@Configuration
@ImportHttpServices(group = "sys", types = [ISysDictClient::class])
@HttpServiceFallback(value = SysDictClientFallback::class, service = [ISysDictClient::class], group = "sys")
open class SysClientConfiguration
```

### 3. 配置 group 的目标

```yaml
spring:
  http:
    serviceclient:
      sys:
        base-url: lb://kudos-ms-sys      # lb:// = 走 Nacos + LoadBalancer
        connect-timeout: 2s
        read-timeout: 5s
```

同一个 group 可以挂多个接口——`kudos-ms-sys-client` 下十几个 `ISys*Proxy` 全指向同一个服务，
正好一个 group，比 Feign 每个接口重复写 `name = "sys"` 更贴合。

### 4. 写 fallback

```kotlin
class SysDictClientFallback :
    ISysDictClient,
    AbstractHttpFallbackSupport("SysDictClientFallback") {

    override fun byCode(code: String): DictItem? {
        warnRead("byCode", code)
        return null
    }
}
```

规则与 Feign 版一致：读接口 warn + 安全默认值；写接口 error + 显式失败值；**fallback 内不要再抛异常**。

### 需要知道降级原因时

`@HttpServiceFallback` 走无参构造，拿不到触发降级的异常——但
`CircuitBreakerConfigurerUtils.resolveFallbackMethod` 会**查两次方法**：先找
`name(Throwable, ...args)`，找不到才用 `name(...args)`。所以带 `Throwable` 首参的重载就是
Feign `FallbackFactory<T>.create(cause)` 的替代品：

```kotlin
open class SysDictClientFallback : ISysDictClient, AbstractHttpFallbackSupport("SysDictClientFallback") {

    // 降级时实际被调用的是这个
    fun byCode(cause: Throwable?, code: String): DictItem? {
        warnRead("byCode", cause, code)   // 日志能分出 client-error-4xx / server-error-5xx / unreachable
        return null
    }

    // 保留契约签名，让编译器强制检查"每个契约方法都有 fallback"
    override fun byCode(code: String): DictItem? = byCode(null, code)
}
```

`kudos-ms-msg-client` 就是这么把原来的 `fallbackFactory` 平移过来的。

### Kotlin 侧的硬约束

两条，踩中都是启动期报错：

1. **必须 `open class`**。Spring Cloud 会给 fallback 套 CGLIB 代理，Kotlin 类默认 final，会抛
   `AopConfigException: Could not generate CGLIB subclass ... final class or a non-visible class`。
   `kotlin-spring` allopen 插件救不了——它只对带 `@Component` / `@Service` 等注解的类生效，而
   `@HttpServiceFallback` 绑定的 fallback 不带这些注解。**迁移 kudos-ms 时每个 `*Fallback` 类都要加
   `open`。**
2. **不能用构造注入**：框架走无参构造实例化。

## 上下文透传

`KudosContextRequestInterceptor` 由 `HttpClientAutoConfiguration` 装到**每一个** group 的
`RestClient.Builder` 上，写入的头与 Feign 版逐字节相同：

| Header | 来源 |
|---|---|
| `TENANT_ID` | `context.tenantId` |
| `SUB_SYS_CODE` | `context.subSystemCode`（为 null 时**不写该头**，见下） |
| `TRACE_KEY` | `context.traceKey`（缺则生成 UUID **并写回 context**） |
| `DATASOURCE_ID` | `context.dataSourceId`（可选） |
| `LOCAL` | `context.clientInfo?.locale`（缺则 `zh_CN`） |
| `RPC_REQUEST` | 常量 `"true"` |

`HttpHeaders.set(name, null)` 与 Feign 的 `RequestTemplate.header(name, null)` 行为不同：前者仍会把头发
出去、值为空串，provider 侧读到的是 `""` 而不是"没有这个头"。因此可空字段（`SUB_SYS_CODE` /
`DATASOURCE_ID`）一律用 `?.let { set(...) }` 跳过，保证两种传输在 provider 眼里完全一致。

`RPC_REQUEST`（线上值 `_rpc_request`）是 provider 侧 `InternalRpcContextWebFilter` /
`ClientCacheWebFilter` 的准入标记：没带这个头的请求（浏览器 / curl）不会被当成内部调用，也就伪造不了
租户上下文。

迁移期间它叫 `FEIGN_REQUEST` / `_feign_request` 且刻意不改，为的是让 provider 侧一行代码都不用动——
这正是分步迁移（每个 ms 模块独立切换）的前提。Feign 全部下线后已于 2026-08-20 改为中性名，
**线上值也一并改了，新旧版本不能混跑**。

### 签名

配置 `kudos.ability.distributed.client.http.contextSignatureSecret` 后写入
`X-Kudos-Context-Timestamp` / `-Nonce` / `-Signature`，HMAC-SHA256，payload 布局与 Feign 版及
provider 侧 `InternalRpcContextSignatureVerifier` 一致：

```
method \n url \n TENANT_ID \n SUB_SYS_CODE \n TRACE_KEY \n DATASOURCE_ID \n LOCAL \n timestamp \n nonce
```

**`url` 签的是 path（含 query），不是绝对 URL。** 理由有两条：provider 只能从
`HttpServletRequest.requestURI` 重建路径；而且 LoadBalancer 拦截器只改写 scheme + authority
（`lb://svc` → `http://10.0.0.1:8080`）不改 path，签 path 让签名与拦截器顺序无关。

## 扩展 SPI `IHttpRequestContextProcess`

与 Feign 版同义，只有第一个参数从 `RequestTemplate` 变成 `HttpRequest`：

```kotlin
@Component
class SeataXidProcessor : IHttpRequestContextProcess {
    override fun processContext(request: HttpRequest, context: KudosContext) {
        RootContext.getXID()?.let { request.headers.add("TX_XID", it) }
    }
}
```

按 Spring `Ordered` / `@Order` 排序，首次调用后缓存。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/HttpClientAutoConfiguration` | 装配入口（拦截器 + group configurer） |
| `init/HttpClientDefaultsEnvironmentPostProcessor` | 下发 Resilience4j 默认值（见下文「线程池会切断上下文」） |
| `interceptor/KudosContextRequestInterceptor` | 上下文 → HTTP header 透传 + 签名 |
| `support/IHttpRequestContextProcess` | 跨服务透传字段的扩展 SPI |
| `fallback/AbstractHttpFallbackSupport` | fallback 类的日志助手基类 |
| `fallback/HttpFallbackStatusResolver` | 异常 → HTTP 状态码分类 |

## 测试覆盖

| 测试 | 覆盖 |
|---|---|
| `HttpServiceClientTest` | 端到端（Nacos 容器）：`lb://` 服务发现、GET/POST 往返、异常透传、上下文头**由 provider 侧回显断言**、`@HttpServiceFallback` 实际触发、**同名 group 跨多个 `@ImportHttpServices` 合并**、**`Throwable` 首参 fallback 收到异常** |
| `KudosContextRequestInterceptorTest` | 16 个用例，逐条对应已删除的 `GlobalHeaderRequestInterceptorTest`，迁移时用来保证行为不漂移 |
| `HttpFallbackStatusResolverTest` | 状态码分类，含 `ResourceAccessException` 包裹与循环 cause 链 |
| `AbstractHttpFallbackSupportTest` | `describeStatus` 四类分支（Feign 版这块**零覆盖**，没有把洞一起搬过来） |
| `HttpClientDefaultsEnvironmentPostProcessorTest` | 默认值确实下发、应用显式配置优先、property source 排在最后 |

## ⚠️ Resilience4j 的线程池会切断上下文（本模块已默认处理）

`spring-cloud-starter-circuitbreaker-resilience4j` 是本模块的 `api` 依赖（`@HttpServiceFallback` 靠它
才生效）。它开箱默认把每次调用包进一个**由线程池承载的 TimeLimiter**——而 `KudosContextHolder` 是
ThreadLocal，于是出站调用跑在池线程上，`KudosContextRequestInterceptor` 读到的是**空白上下文**：
请求发出去时没有 tenantId、没有 subSysCode、traceKey 也是新的。多租户系统里这是隔离性事故，不是性能话题。

**本模块通过 `HttpClientDefaultsEnvironmentPostProcessor` 默认下发：**

```properties
spring.cloud.circuitbreaker.resilience4j.disable-thread-pool=true
```

调用回到调用方线程，上下文恢复，**熔断器与 `@HttpServiceFallback` 照常工作**（两者都有端到端用例覆盖）。
这是**默认值不是覆盖**——应用自己配的永远优先。

### 代价：TimeLimiter 不再限时，超时改由 HTTP 层负责

在调用方线程上 Resilience4j 无法中断调用，所以 TimeLimiter **不再强制任何东西**。这是实测结论，不是推断：

| 配置 | 上下文透传 | 3 秒调用 + 1 秒限时 |
|---|---|---|
| 默认（线程池开） | ❌ tenantId 丢失 | 1 秒被切断 |
| `disable-thread-pool: true`（本模块默认） | ✅ | **跑完 3 秒，成功返回** |

所以**真正的超时必须配在 group 上**：

```yaml
spring:
  http:
    serviceclient:
      sys:
        base-url: lb://kudos-ms-sys
        connect-timeout: 2s
        read-timeout: 5s     # ← 这才是真正的超时
```

读超时会以 `ResourceAccessException` 冒出来，并像其他失败一样进入该 group 的 fallback
（`HttpFallbackStatusResolver` 把它映射成 504）。`HttpServiceClientTest` 的
`a slow call is cut off by the group read-timeout, not by the TimeLimiter` 钉住了这条路径。

> **配了 `base-url` 却没配 `read-timeout` = 该 group 的调用没有任何时长上限。** 这是本模块下发默认值
> 之后最容易漏的一项，务必进部署清单。

如果某个应用确实需要硬性的总时长限制，可以把 `disable-thread-pool` 改回 `false` —— 但**必须自己把
kudos 上下文传播到 Resilience4j 的线程池上**，否则就是拿租户隔离换超时。

## 已知限制 / 后续工作

- ✅ **已解决：签名链路端到端验证**。`HttpServiceClientTest` 的
  `the client signs context headers and the provider verifies them` 两头都钉住了：客户端确实把 HMAC 头
  发上了线（provider 回显断言），且**一个带内部标记但没签名的请求会被 provider 回 401**——后半句是关键，
  否则"测试全绿"同样可以由"验签器压根没注册"来解释，而这正是这条链路长期未被验证的原因。
  （被替代的 client-feign 模块从未做到这点：它的客户端单测把签名 payload 里的 url 钉成绝对 URL
  `http://localhost/users`，而 provider 只按 path 重建候选，两边从未在一个测试里对接过。）
- ❗ **mock provider 不能用 `@EnableKudos`**。它与客户端跑在同一个 JVM，`@EnableKudos` 会覆盖 JVM 级的
  `SpringKit.applicationContext`，之后客户端的 `KudosContextRequestInterceptor` 会去 provider 的上下文里
  解析 `IHttpRequestContextProcess` bean。所以 provider 侧那两个 filter 是手工装配的
  （`MockMsSignatureVerificationConfiguration`）。同样的原因，**traceKey 写回**那半条规则只在
  `KudosContextRequestInterceptorTest` 里断言，不在端到端测试里——在那里断言测的是测试脚手架而不是产品。
- **重试未装配**。`spring.http.serviceclient.*` 只有超时，没有重试策略；幂等方法的重试需要显式加
  `LoadBalancedRetryFactory` 或在 Resilience4j 侧配置。
- **降级事件无指标**。与 Feign 版同样的问题：只有日志，没有 Micrometer 计数器。
- ✅ **已解决：头名 / 配置键 / 缓存 region 名中性化**（2026-08-20）。`FEIGN_REQUEST` → `RPC_REQUEST`（线上值同改）、`...nacos.feign-context-filter.*` → `rpc-context-filter.*`、`FEIGN-CACHE` → `RPC-CACHE`、UID 盐 `feignCache` → `rpcCache`。**配置键与线上值都变了，见部署清单。**

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.spring.boot.restclient)
api(libs.spring.cloud.loadbalancer)
api(libs.spring.cloud.starter.circuitbreaker.resilience4j)
```

`spring-cloud-starter-circuitbreaker-resilience4j` 不是可选项：
`CircuitBreakerInterfaceClientsAutoConfiguration` 的条件是
`@ConditionalOnBean(CircuitBreakerFactory)`，缺了它 `@HttpServiceFallback` **不会报错，只会静默失效**。
