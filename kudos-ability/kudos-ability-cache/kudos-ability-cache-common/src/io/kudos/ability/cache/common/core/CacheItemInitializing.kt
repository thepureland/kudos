package io.kudos.ability.cache.common.core

import io.kudos.ability.cache.common.support.CacheConfig

/**
 * Cache item initialization.
 *
 * @author K
 * @since 1.0.0
 */
interface CacheItemInitializing {
    /**
     * Cache items that need to be initialized.
     *
     * @param cacheConfigMap
     */
    fun initCacheAfterSystemInit(cacheConfigMap: Map<String, CacheConfig>)
}