# kudos-ability-comm-websocket-ktor

业务层 WebSocket 抽象（Ktor 端）。在 `kudos-ability-web-ktor` 已经 `install(WebSockets)`
的基础上，封装：

1. **会话包装**（`KudosWebSocketSession`）：原生 `DefaultWebSocketServerSession` 加上
   `sessionId` / `userId` / `tenantId` / `attributes` 元数据
2. **进程级会话注册中心**（`KudosWebSocketRegistry`）：按 sessionId / userId / tenantId
   三套索引并发安全维护
3. **业务回调 SPI**（`IKudosWebSocketHandler`）：`onConnect` / `onText` / `onBinary` /
   `onDisconnect` 四个钩子，默认 no-op
4. **广播 / 单播契约**（`IWebSocketBroadcaster`）：按用户 / 租户 / 全部 / sessionId
   分发，文本与二进制对等，单 session 失败不影响其余
5. **消息编解码 SPI**（`IWebSocketMessageEncoder`）+ `TypedWebSocketHandler` 消费端
6. **路由扩展**（`Route.kudosWebSocket`）：`鉴权 → register → loop → unregister` 的完整模板
7. **自动装配**（`WebSocketAutoConfiguration`）：注册中心 / 广播器交给 Spring，
   单机与集群只差一个配置开关

## 与 `kudos-ability-web-ktor` 的边界

| 模块 | 职责 | 文件 |
|---|---|---|
| `kudos-ability-web-ktor` | 装上 Ktor `WebSockets` 插件（`pingPeriod` / `maxFrameSize` 等） | `KtorPlugins.installPlugins` |
| 本模块 | 业务层会话管理 / 广播 / 编解码契约 | 本目录 |

业务侧只用 web-ktor 也能写 `webSocket("/echo") { ... }` 收发原生 frame；引入本模块的收益
是不用每个服务都重写"按 user 维护 session 列表 / 广播给一组用户"等通用代码。

## 设计要点

### 注册中心的索引更新走 `compute`，且**不**跨进程同步

三套索引（`byId` / `byUserId` / `byTenantId`）都是 `ConcurrentHashMap`，二级桶的每次增删都在
`compute` / `computeIfPresent` 的 lambda 内完成：

```kotlin
private fun attach(index: ConcurrentHashMap<String, MutableSet<String>>, key: String, sessionId: String) {
    index.compute(key) { _, ids -> (ids ?: newConcurrentSet()).apply { add(sessionId) } }
}
```

这不是风格问题。`ConcurrentHashMap` 只保证同一 key 的 compute 互斥，如果像
`computeIfAbsent(uid) { ... }.add(id)` 那样在锁外改集合，另一个线程的 `unregister` 可能正好把空桶
从 map 里摘掉——新 session 就被加进了一个已经游离的集合，此后 `findByUserId` 永远看不到它，
**连接是活的但广播不到**。重连场景（旧连接注销 + 新连接注册）天然会触发这个交错。
`KudosWebSocketRegistryTest.concurrentRegisterAndUnregister_neverLosesALiveSessionFromTheUserIndex`
就是这条不变式的回归测试。

重复 sessionId 注册时，被顶替 session 的 userId / tenantId 桶会被清理，避免"旧用户的索引解析到新
用户的连接"这种串号。`unregister(session)` 重载只在该实例仍是当前注册者时才摘除，防止迟到的
`finally` 把继任连接踢掉。

跨进程的"全局广播"由 `distributed/` 子包提供（见下文）。

### `Route.kudosWebSocket` 的模板

```kotlin
webSocket(path) {
    val session = sessionFactory(this) ?: run { close(rejectCloseReason); return@webSocket }
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
            close(CloseReason(NORMAL, ""))
        }
    }
    if (cause is CancellationException) throw cause
}
```

三个细节：

