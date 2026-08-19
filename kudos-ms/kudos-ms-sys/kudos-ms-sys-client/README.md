# kudos-ms-sys-client

## 定位

**其他微服务** 远程调用系统（`sys`）原子服务时的 **客户端模块**：仅依赖 **`kudos-ms-sys-common`**（及 interface client 能力模块），**不**依赖 `kudos-ms-sys-core`，避免将 ORM、缓存实现等打入调用方 classpath。

传输实现是 **Spring Interface Clients**（`@HttpExchange` + `RestClient`），不再是 OpenFeign。
迁移记录见文末「从 OpenFeign 迁移」。

每个 **`ISys*Proxy`** 继承 `common` 中对应的 **`ISys*Api`**，保证 **本地注入 `ISys*Api` 与远程调用** 在方法签名上完全一致，便于测试替换与契约统一。

---

## 一个 group，不是 19 个服务名

```kotlin
// 注册全部 19 个 proxy + 19 个 fallback，见 init/SysClientAutoConfiguration
@ImportHttpServices(group = "sys", types = [ISysDictProxy::class, /* ... */])
@HttpServiceFallback(value = SysDictFallback::class, service = [ISysDictProxy::class], group = "sys")
```

proxy 接口上**不再出现服务名**——目标由部署配置决定：

```yaml
spring:
  http:
    serviceclient:
      sys:                            # == SysClientAutoConfiguration.GROUP
        base-url: lb://kudos-ms-sys   # sys api-internal 在 Nacos 中的服务 id
        connect-timeout: 2s
        read-timeout: 5s
```

**这是本次迁移最实质的一处变化。** 原来 19 个 `@FeignClient(name = "sys-dict" / "sys-tenant" / "sys-out-line" …)` 从来就不是 19 个微服务——所有路径都是 `/api/internal/sys/...`，指向同一个 sys 原子服务。收成一个 group 之后：

- 旧 README 里「服务名命名风格不统一（`sys-out-line` 有连字符、`sys-accessruleip` 没有）」这个隐患消失了，因为服务名不再散落在 19 处；
- 旧 README 里「运维想『全局禁 sys』需逐个 name 设置」也随之解决——超时、重试、熔断都是一个 group 一套配置。

> ⚠️ **`base-url` 没有任何编译期或启动期检查**。不配的话 proxy 照样注册成功，直到第一次调用才会因 URL 无法解析而失败。请把它放进部署清单。

---

## 包结构

在 **`io.kudos.ms.sys.client.<业务模块>`** 下与 **common/core** 的模块名对齐；每个模块内再分 **`proxy`**（`ISys*Proxy`）与 **`fallback`**（`Sys*Fallback`）子包。少数模块（如 `dict`）额外提供 **`support`** 子包承载客户端侧的辅助逻辑。`init/` 下是统一的注册入口。

| Proxy | Fallback |
|-------|----------|
| `ISysTenantProxy` | `SysTenantFallback` |
| `ISysTenantSystemProxy` | `SysTenantSystemFallback` |
| `ISysTenantResourceProxy` | `SysTenantResourceFallback` |
| `ISysTenantLocaleProxy` | `SysTenantLocaleFallback` |
| `ISysSystemProxy` | `SysSystemFallback` |
| `ISysMicroServiceProxy` | `SysMicroServiceFallback` |
| `ISysSubSystemMicroServiceProxy` | `SysSubSystemMicroServiceFallback` |
| `ISysResourceProxy` | `SysResourceFallback` |
| `ISysDictProxy` | `SysDictFallback` |
| `ISysDictItemProxy` | `SysDictItemFallback` |
| `ISysParamProxy` | `SysParamFallback` |
| `ISysI18nProxy` | `SysI18nFallback` |
| `ISysDomainProxy` | `SysDomainFallback` |
| `ISysDataSourceProxy` | `SysDataSourceFallback` |
| `ISysCacheProxy` | `SysCacheFallback` |
| `ISysAccessRuleProxy` | `SysAccessRuleFallback` |
| `ISysAccessRuleIpProxy` | `SysAccessRuleIpFallback` |
| `ISysLocaleProxy` | `SysLocaleFallback` |
| `ISysOutLineProxy` | `SysOutLineFallback` |

