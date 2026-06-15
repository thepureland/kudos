package io.kudos.ability.cache.remote.redis.keyvalue

import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests for the batch cache annotation under the **positional-zip** key semantics
 * (see [io.kudos.ability.cache.common.batch.keyvalue.DefaultKeysGenerator]): all collection/array
 * parameters composing the key must share the same length L, producing exactly L keys; mismatched
 * lengths are rejected (the legacy cartesian-product behavior is intentionally unsupported).
 *
 * Also runs the cache access in a watchdog-guarded thread that propagates failures: the legacy
 * pattern (`Thread { ... latch.countDown() }` + `latch.await()`) hung the whole suite forever when
 * an assertion or exception fired inside the thread.
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
        runInThread { doTest() }
    }

    /** Mismatched collection/array lengths must be rejected under positional-zip semantics. */
    @Test
    fun batchLoad_mismatchedCollectionLengths_isRejected() {
        runInThread {
            val ex = assertFailsWith<IllegalStateException> {
                testCacheService.batchLoad("1", listOf(2, 3, 4), arrayOf("5", "6"), true, 7)
            }
            assertTrue(
                ex.message!!.contains("positional zip"),
                "rejection must explain the zip contract; got: ${ex.message}"
            )
        }
    }

    private fun doTest() {
        // Load and cache a single entry (key 1::2::5::7).
        val obj1 = testCacheService.load("1", 2, "5", true, 7).first()
        val cachedObj1 = testCacheService.load("1", 2, "5", true, 7).first()
        assertEquals(obj1.time, cachedObj1.time, "second single load must be served from cache")

        // Another way to load and cache a single entry (zip of one element -> key 1::2::6::7).
        val obj2 = testCacheService.batchLoad("1", listOf(2), arrayOf("6"), true, 7).values.first().first()
        val cachedObj2 = testCacheService.batchLoad("1", listOf(2), arrayOf("6"), true, 7).values.first().first()
        assertEquals(obj2.time, cachedObj2.time, "second batch load of the same key must be served from cache")

        // Batch load: equal-length columns zip into 3 keys: 1::2::5::7, 1::3::6::7, 1::4::5::7.
        val map = testCacheService.batchLoad("1", listOf(2, 3, 4), arrayOf("5", "6", "5"), true, 7)
        assertEquals(setOf("1::2::5::7", "1::3::6::7", "1::4::5::7"), map.keys)

        // 1::2::5::7 was cached by the single load above -> not freshly loaded.
        assertEquals(obj1.time, map["1::2::5::7"]!!.first().time, "previously cached entry must be reused")

        // The two missing keys are loaded in the same method call -> share one load timestamp...
        val time36 = map["1::3::6::7"]!!.first().time
        val time45 = map["1::4::5::7"]!!.first().time
        assertNotNull(time36)
        assertEquals(time36, time45, "keys missed together are loaded by one underlying call")
        // ...which is distinct from the earlier loads.
        assertNotEquals(obj1.time, time36)
        assertNotEquals(obj2.time, time36)

        // A repeated batch is now fully cached: same values come back without a fresh load.
        val again = testCacheService.batchLoad("1", listOf(2, 3, 4), arrayOf("5", "6", "5"), true, 7)
        assertEquals(obj1.time, again["1::2::5::7"]!!.first().time)
        assertEquals(time36, again["1::3::6::7"]!!.first().time)
        assertEquals(time45, again["1::4::5::7"]!!.first().time)
    }

    /**
     * Runs [block] on a separate thread (cache access must not run on the test worker thread in
     * these scenarios), propagating any failure and never hanging: join with timeout + rethrow.
     */
    private fun runInThread(block: () -> Unit) {
        val error = AtomicReference<Throwable?>()
        val thread = Thread { try { block() } catch (t: Throwable) { error.set(t) } }
        thread.start()
        thread.join(120_000)
        if (thread.isAlive) {
            thread.interrupt()
            fail("test thread did not finish within 120s")
        }
        error.get()?.let { throw it }
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry?) {
            RedisTestContainer.startIfNeeded(registry)
        }
    }

}
