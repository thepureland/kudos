package io.kudos.ability.cache.local.caffeine.init

import io.kudos.ability.cache.local.caffeine.CaffeineHashCache

/**
 * Caffeine Hash local cache properties.
 *
 * @property maximumSize Maximum number of Hash entries retained per cacheName.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class CaffeineHashCacheProperties {
    var maximumSize: Long = CaffeineHashCache.DEFAULT_MAXIMUM_SIZE
}
