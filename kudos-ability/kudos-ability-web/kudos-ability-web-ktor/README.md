# kudos-ability-web-ktor

Ktor 服务端引擎接入 Spring Boot。给业务侧提供：

1. **可切换的内嵌引擎**：`KtorAutoConfiguration.ktorEngine` 按 yml 切 Netty / Jetty / Tomcat / CIO
2. **配置式插件装配**：`KtorProperties.plugins` 控制 ContentNegotiation / StatusPages /
   WebSockets 是否启用
3. **路由 SPI**：业务实现 `IKtorRouteRegistrar` 并注册为 Spring bean，启动时统一挂到 routing
4. **请求上下文绑定**：`KudosContextPlugin` 把 `KudosContext` 写入 `ApplicationCall.attributes`，
   并同时尝试装入协程上下文

## 设计要点

### 与 Spring Boot 的整合

`KtorAutoConfiguration` 是 `IComponentInitializer`，由 kudos 自定义的 SPI（不是 SB 默认
auto-config SPI）扫描装配。流程：

```
Spring 容器启动
 → KtorAutoConfiguration.ktorProperties()           # 读 yml
 → KtorAutoConfiguration.ktorEngine(properties)     # 反射加载引擎工厂的 INSTANCE
    → KtorContext.application = this
    → installPlugins(properties)                   # 按配置 install ContentNegotiation / StatusPages / WebSockets
    → routing { routeRegistrar.forEach(register) } # 把所有 IKtorRouteRegistrar bean 的路由挂上去
    → monitor.subscribe(ApplicationStarted) { complete }
    → server.start(wait = false)
    → started.join()                               # 同步等待引擎真正就绪再让 Spring 继续
 → @PreDestroy shutDownKtor()                       # Spring 关闭时优雅停 ktor 引擎
```

引擎工厂用 `Class.forName(...).getField("INSTANCE")` 反射加载，避免编译期把 4 套引擎都拉到
classpath。生产 build 只需引依赖里**对应那一种引擎**的 starter；如果配置和实际依赖不匹配，
启动时会在 `Class.forName` 抛 `ClassNotFoundException`。

### StatusPages 异常处理

`installPlugins` 注册的 StatusPages 处理 `IllegalStateException` 和兜底 `Throwable`：
**栈完整记录到日志，对外只回 `500 Internal server error`**。这是从更早版本"直接把
`cause.message` 拼到响应"的修正——避免内部细节（DB 表名、堆栈片段）泄漏给调用方。

### 配置示例

```yaml
kudos:
  ability:
    web:
      ktor:
        engine:
          name: netty      # cio | netty | jetty | tomcat | test
          port: 8080       # 0 = 随机（仅测试用）
        plugins:
          contentNegotiation:
            enabled: true
          statusPages:
            enabled: true
          webSocket:
            enabled: false # 默认开；不需要的服务可关掉减少冷启动开销
```

`engine.name = test` 时不真正启动引擎——`ktorEngine` bean 返回 null，由测试用例自行用
`testApplication { ... }` 装配虚拟引擎。

### `IKtorRouteRegistrar` SPI

```kotlin
@Component
class UserRouteRegistrar : IKtorRouteRegistrar {
    override fun register(routing: Routing) {
        routing.route("/users") {
            get("/{id}") { call.respondText("...") }
            post { /* ... */ }
        }
    }
}
```

所有 Spring 容器里的 `IKtorRouteRegistrar` bean 在 `routing { ... }` 块里被依次调用。

### `KudosContextPlugin`

设计意图：在 servlet 版的 `WebContextInitFilter` 里用 ThreadLocal 装 `KudosContext`；
Ktor 是协程化的，请求上下文优先绑定到 `ApplicationCall.attributes`，路由 handler 里通过
`call.kudosContext()` / `call.kudosContextOrNull()` 读取：

```kotlin
pipeline.intercept(ApplicationCallPipeline.Setup) {
    val ctx = factory(call)
    call.attributes.put(KudosContextCallKey, ctx)
    withContext(KudosContextElement(ctx)) { proceed() }
}
```

