package io.kudos.ability.cache.remote.redis.hash

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.ability.cache.remote.redis.keyvalue.TestCacheConfigProvider

/**
 * Provides configuration for two-level hash cache tests (local + remote): extends [TestCacheConfigProvider]
 * by adding the hash caches "testHash" and "testHashWithTime" (LOCAL_REMOTE strategy).
 * TestRow and TestRowWithTime belong to separate caches to avoid type-conversion errors caused by mixing them in one map.
 *
 * @author K
 * @author AI: Codex
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
        active = true
        writeInTime = true
    }

    private val hashWithTimeCacheConfig = CacheConfig().apply {
        name = "testHashWithTime"
        strategyDictCode = CacheStrategy.LOCAL_REMOTE.name
        strategy = CacheStrategy.LOCAL_REMOTE.name
        ttl = Int.MAX_VALUE
        hash = true
        active = true
        writeInTime = true
    }

    override fun getCacheConfig(name: String): CacheConfig {
        if (name == "testHash") return hashCacheConfig
        if (name == "testHashWithTime") return hashWithTimeCacheConfig
        return delegate.getCacheConfig(name)
    }

    override fun getAllCacheConfigs(): Map<String, CacheConfig> {
        return delegate.getAllCacheConfigs().toMutableMap().apply {
            put("testHash", hashCacheConfig)
            put("testHashWithTime", hashWithTimeCacheConfig)
        }
    }

    override fun getLocalCacheConfigs(): Map<String, CacheConfig> = delegate.getLocalCacheConfigs()
    override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = delegate.getRemoteCacheConfigs()

    override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> {
        return delegate.getLocalRemoteCacheConfigs().toMutableMap().apply {
            put("testHash", hashCacheConfig)
            put("testHashWithTime", hashWithTimeCacheConfig)
        }
    }

    override fun getHashCacheConfigs(): Map<String, CacheConfig> {
        return mapOf(
            "testHash" to hashCacheConfig,
            "testHashWithTime" to hashWithTimeCacheConfig
        )
    }
}