- **`sessionFactory` 是鉴权位点**：它是 `suspend` 的（可以查库 / 查 Redis 验 token），返回 `null`
  即拒绝连接。这一点很重要——工厂返回后立刻 `register`，若采用"先放行、`onConnect` 里再关"的写法，
  那段窗口里连接已经是它自称的 `tenantId` 的成员，会收到该租户的广播。
- **清理在 `NonCancellable` 里**：优雅停机会取消路由协程，而 `onDisconnect` 是 suspend 的——不加这层
  保护，它会在第一个挂起点直接抛 `CancellationException`，业务清理（清在线标记、落审计）在整个集群
  重启时全部静默失效，恰好是最需要它生效的时刻。
- **取消会重新抛出**：清理完再抛，外层结构化并发才能看到"被取消"而不是"正常返回"。

### 分片帧由本模块重组

Ktor **不**做跨帧重组：`WebSocketReader` 把每个分片都当作独立 `Frame` 发出（只有最后一片
`fin = true`），并且 `FrameParser` 会把 continuation 帧的 opcode 还原成原始消息的类型。所以
`for (frame in incoming) { if (frame is Frame.Text) handler.onText(frame.readText()) }` 这种朴素写法
会把半条消息当整条派发；更糟的是跨分片边界的多字节 UTF-8 字符会被解成替换字符。

`dispatchFrames` 因此自己缓冲重组，并且：

- Ping / Pong 允许合法地插在分片中间，跳过时不动缓冲区；
- 单帧和重组后的消息都受 `maxMessageSize`（默认 10 MB）约束，超限直接以 `TOO_BIG` 关闭连接——
  web-ktor 把 `maxFrameSize` 设成了 `Long.MAX_VALUE`，这里是唯一的兜底。

`IKudosWebSocketHandler` 的钩子因此是**每条应用消息一次**，不是每个 wire frame 一次。

### 广播的失败语义与背压

`WebSocketBroadcaster` 并发地给每个 session 发送，单个失败 catch + WARN，返回成功条数，**不重试**
（重试是业务关注点）。在此之上有两个限流开关：

- `maxConcurrentSends`（默认 512）——限制同时在飞的发送数。否则给 10 万 session 广播会一次性起
  10 万协程、堆 10 万个待发帧。
- `sendTimeout`（默认 10s）——限制单个 session 能拖多久。**不读 socket 的客户端不会让 send 失败**，
  它只会让帧在 Ktor 的 outgoing 队列里越堆越多直到 OOM，同时一直占着并发额度。

超时后 `closeOnSendTimeout`（默认 **true**）决定是否关掉该连接。默认关闭是因为：只超时不关闭，已经
排队的帧仍然留在内存里——那等于检测到了泄漏却不处理它。超时本身对协议是安全的：Ktor 的 `send`
在帧被 outgoing channel 接受前挂起，此时取消意味着帧根本没被序列化，不会写出半个帧。

### 三层拦截，各归各位

"路由级中间件"不是一个问题，是三个，各自的正确落点不同：

| 关注点 | 落点 | 理由 |
|---|---|---|
| 握手前鉴权、Origin 校验 | **Ktor 自带** `authenticate("jwt") { kudosWebSocket(...) }` | `kudosWebSocket` 就是普通 `Route` 扩展，天然可被 Ktor 的 `Authentication` 插件包住（需引入 `ktor-server-auth`）。协议层是 Ktor 插件体系的职责，本模块不重造 |
| 连接准入（配额、封禁、设备数上限） | `IWebSocketConnectInterceptor` | 在 `sessionFactory` 之后、注册之前——身份已知但广播还够不着的唯一窗口 |
| 每条消息（限流、埋点、链路） | `WebSocketHandlerDecorator` | 装饰 `IKudosWebSocketHandler` 即可，无需新 SPI |

#### 为什么消息侧没有单独的拦截器接口

因为不需要。一个包装另一个 handler 的 `IKudosWebSocketHandler` **就是**拦截器，而且比
Spring `HandlerInterceptor` 的 `preHandle`/`postHandle` 更强——装饰器能在同一个栈帧里跨越
delegate 前后，所以计时、`try/finally`、开 tracing span、给 delegate 套一层协程上下文都能写；
不调用 `super` 就是短路。再定义一个 `IWebSocketMessageInterceptor` 只会得到一个能力更弱的
重复抽象。

