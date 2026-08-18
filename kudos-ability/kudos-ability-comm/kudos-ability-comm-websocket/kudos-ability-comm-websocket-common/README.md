# kudos-ability-comm-websocket-common

WebSocket 业务层的**引擎无关**内核。会话注册中心、广播契约、业务回调 SPI、连接准入、编解码契约、
`KudosContext` 桥接与跨进程投递都在这里；`-ktor` 与 `-spring` 只各自贡献"一个
`KudosWebSocketSessionRef` 实现 + 一个路由/端点适配器"。

本模块**不依赖任何 WebSocket 引擎**——没有 Ktor，也没有 spring-websocket。这不是巧合，是约束：
它决定了一个纯 Spring MVC 应用能用注册中心和广播器而不必把 Ktor 拖上 classpath。

## 为什么是独立模块

早先这些抽象和 Ktor 实现同住一个模块，并在 README 里声明"换引擎只需新增一份
`KudosWebSocketSessionRef` 实现"。实际做不到：`KudosWebSocketSessionRef.close()` 的参数类型是
`io.ktor.websocket.CloseReason`，于是每一个"引擎无关"组件都悄悄依赖了 `ktor-websockets`。

拆分时顺带把这个洞补上，引入了 [`WebSocketCloseReason`](src/io/kudos/ability/comm/websocket/common/session/WebSocketCloseReason.kt)
（RFC 6455 状态码 + 原因短语）。各引擎模块在**唯一一处**做映射：

| 引擎模块 | 映射位置 | 映射到 |
|---|---|---|
| `-ktor` | `session/WebSocketCloseReasons.kt` | `io.ktor.websocket.CloseReason` |
| `-spring` | `session/SpringWebSocketSession.close` | `org.springframework.web.socket.CloseStatus` |

## 模块入口

| 路径 | 角色 |
|---|---|
| `session/KudosWebSocketSessionRef` | 会话抽象。registry / broadcaster / handler / 拦截器全部面向它 |
| `session/WebSocketCloseReason` | 引擎无关的关闭原因（`code: Short` + `message`） |
| `session/WebSocketPayload` | 文本 / 二进制载荷的密封类型 |
| `session/KudosWebSocketRegistry` | 三套索引（sessionId / userId / tenantId）的进程级注册中心 |
| `handler/IKudosWebSocketHandler` | onConnect / onText / onBinary / onDisconnect SPI |
| `handler/TypedWebSocketHandler` | 自动解码入站消息为业务类型的 handler 基类 |
| `handler/WebSocketHandlerDecorator` | 消息级拦截基类 + `wrappedBy` 链装配 |
| `handler/RateLimitingWebSocketHandler` | 每连接令牌桶限流装饰器 |
| `connect/IWebSocketConnectInterceptor` | 连接准入 SPI（`Proceed` / `Reject`），注册前执行 |
| `connect/MaxConnectionsInterceptor` | 按 user / tenant / 节点限制并发连接数 |
| `connect/WebSocketAdmission` | `List<IWebSocketConnectInterceptor>.admit(...)`，两个引擎共用 |
| `context/KudosContextThreadElement` | 让 `KudosContextHolder` 的 ThreadLocal 跟随协程而非线程 |
| `context/KudosContextWebSocketHandler` | 每次钩子调用建立 `KudosContext` 的装饰器 |
| `broadcast/IWebSocketBroadcaster` | 广播契约（本地与分布式实现共用） |
| `broadcast/WebSocketBroadcaster` | 进程级实现，带并发上限与发送超时 |
| `codec/IWebSocketMessageEncoder` | 业务对象 ↔ 文本 frame 编 / 解码 SPI |
| `distributed/DistributedWebSocketBroadcaster` | 装饰本地广播器，外加跨进程投递 |
| `distributed/IWebSocketBroadcastChannel` | 跨节点投递通道 SPI（Redis / Kafka / 自定义） |
| `distributed/WebSocketBroadcastEnvelope` | 通道报文：`nodeId` / `targetType` / `targetId` / `text` 或 `binary` / `version` |
| `distributed/redis/RedisWebSocketBroadcastChannel` | Spring Data Redis pub/sub 默认实现 |
| `init/WebSocketAutoConfiguration` | 注册中心 / 广播器 / Redis 通道的 Spring 装配（两个引擎共用） |
| `init/WebSocketProperties` | `kudos.ability.comm.websocket.*` 配置 |