所有 Fallback 直接继承 `io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport`。
降级语义随业务而定（多为返回空集合 / null），目的是在 sys 不可用时避免级联故障，而不是静默吞错。

### Fallback 的两条硬约束

1. **必须 `open`**：Spring Cloud 给 fallback 套 CGLIB 代理，Kotlin 类默认 final。19 个类本来就写了 `open`，迁移时未受影响，但新增时容易漏。
2. **不能加 `@Component`**：迁移时 19 个 fallback 的 `@Component` 全部去掉了。原因不是多余，是**会坏事**——fallback 实现了 `ISys*Proxy`，如果它同时是 Spring bean，`@Autowired ISysDictProxy` 就会在「HTTP 代理 bean」和「fallback bean」之间产生歧义。Feign 时代没暴露是因为 `@FeignClient` 注册的代理默认带 `@Primary`（`spring.cloud.openfeign.client.primary=true`），而 interface client 没有这层保护。

---

## Gradle 依赖

- **`kudos-ms-sys-common`**：契约与序列化类型。
- **`kudos-ability-distributed-client-http`**：interface client 集成（含 `AbstractHttpFallbackSupport` 基类）。

测试额外依赖 `kudos-test-common` / `kudos-test-container` / `spring-boot-starter-web`（契约测试要起真实 servlet 容器）。

---

## 字典码校验：双实现 + ServiceLoader

`common.base` 的 `@DictItemCode` 校验通过 `ServiceLoader` 寻找 **`IDictItemCodeFinder`** 实现：

| 实现 | 模块 | 说明 |
|------|------|------|
| `DictItemCodeFinder` | `kudos-ms-sys-core` | 本进程查 Hash 缓存（部署 sys 的服务） |
| `HttpDictItemCodeFinder` | 本模块 `dict/support/` | 通过 `ISysDictProxy` 远程取（**不部署 sys-core 的下游服务**） |

（原名 `FeignDictItemCodeFinder`，随传输实现一并改名；`resources/META-INF/services/...` 已同步。）

两者在同一 deployment 通常只命中一个：sys 提供方依赖 `core`、下游服务依赖 `client`，**不会同时上线**。该实现通过 `SpringKit.getBean` 在首次校验时 lazy 取 `ISysDictProxy`。

---

## 与其他子模块的关系

| 模块 | 关系 |
|------|------|
| **common** | Proxy 继承 `ISys*Api`，**HTTP 路径注解也在这里**（`@GetExchange` / `@PostExchange`），客户端契约与服务端 request mapping 是同一份声明 |
| **core** | 在 **服务端** 实现 `ISys*Api`；客户端 **不引用** core |
| **api-internal** | `Sys*InternalController` 实现 `ISys*Api`，路由由接口上的 `@HttpExchange` 家族注解提供（Spring MVC 7 支持 `@HttpExchange` 作为服务端映射） |

---

## 使用注意

1. **注册是自动的**：`SysClientAutoConfiguration` 实现 `IComponentInitializer`，调用方 `@EnableKudos` 即自动装配，不需要 `@EnableFeignClients` 这类扫描注解。
2. **必须配 `base-url`**：见上文。
3. **契约变更**：修改 `common.ISys*Api` 时四处必须同步：
   - `core` 中 `Sys*Api`（同进程实现）
   - `api-internal` 中 `Sys*InternalController`（HTTP 暴露）
   - 本模块 `Sys*Proxy`（自动继承，不用手动改）
   - 本模块 `Sys*Fallback`（需手动补 override）

   前三处由编译期强制对齐，**fallback 是唯一可能漏改的地方**。
4. **`Pair` 入参的批量端点**：直接调 `ISysDictProxy.batchGetActiveDictItems(...)`（Pair 版）不可行——Jackson 序列化 `Pair` 后远端无法反序列化。应改调 `SysDictInternalController.batchGetActiveDictItemsHttp`（`List<List<String>>` 版）。当前 client 模块未为该适配端点提供 proxy 方法。

