package io.kudos.ability.cache.local.caffeine

import io.kudos.test.common.EnableKudosTest
import org.junit.jupiter.api.Test
import org.soul.ability.cache.common.MixCacheManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.test.context.ContextConfiguration
import java.util.concurrent.CountDownLatch


/**
 * 本地缓存测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@ContextConfiguration(loader = LocalCacheTestContextLoader::class)
internal class LocalCacheTest {

    @Autowired
    private lateinit var cacheTestService: CacheTestService

    @Autowired
    @Qualifier("soulLocalCacheManager")
    private lateinit var localCacheManager: CacheManager

    @Autowired
    @Qualifier("cacheManager")
    private lateinit var mixCacheManager: MixCacheManager

    private val CACHE_NAME = "test"

    @Test
    fun testLocalCache() {
        val localCache = localCacheManager.getCache(CACHE_NAME)
        assert(localCache != null)
        val mixCache = mixCacheManager.getCache(CACHE_NAME)
        assert(mixCache != null)

        val latch = CountDownLatch(1)
        Thread{
            val key = "local_key"

            val value1 = cacheTestService.getFromDB(key)
            val value2 = cacheTestService.getFromDB(key)
            assert(value1 === value2)

            val value3 = localCache!!.get(key, String::class.java)
            val value4 = mixCache!!.get(key, String::class.java)
            assert(value3 === value4)
            assert(value3 === value2)

            latch.countDown()
        }.start()
        latch.await()
    }



}

