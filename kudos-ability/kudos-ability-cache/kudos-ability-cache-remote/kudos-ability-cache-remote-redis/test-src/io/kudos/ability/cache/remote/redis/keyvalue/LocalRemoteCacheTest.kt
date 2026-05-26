package io.kudos.ability.cache.remote.redis.keyvalue

import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * Mix-cache (two-level: local + remote) test cases.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest
@Import(CacheTestService::class, TestCacheConfigProvider::class)
@EnabledIfDockerInstalled
internal class LocalRemoteCacheTest {

    companion object {

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("kudos.cache.config.strategy") { CacheStrategy.LOCAL_REMOTE.name }
            RedisTestContainer.startIfNeeded(registry)
        }

    }

    @Resource
    private lateinit var cacheTestService: CacheTestService

    @Autowired
    @Qualifier("localCacheManager")
    private lateinit var localCacheManager: CacheManager

    @Autowired
    @Qualifier("remoteCacheManager")
    private lateinit var remoteCacheManager: CacheManager

    @Resource
    private lateinit var mixCacheManager: MixCacheManager

    @Resource
    private lateinit var versionConfig: CacheVersionConfig

    private val CACHE_NAME = "test"

    @Test
    fun testRemoteCache() {
        val localCache = localCacheManager.getCache(CACHE_NAME)
        assert(localCache != null)
        val remoteCache = remoteCacheManager.getCache(CACHE_NAME)
        assert(remoteCache != null)
        val mixCache = mixCacheManager.getCache(CACHE_NAME)
        assert(mixCache != null)

        val latch = CountDownLatch(1)
        Thread {
            val key = "local_remote_key"

            val value1 = cacheTestService.getData(key)
            val value2 = cacheTestService.getData(key)
            assert(value1 == value2)

            val value3 = localCache!!.get(key, String::class.java)
            val value4 = remoteCache!!.get(key, String::class.java)
            val value5 = mixCache!!.get(key, String::class.java)
            assert(value3 == value4)
            assert(value4 == value5)
            assert(value3 == value2)

            latch.countDown()
        }.start()
        latch.await()
    }

    @Test
    fun testExistsKey() {
        assertFalse(KeyValueCacheKit.existsKey(CACHE_NAME, "non_existent_key"))

        val key = "exists_local_remote_key"
        cacheTestService.getData(key)
        assertTrue(KeyValueCacheKit.existsKey(CACHE_NAME, key))

        val keyLocalOnly = "exists_after_clear_remote_key"
        cacheTestService.getData(keyLocalOnly)
        mixCacheManager.clearLocal(CACHE_NAME, keyLocalOnly)
        assertTrue(KeyValueCacheKit.existsKey(CACHE_NAME, keyLocalOnly))

        val keyRemoteOnly = "exists_remote_only_key"
        val realName = versionConfig.getFinalCacheName(CACHE_NAME)
        remoteCacheManager.getCache(realName)!!.put(keyRemoteOnly, "remote_only_value")
        assertTrue(KeyValueCacheKit.existsKey(CACHE_NAME, keyRemoteOnly))
    }

    /**
     * Verifies that a cached null value (the cache-penetration marker) hits the local layer and is not
     * mistakenly treated as a miss that falls through to remote.
     * Approach: insert null locally and a different value remotely; mixCache.get must return the local null
     * (a wrapper is present but its value is null).
     */
    @Test
    fun testCachedNullHitsLocal() {
        val key = "cached_null_key"
        val realName = versionConfig.getFinalCacheName(CACHE_NAME)
        val localCache = localCacheManager.getCache(realName)!!
        val remoteCache = remoteCacheManager.getCache(realName)!!

        localCache.put(key, null)
        remoteCache.put(key, "should_not_be_returned")

        val mixCache = mixCacheManager.getCache(CACHE_NAME)!!
        val wrapper = mixCache.get(key)
        assertTrue(wrapper != null, "Local cached null: wrapper must not be null (hit)")
        assertTrue(wrapper.get() == null, "Locally cached null must be returned as-is and not overridden by the remote value")
    }

}

