# kudos-ability-comm-websocket

WebSocket 业务层封装的实现集合。

| 子模块 | 实现 |
|---|---|
| [`kudos-ability-comm-websocket-ktor`](kudos-ability-comm-websocket-ktor/README.md) | Ktor 端业务层抽象（会话注册中心 / 广播 / 编解码 SPI / 分布式投递 / Spring 自动装配） |

注：基础 WebSocket 协议接入已经在 `kudos-ability-web-ktor.installPlugins` 里通过 ktor
`WebSockets` 插件覆盖；本目录提供"业务层抽象"（连接管理 / 会话索引 / 广播 SPI），
插件装配仍由 web-ktor 负责，本模块不重复装配。

## 这层封装解决什么

只用 web-ktor 也能写 `webSocket("/echo") { ... }` 收发原生 frame。引入本模块是为了不必在每个
服务里重写下面这些通用逻辑——而它们各自都有不显眼的坑：

| 能力 | 不用本模块时容易踩的坑 |
|---|---|
| 按 user / tenant 维护 session 索引 | 二级索引的增删若不在 `ConcurrentHashMap.compute` 内完成，注册与注销并发时会把新 session 加进已被摘除的游离集合——连接是活的，但永远广播不到 |
| 把 frame 还原成"一条业务消息" | Ktor **不做**跨帧重组，分片消息会被当成多条投递，跨分片边界的多字节 UTF-8 直接变成乱码 |
| 连接关闭时的清理 | `onDisconnect` 是 suspend 的，优雅停机取消协程后它会在第一个挂起点中止，业务清理恰好在整个集群重启时静默失效 |
| 广播给一组用户 | 不读 socket 的客户端不会让 send 失败，只会让帧在发送队列里堆到 OOM |
| 多实例下的全局广播 | 自回声、投递保序、滚动升级期的报文兼容都要自己处理 |

上述每一条在子模块里都有对应的实现与回归测试，详见
[子模块 README 的"设计要点"](kudos-ability-comm-websocket-ktor/README.md#设计要点)。

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
（`kudos-ability-cache-remote-redis` 提供的那个即可）。spring-data-redis 在本模块是
`compileOnly`，单实例部署不为它付出任何代价。

完整配置项、SPI 契约、分布式语义与已知限制见
[`kudos-ability-comm-websocket-ktor/README.md`](kudos-ability-comm-websocket-ktor/README.md)。

## 换非 Ktor 引擎

注册中心、广播器、handler SPI 都面向 `KudosWebSocketSessionRef` 抽象，不依赖 Ktor 类型。
若将来新增基于 netty 等引擎的实现，新建平级子模块并提供一份 `KudosWebSocketSessionRef`
实现即可，本目录其余抽象可直接复用。