## 设计要点

### 注册中心的索引更新走 `compute`，且**不**跨进程同步

三套索引都是 `ConcurrentHashMap`，二级桶的每次增删都在 `compute` / `computeIfPresent` 的 lambda 内完成：

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

### 广播的失败语义与背压

`WebSocketBroadcaster` 并发地给每个 session 发送，单个失败 catch + WARN，返回成功条数，**不重试**
（重试是业务关注点）。在此之上有两个限流开关：

- `maxConcurrentSends`（默认 512）——限制同时在飞的发送数。否则给 10 万 session 广播会一次性起
  10 万协程、堆 10 万个待发帧。
- `sendTimeout`（默认 10s）——限制单个 session 能拖多久。**不读 socket 的客户端不会让 send 失败**，
  它只会让帧在发送队列里越堆越多直到 OOM，同时一直占着并发额度。

超时后 `closeOnSendTimeout`（默认 **true**）决定是否关掉该连接。默认关闭是因为：只超时不关闭，已经
排队的帧仍然留在内存里——那等于检测到了泄漏却不处理它。

### 三层拦截，各归各位

"中间件"不是一个问题，是三个，各自的正确落点不同：

| 关注点 | 落点 | 理由 |
|---|---|---|
| 握手前鉴权、Origin 校验 | **引擎自带**：Ktor 的 `authenticate("jwt") { ... }`；Spring 的 `IWebSocketHandshakeGuard` | 协议层属于引擎职责。只有在握手前拒绝，客户端才拿得到一个真正的 HTTP 状态码 |
| 连接准入（配额、封禁、设备数上限） | `IWebSocketConnectInterceptor`（本模块） | 在会话工厂之后、注册之前——身份已知但广播还够不着的唯一窗口 |
| 每条消息（限流、埋点、链路） | `WebSocketHandlerDecorator`（本模块） | 装饰 `IKudosWebSocketHandler` 即可，无需新 SPI |

#### 为什么消息侧没有单独的拦截器接口

因为不需要。一个包装另一个 handler 的 `IKudosWebSocketHandler` **就是**拦截器，而且比
Spring `HandlerInterceptor` 的 `preHandle`/`postHandle` 更强——装饰器能在同一个栈帧里跨越
delegate 前后，所以计时、`try/finally`、开 tracing span、给 delegate 套一层协程上下文都能写；
不调用 `super` 就是短路。再定义一个 `IWebSocketMessageInterceptor` 只会得到一个能力更弱的重复抽象。

`wrappedBy` 让链条按执行顺序书写而不是由内向外嵌套：

```kotlin
val handler = chatHandler.wrappedBy(
    { next -> TracingHandler(next) },                              // 最外层
    { next -> RateLimitingWebSocketHandler(next, permitsPerSecond = 10.0) },
)
```

Spring 场景下用 `ObjectProvider<...>.orderedStream()` 取工厂列表即可——它本身就按 `@Order` 排序。

#### 连接准入为什么要单独一层

会话工厂返回 `null` 解决的是"认不出你是谁"，而配额、封禁、设备数上限是"认出来了但不放行"。把两者
揉进同一个 lambda，多个独立策略就只能串成一段越写越长的代码，顺序和短路全靠约定。
`IWebSocketConnectInterceptor` 是一个可组合的列表，按序执行、首个拒绝生效、**抛异常按拒绝处理**
（准入检查失败不能被读成"有配额"）。

这条 fail-closed 规则是安全属性，所以执行逻辑收敛在 `connect/WebSocketAdmission.kt` 的
`List<IWebSocketConnectInterceptor>.admit(...)` 一处，由两个引擎共用——两份手工维护的安全属性一定会漂移。

自带 `MaxConnectionsInterceptor`。注意它是**尽力而为**：计数发生在注册之前，并发冲进来的 N 个连接
可能都读到"未超限"，最多超出 N-1 个。要做成精确上限就得在连接路径上跨"准入+注册"加锁，代价不划算
——硬上限属于网关层，这一层是为了拦住失控客户端开几千个 socket。

