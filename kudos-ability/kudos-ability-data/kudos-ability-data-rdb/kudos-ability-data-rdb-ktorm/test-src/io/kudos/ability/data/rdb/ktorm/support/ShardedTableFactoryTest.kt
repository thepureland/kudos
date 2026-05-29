package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.schema.Table
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [ShardedTableFactory]. Cover the contract: per-key caching, distinct instances
 * for distinct keys, and the "factory invoked at most once per key" guarantee even under
 * concurrent first-touch from multiple threads.
 */
internal class ShardedTableFactoryTest {

    @Test
    fun get_returnsCachedInstanceForSameKey() {
        val factory = ShardedTableFactory(::StubTable)

        val a1 = factory.get("audit_log_2026q1")
        val a2 = factory.get("audit_log_2026q1")

        assertSame(a1, a2, "second call with the same physical name must return the cached instance — otherwise every query allocates a new Table and re-runs bindTo")
        assertEquals("audit_log_2026q1", a1.tableName)
    }

    @Test
    fun get_returnsDistinctInstancesForDistinctKeys() {
        val factory = ShardedTableFactory(::StubTable)

        val q1 = factory.get("audit_log_2026q1")
        val q2 = factory.get("audit_log_2026q2")

        assertEquals("audit_log_2026q1", q1.tableName)
        assertEquals("audit_log_2026q2", q2.tableName)
        assertTrue(q1 !== q2, "distinct physical names must produce distinct Table instances")
    }

    @Test
    fun knownNames_reflectsCachedKeys() {
        val factory = ShardedTableFactory(::StubTable)

        factory.get("a")
        factory.get("b")
        factory.get("a") // duplicate access; should not introduce a second key

        assertEquals(setOf("a", "b"), factory.knownNames())
    }

    @Test
    fun clear_dropsAllCachedInstances() {
        val factory = ShardedTableFactory(::StubTable)
        factory.get("x")
        factory.get("y")

        factory.clear()

        assertEquals(emptySet(), factory.knownNames())
    }

    @Test
    fun concurrentFirstTouch_invokesFactoryAtMostOncePerKey() {
        val factoryCallCount = AtomicInteger(0)
        val factory = ShardedTableFactory<StubTable> { name ->
            factoryCallCount.incrementAndGet()
            StubTable(name)
        }

        val pool = Executors.newFixedThreadPool(8)
        val barrier = CountDownLatch(1)
        val threadCount = 8
        val results = mutableListOf<StubTable>()
        val resultsLock = Any()

        repeat(threadCount) {
            pool.submit {
                barrier.await()
                val table = factory.get("contended-key")
                synchronized(resultsLock) { results += table }
            }
        }
        barrier.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "executor must finish under 5s")

        assertEquals(1, factoryCallCount.get(),
            "ConcurrentHashMap.computeIfAbsent must serialize per-key, so the factory runs exactly once even under contention")
        assertEquals(threadCount, results.size)
        val first = results.first()
        assertTrue(results.all { it === first }, "every concurrent caller must see the same cached instance")
    }

    /** Minimal Ktorm Table subclass used for assertions. No columns are needed — only `tableName`. */
    private class StubTable(name: String) : Table<Nothing>(name)
}
