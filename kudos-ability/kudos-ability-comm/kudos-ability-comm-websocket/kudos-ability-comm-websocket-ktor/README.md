# kudos-ability-comm-websocket-ktor

业务层 WebSocket 抽象（Ktor 端）。在 `kudos-ability-web-ktor` 已经 `install(WebSockets)`
的基础上，封装：

1. **会话包装**（`KudosWebSocketSession`）：原生 `DefaultWebSocketServerSession` 加上
   `sessionId` / `userId` / `tenantId` / `attributes` 元数据
2. **进程级会话注册中心**（`KudosWebSocketRegistry`）：按 sessionId / userId / tenantId
   三套索引并发安全维护
3. **业务回调 SPI**（`IKudosWebSocketHandler`）：`onConnect` / `onText` / `onBinary` /
   `onDisconnect` 四个钩子，默认 no-op
4. **广播 / 单播工具**（`WebSocketBroadcaster`）：按用户 / 租户 / 全部 / sessionId
   分发，单 session 失败不影响其余
5. **消息编解码 SPI**（`IWebSocketMessageEncoder`）：业务对象 ↔ 文本 frame 抽象
6. **路由扩展**（`Route.kudosWebSocket`）：`register → loop → unregister` 的完整模板

## 与 `kudos-ability-web-ktor` 的边界

| 模块 | 职责 | 文件 |
|---|---|---|
| `kudos-ability-web-ktor` | 装上 Ktor `WebSockets` 插件（`pingPeriod` / `maxFrameSize` 等） | `KtorPlugins.installPlugins` |
| 本模块 | 业务层会话管理 / 广播 / 编解码契约 | 本目录 |

业务侧只用 web-ktor 也能写 `webSocket("/echo") { ... }` 收发原生 frame；引入本模块的收益
是不用每个服务都重写"按 user 维护 session 列表 / 广播给一组用户"等通用代码。

## 设计要点

### 注册中心**不**跨进程同步

```kotlin
class KudosWebSocketRegistry {
    private val byId = ConcurrentHashMap<String, KudosWebSocketSession>()
    private val byUserId = ConcurrentHashMap<String, MutableSet<String>>()
    private val byTenantId = ConcurrentHashMap<String, MutableSet<String>>()
    // ...
}
```

单实例部署够用。多实例部署的"全局广播"由 `distributed/` 子包提供（见下文"分布式广播"段）——
`DistributedWebSocketBroadcaster` 装饰 `WebSocketBroadcaster`，每次广播额外往
`IWebSocketBroadcastChannel` 上发一份；其它节点收到后用各自的进程级注册中心交付。

### `Route.kudosWebSocket` 的 `register → loop → unregister` 模板

```kotlin
webSocket(path) {
    val session = sessionFactory(this)
    registry.register(session)
    var cause: Throwable? = null
    try {
        handler.onConnect(session)
        for (frame in incoming) { /* 按 frame 类型派发 */ }
    } catch (t: Throwable) {
        cause = t
        log.warn(...)
    } finally {
        handler.onDisconnect(session, cause)
        registry.unregister(session.sessionId)
        close(CloseReason(CloseReason.Codes.NORMAL, ""))
    }
}
```

无论正常退出还是抛异常，都会进入 `finally` 解绑——避免注册中心被关闭已久的 session 充满。
`onDisconnect` 的 `cause` 参数告诉业务侧本次是否异常关闭。

### `sessionFactory` 让业务侧填鉴权信息

```kotlin
routing {
    kudosWebSocket("/chat", registry, ChatHandler()) { rawSession ->
        KudosWebSocketSession(
            raw = rawSession,
            userId = rawSession.call.principal<JWTPrincipal>()?.payload?.subject,
            tenantId = rawSession.call.request.headers["X-Tenant-Id"],
        )
    }
}
```

