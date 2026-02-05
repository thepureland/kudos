package io.kudos.ability.cache.local.caffeine.keyvalue

import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.CountDownLatch
import kotlin.test.Test


/**
 * 本地缓存测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@Import(CacheTestService::class, TestCacheConfigProvider::class)
internal class LocalCacheTest {

    @Autowired
    private lateinit var cacheTestService: CacheTestService

    @Autowired
    @Qualifier("localCacheManager")
    private lateinit var localCacheManager: CacheManager

    @Autowired
    @Qualifier("mixCacheManager")
    private lateinit var mixCacheManager: MixCacheManager

    private val CACHE_NAME = "test"

    companion object {

        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("cache.config.strategy") { CacheStrategy.SINGLE_LOCAL.name }
        }

    }

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

