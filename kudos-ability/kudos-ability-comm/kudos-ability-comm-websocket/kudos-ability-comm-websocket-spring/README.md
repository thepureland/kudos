# kudos-ability-comm-websocket-spring

业务层 WebSocket 抽象的 **Spring（Servlet / Spring MVC）实现**。

引擎无关的部分——会话注册中心、广播器、`IKudosWebSocketHandler` SPI、连接准入、编解码契约、
`KudosContext` 桥接、跨进程投递——全部来自
[`kudos-ability-comm-websocket-common`](../kudos-ability-comm-websocket-common/README.md)，与
`-ktor` 完全共用。本模块只做 Spring 独有的那部分：

1. **会话适配**（`SpringWebSocketSession`）：把 Spring 的 `WebSocketSession` 适配成 `KudosWebSocketSessionRef`
2. **回调桥接**（`KudosSpringWebSocketHandler`）：把 Spring 阻塞式的 `WebSocketHandler` 回调桥到
   `suspend` SPI，保序、有界、带背压
3. **握手层**（`KudosHandshakeInterceptor` + `IWebSocketHandshakeGuard`）：升级**之前**用真正的
   HTTP 状态码拒绝
4. **端点装配**（`SpringWebSocketAutoConfiguration` + `IKudosWebSocketEndpoint`）：多端点注册、
   Origin 策略、容器缓冲区

因为业务 SPI 是同一套，**同一个 `IKudosWebSocketHandler` 实现可以不加改动地在 Ktor 与 Spring 两侧运行**。

## 快速上手

```kotlin
@Component
class ChatHandler(private val broadcaster: IWebSocketBroadcaster) : IKudosWebSocketHandler {
    override suspend fun onText(session: KudosWebSocketSessionRef, text: String) {
        broadcaster.broadcastToTenant(session.tenantId ?: "default", text)
    }
}
```

只有一个 handler bean 时，它会被自动挂到 `kudos.ability.comm.websocket.spring.path`（默认 `/ws`）。
需要识别用户就再给一个会话工厂：

```kotlin
@Bean
fun wsSessionFactory(userService: UserService) = ISpringWebSocketSessionFactory { raw ->
    val token = HandshakeAttrs.queryParam(raw, "token") ?: return@ISpringWebSocketSessionFactory null
    val user = userService.findByToken(token) ?: return@ISpringWebSocketSessionFactory null
    SpringWebSocketSession(raw, userId = user.id, tenantId = user.tenantId)
}
```

多个端点则声明 `IKudosWebSocketEndpoint` bean：

```kotlin
@Bean fun chatEndpoint(h: ChatHandler, f: ISpringWebSocketSessionFactory) =
    KudosWebSocketEndpoint("/ws/chat", h, sessionFactory = f)

@Bean fun notifyEndpoint(h: NotifyHandler, f: ISpringWebSocketSessionFactory) =
    KudosWebSocketEndpoint("/ws/notify", h, sessionFactory = f,
        connectInterceptors = listOf(MaxConnectionsInterceptor(registry, maxPerUser = 4)))
```

## 设计要点

### 阻塞回调 → suspend SPI：每连接一个有界队列 + 单消费者协程

这是本模块真正在解决的问题。Spring 的 `WebSocketHandler` 回调是**阻塞的、一次一条**，在容器线程上
执行；`IKudosWebSocketHandler` 的钩子是 `suspend` 的。朴素的桥接方式有三种，三种都错：

| 写法 | 后果 |
|---|---|
| 每条消息 `runBlocking { }` | 整个业务处理期间占住一个 Tomcat 请求线程——那是和所有 HTTP 端点共享的池 |
| 每条消息 `scope.launch { }` | 线程释放了，但**消息顺序没了**：相差 1ms 的两条聊天消息各起一个协程，谁先完成看调度器。用户立刻能察觉，事后无从追查 |
| 以上任一但不设上限 | 发得比处理快的客户端能在堆里排出无限长的待办 |