`WebSocketHandlerDecorator` 只是免去"关心一个钩子却要手写四个委托"的样板，`wrappedBy` 让链条
按执行顺序书写而不是由内向外嵌套：

```kotlin
val handler = chatHandler.wrappedBy(
    { next -> TracingHandler(next) },                              // 最外层
    { next -> RateLimitingWebSocketHandler(next, permitsPerSecond = 10.0) },
)
```

Spring 场景下用 `ObjectProvider<...>.orderedStream()` 取工厂列表即可——它本身就按 `@Order`
排序，本模块无需自带排序逻辑。

自带 `RateLimitingWebSocketHandler`（每连接令牌桶，桶存在 `session.attributes` 里，随连接
自动消亡；默认丢弃超限消息而非断连，覆写 `onLimitExceeded` 可改为关闭或回错误帧）。

#### 连接准入为什么要单独一层

`sessionFactory` 返回 `null` 解决的是"认不出你是谁"，而配额、封禁、设备数上限是"认出来了但
不放行"。把两者揉进同一个 lambda，多个独立策略就只能串成一段越写越长的代码，顺序和短路全靠
约定。`IWebSocketConnectInterceptor` 是一个可组合的列表，按序执行、首个拒绝生效、**抛异常按
拒绝处理**（准入检查失败不能被读成"有配额"）。

```kotlin
kudosWebSocket(
    "/ws/chat", registry, handler,
    connectInterceptors = listOf(MaxConnectionsInterceptor(registry, maxPerUser = 8)),
) { raw -> ... }
```

自带 `MaxConnectionsInterceptor`（按 user / tenant / 本节点限制并发连接数）。注意它是
**尽力而为**：计数发生在注册之前，并发冲进来的 N 个连接可能都读到"未超限"，最多超出 N-1 个。
要做成精确上限就得在连接路径上跨"准入+注册"加锁，代价不划算——硬上限属于网关层，这一层是为了
拦住失控客户端开几千个 socket。

### KudosContext 与会话打通

`KudosContextWebSocketHandler` 装饰器把 session 的 `tenantId` 建立成一个 `KudosContext`，
覆盖全部四个钩子（含 `onDisconnect`——业务清理往往也要访问数据库）。没有它，session 上明明
有身份信息，下游却看不见：kudos 的租户缓存 key、ktorm 的 `currentDatabase()` 路由、审计字段
填充读的都是 `KudosContextHolder`，而 WebSocket 路径从没往里写过东西。

```kotlin
val handler = chatHandler.wrappedBy({ next -> KudosContextWebSocketHandler(next) })
```

#### 为什么不是 `set` + `finally { clear() }`

因为 handler 是协程，可能在挂起后**换一个线程**恢复。在原线程 `set` 的 ThreadLocal 在新线程上
根本不存在；而且它也无法清理自己途经的那些线程，正好制造出
`kudos-ability-cache/README.md` 记录过的池线程污染。

所以桥接走 `KudosContextThreadElement`（`kotlinx.coroutines.ThreadContextElement`）：协程每次在
某个线程上恢复时由运行时调用 `updateThreadContext`、离开时调用 `restoreThreadContext`，
ThreadLocal 因此跟着**协程**走而不是跟着线程走，并且原有绑定会被原样还原。
`KudosContextWebSocketHandlerTest` 里有一个用例专门跑到 `Dispatchers.Default` 的 worker 上读
上下文——手写 set/clear 会在那里失败。

同时也挂上 `KudosContextElement`，让 `currentKudosContext()` 一并可用；只挂一边等于对半个代码库
静默失效。

#### 为什么放在本模块而不是改 `kudos-context`

