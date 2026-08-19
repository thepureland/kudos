# kudos-ability-cache-interservice-client

跨服务缓存协作的 **client 端**（调用方）模块。在跨服务调用上挂"协商缓存"：把本地缓存项的 UID 写到
请求头，让 provider 端在 UID 命中时只回 `cache-status: 304`，client 端直接复用本地副本，
**省掉一次 body 的网络传输**。

传输实现是 **Spring Interface Clients**（`@HttpExchange` + `RestClient`）。2026-08-19 从 OpenFeign
迁移，改动比其他模块大，见文末「从 OpenFeign 迁移」。

## 设计要点

### 协议契约（与 provider 端 `ClientCacheableAspect` 配合）

**线上协议一字未改**，所以 provider 端不需要任何改动：

| 方向 | 头 | 含义 |
|---|---|---|
| 请求 | `cache-key` | 客户端本地指纹；provider 只检查存在性，从不解释其值 |
| 请求 | `cache-uid` | 本地已有副本的内容指纹（没有副本时不带） |
| 响应 | `cache-status: 304` | 你的副本是最新的，body 故意为空 |
| 响应 | `cache-status: 200` + `cache-uid` | 新数据，附新指纹 |

### 一个拦截器，缓存的是字节

Feign 有两个独立接缝——`RequestInterceptor` 与 `Decoder` 装饰器，且 Feign 的 `Response` 带着它的
`Request`，所以 decoder 能读回自己发出去的 `cache-key`，进而返回**上次解码好的对象**。

`RestClient` 没有这个反向引用：`ClientHttpResponse` 不知道自己的 request。拆成两个组件就得靠
ThreadLocal 传 cache key。所以本模块把两侧合并进**一个** `ClientHttpRequestInterceptor`
（`HttpCacheNegotiationInterceptor`）——cache key 只是发送与接收之间的一个局部变量——代价是这个接缝
位于反序列化**之下**，缓存的只能是原始响应字节。

权衡说清楚：

- **失去**：304 命中时要从缓存字节重新反序列化一次，而不是直接拿到现成对象。
  但**网络 body 传输仍然省掉了**——那才是这套机制存在的理由。
- **得到**：调用方不再共享同一个可变对象。旧设计把同一个实例发给该端点的每一个调用方，
  谁改了自己的返回值就悄悄污染了所有人的缓存（旧端到端测试断言的 `obj1 === obj2` 正是这个症状）。

### 缓存项必须连 `Content-Type` 一起存

304 没有 body，也就没有 `Content-Type`。若照 304 自己的头回放缓存字节，消息转换器看到的是
`application/octet-stream`，直接抛
`UnknownContentTypeException: no suitable HttpMessageConverter found`。所以缓存的是
`CachedResponseBody(contentType, bytes)`，回放时把 200 那次的 content type 一起恢复。
（这条是端到端测试抓出来的——单测的桩响应没设 content type，测不出来。已在单测里补断言钉住。）

### 启动期注册 cache region

```kotlin
override fun afterPropertiesSet() {
    if (!hasLocalCache()) return                 // 没本地缓存管理器，整体退化
    val cacheName = ClientCacheKey.FEIGN_CACHE_PREFIX
    cacheManager!!.initCacheAfterSystemInit(mapOf(cacheName to CacheConfig().apply {
        name = cacheName
        ignoreVersion = true                     // 协商缓存与业务缓存版本独立
        ttl = properties.ttlSeconds              // 默认 600s，可配置
    }))
}
```

`ignoreVersion = true`——本地协商缓存项的 key 是请求指纹（含 tenantId、appName、被调方、url、
method、body），跟业务侧的 `kudos.ability.cache.version` 无关。

### Key 生成

组成与 Feign 版一致，两处因传输而变：

- **被调方标识**改成请求的 scheme + authority。Feign 时代用的是 `@FeignClient` 名 + target url，
  因为拦截器跑在 base URL 附加**之前**；`ClientHttpRequestInterceptor` 跑在 group 的 `base-url`
  应用**之后**，直接就有解析后的主机。
- **url 只取 path + query**，这样 LoadBalancer 选中同一服务的不同实例时不会把缓存打散。

`tenantId` 走 `KudosContextHolder.getOrNull()`（**不是 `get()`**：`get()` 会在没有上下文时创建一个并
绑到当前线程，而这里跑在 HTTP 客户端 / `@Async` 线程池上，既泄漏一个线程池永不清理的上下文，又会得到
`tenantId` 为空串的上下文，把所有租户塌缩到同一个 key）。

