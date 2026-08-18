# kudos-ability-comm-websocket-ktor

业务层 WebSocket 抽象的 **Ktor 实现**。

引擎无关的部分——会话注册中心、广播器、`IKudosWebSocketHandler` SPI、连接准入、编解码契约、
`KudosContext` 桥接、跨进程投递、Spring 自动装配——全部来自
[`kudos-ability-comm-websocket-common`](../kudos-ability-comm-websocket-common/README.md)，与
`-spring` 完全共用。本模块只做 Ktor 独有的三件事：

1. **会话适配**（`KudosWebSocketSession`）：把 `DefaultWebSocketServerSession` 适配成 `KudosWebSocketSessionRef`
2. **关闭原因映射**（`WebSocketCloseReasons`）：`WebSocketCloseReason` → `io.ktor.websocket.CloseReason`
3. **路由扩展**（`Route.kudosWebSocket`）：`识别 → 准入 → 注册 → 循环 → 注销` 的完整模板，外加**分片重组**

因为业务 SPI 是同一套，同一个 `IKudosWebSocketHandler` 实现可以不加改动地在 Ktor 与 Spring 两侧运行。

## 与 `kudos-ability-web-ktor` 的边界

| 模块 | 职责 | 文件 |
|---|---|---|
| `kudos-ability-web-ktor` | 装上 Ktor `WebSockets` 插件（`pingPeriod` / `maxFrameSize` 等） | `KtorPlugins.installPlugins` |
| 本模块 | 业务层路由模板与会话适配 | 本目录 |
| `-common` | 注册中心 / 广播 / SPI / 准入 / 装配 | 上级目录 |

业务侧只用 web-ktor 也能写 `webSocket("/echo") { ... }` 收发原生 frame；引入本模块的收益是不用每个
服务都重写"按 user 维护 session 列表 / 广播给一组用户 / 分片重组"等通用代码。

## 快速上手

```kotlin
@Component
class ChatRouteRegistrar(
    private val registry: KudosWebSocketRegistry,
    private val broadcaster: IWebSocketBroadcaster,   // 注意是接口
) : IKtorRouteRegistrar {
    override fun register(route: Route) {
        route.kudosWebSocket("/ws/chat", registry, ChatHandler(broadcaster)) { raw ->
            val userId = raw.call.request.headers["X-User-Id"] ?: return@kudosWebSocket null
            KudosWebSocketSession(raw, userId = userId, tenantId = raw.call.request.headers["X-Tenant-Id"])
        }
    }
}
```

`registry` 与 `broadcaster` 由 common 模块的 `WebSocketAutoConfiguration` 提供，直接注入即可。

## 设计要点

### `Route.kudosWebSocket` 的模板

```kotlin
webSocket(path) {
    val session = sessionFactory(this) ?: run { close(rejectCloseReason.toKtor()); return@webSocket }
    connectInterceptors.admit(session, path)?.let { close(it.reason.toKtor()); return@webSocket }
    registry.register(session)
    var cause: Throwable? = null
    try {
        handler.onConnect(session)
        dispatchFrames(incoming, session, handler, maxMessageSize)
    } catch (t: Throwable) {
        cause = t
    } finally {
        withContext(NonCancellable) {
            handler.onDisconnect(session, cause)   // runCatching 包裹
            registry.unregister(session)
            close(WebSocketCloseReason.NORMAL.toKtor())
        }
    }
    if (cause is CancellationException) throw cause
}
```

三个细节：

- **`sessionFactory` 是识别位点**：它是 `suspend` 的（可以查库 / 查 Redis 验 token），返回 `null`
  即拒绝连接。这一点很重要——工厂返回后紧接着准入、随即 `register`，若采用"先放行、`onConnect` 里再关"
  的写法，那段窗口里连接已经是它自称的 `tenantId` 的成员，会收到该租户的广播。
- **清理在 `NonCancellable` 里**：优雅停机会取消路由协程，而 `onDisconnect` 是 suspend 的——不加这层
  保护，它会在第一个挂起点直接抛 `CancellationException`，业务清理（清在线标记、落审计）在整个集群
  重启时全部静默失效，恰好是最需要它生效的时刻。
- **取消会重新抛出**：清理完再抛，外层结构化并发才能看到"被取消"而不是"正常返回"。

准入逻辑本身（按序执行、首个拒绝生效、抛异常按拒绝处理）在 common 的
`List<IWebSocketConnectInterceptor>.admit(...)`，与 Spring 侧共用同一份实现。

握手前的鉴权、Origin 校验不在本模块——`kudosWebSocket` 就是普通 `Route` 扩展，天然可被 Ktor 的
`Authentication` 插件包住：`authenticate("jwt") { kudosWebSocket(...) }`（需引入 `ktor-server-auth`）。
协议层是 Ktor 插件体系的职责，本模块不重造。

### 分片帧由本模块重组

**这是 Ktor 与 Spring 最实质的差异**：Servlet 容器会替你重组，Ktor 不会。

Ktor 的 `WebSocketReader` 把每个分片都当作独立 `Frame` 发出（只有最后一片 `fin = true`），并且
`FrameParser` 会把 continuation 帧的 opcode 还原成原始消息的类型。所以
`for (frame in incoming) { if (frame is Frame.Text) handler.onText(frame.readText()) }` 这种朴素写法
会把半条消息当整条派发；更糟的是跨分片边界的多字节 UTF-8 字符会被解成替换字符。