工厂函数在 `register` 之前调用，返回的 session 立刻被加入注册中心——按 userId / tenantId
广播因此可工作。**鉴权失败时**业务侧可以选择：(a) 用工厂函数返回 anonymous session 再
靠 `onConnect` 早关闭，(b) 在工厂函数外面提前 `call.respond(401)` 后 `return`
（但那样 webSocket DSL 已经握手了，需要立刻 close）。前者更易写。

### 广播失败语义

`WebSocketBroadcaster` 用 `coroutineScope { sessions.map { async { ... } }.awaitAll() }`——
所有 session 并发发送，单个失败 catch + WARN，返回成功条数。**不重试**——重试策略是
业务关注点，本模块只确保"一个慢 / 坏 session 不会拖累其他"。

## 模块入口

| 路径 | 角色 |
|---|---|
| `session/KudosWebSocketSession` | 业务层会话包装，带 sessionId / userId / tenantId / attributes |
| `session/KudosWebSocketRegistry` | 三套索引的进程级注册中心 |
| `handler/IKudosWebSocketHandler` | onConnect / onText / onBinary / onDisconnect SPI |
| `routing/KudosWebSocketRouting` | `Route.kudosWebSocket(path, registry, handler)` 路由扩展函数 |
| `broadcast/WebSocketBroadcaster` | 单播 / 按 user / 按 tenant / 全部广播（进程级） |
| `codec/IWebSocketMessageEncoder` | 业务对象 ↔ 文本 frame 编 / 解码 SPI |
| `distributed/DistributedWebSocketBroadcaster` | 装饰 `WebSocketBroadcaster` 同 API，外加跨进程投递 |
| `distributed/IWebSocketBroadcastChannel` | 跨节点投递通道 SPI（Redis / Kafka / 自定义） |
| `distributed/WebSocketBroadcastEnvelope` | 通道里跑的报文：`nodeId` / `targetType` / `targetId` / `text` |
| `distributed/redis/RedisWebSocketBroadcastChannel` | Spring Data Redis pub/sub 默认实现 |

## 分布式广播

多实例部署时，把进程级 `WebSocketBroadcaster` 用 `DistributedWebSocketBroadcaster` 装饰一层，
所有广播 API（`broadcast` / `broadcastToUser` / `broadcastToTenant` / `unicast`）签名不变，行为
变成"本节点本地广播 + 同时往 `IWebSocketBroadcastChannel` 发一份让别的节点也广播"。

### 自带 Redis 实现

```kotlin
val nodeId = UUID.randomUUID().toString()  // 进程身份，用来过滤 pub/sub 自回声
val channel = RedisWebSocketBroadcastChannel(
    redisTemplate = redisTemplates.defaultRedisTemplate,
    container = redisMessageListenerContainer,
    redisChannel = "kudos:ws:broadcast",
)
val broadcaster = DistributedWebSocketBroadcaster(
    local = WebSocketBroadcaster(registry),
    channel = channel,
    nodeId = nodeId,
)
```

业务侧用 `broadcaster.broadcastToUser(userId, text)` 一句，本节点 + 别的节点上同一个 `userId`
的 session 都会收到。`RedisWebSocketBroadcastChannel` 复用业务方提供的
`RedisMessageListenerContainer`（可与 `kudos-ability-cache-remote-redis` 共享同一个 container），
所以引一份 Spring Data Redis 即可。

### 自带语义

- **返回值仅代表本地**：`broadcast()` 返回本节点投递成功的 session 数，`unicast()` 返回本节点是否
  持有该 sessionId。远端是否投递成功不在返回值里——transport 大多无 ack
- **自回声过滤**：Redis pub/sub 会把发出的消息也推回发送方，`DistributedWebSocketBroadcaster`
  按 `envelope.nodeId == nodeId` 静默丢弃自回声，保证发起节点只本地广播一次
- **publish 失败不阻断本地**：`channel.publish` 抛异常会 WARN 后继续做本地广播，避免远端 Redis
  抖动时本节点的 session 也跟着收不到消息
