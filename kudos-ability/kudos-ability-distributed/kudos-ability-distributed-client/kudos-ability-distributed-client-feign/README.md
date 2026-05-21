# kudos-ability-distributed-client-feign

OpenFeign 客户端封装。提供 kudos 上下文的"跨服务自动传递"和"统一降级"两个核心能力。

## 设计要点

### 上下文自动透传（`GlobalHeaderRequestInterceptor`）

每个 Feign 请求自动从 `KudosContextHolder` 抓出当前上下文，写到请求头里：

| Header | 来源 | 用途 |
|---|---|---|
| `TENANT_ID` | `context.tenantId` | 多租户路由 |
| `SUB_SYS_CODE` | `context.subSystemCode` | 子系统标识 |
| `TRACE_KEY` | `context.traceKey`（缺则生成 UUID） | 分布式链路追踪 |
| `DATASOURCE_ID` | `context.dataSourceId`（可选） | 多数据源路由 |
| `LOCAL` | `context.clientInfo?.locale`（缺则 `zh_CN`） | i18n |
| `FEIGN_REQUEST` | 常量 `"true"` | 让服务端识别是不是来自 Feign（区别于浏览器） |

服务端的对应 filter（`WebContextInitFilter`）反向读这些头重建 `KudosContext`——一对完整的契约。

如配置 `kudos.ability.distributed.client.feign.contextSignatureSecret`，拦截器会额外写入
`X-Kudos-Context-Timestamp` / `X-Kudos-Context-Nonce` / `X-Kudos-Context-Signature`，对标准
上下文头做 HMAC-SHA256 签名；未配置时保持历史明文透传行为。

### 扩展 SPI `IFeignRequestContextProcess`

不是所有要透传的状态都在 `KudosContext` 里。典型例子：**Seata 全局事务 XID** 放在
`io.seata.core.context.RootContext` 这个独立的 ThreadLocal。本 SPI 是为这种情况留的扩展点：

```kotlin
@Component
class SeataFeignXidProcessor : IFeignRequestContextProcess {
    override fun processContext(template: RequestTemplate, context: KudosContext) {
        RootContext.getXID()?.let { template.header("TX_XID", it) }
    }
}
```

`GlobalHeaderRequestInterceptor` 在写完标准头之后用
`SpringKit.getBeansOfType<IFeignRequestContextProcess>()` 拿到所有实现，依次调用——业务侧
新增透传字段不必改本模块代码，注册一个 bean 即可。

**生产中现成的实现**：`SeataFeignXidProcessor`（在 `kudos-ability-distributed-tx-seata`
模块）。配套服务端 `SeataXidServletFilter` 把 `TX_XID` 头 bind 回 `RootContext`，
`@GlobalTransactional` 跨服务传播才能工作。

### 降级工厂（`GlobalFeignFallBackFactory`）

```kotlin
@FeignClient(name = "user-service", fallbackFactory = GlobalFeignFallBackFactory::class)
interface UserClient { ... }
```

按异常类型映射 HTTP 状态码到 `HttpResult`：

| 异常 | 状态码 |
|---|---|
| 任意嵌套层级的 `FeignException`（有 status 字段） | 透传 |
| `SocketTimeoutException` / `TimeoutException` | 504 |
| `ConnectException` | 503 |
| 其它 | 503 |

调用方拿到 `HttpResult(status, message)` 决定是否重试 / 告警 / 入死信。
若 Feign 接口返回业务对象而不是 `HttpResult`，可复用 `FeignFallbackStatusResolver` 获取同一套
状态码映射，再在业务自己的 fallback 中返回安全默认值。

### Fallback 通用模板（`AbstractFeignFallbackSupport`）

业务侧的 `@FeignClient(fallback = MyFallback::class)` 简化基类。已经把"读 / 写降级"的
日志习惯写好了：

```kotlin
@Component
class UserClientFallback(...) : UserClient, AbstractFeignFallbackSupport("UserClientFallback") {
    override fun getUser(id: Long): User? {
        warnRead("getUser", id)     // log.warn(...)
        return null                 // 安全默认
    }
    override fun createUser(...): Boolean {
        errorWrite("createUser", ...) // log.error(...)
        return false                  // 显式失败
    }
}
```

规则：
- **读接口**：log.warn + 安全默认值（null / 空集合 / false）—— 允许调用方继续工作但拿不到最新数据
- **写接口**：log.error + 显式失败值（0 / false）—— 调用方据此判断是否补偿
- Fallback 内**不要再抛异常**——否则会绕过 Feign 容错机制

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/OpenFeignAutoConfiguration` | 装配入口（`globalHeaderRequestInterceptor` + `globalFeignFallBackFactory`） |
| `interceptor/GlobalHeaderRequestInterceptor` | 上下文 → Feign header 透传 |
| `support/IFeignRequestContextProcess` | 跨服务透传字段的扩展 SPI |
| `fallback/GlobalFeignFallBackFactory` | 异常 → HTTP 状态码 → `HttpResult` 降级 |
| `fallback/AbstractFeignFallbackSupport` | 业务自定义 Fallback 类的日志助手基类 |

## 测试覆盖

- `OpenFeignTest`（与 `MockMsApplication` + `IFeignClient` 配套）——基于 Nacos 测试容器的
  端到端 Feign 调用，覆盖上下文透传 + Fallback 链路
- `GlobalHeaderRequestInterceptorTest` —— 纯单测覆盖标准上下文头、traceKey 生成 / 复用、
  processor 排序，以及可选 HMAC 签名头
- `FeignFallbackStatusResolverTest` —— 纯单测覆盖 timeout / connect / unknown 异常到状态码映射

## 已知限制 / 后续工作

- ✅ `GlobalHeaderRequestInterceptor` 已把 `IFeignRequestContextProcess` 缓存到 `by lazy`
  字段——首次请求触发解析，后续直接复用，热路径不再走 `SpringKit.getBeansOfType` 反射
- ✅ 上下文头透传已支持可选 HMAC-SHA256 签名：配置
  `kudos.ability.distributed.client.feign.contextSignatureSecret` 后会写入 timestamp / nonce /
  signature 头；未配置时保持历史明文透传，仍建议生产用 mTLS / 服务网格保护传输通道
- ✅ `traceKey` 缺失时生成的 UUID 已**反写回 `KudosContext`**——同一逻辑请求里后续所有
  出站 Feign 调用复用同一 traceKey，链路能在 APM 上 stitch 成完整 trace。要让 traceKey
  跨服务统一传递仍需要服务端的 `FeignContextWebFilter` 配合（默认已注册）
- ✅ `GlobalFeignFallBackFactory` 仍保留 `HttpResult(status, message)` 的简单默认行为；状态码
  映射已抽为公共 `FeignFallbackStatusResolver`，返回业务对象的 fallback 可复用同一套异常分类
- ✅ `IFeignRequestContextProcess` 已按 Spring `Ordered` / `@Order` 规则排序，多个实现
  需要确定先后关系时可显式声明 order，并补单测锁住顺序

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.spring.cloud.loadbalancer)
api(libs.spring.cloud.starter.openfeign)

testImplementation(project(":kudos-test:kudos-test-container"))
testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-discovery:kudos-ability-distributed-discovery-nacos"))
testImplementation(libs.spring.boot.starter.web)
```
