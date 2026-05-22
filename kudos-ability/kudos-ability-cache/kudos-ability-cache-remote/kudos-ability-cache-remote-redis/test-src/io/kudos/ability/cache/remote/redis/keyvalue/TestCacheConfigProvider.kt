package io.kudos.ability.cache.remote.redis.keyvalue

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider

/**
 * 远程 K-V 缓存测试配置提供者。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class TestCacheConfigProvider: ICacheConfigProvider {

    private val CACHE_NAME = "test"

    private val cacheConfig = CacheConfig().apply {
        name = "test"
        strategyDictCode = CacheStrategy.REMOTE.name
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
