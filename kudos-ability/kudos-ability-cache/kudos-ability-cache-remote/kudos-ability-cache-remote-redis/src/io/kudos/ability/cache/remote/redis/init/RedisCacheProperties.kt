package io.kudos.ability.cache.remote.redis.init

/**
 * Redis cache module properties.
 *
 * @property nodeId Node identifier used in cache-invalidation broadcasts; auto-generated as a UUID at startup if blank.
 * @property ttlJitterPercent Percentage of random spread applied to each entry's TTL; `0` (the default) disables it.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedisCacheProperties {
    var nodeId: String? = null

    /**
     * Random spread applied to every entry's TTL, as a percentage of that entry's configured TTL.
     *
     * A cache configured with a fixed TTL expires every key written in the same burst at the same instant —
     * reload traffic then arrives as one spike against the source of truth (cache avalanche). With a jitter
     * of `p`, each entry's TTL is drawn uniformly from `ttl * (1 ± p/100)`, spreading those expiries out.
     *
     * Applied per entry, not per cache: jittering a whole cache by one factor would still expire all of its
     * keys together, which is the thing being avoided.
     *
     * Defaults to `0` (exact TTLs, unchanged behaviour) because it alters when entries expire, which some
     * callers assert on. `10` is a reasonable starting point for caches with large, bursty write sets.
     * Valid range is 0..50; anything outside fails fast at startup.
     */
    var ttlJitterPercent: Int = 0
}