把 `KudosContextElement` 本身改成 `ThreadContextElement` 会全进程生效，包括那 27 处
`KudosContextHolder.get()`——它是"读时创建并写入"的语义，于是在协程内部懒创建出来的上下文会在
`restoreThreadContext` 时被静默丢弃。`XKudosContextHolder` 把解析好的 `DataSource` 缓存进上下文
就是这样一处，而它支撑着所有 ktorm DAO 的 `currentDatabase()`，退化后果是**不报错、只变慢**。
另外 `kudos-context/README.md` 明文记录过"库本身不做自动桥接、责任在入口插件"的决定。

限定在 WebSocket 消息分发上，收益拿到手而爆炸半径只有一个模块。验证充分后再考虑提升为
`kudos-context` 的官方桥接。

**注意**：`KudosContext.user` 默认留空——它是 `IIdEntity<String>`，而 session 只有一个 userId
字符串，凭空造一个半成品实体会让下游拿到比 null 更糟的东西。需要时传入自己的 `contextFactory`
去加载真实用户。

### 编解码 SPI 的消费端

`IWebSocketMessageEncoder` 只是契约，本模块不带默认实现（模块本身不依赖任何 JSON 库）。入站方向由
`TypedWebSocketHandler<T>` 消费：

```kotlin
val handler = typedWebSocketHandler<ChatMessage>(encoder) { session, message ->
    broadcaster.broadcastToTenant(session.tenantId ?: "default", encoder.encode(message.reply()))
}
```

解码失败默认走 `onDecodeFailure`（WARN 后丢弃，不断连接）——一个客户端发了坏消息是客户端的 bug，
不该让一条可能还在正常收发的连接陪葬。出站方向不需要脚手架：`encoder.encode(msg)` 直接交给任意
广播方法即可。

## 模块入口

| 路径 | 角色 |
|---|---|
| `session/KudosWebSocketSessionRef` | 会话抽象（registry / broadcaster / handler 都面向它，不依赖 Ktor 类型） |
| `session/KudosWebSocketSession` | Ktor 实现，额外持有 `raw` |
| `session/WebSocketPayload` | 文本 / 二进制载荷的密封类型 |
| `session/KudosWebSocketRegistry` | 三套索引的进程级注册中心 |
| `handler/IKudosWebSocketHandler` | onConnect / onText / onBinary / onDisconnect SPI |
| `handler/TypedWebSocketHandler` | 自动解码入站消息为业务类型的 handler 基类 |
| `handler/WebSocketHandlerDecorator` | 消息级拦截基类 + `wrappedBy` 链装配 |
| `handler/RateLimitingWebSocketHandler` | 每连接令牌桶限流装饰器 |
| `context/KudosContextThreadElement` | 让 `KudosContextHolder` 的 ThreadLocal 跟随协程而非线程 |
| `context/KudosContextWebSocketHandler` | 每次钩子调用建立 `KudosContext` 的装饰器 |
| `routing/KudosWebSocketRouting` | `Route.kudosWebSocket(...)` 路由扩展函数 + 分片重组 |
| `routing/IWebSocketConnectInterceptor` | 连接准入 SPI（`Proceed` / `Reject`），注册前执行 |
| `routing/MaxConnectionsInterceptor` | 按 user / tenant / 节点限制并发连接数 |
| `broadcast/IWebSocketBroadcaster` | 广播契约（本地与分布式实现共用） |
| `broadcast/WebSocketBroadcaster` | 进程级实现，带并发上限与发送超时 |
| `codec/IWebSocketMessageEncoder` | 业务对象 ↔ 文本 frame 编 / 解码 SPI |
| `distributed/DistributedWebSocketBroadcaster` | 装饰本地广播器，外加跨进程投递 |
| `distributed/IWebSocketBroadcastChannel` | 跨节点投递通道 SPI（Redis / Kafka / 自定义） |
| `distributed/WebSocketBroadcastEnvelope` | 通道报文：`nodeId` / `targetType` / `targetId` / `text` 或 `binary` / `version` |
| `distributed/redis/RedisWebSocketBroadcastChannel` | Spring Data Redis pub/sub 默认实现 |
| `init/WebSocketAutoConfiguration` | 注册中心 / 广播器 / Redis 通道的 Spring 装配 |
| `init/WebSocketProperties` | `kudos.ability.comm.websocket.*` 配置 |

