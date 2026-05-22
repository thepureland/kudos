package io.kudos.ability.cache.local.caffeine.init

import io.kudos.ability.cache.local.caffeine.CaffeineHashCache

/**
 * Caffeine Hash 本地缓存配置。
 *
 * @property maximumSize 单个 cacheName 下最多保留的 Hash 实体数量。
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class CaffeineHashCacheProperties {
    var maximumSize: Long = CaffeineHashCache.DEFAULT_MAXIMUM_SIZE
}
