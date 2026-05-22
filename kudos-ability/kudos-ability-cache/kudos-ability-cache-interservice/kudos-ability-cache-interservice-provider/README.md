# kudos-ability-cache-interservice-provider

跨服务缓存协作的 **provider 端**（被调用方）模块。被 `kudos-ability-cache-interservice-client`
通过 Feign 调用时，告诉 client 端"本次响应是否与你本地缓存的 UID 一致"，从而决定 client
端用本地副本还是用 body。

## 设计要点

### 协议契约（与 client 的 Feign Interceptor 配合）

| 步骤 | 模块 | 类 |
|---|---|---|
| ① client 把本地缓存 UID 写入 `cache-uid` 请求头 | `kudos-ability-cache-interservice-client` | `FeignCacheRequestInterceptor` |
| ② provider 端 servlet filter 把请求包成 `CacheClientRequest`（携带 response 引用） | 本模块 | `ClientCacheWebFilter` |
| ③ provider Controller 方法标了 `@ClientCacheable`，Aspect 拦下 | 本模块 | `ClientCacheableAspect` |
| ④ Aspect 算响应 UID = `MD5(<FQN>#<JSON>)`、写回响应头 `cache-uid` / `cache-status` | 本模块 | `ClientCacheableAspect` |
| ⑤ client 端 Feign Decoder 看 `cache-status`：`304` → 用本地缓存；`200` → decode + 写本地缓存 | `kudos-ability-cache-interservice-client` | `FeignCacheResponseInterceptor` |

### `ClientCacheableAspect` 的两条短路

- **目标类不是 `@RestController` / `@Controller`** → 直接 `IllegalArgumentException`，**编译期**
  没法强制，所以放运行时校验
- **当前请求不是 `CacheClientRequest`**（即非 Feign 调用、普通浏览器 / curl）→ 直接返回原结果，
  不写响应头——避免给 ETag-like 头污染外部直接调用 provider 接口的场景

### Aspect 顺序：`@Order(100)`

`ClientCacheable` 是 HTTP 响应级缓存，必须**最外层**——任何内层 `@Cacheable` /
`@Transactional` 算完之后才能拿到最终的 result 计算 UID。顺序数字越小越外层（Spring AOP 约定），
100 给业务自定义 Aspect 留了较大的余量。

### `ClientCacheWebFilter` 的存在理由

Aspect 在 `@Around` 里需要拿到 `HttpServletResponse` 来 setHeader——但 Spring AOP 默认
只能拿到 `HttpServletRequest`。Filter 把请求包成 `CacheClientRequest`，把 response 引用
存到 wrapper 里，Aspect 就能反向取出。

## 模块入口

| 路径 | 角色 |
|---|---|
| `provider/init/InterServiceCacheProviderAutoConfiguration` | 装配入口：注册 properties / filter / UID generator / aspect |
| `provider/init/InterServiceCacheProviderProperties` | 配置项：`uid-cache-enabled` / `wrap-all-requests` |
| `provider/web/ClientCacheWebFilter` | 把 `HttpServletRequest` 包成 `CacheClientRequest` |
| `provider/web/CacheClientRequest` | `HttpServletRequestWrapper` 子类，额外携带 response 引用 |
| `aop/ClientCacheable` | 注解，标记需要走"双方缓存协商"的 Controller 方法 |
| `aop/ClientCacheableAspect` | `@Around` 实现：算 UID、写响应头、命中时返回 null |
| `aop/ClientCacheUidGenerator` | 响应 UID 生成器；默认每次按 JSON 计算，可选弱引用缓存 |

## 配置示例

provider 端应用只需引入本模块依赖；自动配置会自动注册 filter 与 aspect。

```kotlin
implementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-provider"))
```

Controller 用法：

```kotlin
@RestController
class UserController {
    @ClientCacheable
    @GetMapping("/user/{id}")
    fun getUser(@PathVariable id: String): UserDto { /* ... */ }
}
```

可选配置：

```yaml
kudos:
  ability:
    cache:
      interservice:
        provider:
          uid-cache-enabled: false   # 默认每次按响应 JSON 计算 UID；热点不可变对象可打开弱引用缓存
          wrap-all-requests: false   # 默认只包装带 cache-key/cache-uid 的跨服务缓存请求
```

## 测试覆盖

`InterServiceCacheTest` 在 provider test-src 启动一对 SpringApplication：
- `MockMsApplication`（profile `ms`）暴露 `/same` / `/different1` / `/different2` 三条 endpoint
- 主测试进程（profile `client`）持 Feign client `IMockProxy` 调用前者

| 用例 | 校验 |
|---|---|
| `same()` | 同一调用两次，返回 `===`（同对象）——验证 304 走的是本地缓存对象 |
| `different1()` | `/different1` **没标 @ClientCacheable**，两次返回 `!==`——验证不进缓存协商时按普通调用走 |
| `different2()` | `/different2` 加了 @ClientCacheable，但服务端每次返回随机内容（UID 不一致）——两次返回 `!==`，验证 UID 不匹配时按 200 重新 decode |

补充单测：
- `ClientCacheableAspectTest`：验证目标方法异常直接原样抛出，不再二次包装成 `RuntimeException`
- `ClientCacheWebFilterTest`：验证默认只包装带 `cache-key` / `cache-uid` 的请求，并保留
  `wrap-all-requests=true` 兼容模式

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-common"))
compileOnly(libs.spring.boot.starter.web)

testImplementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-interservice:kudos-ability-cache-interservice-client"))
testImplementation(project(":kudos-ability:kudos-ability-distributed:kudos-ability-distributed-client:kudos-ability-distributed-client-feign"))
testImplementation(project(":kudos-ability:kudos-ability-cache:kudos-ability-cache-local:kudos-ability-cache-local-caffeine"))
testImplementation(project(":kudos-test:kudos-test-container"))
testImplementation(libs.spring.boot.starter.web)
```

`spring-boot-starter-web` 是 `compileOnly`——provider 端必然是 Web 应用，业务侧已经
直接依赖，本模块不重复传递。

`kudos-tools` 的脚手架模板 `${project}-ams-${module}-api-provider/build.gradle.kts`
默认引用本模块——这是它出现在依赖图的现实理由。

## 已知限制 / 后续工作

- ✅ `ClientCacheableAspect` 已直接 `joinPoint.proceed()` 并 `throws Throwable`，目标方法异常
  原样上抛，不再包一层 `RuntimeException`
- ✅ 响应 UID 生成已收口到 `ClientCacheUidGenerator`。默认仍每次按 `<FQN>#<JSON>` 计算；
  热点且返回对象不可变时可设置 `kudos.ability.cache.interservice.provider.uid-cache-enabled=true`
  打开弱引用缓存，降低重复 JSON 序列化开销
- ✅ `ClientCacheWebFilter` 默认只在请求带 `cache-key` 或 `cache-uid` 时包装为
  `CacheClientRequest`；需要旧行为可设置
  `kudos.ability.cache.interservice.provider.wrap-all-requests=true`
