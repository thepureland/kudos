package io.kudos.ability.cache.remote.redis

import io.kudos.test.common.EnableKudosTest
import io.kudos.test.common.container.RedisTestContainer
import org.junit.jupiter.api.Test
import org.soul.ability.cache.common.MixCacheManager
import org.soul.ability.cache.common.enums.CacheStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.CountDownLatch


/**
 * 远程缓存测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@Import(CacheTestService::class, TestCacheConfigProvider::class)
@Testcontainers(disabledWithoutDocker = true)
internal class RemoteCacheTest {

    companion object {

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("kudos.cache.config.strategy") { CacheStrategy.REMOTE.name }
            RedisTestContainer.start(registry)
        }

    }

    @Autowired
    private lateinit var cacheTestService: CacheTestService

    @Autowired
    @Qualifier("soulRemoteCacheManager")
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

