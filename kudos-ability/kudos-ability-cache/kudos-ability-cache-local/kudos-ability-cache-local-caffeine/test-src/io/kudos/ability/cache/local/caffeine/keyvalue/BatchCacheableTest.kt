package io.kudos.ability.cache.local.caffeine.keyvalue

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.local.caffeine.keyvalue.TestCacheConfigProvider
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.CountDownLatch
import kotlin.test.Test

/**
 * 批量缓存注解测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@Import(BatchCacheableTestService::class, TestCacheConfigProvider::class)
class BatchCacheableTest {

    @Autowired
    private lateinit var testCacheService: BatchCacheableTestService

    companion object {

        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "true" }
            registry.add("cache.config.strategy") { CacheStrategy.SINGLE_LOCAL.name }
        }

    }

    @Test
    fun test() {
        val latch = CountDownLatch(1)
        Thread {
            doTest()
            latch.countDown()
        }.start()
        latch.await()
    }

    private fun doTest() {
        // 加载并缓存单条数据
        val obj1 = testCacheService.load("1", 2, "5", true, 7).first()
        val cachedObj1 = testCacheService.load("1", 2, "5", true, 7).first()
        assert(obj1.time == cachedObj1.time)

        // 加载并缓存单条数据的另一种方式
        val obj2 = testCacheService.batchLoad("1", listOf(2), arrayOf("6"), true, 7).values.first().first()
        val cachedObj2 = testCacheService.batchLoad("1", listOf(2), arrayOf("6"), true, 7).values.first().first()
        assert(obj2.time == cachedObj2.time)

        // 批量加载
        val map = testCacheService.batchLoad("1", listOf(2, 3, 4), arrayOf("5", "6"), true, 7)
        assert(map.size == 6)
        assert(map["1::2::5::7"]!!.first().time == obj1.time) // 不是刚加载的，之前已缓存过
        assert(map["1::2::6::7"]!!.first().time == obj2.time) // 不是刚加载的，之前已缓存过
        assert(map["1::2::6::7"]!!.first().time != map["1::3::6::7"]!!.first().time)
        assert(map["1::3::6::7"]!!.first().time == map["1::4::5::7"]!!.first().time)
        assert(map["1::4::5::7"]!!.first().time == map["1::3::5::7"]!!.first().time)
        assert(map["1::3::5::7"]!!.first().time == map["1::4::6::7"]!!.first().time)
    }

}