`dispatchFrames` 因此自己缓冲重组，并且：

- Ping / Pong 允许合法地插在分片中间，跳过时不动缓冲区；
- 单帧和重组后的消息都受 `maxMessageSize`（默认 10 MB）约束，超限直接以 `TOO_BIG` 关闭连接——
  web-ktor 把 `maxFrameSize` 设成了 `Long.MAX_VALUE`，这里是唯一的兜底。

`IKudosWebSocketHandler` 的钩子因此是**每条应用消息一次**，不是每个 wire frame 一次。

### 关闭原因的映射只在一处

`WebSocketCloseReasons.kt` 里的 `toKtor()` 是本模块唯一把
`WebSocketCloseReason` 和 Ktor 类型对上的地方。收敛在一处，是 common 模块得以不依赖
`ktor-websockets` 的前提——那正是拆分前的实际状况，也是当时"换引擎只需一份 SessionRef 实现"这句话
兑现不了的原因。

## 模块入口

| 路径 | 角色 |
|---|---|
| `session/KudosWebSocketSession` | `KudosWebSocketSessionRef` 的 Ktor 实现，额外持有 `raw` |
| `session/WebSocketCloseReasons` | `WebSocketCloseReason` → `io.ktor.websocket.CloseReason` |
| `routing/KudosWebSocketRouting` | `Route.kudosWebSocket(...)` 路由扩展 + `dispatchFrames` 分片重组 |

其余全部在 [common](../kudos-ability-comm-websocket-common/README.md#模块入口)。

## 配置

本模块自身无配置项。引擎无关的配置在 `kudos.ability.comm.websocket.*`（node-id / broadcast /
distributed），见 [common 的 README](../kudos-ability-comm-websocket-common/README.md#自动装配与配置)；
协议级参数（`pingPeriod`、`maxFrameSize`）属于 `kudos-ability-web-ktor`。

## 测试覆盖

共 31 个用例：

| 测试类 | 用例 | 覆盖重点 |
|---|---|---|
| `KudosWebSocketRoutingTest` | 13 | `testApplication` 端到端：收发回环、元数据传递、鉴权拒绝、准入链顺序与失败关闭、生命周期顺序、取消下的清理完成 |
| `DispatchFramesTest` | 12 | 分片重组（含跨边界多字节 UTF-8）、控制帧穿插、Close 中断、超限关闭 |
| `KudosWebSocketSessionTest` | 6 | 会话发送语义、关闭原因到 Ktor `CloseReason` 的映射 |

注册中心、广播、准入、装饰器、`KudosContext`、分布式的 117 个用例在 common 模块。

### 测试约定：`runBlocking` 的测试必须显式写 `: Unit`

本模块 31 个用例里有 16 个是 `fun xxx(): Unit = runBlocking { ... }` 的形式（`dispatchFrames` 与会话
收发天然都是挂起函数）。那个 `: Unit` 不是可省的样板：

```kotlin
@Test
fun sendText_emitsTextFrame() = runBlocking {   // ❌ 块的最后一个表达式若不是 Unit……
    assertIs<Frame.Text>(raw.sent.single())     // ……assertIs 有返回值，方法就带上了返回值
}
```

JUnit 5 遇到有返回值的 `@Test` 方法**不会报错，而是静默跳过整个方法**：测试报告里连条目都不出现，
用例数悄悄少一个，只在构建日志里留一行很容易被忽略的：

```
[WARNING] @Test method '...sendText_emitsTextFrame()' must not return a value. It will not be executed.
```

踩中的门槛很低——`assertNotNull` / `assertIs` / `assertContentEquals` 都有返回值，随手放在最后一行就中招。
本仓库真的因此有过两个从未被执行的测试，是排查另一个问题时从日志里偶然发现的。

写成 `fun sendText_emitsTextFrame(): Unit = runBlocking { ... }` 后，编译器强制丢弃块的值，这个问题从此
不可能发生。新增此类测试时照抄这个签名即可。自查：

```bash
./gradlew test 2>&1 | grep "must not return a value"
```

## 依赖

```kotlin
plugins { alias(libs.plugins.ktor) }   // Ktor BOM

api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-websocket:kudos-ability-comm-websocket-common"))
api(libs.ktor.server.websockets)

testImplementation(project(":kudos-test:kudos-test-common"))
testImplementation(libs.ktor.server.test.host)
```

`kudos-ability-web-ktor` 装上的 `WebSockets` 插件由业务方的 `Application` 共享——本模块**不**做插件装配。

## 已知限制

引擎无关的限制（编解码无默认实现、`KudosContext` 桥接是局部方案、`unicast` 的跨节点语义、无在线状态
查询、无集群踢人、无可观测性埋点）见 [common 的 README](../kudos-ability-comm-websocket-common/README.md#已知限制)。

Ktor 特有的：

- ❗ 每 session 仍无独立发送队列上限：common 的 `sendTimeout` + `closeOnSendTimeout` 已经能回收不读
  socket 的客户端，但严格的 per-session outbox 容量控制（超限即丢旧帧）还没有。Spring 侧因为有
  `ConcurrentWebSocketSessionDecorator` 反而具备了字节级上限，这一侧还缺对等物