---

## 从 OpenFeign 迁移（2026-08-19）

| 改动 | 位置 |
|---|---|
| 19 个 `@FeignClient(name=, fallback=)` → 一个 `@ImportHttpServices` + 19 个 `@HttpServiceFallback` | 本模块 `proxy/` → `init/SysClientAutoConfiguration` |
| 65 个 `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` → `@GetExchange` / … | **`kudos-ms-sys-common` 的 19 个 `ISys*Api`** |
| `AbstractFeignFallbackSupport` → `AbstractHttpFallbackSupport`；去掉 `@Component` | 本模块 19 个 `fallback/` |
| `FeignDictItemCodeFinder` → `HttpDictItemCodeFinder` | 本模块 `dict/support/` + `META-INF/services` |
| Kotlin `javaParameters = true` | 仓库根 `build.gradle.kts` |

**为什么必须动 `-common`**：`HttpServiceProxyFactory` 只认 `@HttpExchange` 家族，完全不认 Spring MVC 的
`@GetMapping`（`HttpServiceMethod` 的注解查找里只有 `HttpExchange` 一个）。而路径注解从一开始就写在
`-common` 的共享接口上，被服务端 controller 和客户端 proxy 同时继承——所以这一步不可能只改 client。
反过来说，这也意味着 **Feign 与 interface client 无法在同一个 `ISys*Api` 上共存**，sys 这条线是一次性
切换，不是并存。参数注解（`@RequestParam` / `@RequestBody` / `@PathVariable`）两边通用，未改动。

**关于 `javaParameters`**：契约里 87 个 `@RequestParam` 全部不带显式名字。
`AbstractNamedValueArgumentResolver` 靠 `MethodParameter.getParameterName()` 取名，而
`HttpServiceProxyFactory` 不装 `ParameterNameDiscoverer`，只能读反射元数据。不开这个标志，运行期报
`Name for argument of type [...] not specified, and parameter name information not available via reflection`。
Feign 靠 `SpringMvcContract` 自己解析注解，从不依赖这个，所以迁移前没暴露。

标志要开在**编译接口的那个模块**上——对 sys 而言是 `kudos-ms-sys-common`，而它**早就为 Jackson
局部开了**，所以本模块的迁移其实不依赖根 build 的这行（实测去掉后契约测试照样通过）。加到根 build
是为了后面三家：`kudos-ms-user` / `-auth` / `-msg` 的 common **都没有**这个设置，不加会在运行期炸。

---

## 测试覆盖

| 测试 | 覆盖 |
|---|---|
| `init/SysClientContractTest` | 起真实 servlet 容器 + mock controller（**不带任何自己的 mapping 注解**），证明共享契约上的 `@GetExchange`/`@PostExchange` 同时作为服务端路由与客户端契约生效；覆盖两个不具名 `@RequestParam` 的 GET、单参 GET 返回 `Set`、`@RequestParam` + `@RequestBody` 混合的 POST、返回原始类型的 GET |
| 19 个 `*FallbackTest` | 各 fallback 的降级返回值与日志分类 |
| `HttpDictItemCodeFinderTest` | lazy 取 bean、去重、空结果、Spring 未就绪时的报错 |

---

## 已知限制 / 后续工作

- ❗ **`base-url` 无启动期校验** — 见上文，漏配到第一次调用才发现
- ❗ **fallback 缺新方法时编译不报错** — 新增 `ISys*Api` 方法时 fallback 不会被强制 override
- ❗ **`Pair` 入参的批量端点 client 侧无代理**
- ❗ **`HttpDictItemCodeFinder` 通过 `SpringKit.getBean` 拿 Proxy** — 启动期校验场景下 Spring 上下文
  未就绪时会失败；当前依赖 lazy 触发的时机
- ❗ **19 个 `@HttpServiceFallback` 与 `types` 列表需手工保持同步** — 新增 proxy 时要在
  `SysClientAutoConfiguration` 两处各加一行，漏了不会报错（只是该 proxy 没有降级 / 没被注册）
