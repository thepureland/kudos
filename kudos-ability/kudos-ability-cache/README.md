# kudos-ability-cache

缓存能力主题——多级缓存框架 + 本地 / 远程 / 跨服务实现。

| 子目录 | 内容 |
|---|---|
| [`kudos-ability-cache-common`](kudos-ability-cache-common/README.md) | 核心：`MixCache` 二级缓存抽象、`@TenantCacheable` 注解 + 切面、跨节点失效广播 SPI |
| [`kudos-ability-cache-local`](kudos-ability-cache-local/README.md) | 本地缓存（Caffeine） |
| [`kudos-ability-cache-remote`](kudos-ability-cache-remote/README.md) | 远程缓存（Redis） |
| [`kudos-ability-cache-interservice`](kudos-ability-cache-interservice/README.md) | 跨服务缓存协作（Feign 协商缓存：client/provider/common 三件套） |

业务侧典型组合：`cache-common` + `cache-local-caffeine` + `cache-remote-redis`（开启
LOCAL_REMOTE 二级缓存）。

## 改进建议（自动分析 2026-06-11）

以下为本次深度审查中发现、但不宜直接修改（涉及行为/接口/设计决策）的事项，按维度分类。

### 功能缺陷 / 待补功能
- **TTL 随机化（雪崩防护）缺失**：`kudos-ability-cache-remote-redis/src/io/kudos/ability/cache/remote/redis/RedisKeyValueCacheManager.kt` 的 `createCache` 按配置固定 TTL；大量同时写入的 key 会同时过期。建议增加 `ttl-jitter`（如 ±10%）配置项。
- **读路径无单飞（击穿防护）**：`kudos-ability-cache-common/src/io/kudos/ability/cache/common/core/keyvalue/MixCache.kt` 的 `mixGetOrLoad` 与 `batch/keyvalue/BatchCacheableAspect.kt` 的 `readCachedData`（源码中已有 TODO）在两级都未命中时直接回源，无 per-key 互斥；目前需业务自觉叠加 `@DistributedCacheGuard`。建议在 `mixGetOrLoad` 内提供可选的本地单飞（per-key `CompletableFuture` 合并）。
- **缓存 null 无法与未命中区分**：`kudos-ability-cache-common/src/io/kudos/ability/cache/common/aop/keyvalue/DistributedCacheGuardAspect.kt` 通过 `KeyValueCacheKit.getValue(...) != null` 判定命中，合法的 null 结果每次都会加分布式锁并回源（穿透）。建议结合 `CacheValueWrapper` 存 sentinel 值。

### 可扩展性 / 对外接口
- **`IKeyValueCacheManager` 命名契约不统一**：`evictByPattern` / `existsKey` 对 `cacheName` 参数，Caffeine 实现期望"版本前缀后的真实名"，Redis 实现期望"逻辑名"（内部自行加前缀），调用方（`MixCacheManager`）需逐处记忆。建议在 `kudos-ability-cache-common/src/io/kudos/ability/cache/common/core/keyvalue/IKeyValueCacheManager.kt` 的 KDoc 中明确契约并统一为逻辑名。

### 可观测性
- **无命中率指标**：`MixCache` 的 get/put/evict 全链路无 Micrometer 埋点，Caffeine 的 `recordStats` 也未强制开启或暴露。线上无法回答"某缓存命中率多少、回源 QPS 多少"。建议在 `MixCache`（`kudos-ability-cache-common/.../core/keyvalue/MixCache.kt`）统一埋 hit/miss/load 计数器，并把 Caffeine/Redis 原生统计接入 `CacheMetrics`。
- **广播热路径重复扫描容器**：`MixCache.pushMsgRedis` 每次写都调 `SpringKit.getBeansOfType<ICacheMessageHandler>()` 全量扫描；`HashCacheKit` 已对 handler 做过惰性索引，建议同样 memoize（注意测试上下文重置）。

### 健壮性
- **`CacheOperatorVo.doNotify` 的兜底不完整**：`kudos-ability-cache-common/src/io/kudos/ability/cache/common/notify/CacheOperatorVo.kt` 只捕获 `NoSuchBeanDefinitionException`；若 `notifyTool.notify` 抛出其它异常，本地清理 fallback 不会执行且异常直接抛入业务线程，与 KDoc 声称的"发送失败则本地清理"不符。建议扩大 catch 范围并记录日志。
