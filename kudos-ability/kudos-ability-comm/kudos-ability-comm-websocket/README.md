# kudos-ability-comm-websocket

WebSocket 业务层封装。一份引擎无关的内核 + 每个运行时栈一份薄适配。

| 子模块 | 角色 |
|---|---|
| [`kudos-ability-comm-websocket-common`](kudos-ability-comm-websocket-common/README.md) | **引擎无关内核**：会话注册中心 / 广播 / 业务 SPI / 连接准入 / 编解码契约 / `KudosContext` 桥接 / 分布式投递 / Spring 装配。不依赖任何 WebSocket 引擎 |
| [`kudos-ability-comm-websocket-ktor`](kudos-ability-comm-websocket-ktor/README.md) | **Ktor 适配**：`Route.kudosWebSocket` 路由扩展 + 分片重组 + 会话适配 |
| [`kudos-ability-comm-websocket-spring`](kudos-ability-comm-websocket-spring/README.md) | **Spring（Servlet / MVC）适配**：`WebSocketHandler` 桥接 + 握手守卫 + 多端点装配 |

注：协议层接入不在本目录。Ktor 侧由 `kudos-ability-web-ktor.installPlugins` 装 `WebSockets` 插件；
Spring 侧由 Servlet 容器负责升级，`-spring` 只调容器的缓冲区等参数。

## 这层封装解决什么

只用 web-ktor 或裸 `spring-websocket` 也能收发原生 frame。引入本目录是为了不必在每个服务里重写下面
这些通用逻辑——而它们各自都有不显眼的坑：

| 能力 | 不用本模块时容易踩的坑 |
|---|---|
| 按 user / tenant 维护 session 索引 | 二级索引的增删若不在 `ConcurrentHashMap.compute` 内完成，注册与注销并发时会把新 session 加进已被摘除的游离集合——连接是活的，但永远广播不到 |
| 把 frame 还原成"一条业务消息" | Ktor **不做**跨帧重组，分片消息会被当成多条投递，跨分片边界的多字节 UTF-8 直接变成乱码 |
| 阻塞回调接协程业务（Spring 侧） | 每条消息 `launch` 会丢消息顺序；每条 `runBlocking` 会占住 Tomcat 请求线程；不设上限则堆内存无界 |
| 并发发送同一连接（Spring 侧） | `WebSocketSession` 非线程安全，广播撞上业务推送会抛 `TEXT_FULL_WRITING` |
| 连接关闭时的清理 | `onDisconnect` 是 suspend 的，优雅停机取消协程后它会在第一个挂起点中止，业务清理恰好在整个集群重启时静默失效 |
| 广播给一组用户 | 不读 socket 的客户端不会让 send 失败，只会让帧在发送队列里堆到 OOM |
| 区分"正常挂断"与"人没了" | 客户端消失产生 close code 1006 且**没有** transport error，只看异常会把掉线当成正常登出 |
| 多实例下的全局广播 | 自回声、投递保序、滚动升级期的报文兼容都要自己处理 |

上述每一条都有对应实现与回归测试，共 **238 个用例**（common 117 / ktor 31 / spring 90），其中 15 个是 Spring 侧跑真实内嵌 Tomcat 的端到端用例。

## 选哪个子模块

| 场景 | 选择 |
|---|---|
| 项目主体是 Ktor（`kudos-ability-web-ktor`） | `-ktor` |
| 项目主体是 Spring MVC（`kudos-ability-web-springmvc`） | `-spring` |
| 两者都有（Ktor 做接入层、Spring 做业务层） | 两个都引，业务 handler 共用 |

`IKudosWebSocketHandler`、`KudosWebSocketRegistry`、`IWebSocketBroadcaster` 三个主要接触面完全一致，
**同一份业务 handler 可以不改一行在两侧运行**，差异只在端点/路由怎么挂。

## 单机与集群

业务侧统一依赖 `IWebSocketBroadcaster` **接口**（不是 `WebSocketBroadcaster` 具体类），
单机与集群的差别只是一个配置开关：

```yaml
kudos:
  ability:
    comm:
      websocket:
        distributed:
          enabled: true          # 打开即换成跨进程广播，调用方零改动
```

关闭时（默认）完全不接触 Redis；打开时装配 `DistributedWebSocketBroadcaster` +
`RedisWebSocketBroadcastChannel`，复用业务方已有的 `RedisMessageListenerContainer`
（`kudos-ability-cache-remote-redis` 提供的那个即可）。spring-data-redis 在 common 模块是
`compileOnly`，单实例部署不为它付出任何代价。

这个开关和引擎选择正交——`-ktor` 与 `-spring` 用的是同一个装配。

## 再加一个引擎

common 里的注册中心、广播器、handler SPI、准入拦截器都面向 `KudosWebSocketSessionRef` 抽象，关闭原因
用引擎无关的 `WebSocketCloseReason`，所以新增引擎只需两步：

1. 实现 `KudosWebSocketSessionRef`（含 `WebSocketCloseReason` → 该引擎关闭类型的映射）
2. 写一个适配器，按 `识别 → 准入 → 注册 → 消息循环 → 注销` 的模板驱动 `IKudosWebSocketHandler`

其余抽象直接复用。详见
[common README 的"新增一个引擎实现"](kudos-ability-comm-websocket-common/README.md#新增一个引擎实现)，
现成参考是 `-ktor` 的 `Route.kudosWebSocket` 和 `-spring` 的 `KudosSpringWebSocketHandler`。
