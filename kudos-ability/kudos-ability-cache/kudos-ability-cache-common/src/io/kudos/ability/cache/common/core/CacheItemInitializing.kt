package io.kudos.ability.cache.common.core

import io.kudos.ability.cache.common.support.CacheConfig

/**
 * 缓存项初始化
 *
 * @author K
 * @since 1.0.0
 */
interface CacheItemInitializing {
    /**
     * 需要初始化的缓存项
     *
     * @param cacheConfigMap
     */
    fun initCacheAfterSystemInit(cacheConfigMap: Map<String, CacheConfig>)
}