## 自动装配与配置

```yaml
kudos:
  ability:
    comm:
      websocket:
        node-id: ""                      # 留空则启动时随机生成
        broadcast:
          send-timeout-millis: 10000
          max-concurrent-sends: 512
          close-on-send-timeout: true
        distributed:
          enabled: false                 # 打开即升级为跨进程广播
          redis-channel: "kudos:ws:broadcast"
          inbound-buffer-capacity: 1024
```

业务侧直接注入即可：

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

**依赖 `IWebSocketBroadcaster` 接口而不是 `WebSocketBroadcaster` 具体类**，是单机→集群零改动的前提：
`distributed.enabled=true` 时装配的是 `DistributedWebSocketBroadcaster`，调用方毫无感知。

Redis 通道只在 spring-data-redis 在 classpath 上、且开关打开时才装配，并且复用业务方已有的
`RedisMessageListenerContainer`（`kudos-ability-cache-remote-redis` 提供的那个即可）。

## 分布式广播

`DistributedWebSocketBroadcaster` 每次广播额外往 `IWebSocketBroadcastChannel` 发一份，其它节点收到后
用各自的进程级注册中心交付。

### 语义

- **返回值仅代表本地**：`broadcast()` 返回本节点投递成功的 session 数，`unicast()` 返回本节点是否
  持有该 sessionId。远端是否投递成功不在返回值里——transport 大多无 ack
- **自回声过滤**：Redis pub/sub 会把发出的消息也推回发送方，按 `envelope.nodeId == nodeId` 静默丢弃，
  保证发起节点只本地广播一次
- **`unicast` 先本地后广播**：sessionId 全集群唯一，本地命中就说明别的节点不可能持有它，此时再发
  envelope 纯属浪费（全集群每个节点都要反序列化一条与自己无关的报文）；只有本地未命中才走通道
- **publish 失败不阻断本地**：`channel.publish` 抛异常会 WARN 后继续做本地广播，避免远端 Redis
  抖动时本节点的 session 也跟着收不到消息
- **入站保序**：Redis 按发布顺序把消息交给 listener，若每条消息各起一个协程分发就把顺序丢了（聊天类
  场景用户直接可见）。实现改为投进一个有界 `Channel` 由单消费者顺序消费——既保序，又能立刻释放
  Spring 的 listener 线程。代价是慢 handler 会拖住它后面的消息，这是有序投递的固有代价；缓冲区满
  （默认 1024）时丢弃并 WARN，好过阻塞 listener 线程拖垮共享同一 container 的其它订阅
- **handler 异常隔离**：单个 inbound 处理失败 ERROR 后继续，下一条消息照常处理（默认 `MessageListener`
  抛异常会终止整个 listener 线程）
- **版本兜底**：envelope 带 `version`，收到高于自己 `CURRENT_VERSION` 的报文直接丢弃并 WARN——滚动
  升级期间半懂不懂地解释新报文比丢掉它更危险

### 生命周期

`subscribe` 返回 `WebSocketBroadcastSubscription`（`AutoCloseable`），`DistributedWebSocketBroadcaster`
和 `RedisWebSocketBroadcastChannel` 都实现了 `AutoCloseable`（Spring 会在容器关闭时自动调用 `close()`）。
没有退订路径的话，每次测试、热更新、容器 refresh 都会往一个与进程同寿的通道里泄漏 handler。

### 换其它 transport

实现 `IWebSocketBroadcastChannel`（`publish` + `subscribe`）即可。Kafka topic / NATS subject /
内存桥接（见 `test-src/InMemoryBroadcastChannel.kt`）都按这个 SPI 走。

## 测试覆盖

共 125 个用例：