### KudosContext 与会话打通

`KudosContextWebSocketHandler` 装饰器把 session 的 `tenantId` 建立成一个 `KudosContext`，覆盖全部
四个钩子（含 `onDisconnect`——业务清理往往也要访问数据库）。没有它，session 上明明有身份信息，
下游却看不见：kudos 的租户缓存 key、ktorm 的 `currentDatabase()` 路由、审计字段填充读的都是
`KudosContextHolder`，而 WebSocket 路径从没往里写过东西。

#### 为什么不是 `set` + `finally { clear() }`

因为 handler 是协程，可能在挂起后**换一个线程**恢复。在原线程 `set` 的 ThreadLocal 在新线程上根本
不存在；而且它也无法清理自己途经的那些线程，正好制造出 `kudos-ability-cache/README.md` 记录过的
池线程污染。

所以桥接走 `KudosContextThreadElement`（`kotlinx.coroutines.ThreadContextElement`）：协程每次在某个
线程上恢复时由运行时调用 `updateThreadContext`、离开时调用 `restoreThreadContext`，ThreadLocal 因此
跟着**协程**走而不是跟着线程走，并且原有绑定会被原样还原。`KudosContextWebSocketHandlerTest` 里有
一个用例专门跑到 `Dispatchers.Default` 的 worker 上读上下文——手写 set/clear 会在那里失败。

同时也挂上 `KudosContextElement`，让 `currentKudosContext()` 一并可用；只挂一边等于对半个代码库静默失效。

#### 为什么放在本模块而不是改 `kudos-context`

把 `KudosContextElement` 本身改成 `ThreadContextElement` 会全进程生效，包括那 27 处
`KudosContextHolder.get()`——它是"读时创建并写入"的语义，于是在协程内部懒创建出来的上下文会在
`restoreThreadContext` 时被静默丢弃。`XKudosContextHolder` 把解析好的 `DataSource` 缓存进上下文
就是这样一处，而它支撑着所有 ktorm DAO 的 `currentDatabase()`，退化后果是**不报错、只变慢**。
另外 `kudos-context/README.md` 明文记录过"库本身不做自动桥接、责任在入口插件"的决定。

**注意**：`KudosContext.user` 默认留空——它是 `IIdEntity<String>`，而 session 只有一个 userId
字符串，凭空造一个半成品实体会让下游拿到比 null 更糟的东西。需要时传入自己的 `contextFactory`。

### 编解码 SPI 的消费端

`IWebSocketMessageEncoder` 只是契约，本模块不带默认实现（模块本身不依赖任何 JSON 库）。入站方向由
`TypedWebSocketHandler<T>` 消费：

```kotlin
val handler = typedWebSocketHandler<ChatMessage>(encoder) { session, message ->
    broadcaster.broadcastToTenant(session.tenantId ?: "default", encoder.encode(message.reply()))
}
```

解码失败默认走 `onDecodeFailure`（WARN 后丢弃，不断连接）——一个客户端发了坏消息是客户端的 bug，
不该让一条可能还在正常收发的连接陪葬。出站方向不需要脚手架：`encoder.encode(msg)` 直接交给任意广播方法即可。

## 自动装配与配置

`WebSocketAutoConfiguration` 由 kudos 的 `ComponentInitializerSelector` 自动导入，两个引擎模块共用：

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

业务侧直接注入 `KudosWebSocketRegistry` 与 `IWebSocketBroadcaster` 即可。**依赖
`IWebSocketBroadcaster` 接口而不是 `WebSocketBroadcaster` 具体类**，是单机→集群零改动的前提：
`distributed.enabled=true` 时装配的是 `DistributedWebSocketBroadcaster`，调用方毫无感知。

Redis 通道只在 spring-data-redis 在 classpath 上、且开关打开时才装配，并且复用业务方已有的
`RedisMessageListenerContainer`（`kudos-ability-cache-remote-redis` 提供的那个即可）。

## 分布式广播

`DistributedWebSocketBroadcaster` 每次广播额外往 `IWebSocketBroadcastChannel` 发一份，其它节点收到后
用各自的进程级注册中心交付。

