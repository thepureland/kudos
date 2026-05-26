package io.kudos.context.core

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for KudosContextHolder.
 *
 * Coverage:
 * - [KudosContextHolder.get] auto-creation semantics ("lazy initialization", as documented)
 * - [KudosContextHolder.getOrNull] returns null when uninitialized (opposite of get)
 * - [KudosContextHolder.clear] clears the ThreadLocal
 * - `InheritableThreadLocal` propagation to child threads
 * - Isolation between threads
 *
 * @author K
 * @since 1.0.0
 */
internal class KudosContextHolderTest {

    @BeforeTest
    fun resetBefore() = KudosContextHolder.clear()

    @AfterTest
    fun resetAfter() = KudosContextHolder.clear()

    // ============================================================
    // get vs getOrNull semantic differences
    // ============================================================

    @Test
    fun getOrNullReturnsNullWhenUninitialized() {
        assertNull(KudosContextHolder.getOrNull(), "getOrNull should be null before set")
    }

    @Test
    fun getCreatesContextOnFirstCallAndCachesIt() {
        // KDoc is explicit: get() auto-creates and writes to ThreadLocal when uninitialized
        val first = KudosContextHolder.get()
        assertNotNull(first)
        val second = KudosContextHolder.get()
        assertSame(first, second, "the second call should return the same instance (cached)")
    }

    @Test
    fun getOrNullReturnsExistingAfterGetCreatesIt() {
        val created = KudosContextHolder.get()
        val read = KudosContextHolder.getOrNull()
        assertSame(created, read, "after get() creates it, getOrNull should read it back")
    }

    // ============================================================
    // set / clear
    // ============================================================

    @Test
    fun setReplacesCurrentContext() {
        val first = KudosContextHolder.get()
        val replacement = KudosContext()
        KudosContextHolder.set(replacement)
        assertSame(replacement, KudosContextHolder.getOrNull())
        assertNotSame(first, KudosContextHolder.getOrNull())
    }

    @Test
    fun clearRemovesContext() {
        KudosContextHolder.get()
        assertNotNull(KudosContextHolder.getOrNull())
        KudosContextHolder.clear()
        assertNull(KudosContextHolder.getOrNull(), "after clear, the state should be uninitialized")
    }

    // ============================================================
    // Cross-thread behavior: InheritableThreadLocal should propagate to child threads
    // ============================================================

    @Test
    fun contextPropagatesToChildThread() {
        val parentCtx = KudosContextHolder.get()
        val childCtx = AtomicReference<KudosContext?>()

        // Use `new Thread` directly so InheritableThreadLocal copies on creation
        val t = Thread {
            childCtx.set(KudosContextHolder.getOrNull())
        }
        t.start()
        t.join(2000)

        assertSame(parentCtx, childCtx.get(), "InheritableThreadLocal should propagate the parent thread's context to the child thread")
    }

    @Test
    fun contextIsIsolatedAcrossThreadsThatDontInherit() {
        // Use an existing thread reused by ExecutorService: the context will not be propagated
        val pool = Executors.newSingleThreadExecutor()
        // Force the pool's worker thread to be created first (the main-thread ThreadLocal is still empty at this point)
        pool.submit { /* warmup */ }.get(1, TimeUnit.SECONDS)

        // Now the main thread creates the context
        val parentCtx = KudosContextHolder.get()
        val workerCtx = AtomicReference<KudosContext?>()
        val done = CountDownLatch(1)
        pool.submit {
            workerCtx.set(KudosContextHolder.getOrNull())
            done.countDown()
        }
        assertTrue(done.await(2, TimeUnit.SECONDS))
        pool.shutdown()

        // Key point: the worker thread already exists (created during warmup), and its InheritableThreadLocal
        // did not inherit the value set later by the main thread. This is a well-known pitfall — in pool scenarios, propagation must be explicit.
        assertNotSame(
            parentCtx,
            workerCtx.get(),
            "an already-existing worker in a thread pool will not automatically see context set later by the main thread"
        )
    }

    @Test
    fun clearOnOneThreadDoesNotAffectAnother() {
        val mainCtx = KudosContextHolder.get()
        val otherSeen = AtomicReference<KudosContext?>()
        val phase1 = CountDownLatch(1)
        val phase2 = CountDownLatch(1)

        val t = Thread {
            // Child thread inherits mainCtx
            val inherited = KudosContextHolder.getOrNull()
            otherSeen.set(inherited)
            phase1.countDown()
            // Wait until the main thread has cleared, then check: the child thread's reference must not be cleared
            assertTrue(phase2.await(2, TimeUnit.SECONDS))
            val afterMainClear = KudosContextHolder.getOrNull()
            assertSame(mainCtx, afterMainClear, "clear on the main thread should not affect the child thread")
        }
        t.start()
        assertTrue(phase1.await(2, TimeUnit.SECONDS))
        KudosContextHolder.clear()
        assertNull(KudosContextHolder.getOrNull(), "main thread cleared")
        phase2.countDown()
        t.join(2000)

        assertSame(mainCtx, otherSeen.get())
    }
}
