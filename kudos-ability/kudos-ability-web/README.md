# kudos-ability-web

Web 服务端能力主题。

| 子目录 | 内容 |
|---|---|
| [`kudos-ability-web-common`](kudos-ability-web-common/README.md) | 共享 base（占位） |
| [`kudos-ability-web-springmvc`](kudos-ability-web-springmvc/README.md) | Spring MVC：请求生命周期 + 响应包装 + 异常映射 + CRUD Controller 基类 + Tomcat/Jetty 切换 |
| [`kudos-ability-web-ktor`](kudos-ability-web-ktor/README.md) | Ktor 服务端：4 种引擎切换 + IKtorRouteRegistrar SPI + 插件配置 |
| [`kudos-ability-web-swagger`](kudos-ability-web-swagger/README.md) | OpenAPI 3.0 文档（基于 springdoc-openapi 3.x，仅 Spring MVC） |
| [`kudos-ability-web-guest`](kudos-ability-web-guest/README.md) | 匿名访客识别 + 在线统计：AES Cookie + 两段式校验 + Redis 存储 + 4 SPI |

业务侧二选一：传统 SpringMVC（servlet 同步）或 Ktor（协程异步）。

## 改进建议（自动分析 2026-06-11）

跨模块的组级问题（单模块问题见各子模块 README 的同名章节）：

- **双实现抽象不对齐**：springmvc 拥有统一响应包装（`GlobalResponseBodyHandler`）、统一异常
  映射（两层 advice）、上下文装配（`WebContextInitFilter`，含 IP/UA/traceKey 解析），而 ktor
  侧只有 StatusPages 兜底 + 仅设 traceKey 的 `KudosContextPlugin`。业务从 springmvc 迁移到
  ktor 会丢失 `ApiResponse` 包装与 ClientInfo 语义。建议把"响应包装契约、trace header 名、
  ClientInfo 装配规则"等框架无关抽象上移到 `kudos-ability-web-common`（当前为空壳占位），
  两套实现各自落地同一契约。
- **可观测性短板（两套实现共有）**：均无访问日志、慢请求日志、Micrometer 指标（QPS / 延迟 /
  错误率）。建议在 web-common 定义观测口径与配置项，springmvc 用 Filter、ktor 用
  `CallLogging`/自定义插件分别实现。
- **限流缺失（两套实现共有）**：组内无任何请求限流 / 并发护栏能力；springmvc 的
  `server.max-request-hold` 配置是无实现的幽灵配置（详见 springmvc README）。
- **traceKey header 名不一致**：springmvc 读 `Consts.RequestHeader.TRACE_KEY`，ktor 默认
  factory 读硬编码的 `X-Trace-Id`——同一套业务在两种 runtime 下 trace 透传行为不同，应统一
  到共享常量（`kudos-ability-web-ktor/src/.../plugins/KudosContextPlugin.kt`）。