### 语义

- **返回值仅代表本地**：`broadcast()` 返回本节点投递成功的 session 数，`unicast()` 返回本节点是否
  持有该 sessionId。远端是否投递成功不在返回值里——transport 大多无 ack
- **自回声过滤**：Redis pub/sub 会把发出的消息也推回发送方，按 `envelope.nodeId == nodeId` 静默丢弃
- **`unicast` 先本地后广播**：sessionId 全集群唯一，本地命中就说明别的节点不可能持有它，此时再发
  envelope 纯属浪费；只有本地未命中才走通道
- **publish 失败不阻断本地**：`channel.publish` 抛异常会 WARN 后继续做本地广播，避免远端 Redis
  抖动时本节点的 session 也跟着收不到消息
- **入站保序**：Redis 按发布顺序把消息交给 listener，若每条消息各起一个协程分发就把顺序丢了。实现
  改为投进一个有界 `Channel` 由单消费者顺序消费——既保序，又能立刻释放 Spring 的 listener 线程。
  缓冲区满（默认 1024）时丢弃并 WARN，好过阻塞 listener 线程拖垮共享同一 container 的其它订阅
- **handler 异常隔离**：单个 inbound 处理失败 ERROR 后继续（默认 `MessageListener` 抛异常会终止整个
  listener 线程）
- **版本兜底**：envelope 带 `version`，收到高于自己 `CURRENT_VERSION` 的报文直接丢弃并 WARN——滚动
  升级期间半懂不懂地解释新报文比丢掉它更危险

### 生命周期

`subscribe` 返回 `WebSocketBroadcastSubscription`（`AutoCloseable`），`DistributedWebSocketBroadcaster`
和 `RedisWebSocketBroadcastChannel` 都实现了 `AutoCloseable`。没有退订路径的话，每次测试、热更新、
容器 refresh 都会往一个与进程同寿的通道里泄漏 handler。

### 换其它 transport

实现 `IWebSocketBroadcastChannel`（`publish` + `subscribe`）即可。Kafka topic / NATS subject /
内存桥接（见 `test-src/InMemoryBroadcastChannel.kt`）都按这个 SPI 走。

## 测试覆盖

共 117 个用例，全部不需要任何引擎、容器或真实 Redis：

| 测试类 | 用例 | 覆盖重点 |
|---|---|---|
| `WebSocketBroadcasterTest` | 16 | 扇出计数、失败隔离、二进制路径、发送超时与关闭策略、并发上限 |
| `DistributedWebSocketBroadcasterTest` | 15 | 双节点内存桥接、自回声过滤、本地优先 unicast、版本兜底、close 退订 |
| `WebSocketCloseReasonTest` | 9 | RFC 6455 码值逐条核对、`byCode` 已知/未知、`knownCode` 分类、共享常量语义、结构相等 |
| `WebSocketAdmissionTest` | 7 | 空链放行、全放行、首个拒绝生效且后续不跑、**抛异常 fail closed**、取消重新抛出 |
| `KudosWebSocketRegistryTest` | 13 | 三套索引的增删、重复 sessionId 的索引清理、并发注册/注销不丢失活跃 session |
| `RedisWebSocketBroadcastChannelTest` | 11 | 订阅/发布委托、反序列化异常隔离、投递保序、close 与退订 |
| `KudosContextWebSocketHandlerTest` | 9 | 双通道可见、**跨线程跳变后仍可见**、返回后清理、原有绑定还原、异常路径不残留 |
| `MaxConnectionsInterceptorTest` | 7 | user / tenant / 节点三个维度、0 关闭维度、匿名会话跳过身份维度 |
| `RateLimitingWebSocketHandlerTest` | 7 | 令牌桶突发/补充/封顶、按连接隔离、文本与二进制共用额度、覆写关闭策略 |
| `WebSocketAutoConfigurationTest` | 7 | 本地/分布式装配决策、nodeId 生成、配置项确实传到广播器 |
| `WebSocketPayloadTest` | 7 | `Binary` 的结构化相等与 hashCode（非 data class 是有意的，默认实现会按引用比较）、`toString` 只打尺寸、`Text` 值语义、`send` 按类型分派、密封层级仍只有两个分支 |
| `WebSocketHandlerDecoratorTest` | 4 | 全钩子透明委托、`wrappedBy` 外层优先、around 与短路 |
| `WebSocketBroadcastEnvelopeTest` | 4 | 报文相等性与 Java 序列化往返 |
| `IKudosWebSocketHandlerTest` | 1 | SPI 默认实现全为 no-op 且不触碰 session |

