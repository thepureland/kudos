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