所以每条连接拿到**一个有界 `Channel` + 恰好一个消费者协程**：单消费者保证顺序，入队即释放容器线程，
有界让"溢出"变成一个可以选择的策略而不是 OOM。

### 背压是默认，而且它不是免费的

`InboundOverflowPolicy.BACKPRESSURE`（默认）在队列满时**阻塞容器线程**直到消费跟上。这是刻意的：
阻塞在那里，容器就不再读这条连接的 socket，TCP 流控随即生效，客户端在传输层被限速——这是 WebSocket
唯一一个既能限速又不丢数据的位置。

代价是每条积压连接占一个容器线程。Java 21+ 打开 `spring.threads.virtual.enabled=true` 后几乎免费；
纯平台线程池下则不然，所以另外提供 `DROP_LATEST` 与 `CLOSE` 给宁可卸载流量也不愿占线程的部署。
注意快路径完全不阻塞——队列有空位时 `trySend` 直接成功，这是常态。

### 并发发送必须包一层 `ConcurrentWebSocketSessionDecorator`

Spring 的 `WebSocketSession` **不是并发安全的**。两个线程同时 `sendMessage` 会抛
`IllegalStateException: The remote endpoint was in state [TEXT_FULL_WRITING]`。这不是理论风险：
广播天然并发地打到多个 session，而"业务推送撞上同连接的回复"是常态而非罕见竞态。

因此每个 session 在构造业务会话之前先被包进 Spring 自带的 `ConcurrentWebSocketSessionDecorator`：
它串行化发送，**并且**限制一个不读 socket 的客户端最多能堆多少字节（`send.buffer-size-limit-bytes`），
超限按 `TERMINATE` 断开。这是 common 模块里 `sendTimeout` 在 Spring 侧的对等物——
**没有这道限制，一个卡死的客户端就能把进程堆吃光**。

由此 `SpringWebSocketSession.raw` 通常是那个装饰器而不是容器的原始 session；确实需要底层对象时用
`WebSocketSessionDecorator.unwrap(raw)`，不要假定具体类型。

另外所有 `sendMessage` / `close` 都在 `Dispatchers.IO` 上执行——它们是阻塞调用，直接在协程线程上跑会钉住调度器。

### 分片重组交给容器

`supportsPartialMessages()` 返回 `false`，Servlet 容器会在调用 `handleMessage` 之前把分片重组成一条
完整消息。所以本模块**不需要** Ktor 那侧的 `dispatchFrames` 重组逻辑。

代价是"单条消息最大多大"由容器缓冲区决定，而不是本模块里的任何检查。Jakarta WebSocket 默认只有
**8 KB**，超出会失败且报错位置离真正原因很远，所以 `SpringWebSocketAutoConfiguration` 默认贡献一个
`ServletServerContainerFactoryBean` 把它抬到 64 KB（可配）。

> ⚠️ **那 8 KB 默认在客户端侧同样存在。** `container.max-*-buffer-size` 只管服务端收；如果你的客户端
> 也是 JVM（Java/Kotlin 写的服务间长连接、压测工具、集成测试），它自己的 `WebSocketContainer` 默认同样
> 是 8 KB，服务端推回一条超过 8 KB 的消息时会在**客户端**失败——现象是连接莫名断开，很容易误判成服务端
> 的问题。客户端要一并调：
>
> ```kotlin
> val container = ContainerProvider.getWebSocketContainer().apply {
>     defaultMaxTextMessageBufferSize = 1024 * 1024
>     defaultMaxBinaryMessageBufferSize = 1024 * 1024
> }
> StandardWebSocketClient(container)
> ```
>
> 浏览器客户端不受此限（浏览器自己管缓冲）。这一条是写 `SpringWebSocketEndToEndTest` 的 32 KB 用例时
> 实测踩到的——第一版红了，红在客户端而不是服务端。

