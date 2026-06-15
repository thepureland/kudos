package io.kudos.ability.cache.common.support

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the process-level [CacheCleanRegister].
 *
 * Coverage:
 *  - register + getCleanListener returns the registered listeners in order.
 *  - multiple listeners under the same key accumulate.
 *  - unknown key returns null.
 *  - thread-safety: concurrent registration of N listeners under one key never loses an entry
 *    (the class explicitly documents COW + ConcurrentHashMap thread-safety).
 *
 * @author K
 * @since 1.0.0
 */
internal class CacheCleanRegisterTest {

    private class NoopListener : ICacheCleanListener {
        override fun cleanCache(cacheName: String, key: Any?) {}
        override fun afterPropertiesSet() {}
    }

    @Test
    fun register_thenGet_returnsListener() {
        val key = "k_${System.nanoTime()}"
        val l = NoopListener()
        CacheCleanRegister.register(key, l)
        val listeners = CacheCleanRegister.getCleanListener(key)
        assertEquals(1, listeners?.size)
        assertTrue(listeners!!.contains(l))
    }

    @Test
    fun register_multipleUnderSameKey_accumulate() {
        val key = "k_${System.nanoTime()}"
        val a = NoopListener()
        val b = NoopListener()
        CacheCleanRegister.register(key, a)
        CacheCleanRegister.register(key, b)
        val listeners = CacheCleanRegister.getCleanListener(key)!!
        assertEquals(2, listeners.size)
        // CopyOnWriteArrayList preserves insertion order.
        assertEquals(listOf(a, b), listeners.toList())
    }

    @Test
    fun getCleanListener_unknownKey_returnsNull() {
        assertNull(CacheCleanRegister.getCleanListener("absent_${System.nanoTime()}"))
    }

    @Test
    fun register_isThreadSafe_underConcurrency() {
        val key = "concurrent_${System.nanoTime()}"
        val threads = 16
        val perThread = 50
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        try {
            repeat(threads) {
                pool.submit {
                    start.await()
                    repeat(perThread) { CacheCleanRegister.register(key, NoopListener()) }
                    done.countDown()
                }
            }
            start.countDown()
            assertTrue(done.await(30, TimeUnit.SECONDS), "all registration threads should finish")
        } finally {
            pool.shutdownNow()
        }
        assertEquals(threads * perThread, CacheCleanRegister.getCleanListener(key)?.size,
            "no registrations should be lost under concurrency")
    }
}
