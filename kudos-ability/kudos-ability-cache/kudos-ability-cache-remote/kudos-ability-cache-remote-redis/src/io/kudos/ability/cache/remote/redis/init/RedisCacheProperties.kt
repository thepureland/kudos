package io.kudos.ability.cache.remote.redis.init

/**
 * Redis 缓存模块配置。
 *
 * @property nodeId 缓存失效广播中的节点标识；为空时启动期自动生成 UUID。
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedisCacheProperties {
    var nodeId: String? = null
}
