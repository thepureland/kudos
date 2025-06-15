package io.kudos.ability.cache.local.caffeine

import org.soul.ability.cache.common.enums.CacheStrategy
import org.soul.ability.cache.common.support.CacheConfig
import org.soul.ability.cache.common.support.ICacheConfigProvider


/**
 * 模拟缓存配置信息提供者
 *
 * @author K
 * @since 1.0.0
 */
class TestCacheConfigProvider: ICacheConfigProvider {

    private val CACHE_NAME = "test"

    private val cacheConfig = CacheConfig().apply {
        name = "test"
        strategyDictCode = CacheStrategy.SINGLE_LOCAL.name
        ttl = Int.MAX_VALUE
    }

    override fun getCacheConfig(name: String): CacheConfig {
        return cacheConfig
    }

    override fun getAllCacheConfigs(): Map<String, CacheConfig> {
        return mapOf(CACHE_NAME to cacheConfig)
    }

    override fun getLocalCacheConfigs(): Map<String, CacheConfig> {
        return mapOf(CACHE_NAME to cacheConfig)
    }

    override fun getRemoteCacheConfigs(): Map<String, CacheConfig> {
        return mapOf(CACHE_NAME to cacheConfig)
    }

    override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> {
        return mapOf(CACHE_NAME to cacheConfig)
    }

}