# kudos-ability-cache-interservice-client

跨服务缓存协作的 **client 端**（调用方）模块。在 Feign 调用上挂"协商缓存"：把本地
缓存项的 UID 写到请求头，让 provider 端在 UID 命中时只回 `cache-status: 304`，client
端直接复用本地副本，省一次 JSON 反序列化 + 网络传输 body 的开销。

## 设计要点

### 协议契约（与 provider 端 `ClientCacheableAspect` 配合）

| 阶段 | 类 | 行为 |
|---|---|---|
| 调用前：算 cache-key + 携带 cache-uid | `FeignCacheRequestInterceptor` | 用 `tenantId + appName + url::method::body` 算 `apr1Crypt` MD5 作为 key；查本地缓存拿 UID 塞请求头 |
| 调用后：按 cache-status 决定 decode 还是用本地 | `FeignCacheResponseInterceptor` | 把 Spring Cloud 提供的 Feign Decoder 用装饰器包一层：`304` 取本地副本，`200` decode 完写本地缓存 |
| 本地缓存读写 | `ClientCacheHelper` | 复用 `IKeyValueCacheManager`（默认 `localCacheManager` qualifier）；cache region 名固定为 `ClientCacheKey.FEIGN_CACHE_PREFIX` |
| 反序列化（替代 SpringMessageConverter） | `JacksonDecoder` | 直接走 tools.jackson 的 `ObjectMapper`，跳过 `SpringEncoderHttpMessageConverters` 那一层 |

### `FeignCacheResponseInterceptor` 的退路

Decoder 链可视为：

```
RequestForwardingDecoder
  → FeignCacheResponseInterceptor   ← 最外层
      → OptionalDecoder              ← 支持 Optional<T>
          → ResponseEntityDecoder    ← 支持 ResponseEntity<T>
              → JacksonDecoder       ← 真实反序列化
```

每个外层 decoder 都接受"内层 decoder"作为构造参数，标准的装饰器模式。短路条件：
- `cacheHelper.hasLocalCache() == false`（没装本地缓存管理器）→ 直接走原始 decoder，
  本模块功能整体退化为透明
- 响应缺 `cache-uid` / `cache-status` 头 → 直接走原始 decoder（说明 provider 端没启
  `@ClientCacheable`）
- 请求头 `cache-key` 缺失 → 直接走原始 decoder（不该发生，但 defensive）

### `ClientCacheHelper.afterPropertiesSet`：启动期注册 cache region

```kotlin
override fun afterPropertiesSet() {
    if (!hasLocalCache()) return                 // 没本地缓存管理器，整体退化
    val cacheName = ClientCacheKey.FEIGN_CACHE_PREFIX
    cacheManager!!.initCacheAfterSystemInit(mapOf(cacheName to CacheConfig().apply {
        name = cacheName
        ignoreVersion = true                     // Feign 缓存与业务缓存版本独立
        ttl = 600                                // 10 分钟，写死
    }))
}
```

`ignoreVersion = true`——本地 Feign 缓存项的 key 是请求指纹（含 tenantId、appName、url、
body、method），跟业务侧的 `kudos.ability.cache.version` 无关。`ttl = 600` 当前是硬编码，
未来要让业务侧可配的话见"已知限制"。

### Key 生成中的 `tenantId` 注入

```kotlin
val tenantId = KudosContextHolder.get().tenantId ?: ""
```

多租户场景下，相同 URL + 相同 body 但不同 tenant 的请求结果可能完全不同——必须把
tenantId 拼进 key。复用 kudos 上下文的 `KudosContextHolder`（由
`kudos-ability-distributed-client-feign` 的 `GlobalHeaderRequestInterceptor` 写入）。

### `feignDecoder` bean 用 `@Primary`

```kotlin
@Bean("feignDecoder")
@Primary
@ConditionalOnMissingBean(name = ["feignDecoder"])
open fun feignDecoder(...): Decoder { /* ... */ }
```

`@Primary` 强行覆盖 Spring Cloud OpenFeign 默认装配的 `SpringDecoder`——只要本模块在
classpath，所有 `@FeignClient` 都走本模块的缓存装饰链。要退出请提供同名 `feignDecoder`
bean（`@ConditionalOnMissingBean(name = ["feignDecoder"])` 让位）。

## 模块入口

| 路径 | 角色 |
|---|---|
| `client/init/InterServiceCacheClientAutoConfiguration` | 装配入口：注册 helper、request interceptor、`@Primary` feignDecoder |
| `client/init/JacksonDecoder` | tools.jackson 版本的 Feign Decoder（替代 SpringDecoder） |
| `client/core/ClientCacheHelper` | 本地缓存读写 + 启动期 region 注册 |
| `client/feign/FeignCacheRequestInterceptor` | 请求前算 cache-key、塞 cache-uid 请求头 |
| `client/feign/FeignCacheResponseInterceptor` | 响应后按 cache-status 决定 decode 还是返回本地缓存 |

## 配置示例

应用引入：

```kotlin
implementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-client"))
implementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine")) // 本地缓存实现
implementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
```

需要确保有 `localCacheManager` qualifier 的 `IKeyValueCacheManager` bean——
`kudos-ability-cache-local-caffeine` 默认提供。没有的话本模块所有缓存行为整体退化为
原 SpringDecoder 直通。

## 测试覆盖

本模块的端到端测试位于 `cache-interservice-provider/test-src/`，因为需要一对 JVM
（client + provider）协作。见 provider README 的"测试覆盖"段。

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-common"))
api(libs.spring.boot.starter.web)
compileOnly(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))

testImplementation(project(":kudos-test:kudos-test-container"))
```

`compileOnly` 是因为 `FeignCacheRequestInterceptor` 要 implements `feign.RequestInterceptor`
但运行时由消费方应用决定是否真的拉 OpenFeign 依赖——非 Feign 应用也能 compile 但 bean
不会注册（`@ConditionalOnMissingBean` 之外应该再加 `@ConditionalOnClass(RequestInterceptor::class)`，
见下方"已知限制"）。

## 已知限制 / 后续工作

- ✅ `InterServiceCacheClientAutoConfiguration` 已标 `@Configuration`——CGLIB 代理 + bean
  方法互调保持同一实例语义
- ✅ `FeignCacheRequestInterceptor` 已改为构造器注入，由 auto-configuration 显式创建
- ❗ Feign 本地缓存 TTL = 600s 硬编码在 `ClientCacheHelper.afterPropertiesSet`；要可配
  需要走 `@ConfigurationProperties` 暴露
- ❗ `@Primary` 强行覆盖默认 Feign Decoder——同进程内业务方如果手动装了 `feignDecoder`
  会被本模块抢占，需要在 README 中显式提醒
- ✅ `InterServiceCacheClientAutoConfiguration` 已加 `@ConditionalOnClass(feign.RequestInterceptor::class)`
- ❗ `feignCache().get<ClientCacheItem>(cacheKey)` 用扩展函数依赖响应对象类型与 cache 实现一致；
  Caffeine cache 当前能命中，但 Redis 反序列化路径上 `ClientCacheItem` 是 JDK Serializable，
  与 RedisTemplate 默认 GenericJackson2 配置不兼容——只在 local cache 路径有验证