### 握手守卫：在升级之前拒绝

`IWebSocketHandshakeGuard` 在 HTTP 阶段执行，拒绝时返回真正的 **401 / 403**。

这一层为什么值得单独存在，看客户端就明白：会话工厂返回 `null` 发生在升级**成功之后**，客户端看到的
是"socket 开了又关"——而所有 WebSocket 客户端都会把这理解成瞬时网络故障并重连，于是一个错误的凭证
变成一个重连风暴。守卫拒绝则是一个客户端读得懂、记得下、能据此停手的 HTTP 错误。

守卫抛异常按拒绝处理（fail closed）。`KudosHandshakeInterceptor` 同时把 query 参数解析一次缓存进
handshake attributes，供会话工厂读取——浏览器的 `WebSocket` 构造器**无法设置请求头**，所以 `?token=`
是 Web 客户端识别自己的主要方式，而 Spring 只给出一个未解析的原始 query 字符串。

解析走 `URI.getRawQuery()` 而非 `getQuery()`：后者已经解码，值里合法包含 `&` 或 `=` 的场景
（带 padding 的 base64 token、签名 URL）会被从错误的位置切开并静默截断。先切分后解码是唯一能扛住
这种输入的顺序。

### 三个连接期钩子，不要混用

| 钩子 | 时机 | 用途 | 拒绝时客户端看到 |
|---|---|---|---|
| `IWebSocketHandshakeGuard` | 升级**前**（HTTP） | 凭证缺失、Origin、IP 黑名单 | HTTP 401 / 403 |
| `ISpringWebSocketSessionFactory` | 升级后、注册前 | **识别身份**，`null` = 认不出你是谁 | 连接建立后立即关闭 |
| `IWebSocketConnectInterceptor`（common） | 工厂之后、注册前 | **准入策略**：配额、设备数、封禁 | 带策略 close code 关闭 |

后两者的先后顺序不是随意的：注册紧跟在准入之后，若采用"先注册、稍后再关"的写法，那段窗口里连接
已经是它自称的 `tenantId` 的成员，会收到该租户的广播。

### 关闭原因要能区分"正常挂断"和"人没了"

客户端消失（合盖、蜂窝切换）产生的是 close code **1006**，且**没有** transport error。若只上报
transport error，业务侧会以为一条掉线的连接是正常关闭的——而在线状态标记、"这是登出还是网络抖动"
这类判断恰恰全靠这个信号。

所以 `onDisconnect` 的 cause 依次取：业务异常 → transport error → 非 1000 的关闭状态合成的
`WebSocketAbnormalCloseException`。正常关闭（1000）时 cause 为 `null`。

### 优雅停机

`KudosSpringWebSocketHandler` 与 `SpringWebSocketAutoConfiguration` 都实现了 `DisposableBean`：
上下文关闭时先关闭各连接的入站队列（让在途消息处理完），等待至 `shutdown-timeout-millis`，再取消。
`onDisconnect` 跑在 `NonCancellable` 里，所以挂起式的业务清理不会在第一个挂起点被打断。

没有这一步，滚动重启时每个进程都会带着半处理的消息退出、跳过所有 `onDisconnect`——恰好是全集群同时
这么做、在线状态最需要被清理的时刻。

## 与参考实现（soul 的 `soul-ability-comm-websocket-spring`）的差异

对照着写，但下面几处是明确改掉的：

