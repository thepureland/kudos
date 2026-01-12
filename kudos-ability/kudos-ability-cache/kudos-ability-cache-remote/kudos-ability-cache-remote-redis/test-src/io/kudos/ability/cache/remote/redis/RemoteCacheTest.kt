package io.kudos.ability.cache.remote.redis

import io.kudos.ability.cache.common.core.MixCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.CountDownLatch
import kotlin.test.Test


/**
 * 远程缓存测试用例
 *
 * @author K
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

    @Autowired
    private lateinit var cacheTestService: CacheTestService

    @Autowired
    @Qualifier("remoteCacheManager")
    private lateinit var remoteCacheManager: CacheManager

    @Autowired
    private lateinit var mixCacheManager: MixCacheManager

    private val CACHE_NAME = "test"

    @Test
    fun testRemoteCache() {
        val remoteCache = remoteCacheManager.getCache(CACHE_NAME)
        assert(remoteCache != null)
        val mixCache = mixCacheManager.getCache(CACHE_NAME)
        assert(mixCache != null)

        val latch = CountDownLatch(1)
        Thread{
            val key = "remote_key"

            val value1 = cacheTestService.getFromDB(key)
            val value2 = cacheTestService.getFromDB(key)
            assert(value1 == value2)

            val value3 = remoteCache!!.get(key, String::class.java)
            val value4 = mixCache!!.get(key, String::class.java)
            assert(value3 == value4)
            assert(value3 == value2)

            latch.countDown()
        }.start()
        latch.await()
    }

}

