package io.kudos.ability.cache.remote.redis.hash

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.ability.cache.remote.redis.keyvalue.TestCacheConfigProvider

/**
 * 为 Hash 缓存测试提供配置：在 [TestCacheConfigProvider] 基础上增加 hash 缓存 "testHash"（REMOTE 策略）。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
class HashTestCacheConfigProvider : ICacheConfigProvider {

    private val delegate = TestCacheConfigProvider()

    private val hashCacheConfig = CacheConfig().apply {
        name = "testHash"
        strategyDictCode = CacheStrategy.REMOTE.name
        strategy = CacheStrategy.REMOTE.name
        ttl = Int.MAX_VALUE
        hash = true
        active = true
        writeInTime = true
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
    override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = delegate.getLocalRemoteCacheConfigs()

    override fun getHashCacheConfigs(): Map<String, CacheConfig> {
        return mapOf("testHash" to hashCacheConfig)
    }
}