| 测试类 | 用例 | 覆盖重点 |
|---|---|---|
| `KudosWebSocketRegistryTest` | 13 | 三套索引的增删、重复 sessionId 的索引清理、并发注册/注销不丢失活跃 session |
| `KudosWebSocketRoutingTest` | 13 | `testApplication` 端到端：收发回环、元数据传递、鉴权拒绝、准入链顺序与失败关闭、生命周期顺序、取消下的清理完成 |
| `DispatchFramesTest` | 12 | 分片重组（含跨边界多字节 UTF-8）、控制帧穿插、Close 中断、超限关闭 |
| `WebSocketHandlerDecoratorTest` | 4 | 全钩子透明委托、`wrappedBy` 外层优先、around 与短路 |
| `RateLimitingWebSocketHandlerTest` | 7 | 令牌桶突发/补充/封顶、按连接隔离、文本与二进制共用额度、覆写关闭策略 |
| `MaxConnectionsInterceptorTest` | 7 | user / tenant / 节点三个维度、0 关闭维度、匿名会话跳过身份维度 |
| `KudosContextWebSocketHandlerTest` | 9 | ThreadLocal 与协程元素双通道可见、**跨线程跳变后仍可见**、返回后清理、原有绑定还原、异常路径不残留 |
| `WebSocketBroadcasterTest` | 16 | 扇出计数、失败隔离、二进制路径、发送超时与关闭策略、并发上限 |
| `DistributedWebSocketBroadcasterTest` | 15 | 双节点内存桥接、自回声过滤、本地优先 unicast、版本兜底、close 退订 |
| `RedisWebSocketBroadcastChannelTest` | 11 | 订阅/发布委托、反序列化异常隔离、投递保序、close 与退订 |
| `WebSocketAutoConfigurationTest` | 7 | 本地/分布式装配决策、nodeId 生成、配置项确实传到广播器 |
| `WebSocketBroadcastEnvelopeTest` | 4 | 报文相等性与 Java 序列化往返 |
| `KudosWebSocketSessionTest` / `IKudosWebSocketHandlerTest` | 6 / 1 | 会话发送/关闭语义、SPI 默认实现 |

`RedisWebSocketBroadcastChannel` 的测试用 mock 容器 + 连接无关的 `RedisTemplate`，不需要真实 Redis。

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

- ❗ 编解码 SPI 仍无默认实现——`TypedWebSocketHandler` 解决了"怎么用"，但具体的 JSON 实现要业务侧
  按 Jackson / kotlinx.serialization 注入。等 kudos 主体确定默认 JSON 库后再补
- ❗ **`KudosContext` 桥接是本模块局部方案**：`KudosContextWebSocketHandler` 只在 WebSocket 消息
  分发范围内生效，`kudos-context` 里 `KudosContextElement` 与 `KudosContextHolder` 互不感知的
  全局状况没有改变（那是一个跨模块决策，见上文"为什么放在本模块"）。此外 `KudosContext.user`
  需要业务自己用 `contextFactory` 填充
- ❗ `unicast` 跨节点的返回值仍按本地语义：本节点没该 session 即返回 false，但远端可能投递成功；
  调用方若需要全局 ack 应改走 `broadcastToUser` + 业务层标记
- ❗ 在线人数 / 用户在线状态的全局查询（soul 的 `getWsOnlineAllUser` 等）还没移植——按需补
  `IWebSocketPresenceStore` 加 Redis 实现
- ❗ 没有集群级踢人（按 userId 强制下线）——`TargetType` 加一个 `CLOSE` 即可，等有需求再加
- ❗ 无可观测性埋点：目前只有 `registry.size`。session 数 gauge、广播成功/失败计数、publish 失败计数
  接 Micrometer 是低成本高回报，但会引入新依赖，暂缓
- ❗ 每 session 仍无独立发送队列上限：`sendTimeout` + `closeOnSendTimeout` 已经能回收不读 socket 的
  客户端，但严格的 per-session outbox 容量控制（超限即丢旧帧）还没有
