# kudos-ability-comm-websocket

WebSocket 业务层封装的实现集合。

| 子模块 | 实现 |
|---|---|
| [`kudos-ability-comm-websocket-ktor`](kudos-ability-comm-websocket-ktor/README.md) | Ktor 端业务层抽象（会话注册中心 / 广播 / 编解码 SPI / 分布式投递） |

注：基础 WebSocket 协议接入已经在 `kudos-ability-web-ktor.installPlugins` 里通过 ktor
`WebSockets` 插件覆盖；本目录提供"业务层抽象"（连接管理 / 会话索引 / 广播 SPI），
插件装配仍由 web-ktor 负责，本模块不重复装配。