| soul 的做法 | 本模块 | 为什么 |
|---|---|---|
| `setAllowedOrigins("*")` 硬编码 | 默认空 = Spring 的同源限制，`"*"` 需显式配置且启动告警 | WebSocket 握手不受同源策略约束、浏览器照样带 cookie。允许任意源等于让任何网页以你已登录用户的身份开一条认证连接 |
| 单个硬编码路径 + 固定的 SockJS 端点 | `IKudosWebSocketEndpoint` 多端点；SockJS 默认关闭、按端点可开 | 一个服务往往要挂多个语义不同的端点；SockJS 现今是少数派需求，不该默认摊开一片 URL |
| 握手拦截器写死 `wsToken` 参数名并直接 `return false` | `IWebSocketHandshakeGuard` SPI，自带 HTTP 状态码 | 参数名是业务决定；且 soul 拒绝时不设状态码，客户端拿到的信息不足以停止重连 |
| 继承 `HttpSessionHandshakeInterceptor` | 直接实现 `HandshakeInterceptor` | 前者默认会**创建** `HttpSession`，让每条 WebSocket 连接都分配服务端会话状态——对 token 认证的客户端纯属浪费 |
| 在 `WebSocketHandler` 回调里直接跑业务 | 每连接队列 + 单消费者协程 | 见上文"阻塞回调 → suspend SPI" |
| `session.sendMessage` 裸调用 | `ConcurrentWebSocketSessionDecorator` + `Dispatchers.IO` | 裸调用在并发广播下会抛 `TEXT_FULL_WRITING`，且没有缓冲区上限可言 |
| 文本 `"ping"` / `"pong"` 心跳 | 协议级 Ping/Pong 由容器处理，不上抛业务 | 应用层重造心跳只在浏览器无法发控制帧时才需要，不该是默认 |
| 无容器缓冲区配置 | 默认抬到 64 KB 并可配 | Jakarta 默认 8 KB，超出时的报错离真正原因很远 |
| 无停机处理 | `DisposableBean` 排空 + 超时取消 | 见上文"优雅停机" |
| 无并发上限 / 无背压 | 入站队列策略 + 出站缓冲上限 | 两个方向各有一条通往 OOM 的路 |

## 配置

完整默认值见 [`resources/kudos-ability-comm-websocket-spring.yml`](resources/kudos-ability-comm-websocket-spring.yml)。

```yaml
kudos:
  ability:
    comm:
      websocket:
        spring:
          enabled: true
          path: /ws                        # 仅用于"单 handler 无端点 bean"的兜底
          # allowed-origins: []            # 留空 = 同源限制（安全默认）
          # allowed-origin-patterns: []    # 仅在 allowed-origins 为空时生效
          sock-js: false
          shutdown-timeout-millis: 10000
          inbound:
            buffer-capacity: 64
            overflow: BACKPRESSURE         # BACKPRESSURE | DROP_LATEST | CLOSE
          send:
            time-limit-millis: 10000
            buffer-size-limit-bytes: 524288
            overflow-strategy: TERMINATE   # TERMINATE | DROP
          container:
            max-text-message-buffer-size: 65536
            max-binary-message-buffer-size: 65536
            max-session-idle-timeout-millis: 0   # 0 = 沿用容器默认
            async-send-timeout-millis: 0
```

