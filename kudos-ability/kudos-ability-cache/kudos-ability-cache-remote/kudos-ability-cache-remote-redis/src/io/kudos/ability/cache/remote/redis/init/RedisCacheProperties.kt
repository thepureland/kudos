package io.kudos.ability.cache.remote.redis.init

/**
 * Redis cache module properties.
 *
 * @property nodeId Node identifier used in cache-invalidation broadcasts; auto-generated as a UUID at startup if blank.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedisCacheProperties {
    var nodeId: String? = null
}
