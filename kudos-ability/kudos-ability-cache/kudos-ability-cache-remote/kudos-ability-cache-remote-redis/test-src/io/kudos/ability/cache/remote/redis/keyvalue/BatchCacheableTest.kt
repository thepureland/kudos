package io.kudos.ability.cache.remote.redis.keyvalue

import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.CountDownLatch
import kotlin.test.Test

/**
 * Tests for the batch cache annotation.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest
@Import(BatchCacheableTestService::class, TestCacheConfigProvider::class)
@EnabledIfDockerInstalled
class BatchCacheableTest {

    @Autowired
    private lateinit var testCacheService: BatchCacheableTestService

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
        // Load and cache a single entry.
        val obj1 = testCacheService.load("1", 2, "5", true, 7).first()
        val cachedObj1 = testCacheService.load("1", 2, "5", true, 7).first()
        assert(obj1.time == cachedObj1.time)

        // Another way to load and cache a single entry.
        val obj2 = testCacheService.batchLoad("1", listOf(2), arrayOf("6"), true, 7).values.first().first()
        val cachedObj2 = testCacheService.batchLoad("1", listOf(2), arrayOf("6"), true, 7).values.first().first()
        assert(obj2.time == cachedObj2.time)

        // Batch load.
        val map = testCacheService.batchLoad("1", listOf(2, 3, 4), arrayOf("5", "6"), true, 7)
        assert(map.size == 6)
        assert(map["1::2::5::7"]!!.first().time == obj1.time) // Not freshly loaded; previously cached.
        assert(map["1::2::6::7"]!!.first().time == obj2.time) // Not freshly loaded; previously cached.
        assert(map["1::2::6::7"]!!.first().time != map["1::3::6::7"]!!.first().time)
        assert(map["1::3::6::7"]!!.first().time == map["1::4::5::7"]!!.first().time)
        assert(map["1::4::5::7"]!!.first().time == map["1::3::5::7"]!!.first().time)
        assert(map["1::3::5::7"]!!.first().time == map["1::4::6::7"]!!.first().time)
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            RedisTestContainer.startIfNeeded(registry)
        }
    }

}

