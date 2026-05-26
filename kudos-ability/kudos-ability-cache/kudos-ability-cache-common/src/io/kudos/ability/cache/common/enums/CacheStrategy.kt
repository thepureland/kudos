package io.kudos.ability.cache.common.enums

/**
 * Cache strategy. Decides where a single cache entry resides: local only / remote only / two-tier linked.
 *
 * Read by `MixCacheManager` at wiring time to select the concrete `Cache` implementation (Caffeine / Redis / Mix).
 * Invalidation broadcasts also rely on this enum: only [LOCAL_REMOTE] publishes cross-node invalidate messages.
 *
 * @author K
 * @since 1.0.0
 */
enum class CacheStrategy {
    /** Single-node local cache (e.g. Caffeine). Cross-node sync must be triggered explicitly via MQ + `CacheNotifyListener`. */
    SINGLE_LOCAL,

    /** Remote cache only (e.g. Redis), no local copy. All nodes read/write the same instance directly. */
    REMOTE,

    /** Two-tier linked: local first, fall back to remote on miss; writes broadcast invalidation so other nodes evict their local copy. */
    LOCAL_REMOTE,
}
