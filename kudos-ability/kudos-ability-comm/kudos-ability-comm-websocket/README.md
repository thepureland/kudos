# kudos-ability-comm-websocket

WebSocket 业务层封装的实现集合。

| 子模块 | 实现 |
|---|---|
| [`kudos-ability-comm-websocket-ktor`](kudos-ability-comm-websocket-ktor/README.md) | Ktor 端（占位 / 空源码） |

注：基础 WebSocket 协议接入已经在 `kudos-ability-web-ktor.installPlugins` 里通过 ktor
`WebSockets` 插件覆盖；本目录预留给"业务层抽象"（连接管理 / 会话索引 / 广播 SPI），
和 web-ktor 的边界仍待划定。
