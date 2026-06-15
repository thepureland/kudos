package io.kudos.ability.cache.local.caffeine.keyvalue

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Test cases for the batch cache annotation.
 *
 * Covers (under positional-zip key semantics, see DefaultKeysGenerator):
 * - single-entry load via @Cacheable then cache hit
 * - single-entry load via @BatchCacheable then cache hit
 * - batch zip load with partial cache hits (cached entries keep their original load time,
 *   missing entries are loaded in one underlying call and share the same load time)
 * - extra entries returned by the underlying method are cached via putIfAbsent
 * - mismatched collection/array lengths are rejected with IllegalStateException
 *
 * @author K
 * @author AI: Codex
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
        // Run in a separate thread (legacy pattern), but propagate any failure back to the JUnit
        // thread; otherwise an exception inside the thread would leave the latch un-counted and
        // hang the test forever.
        var failure: Throwable? = null
        val latch = CountDownLatch(1)
        Thread {
            try {
                doTest()
            } catch (t: Throwable) {
                failure = t
            } finally {
                latch.countDown()
            }
        }.start()
        latch.await()
        failure?.let { throw it }
    }

    private fun doTest() {
        // Load and cache a single entry
        val obj1 = testCacheService.load("1", 2, "5", true, 7).first()
        val cachedObj1 = testCacheService.load("1", 2, "5", true, 7).first()
        assert(obj1.time == cachedObj1.time)

        // Another way to load and cache a single entry
        val obj2 = testCacheService.batchLoad("1", listOf(2), arrayOf("6"), true, 7).values.first().first()
        val cachedObj2 = testCacheService.batchLoad("1", listOf(2), arrayOf("6"), true, 7).values.first().first()
        assert(obj2.time == cachedObj2.time)

        // Batch load with positional-zip keys: ages and names must have the same length; the i-th
        // key is built from the i-th element of each collection ("1::2::5::7", "1::2::6::7",
        // "1::3::6::7", "1::4::5::7").
        val map = testCacheService.batchLoad("1", listOf(2, 2, 3, 4), arrayOf("5", "6", "6", "5"), true, 7)
        assert(map.size == 4)
        assert(map["1::2::5::7"]!!.first().time == obj1.time) // not freshly loaded; already cached previously
        assert(map["1::2::6::7"]!!.first().time == obj2.time) // not freshly loaded; already cached previously
        // Both missing entries are loaded by one underlying call and stamped with the same time
        assert(map["1::3::6::7"]!!.first().time == map["1::4::5::7"]!!.first().time)
        assert(map["1::3::6::7"]!!.first().time != obj1.time)
        assert(map["1::3::6::7"]!!.first().time != obj2.time)

        // The underlying method returned extra combinations (filter uses `in` semantics); they were
        // cached via putIfAbsent, so loading one of them now is a pure cache hit with the same time.
        val extra = testCacheService.batchLoad("1", listOf(3), arrayOf("5"), true, 7)
        assert(extra["1::3::5::7"]!!.first().time == map["1::3::6::7"]!!.first().time)
    }

    @Test
    fun batchLoad_rejectsMismatchedCollectionLengths() {
        // Positional zip: collection/array parameters of differing lengths are rejected, a
        // cartesian product is intentionally not supported.
        val ex = assertFailsWith<IllegalStateException> {
            testCacheService.batchLoad("1", listOf(2, 3, 4), arrayOf("5", "6"), true, 7)
        }
        assertTrue(ex.message!!.contains("positional zip"))
    }

}
