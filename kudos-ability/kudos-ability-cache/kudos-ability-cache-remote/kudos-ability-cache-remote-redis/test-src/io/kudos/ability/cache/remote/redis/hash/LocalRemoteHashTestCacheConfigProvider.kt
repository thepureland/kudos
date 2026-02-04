package io.kudos.ability.cache.remote.redis.hash

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.ability.cache.remote.redis.keyvalue.TestCacheConfigProvider

/**
 * 为 Hash 两级缓存（本地+远程）测试提供配置：在 [TestCacheConfigProvider] 基础上增加 hash 缓存 "testHash"（LOCAL_REMOTE 策略）。
 *
 * @author K
 * @since 1.0.0
 */
class LocalRemoteHashTestCacheConfigProvider : ICacheConfigProvider {

    private val delegate = TestCacheConfigProvider()

    private val hashCacheConfig = CacheConfig().apply {
        name = "testHash"
        strategyDictCode = CacheStrategy.LOCAL_REMOTE.name
        strategy = CacheStrategy.LOCAL_REMOTE.name
        ttl = Int.MAX_VALUE
        hash = true
    }

    override fun getCacheConfig(name: String): CacheConfig? {
        if (name == "testHash") return hashCacheConfig
        return delegate.getCacheConfig(name)
    }

    override fun getAllCacheConfigs(): Map<String, CacheConfig> {
        return delegate.getAllCacheConfigs().toMutableMap().apply { put("testHash", hashCacheConfig) }
    }

    override fun getLocalCacheConfigs(): Map<String, CacheConfig> = delegate.getLocalCacheConfigs()
    override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = delegate.getRemoteCacheConfigs()

    override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> {
        return delegate.getLocalRemoteCacheConfigs().toMutableMap().apply { put("testHash", hashCacheConfig) }
    }

    override fun getHashCacheConfigs(): Map<String, CacheConfig> {
        return mapOf("testHash" to hashCacheConfig)
    }
}
