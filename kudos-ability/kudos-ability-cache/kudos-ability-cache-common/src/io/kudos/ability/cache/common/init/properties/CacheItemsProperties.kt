package io.kudos.ability.cache.common.init.properties

import io.kudos.ability.cache.common.support.CacheConfig

/**
 * Cache items configuration container. Supports two configuration forms:
 *
 * 1. `kudos.ability.cache.cache-items[]`: each entry is a query-string-style string.
 * 2. `kudos.ability.cache.cache-item-configs[]`: each entry is a structured [CacheConfig] object.
 *
 * ```yaml
 * kudos:
 *   ability:
 *     cache:
 *       cache-items:
 *         - name=USER_CACHE&strategy=LOCAL_REMOTE&ttl=900
 *         - name=DEMO&strategy=REMOTE&ttl=1800&writeOnBoot=true
 *       cache-item-configs:
 *         - name: ORDER_CACHE
 *           strategy: LOCAL_REMOTE
 *           ttl: 900
 * ```
 *
 * String parsing is implemented in [io.kudos.ability.cache.common.support.DefaultCacheConfigProvider.cacheItemToConfig]:
 * split tokens by `&`, split key/value by `=`, then reflectively set fields on `CacheConfig`.
 *
 * **Design trade-off**: a flat string is used rather than a nested yml object so that long lists stay
 * compact and diff-friendly in yml; the cost is losing IDE field hints and compile-time validation, so
 * parsing throws on unknown fields. For new configurations, prefer [cacheItemConfigs]; [cacheItems] is
 * retained for backward compatibility with existing yml.
 *
 * @property cacheItems list of strings, each of the form `name=X&strategy=Y&...`
 * @property cacheItemConfigs structured list of cache items, each bound to [CacheConfig] fields
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class CacheItemsProperties {
    var cacheItems: MutableList<String> = mutableListOf()
    var cacheItemConfigs: MutableList<CacheConfig> = mutableListOf()
}