- **handler 异常隔离**：单个 inbound 处理失败 ERROR 后继续，下一条消息照常处理（默认 `MessageListener`
  抛异常会终止整个 listener 线程）

### 换其它 transport

实现 `IWebSocketBroadcastChannel`（`publish` + `subscribe`）即可。Kafka topic / NATS subject /
内存桥接（见 `test-src/InMemoryBroadcastChannel.kt`）都按这个 SPI 走。

## 配置示例

业务侧 `Application` 装配：

```kotlin
val registry = KudosWebSocketRegistry()
val broadcaster = WebSocketBroadcaster(registry)
val handler = object : IKudosWebSocketHandler {
    override suspend fun onText(session: KudosWebSocketSession, text: String) {
        broadcaster.broadcastToTenant(session.tenantId ?: "default", text)
    }
}

routing {
    kudosWebSocket("/ws/chat", registry, handler) { raw ->
        KudosWebSocketSession(
            raw = raw,
            userId = raw.call.request.headers["X-User-Id"],
            tenantId = raw.call.request.headers["X-Tenant-Id"],
        )
    }
}
```

启用 Ktor WebSockets 插件（默认开）：

```yaml
kudos:
  ability:
    web:
      ktor:
        plugins:
          web-socket:
            enabled: true
```

## 测试覆盖

**当前无单元测试**——Ktor WebSocket 测试需要 `ktor-server-test-host`，本模块作为最小
可用抽象先发布。建议端到端测试在业务侧通过 `testApplication { ... }` 模式覆盖：
- 启用 `WebSockets` 插件
- 装上本模块的 `kudosWebSocket` 路由
- 用 `client.webSocket(...)` 客户端发送 frame，断言 registry / broadcaster 的行为

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.ktor.server.websockets)

// 可选：使用 distributed/redis 时业务侧引入；本模块 compileOnly，单实例部署不付出代价
compileOnly(libs.spring.boot.starter.data.redis)

testImplementation(project(":kudos-test:kudos-test-common"))
```

`kudos-ability-web-ktor` 装上的 `WebSockets` 插件由业务方的 `Application` 共享——本模块
**不**做插件装配，避免与 web-ktor 重复。

## 已知限制 / 后续工作

- ✅ 多实例全局广播：`distributed/` 子包提供（Redis 自带实现 + 任意 transport 的 SPI）
- ❗ 没有 backpressure 机制——单个慢 session 不会拖累并发广播（每个 send 在自己的 coroutine
  里），但**会无限堆积它自己的发送队列**直到 OOM。业务侧需要时可在 handler 里加 send
  超时 / 队列容量限制
- ❗ 编解码 SPI 是接口而没有默认实现——业务侧需要按 Jackson / kotlinx.serialization 等
  自己注入。等 kudos 主体确定默认 JSON 库后再补默认实现
- ❗ 没有路由级中间件（鉴权 / 限流 / 链路追踪）支持——只能在业务 handler 的 `onConnect`
  里手动做。未来抽出 `WebSocketInterceptor` 类似 Spring HandlerInterceptor 的 chain
- ❗ `unicast` 跨节点的返回值仍按本地语义：本节点没该 session 即返回 false，但远端可能投递成功；
  调用方若需要全局 ack 应改走 `broadcastToUser` + 业务层标记
- ❗ 在线人数 / 用户在线状态的全局查询（soul 的 `getWsOnlineAllUser` 等）还没移植——按需补
  `IWebSocketPresenceStore` 加 Redis 实现
- ❗ 缺路由层端到端单测；现有覆盖：`KudosWebSocketRegistryTest`、`KudosWebSocketRoutingTest`、
  新增 `DistributedWebSocketBroadcasterTest`（双节点内存桥接，覆盖 user/tenant/all/session 路径 +
  自回声过滤 + handler 异常隔离）