广播、分布式等引擎无关的配置在 `kudos.ability.comm.websocket.*` 下，见
[common 的 README](../kudos-ability-comm-websocket-common/README.md#自动装配与配置)。

## 模块入口

| 路径 | 角色 |
|---|---|
| `session/SpringWebSocketSession` | `KudosWebSocketSessionRef` 的 Spring 实现，额外持有 `raw` |
| `handler/KudosSpringWebSocketHandler` | Spring `WebSocketHandler` → `IKudosWebSocketHandler` 的桥接与生命周期模板 |
| `handler/ISpringWebSocketSessionFactory` | 识别位点：构造业务会话，`null` = 拒绝 |
| `handler/InboundOverflowPolicy` | 入站队列满时的策略 |
| `handler/WebSocketAbnormalCloseException` | 非正常关闭合成的 `onDisconnect` cause |
| `handshake/KudosHandshakeInterceptor` | 解析并缓存 query 参数 + 执行握手守卫 |
| `handshake/IWebSocketHandshakeGuard` | 升级前准入 SPI（`Proceed` / `Reject(status, reason)`） |
| `handshake/HandshakeAttrs` | 从 `WebSocketSession` 读 query 参数 / 握手头 |
| `init/SpringWebSocketAutoConfiguration` | 端点注册、Origin 策略、容器缓冲区、停机 |
| `init/IKudosWebSocketEndpoint` | 端点声明 SPI + `KudosWebSocketEndpoint` 便捷实现 |
| `init/SpringWebSocketProperties` | `kudos.ability.comm.websocket.spring.*` 配置 |

## 测试覆盖

共 90 个用例：75 个不启容器的单元测试（`FakeWebSocketSession` + 记录型 registry），外加 15 个跑真实内嵌 Tomcat
+ 真实握手 + 真实客户端的端到端用例。

| 测试类 | 用例 | 覆盖重点 |
|---|---|---|
| `KudosSpringWebSocketHandlerTest` | 19 | **消息保序**（200 条）、注册/注销必达、关闭后仍排空队列、二进制、Ping/Pong 不上抛、工厂拒绝与准入拒绝均在注册前、准入抛异常 fail closed、正常/异常关闭的 cause、业务抛异常仍注销、`DROP_LATEST` 不阻塞、`destroy` 排空、**BACKPRESSURE 溢出不丢消息**、`CLOSE` 策略断开、`supportsPartialMessages` 为 false、会话确实被包进并发装饰器 |
| `SpringWebSocketAutoConfigurationTest` | 20 | 端点 bean 注册、单 handler 兜底、多 handler 不猜、端点优先、Origin 默认与覆盖、pattern 仅在 origins 为空时生效、SockJS 开关、握手拦截器挂载、容器缓冲区透传、0 超时不设置、端点级 vs 全局的准入拦截器与握手守卫覆盖关系、队列/发送/停机配置确实传到 `Options` |
| `HandshakeAttrsTest` | 15 | 原始 query 解析、百分号与 `+` 解码、**值内含编码分隔符不被切错**、base64 padding、重复参数、无值 flag、空/缺失、缓存优先、握手头大小写不敏感与重复头取首值 |
| `KudosHandshakeInterceptorTest` | 7 | 无守卫放行并缓存参数、守卫拒绝带状态码、守卫抛异常 fail closed、首个拒绝生效且后续不跑、attributes 传递 |
| `SpringWebSocketSessionTest` | 8 | 文本/二进制映射、`send` 分派、关闭码与原因映射、已关闭不重复关闭、sessionId 沿用容器 id |
| `SpringWebSocketEndToEndTest` | 13 | **真实 Tomcat + 真实握手**：升级并回环、**32KB 消息证明容器缓冲区确实被抬高**、二进制往返、服务端广播打到客户端、注册中心全生命周期跟踪、正常关闭 cause 为 null、守卫拒绝是 **401 而非"开了又关"**、守卫放行、工厂拒绝以 1008 关闭且从未入注册中心、第二个端点独立挂载、**跨域 Origin 默认被 403 拒绝**、**SockJS 端点走真实 `SockJsClient` 协商**（WebSocket transport）、无会话工厂的端点接受匿名连接 |
| `IKudosWebSocketEndpointTest` | 6 | 默认工厂产出匿名会话且从不拒绝、集合默认为空（= 继承模块级 bean）、`allowedOrigins`/`sockJs` 默认为 null 而非空（"未设置"与"显式为空"必须可区分）、接口实现继承同一套默认 |
| `SpringWebSocketDisabledTest` | 2 | `enabled=false` 时装配类不加载、端点 404 而非升级 |

### 测试约定：`runBlocking` 的测试必须显式写 `: Unit`

本模块 90 个用例里有 9 个是 `fun xxx(): Unit = runBlocking { ... }` 的形式（会话收发与会话工厂都是挂起
函数）。那个 `: Unit` 不是可省的样板：

```kotlin
@Test
fun theDefaultSessionFactory_neverRejects() = runBlocking {   // ❌ 块的最后一个表达式若不是 Unit……
    assertNotNull(endpoint.sessionFactory.create(session))    // ……assertNotNull 有返回值，方法就带上了返回值
}
```

JUnit 5 遇到有返回值的 `@Test` 方法**不会报错，而是静默跳过整个方法**：测试报告里连条目都不出现，
用例数悄悄少一个，只在构建日志里留一行很容易被忽略的：

```
[WARNING] @Test method '...theDefaultSessionFactory_neverRejects()' must not return a value. It will not be executed.
```

踩中的门槛很低——`assertNotNull` / `assertIs` / `assertContentEquals` 都有返回值，随手放在最后一行就中招。
**上面那个方法名不是编的**：`IKudosWebSocketEndpointTest.theDefaultSessionFactory_neverRejects` 和
`SpringWebSocketSessionTest.send_dispatchesByPayloadKind` 就这样一直没被执行过，是排查 SockJS 问题时从
构建日志里偶然发现的。

写成 `fun theDefaultSessionFactory_neverRejects(): Unit = runBlocking { ... }` 后，编译器强制丢弃块的值，
这个问题从此不可能发生。新增此类测试时照抄这个签名即可。自查：

```bash
./gradlew test 2>&1 | grep "must not return a value"
```

## 依赖

```kotlin
api(project(":kudos-ability:kudos-ability-comm:kudos-ability-comm-websocket:kudos-ability-comm-websocket-common"))
api(libs.spring.websocket)
api(libs.spring.boot.starter.web)   // WebSocket 端点只存在于 Servlet Web 应用中

testImplementation(project(":kudos-test:kudos-test-common"))
testImplementation(libs.spring.boot.starter.webmvc.test)
```

刻意**不**依赖 `kudos-ability-web-springmvc`：本模块需要的是 Servlet + MVC 运行时，不是那个模块的
Controller、Filter 和异常处理。两者同时使用是常见组合，但耦合会让每个纯 WebSocket 服务都被迫继承
一整套它从不注册 Controller 的 HTTP 栈。

## 已知限制 / 后续工作

- ❗ **`BACKPRESSURE` 下队列满会占住容器线程**。这是有意的取舍（不丢数据），但平台线程池部署下要么
  打开虚拟线程，要么改用 `DROP_LATEST`
- ❗ **`MaxConnectionsInterceptor` 的计数是本节点的**，多实例部署下"每用户 8 条"实际是"每实例每用户 8 条"
- ❗ 没有 STOMP 支持。本模块走原生 frame + `IWebSocketMessageEncoder`；需要订阅/发布语义的话，
  Spring 自带的 `@EnableWebSocketMessageBroker` 是另一条独立的路，与本模块不共存
- ❗ SockJS 端点走的是同一套 handler，但 SockJS 自己的会话/心跳机制未做额外适配，长期只做兼容不做优化
- ❗ **SockJS 的 XHR 回退 transport 无法用进程内测试覆盖**，原因不在本模块：Spring 的
  `TransportHandlingSockJsService` 会校验 `xhr_send` 请求与该 SockJS 会话来自同一 `InetSocketAddress`，
  而 `InetSocketAddress.equals` **连端口一起比**。接收连接（流式长连接或轮询）与 `xhr_send` 必然是两条
  TCP 连接、两个源端口，于是恒定返回 404（服务端日志：`The remote address for the session and the request
  do not match.`）。JVM 侧的 `RestTemplateXhrTransport` 无法控制源端口，唯一能让它通过的办法是赌 HTTP
  连接池恰好复用同一条连接——那种测试会随机失败，不值得写。SockJS 的 **WebSocket transport** 路径已由
  `SpringWebSocketEndToEndTest` 真实覆盖；XHR 回退如需验证，用浏览器手工验
- ❗ 未做可观测性埋点（连接数 gauge、队列深度、丢弃计数）；`KudosSpringWebSocketHandler.activeConnections`
  已经暴露出来，接 Micrometer 是下一步
