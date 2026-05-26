package io.kudos.ability.cache.local.caffeine.hash

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.ability.cache.local.caffeine.keyvalue.TestCacheConfigProvider
import org.springframework.context.annotation.Primary

/**
 * Provides config for Hash cache tests: extends [TestCacheConfigProvider] with the hash cache "testHash".
 * Marked @Primary so CacheKit / MixHashCacheManager use this config in tests (including active and writeInTime).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Primary
class HashTestCacheConfigProvider : ICacheConfigProvider {

    private val delegate = TestCacheConfigProvider()

    private val hashCacheConfig = CacheConfig().apply {
        name = "testHash"
        strategyDictCode = CacheStrategy.SINGLE_LOCAL.name
        strategy = CacheStrategy.SINGLE_LOCAL.name
        ttl = Int.MAX_VALUE
        hash = true
        active = true
        writeInTime = true
    }

    override fun getCacheConfig(name: String): CacheConfig {
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
