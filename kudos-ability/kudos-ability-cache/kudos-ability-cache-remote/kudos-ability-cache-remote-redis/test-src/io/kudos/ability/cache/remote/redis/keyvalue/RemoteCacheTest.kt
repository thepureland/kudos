package io.kudos.ability.cache.remote.redis.keyvalue

import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.cache.get
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail


/**
 * Remote cache test cases.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest
@Import(CacheTestService::class, TestCacheConfigProvider::class)
@EnabledIfDockerInstalled
internal class RemoteCacheTest {

    companion object {

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("kudos.cache.config.strategy") { CacheStrategy.REMOTE.name }
            RedisTestContainer.startIfNeeded(registry)
        }

    }

    @Resource
    private lateinit var cacheTestService: CacheTestService

    @Autowired
    @Qualifier("remoteCacheManager")
    private lateinit var remoteCacheManager: CacheManager

    @Resource
    private lateinit var mixCacheManager: MixCacheManager

    private val CACHE_NAME = "test"

    @Test
    fun testRemoteCache() {
        val remoteCache = remoteCacheManager.getCache(CACHE_NAME)
        assert(remoteCache != null)
        val mixCache = mixCacheManager.getCache(CACHE_NAME)
        assert(mixCache != null)

        // Run on a separate thread, propagating failures with a join timeout: the legacy
        // latch.await() pattern hung the suite forever when an assertion fired inside the thread.
        val error = AtomicReference<Throwable?>()
        val thread = Thread {
            try {
                val key = "remote_key"

                val value1 = cacheTestService.getFromDB(key)
                val value2 = cacheTestService.getFromDB(key)
                assert(value1 == value2)

                val value3 = remoteCache!!.get<String>(key)
                val value4 = mixCache!!.get<String>(key)
                assert(value3 == value4)
                assert(value3 == value2)
            } catch (t: Throwable) {
                error.set(t)
            }
        }
        thread.start()
        thread.join(120_000)
        if (thread.isAlive) {
            thread.interrupt()
            fail("test thread did not finish within 120s")
        }
        error.get()?.let { throw it }
    }

    @Test
    fun testExistsKey() {
        assertFalse(KeyValueCacheKit.existsKey(CACHE_NAME, "non_existent_key"))

        val key = "exists_remote_key"
        cacheTestService.getFromDB(key)
        assertTrue(KeyValueCacheKit.existsKey(CACHE_NAME, key))
    }

}