### 测试约定：`runBlocking` 的测试必须显式写 `: Unit`

本模块 117 个用例里有 51 个是 `fun xxx(): Unit = runBlocking { ... }` 的形式（广播、分布式投递、handler
SPI 全是挂起函数）。那个 `: Unit` 不是可省的样板：

```kotlin
@Test
fun binaryPayloads_takeTheSendBinaryPath() = runBlocking {   // ❌ 块的最后一个表达式若不是 Unit……
    assertContentEquals(bytes, session.received.single())    // ……assertContentEquals 有返回值，方法就带上了返回值
}
```

JUnit 5 遇到有返回值的 `@Test` 方法**不会报错，而是静默跳过整个方法**：测试报告里连条目都不出现，
用例数悄悄少一个，只在构建日志里留一行很容易被忽略的：

```
[WARNING] @Test method '...binaryPayloads_takeTheSendBinaryPath()' must not return a value. It will not be executed.
```

踩中的门槛很低——`assertNotNull` / `assertIs` / `assertContentEquals` 都有返回值，随手放在最后一行就中招。
本目录里真的因此有过两个从未被执行的测试（都在 `-spring` 模块），是排查另一个问题时从构建日志里偶然发现的。

写成 `fun binaryPayloads_takeTheSendBinaryPath(): Unit = runBlocking { ... }` 后，编译器强制丢弃块的值，
这个问题从此不可能发生。新增此类测试时照抄这个签名即可。自查：

```bash
./gradlew test 2>&1 | grep "must not return a value"
```

## 依赖

```kotlin
api(project(":kudos-context"))
api(libs.kotlinxCoroutines)                        // 公开 API 里有 suspend / ThreadContextElement

// 可选：使用 distributed/redis 时业务侧引入；本模块 compileOnly，单实例部署不付出代价
compileOnly(libs.spring.boot.starter.data.redis)
```

没有任何 WebSocket 引擎依赖，这是本模块存在的意义。

## 新增一个引擎实现

1. 实现 `KudosWebSocketSessionRef`（把 `WebSocketCloseReason` 映射到该引擎的关闭类型）
2. 写一个适配器，按下面的模板驱动 `IKudosWebSocketHandler`：

```
会话工厂（识别身份，null = 拒绝）
  → connectInterceptors.admit(...)（准入，首个拒绝生效）
  → registry.register(session)
  → onConnect + 消息循环
  → finally { withContext(NonCancellable) { onDisconnect; registry.unregister; close } }
```

注册中心、广播器、装饰器、准入、编解码、`KudosContext` 桥接与分布式投递全部直接复用。
现成的两份参考：`-ktor` 的 `Route.kudosWebSocket`，`-spring` 的 `KudosSpringWebSocketHandler`。

## 已知限制

- ❗ 编解码 SPI 无默认实现——`TypedWebSocketHandler` 解决了"怎么用"，具体 JSON 实现要业务侧注入
- ❗ `KudosContext` 桥接是局部方案：只在 WebSocket 钩子分发范围内生效，`kudos-context` 里
  `KudosContextElement` 与 `KudosContextHolder` 互不感知的全局状况没有改变
- ❗ `unicast` 跨节点的返回值仍按本地语义；需要全局 ack 应改走 `broadcastToUser` + 业务层标记
- ❗ 在线人数 / 用户在线状态的全局查询还没有——按需补 `IWebSocketPresenceStore` 加 Redis 实现
- ❗ 没有集群级踢人（按 userId 强制下线）——`TargetType` 加一个 `CLOSE` 即可，等有需求再加
- ❗ 无可观测性埋点：目前只有 `registry.size`。接 Micrometer 是低成本高回报，但会引入新依赖，暂缓
