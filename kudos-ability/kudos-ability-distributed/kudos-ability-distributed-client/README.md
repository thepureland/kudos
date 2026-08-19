# kudos-ability-distributed-client

跨服务**客户端**调用封装。

| 子模块 | 实现 |
|---|---|
| [`kudos-ability-distributed-client-http`](kudos-ability-distributed-client-http/README.md) | Spring Interface Clients（`@HttpExchange` + `RestClient`）：上下文自动透传 + 统一降级 + Seata XID 扩展点 |

预留位置给未来其他客户端形式（gRPC / Dubbo 等）。

## 关于已删除的 `kudos-ability-distributed-client-feign`

2026-08-19 全仓从 OpenFeign 迁移到 interface client，该模块与 `spring-cloud-starter-openfeign`
依赖一并删除。迁移动因与做法记在
[`kudos-ability-distributed-client-http`](kudos-ability-distributed-client-http/README.md)，
逐模块的改动记在各 `*-client` 模块自己的 README。要点：

- OpenFeign 官方已 feature-complete（只修 bug 不加特性），并建议迁移到 Spring HTTP Service Clients；
- 原本挡路的两个缺口——`lb://` 服务发现与降级——在 Spring Cloud 2025.1 已由
  `LoadBalancerRestClientHttpServiceGroupConfigurer` 和 `@HttpServiceFallback` 补齐；
- 路径注解写在 `-common` 的共享接口上被服务端与客户端同时继承，而 `HttpServiceProxyFactory`
  只认 `@HttpExchange` 家族，所以两种传输**无法在同一份契约上共存**，每个模块都是一次性切换。
