package io.kudos.ability.cache.remote.redis

import io.kudos.test.common.EnableKudosTest
import io.kudos.test.common.TestSpringBootContextLoader
import org.junit.jupiter.api.Test
import org.soul.ability.cache.common.MixCacheManager
import org.soul.ability.cache.common.enums.CacheStrategy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.util.concurrent.CountDownLatch


/**
 * 混合缓存(两级缓存: 本地+远程)测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@Import(CacheTestService::class, TestCacheConfigProvider::class)
@ContextConfiguration(loader = LocalRemoteCacheTest.LocalRemoteCacheContextLoader::class)
internal class LocalRemoteCacheTest {

    @Autowired
    private lateinit var cacheTestService: CacheTestService

    @Autowired
    @Qualifier("soulLocalCacheManager")
    private lateinit var localCacheManager: CacheManager

    @Autowired
    @Qualifier("soulRemoteCacheManager")
    private lateinit var remoteCacheManager: CacheManager

    @Autowired
    private lateinit var mixCacheManager: MixCacheManager

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

    class LocalRemoteCacheContextLoader : TestSpringBootContextLoader() {

        override fun getDynamicProperties(): Map<String, String> {
            return mapOf(
                "kudos.ability.cache.enabled" to "true",
                "kudos.cache.config.strategy" to CacheStrategy.LOCAL_REMOTE.name
            )
        }

    }

}