body 按**原始字节**参与哈希，绝不 `String(body)`：用平台字符集解码任意 body 既不跨机器确定，
对二进制负载还是有损的，两个不同的 body 可能塌缩到同一个 key，从而把别人的缓存响应喂给这次请求。

### 选择参与协商的范围

Feign 版按 `@FeignClient` 名匹配（`includeClients` / `excludeClients`）。interface client 没有
per-interface 名字，选择单位改成 **HTTP service group**：

```yaml
kudos:
  ability:
    cache:
      interservice:
        client:
          ttl-seconds: 600
          exclude-groups: [ thirdparty ]   # 默认两个列表都空 = 全部参与
```

这个开关不是可有可无：装配会给**每一个** group 挂拦截器，不加限制的话 `cache-key` / `cache-uid`
也会发给第三方 API——既泄漏一个内部内容指纹，也可能被对签名 / 白名单敏感的网关拒绝。

## 模块入口

| 路径 | 角色 |
|---|---|
| `client/init/InterServiceCacheClientAutoConfiguration` | 装配入口：helper + 每 group 一个协商拦截器 |
| `client/http/HttpCacheNegotiationInterceptor` | 请求侧算 key/塞头 + 响应侧按 cache-status 回放或缓存 |
| `client/http/CachedResponseBody` | 缓存的响应体：字节 + content type |
| `client/core/ClientCacheHelper` | 本地缓存读写 + 启动期 region 注册 |

## 配置示例

```kotlin
implementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-client"))
implementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine")) // 本地缓存实现
implementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-http"))
```

需要有 `localCacheManager` qualifier 的 `IKeyValueCacheManager` bean——
`kudos-ability-cache-local-caffeine` 默认提供。没有的话本模块整体退化为透明直通。

## 测试覆盖

| 测试 | 覆盖 |
|---|---|
| `http/HttpCacheNegotiationInterceptorTest` | 直接驱动协议：首次调用只发 key 不发 uid 并缓存字节 + content type；二次调用发 uid、304 由本地副本回放且状态/长度/类型被修正；key 稳定性与按 path / body 区分；无协商头直通不缓存；304 但本地条目已失效时降级重试而非报错；group 包含 / 排除 |
| `core/ClientCacheHelperTest` | TTL 配置、非法 TTL 快速失败、缓存项类型不匹配时 evict + miss |

端到端测试在 `cache-interservice-provider/test-src/`（需要 client + provider 一对上下文协作），
覆盖的是"协商对调用方透明"；**能证明 304 真的命中的是上面的协议单测**——从外部看，缓存命中和
重新拉取长得一模一样。

## 从 OpenFeign 迁移（2026-08-19）

| 改动 | 说明 |
|---|---|
| `FeignCacheRequestInterceptor` + `FeignCacheResponseInterceptor` → 一个 `HttpCacheNegotiationInterceptor` | 见上文「一个拦截器，缓存的是字节」 |
| 删除 `@Primary` 的 `feignDecoder` bean 及 `decoder-enabled` 开关 | 无对应物，也不需要 |
| 删除 `JacksonDecoder` | `RestClient` 直接用应用自己的 `HttpMessageConverter` |
| `includeClients` / `excludeClients` → `includeGroups` / `excludeGroups` | 选择单位从 Feign client 名变成 HTTP service group |
| 缓存内容：解码后的对象 → `CachedResponseBody`（字节 + content type） | 见上文 |

**最该注意的一处收益**：旧设计为了加一个特性，用 `@Primary` 覆盖了 Spring Cloud 的
`SpringDecoder`——一个模块在 classpath 上就改变了整个应用的解码链，还得再配一个
`decoder-enabled` 开关来退出。现在什么都没覆盖。

## 已知限制 / 后续工作

- ✅ **已解决：304 命中返回共享可变引用** — 旧版直接返回本地缓存对象本体，业务侧改了返回的 DTO
  就会污染后续所有请求读到的缓存。现在缓存的是字节，每次调用得到独立实例
- ✅ **已解决：`@Primary` 覆盖框架 decoder** — 见上文
- ❗ **304 命中仍要反序列化一次** — 这是接缝下移的代价，见上文权衡
- ❗ **`exclude-groups` 需要显式配置** — 默认对所有 group 生效，包括指向第三方 API 的 group
- ❗ **缓存字节按 group 而非按返回类型隔离** — key 含 url + method + body，同一端点返回类型不会变，
  所以目前安全；但若将来同一 url 按 `Accept` 返回不同表示，key 里没有 `Accept` 会串
- ❗ **无缓存命中率指标** — 只有 debug 日志，没有 Micrometer 埋点
