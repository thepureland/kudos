# kudos-ability-comm-websocket-ktor

WebSocket 通信封装（Ktor 端）。**当前为占位模块**——无任何源码，仅 `build.gradle.kts`
声明了 Ktor `WebSockets` 服务端依赖。

## 设计意图

预留给"业务层 WebSocket 抽象"，区别于 `kudos-ability-web-ktor`：
- `kudos-ability-web-ktor` 提供的是 **Ktor 服务端整合**（含 WebSockets 插件装配），属于"基础设施层"
- 本模块面向 **业务层语义**：连接生命周期管理、按用户/租户的会话索引、广播 / 单播抽象、
  心跳策略、消息编解码协议契约……让业务方不必每个服务都重写一套这些通用代码

目前以上抽象都未实现。

## 当前文件

```
build.gradle.kts
```

无源码。

## build.gradle.kts 的问题

```kotlin
dependencies {
    api(project(":kudos-context"))
    api("io.ktor:ktor-server-websockets-jvm")   // ⚠ 见下方
}
```

❗ **直接写 Maven coordinate 字符串而非走 `libs.versions.toml` 目录**——与项目里其他
所有模块的依赖声明风格不一致。后果：

1. 没有显式版本——依赖某个传递依赖（Ktor BOM）提供版本，BOM 不可用时构建会拿到不可预期
   的版本甚至失败。
2. 升级 Ktor 时这条依赖不会被 catalog 集中升级，容易遗漏。
3. 与 `kudos-ability-web-ktor/build.gradle.kts` 里 `api(libs.ktor.server.websockets)` 不一致。

补正建议（开始写代码时一并做）：把这条依赖移到 `libs.versions.toml`，引用 `libs.ktor.server.websockets`。

## 与 `kudos-ability-web-ktor` 的关系

`kudos-ability-web-ktor.installPlugins` 已经在装配阶段 `install(WebSockets)`（按
`kudos.ability.web.ktor.plugins.web-socket.enabled` 开关）。所以业务侧目前完全可以在
**只依赖 web-ktor 的情况下**写 `routing { webSocket("/echo") { ... } }`，不需要本模块。

本模块**未来**的价值是：在 Ktor 原生 `webSocket { ... }` 之上封装连接管理 / 业务消息
协议层，而不是重复 Ktor 已经做了的"装上 WebSocket 插件"那一步。

## 模块入口

无源码。仅 `build.gradle.kts` 透传依赖。

## 已知限制 / 后续工作

- ❗ 主源码空白；模块仅在依赖图里占位
- ❗ build.gradle.kts 的 Ktor 依赖应改走 `libs.versions.toml` catalog，与项目其他模块统一
- ❗ 设计意图与 `kudos-ability-web-ktor` 的边界目前不清晰——开始写代码前需要先定义本模块
  的具体抽象层（连接管理 / 业务协议 / 广播 SPI 等），否则容易和 web-ktor 重复
- ❗ 没有"WebSocket netty 实现"等并行选项；如果未来想支持非 Ktor 引擎，需要先抽象出
  `comm-websocket-common`（类似 cache / file 的 common 模块），目前只有 ktor 一条路

## 依赖

```kotlin
dependencies {
    api(project(":kudos-context"))
    api("io.ktor:ktor-server-websockets-jvm")
}
```
