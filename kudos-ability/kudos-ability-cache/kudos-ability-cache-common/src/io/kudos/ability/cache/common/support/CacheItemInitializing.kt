package io.kudos.ability.cache.common.support


/**
 * 缓存项初始化
 */
interface CacheItemInitializing {
    /**
     * 需要初始化的缓存项
     *
     * @param cacheConfigMap
     */
    fun initCacheAfterSystemInit(cacheConfigMap: Map<String, CacheConfig>)
}