`KudosContextElement` 仍会被装入当前 pipeline 协程，但 Ktor routing 子管线并不保证沿用
Setup 阶段的 coroutine context；因此业务路由里应优先使用 `call.kudosContext()`。

## 模块入口

| 路径 | 角色 |
|---|---|
| `init/KtorAutoConfiguration` | Spring 装配入口 + 引擎生命周期 |
| `init/KtorProperties` | 引擎 + 插件开关的配置类 |
| `init/KtorPlugins` | `Application.installPlugins(...)` 扩展，按配置 install Ktor 插件 |
| `core/KtorContext` | 全局 `application` / `properties` 引用（lateinit object，供 `installPlugins` 和关闭钩子使用） |
| `core/IKtorRouteRegistrar` | 业务路由注册 SPI |
| `plugins/KudosContextPlugin` | `KudosContext` 请求属性绑定 + 协程元素装配 |

## 测试覆盖

- `CioEngineTest` / `NettyEngineTest` / `JettyEngineTest` / `TomcatEngineTest` /
  `VirtualEngineTest` —— 5 个引擎的启动 + hello-world（Jetty 在当前 CI 环境被 skip）
- `KtorSpringTest` —— Ktor + Spring 协同：`testApplication` 内用 `SpringKit` 取 bean
- `KtorWithoutSpringTest` —— Ktor 独立运行（无 Spring 容器）
- `RouteRegistrarTest` —— `IKtorRouteRegistrar` SPI 端到端（含 JDK HttpClient + Ktor HttpClient
  两种调用方式）
- `KudosContextPluginTest` —— 插件 install 不抛错、factory 被调用、handler 可通过
  `call.kudosContext()` 读取 traceKey

## 已知限制 / 后续工作

- ✅ `KudosContextPlugin` 已改为把上下文写入 `ApplicationCall.attributes`，并提供
  `call.kudosContext()` / `call.kudosContextOrNull()`；路由 handler 可稳定读取。协程
  `KudosContextElement` 仍保留，但 routing 子管线不保证继承 Setup 阶段的 coroutine context，
  业务路由不要依赖直接从 `coroutineContext[KudosContextElement]` 读取
- ❗ `KtorAutoConfiguration.ktorEngine` 返回可为 null（`engine.name = test` 时）—— Spring
  bean 类型 `EmbeddedServer<*, *>?`，依赖此 bean 的代码需自行容忍 null
- ❗ `KtorContext.application` 是 lateinit `object` 属性，多次启动 / 重启场景下不重置；
  本模块假设引擎在 JVM 生命周期内仅启动一次
- ❗ 没有 CSRF / CORS 插件（build.gradle 里有注释但未启用）。需要时业务侧手动 `install(CORS)`
- ❗ 没有全局响应包装（不同于 springmvc 模块的 `GlobalResponseBodyHandler`）；业务侧手动用
  `call.respond(ApiResponse.success(...))`
- ❗ 没有 CRUD Controller 基类等价物；ktor 路由偏函数式风格，业务侧自组装
- ❗ 引擎切换错误（yml 配 netty 但 classpath 没 Netty starter）抛 `ClassNotFoundException`
  时机较晚（bean 初始化），不会在配置校验阶段提前发现

## 依赖

```kotlin
plugins {
    alias(libs.plugins.ktor)   // 自动带入 ktor BOM
}

dependencies {
    api(project(":kudos-context"))

    api(libs.ktor.server.core)
    api(libs.ktor.server.config.yaml)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.server.status.pages)
    api(libs.ktor.server.websockets)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.jetty.jakarta)
    testImplementation(libs.ktor.server.tomcat.jakarta)
    testImplementation(libs.ktor.server.cio)

    testImplementation(project(":kudos-test:kudos-test-common"))
}
```

四个引擎仅 testImplementation，生产 build 需自行选一个 starter 加到 implementation/api。
