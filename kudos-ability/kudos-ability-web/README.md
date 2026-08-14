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
- **限流缺失（两套实现共有）**：组内无任何请求限流 / 并发护栏能力。springmvc 侧那对无实现的
  幽灵配置 `server.max-request-hold` / `server.max-request-exclude` **已删除**——全仓库没有任何
  读取方，留着等于对外宣传一个不存在的护栏。
- ✅ **traceKey 策略已上移到 `kudos-ability-web-common`**（`trace/TraceKeys`）：头名常量、
  校验规则（长度 + 字符集，防日志注入与响应头拆分）、按序解析与兜底生成，两种 runtime 共用一份实现，
  规则本身由 `TraceKeysTest` 单点锁定。这是该模块从空壳变为实体的第一个抽象，
  也是"出现两次以上才上移"标准的首次应用。
  springmvc 读 `X-Trace-Id` → `_UUID` 并回写公开头；ktor 默认只读 `X-Trace-Id`，
  需要延续内部 Feign 调用链时把 `Consts.RequestHeader.TRACE_KEY` 追加进 `traceKeyHeaders` 即可。
  注意 `X-Trace-Id` **不能**放进 `Consts.RequestHeader`——那里是内部头命名空间，
  有 `_` 前缀约定且被 `ConstsTest` 强制。
  **遗留**：ktor 侧仍不回写 trace 响应头，且业务若覆盖 `KudosContextPlugin.factory` 会绕过校验
  （需自行调用 `TraceKeys.resolve`）。
