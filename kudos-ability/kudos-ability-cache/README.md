# kudos-ability-cache

缓存能力主题——多级缓存框架 + 本地 / 远程 / 跨服务实现。

| 子目录 | 内容 |
|---|---|
| [`kudos-ability-cache-common`](kudos-ability-cache-common/README.md) | 核心：`MixCache` 二级缓存抽象、`@TenantCacheable` 注解 + 切面、跨节点失效广播 SPI |
| [`kudos-ability-cache-local`](kudos-ability-cache-local/README.md) | 本地缓存（Caffeine） |
| [`kudos-ability-cache-remote`](kudos-ability-cache-remote/README.md) | 远程缓存（Redis） |
| [`kudos-ability-cache-interservice`](kudos-ability-cache-interservice/README.md) | 跨服务缓存协作（占位） |

业务侧典型组合：`cache-common` + `cache-local-caffeine` + `cache-remote-redis`（开启
LOCAL_REMOTE 二级缓存）。
