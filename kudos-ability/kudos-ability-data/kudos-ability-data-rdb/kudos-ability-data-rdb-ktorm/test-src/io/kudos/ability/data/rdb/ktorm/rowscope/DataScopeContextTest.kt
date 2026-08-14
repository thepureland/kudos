package io.kudos.ability.data.rdb.ktorm.rowscope

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * junit test for [DataScopeContext] — where system authority reaches, and where it deliberately
 * stops.
 *
 * The stopping is the part worth testing. System authority is the one marker that turns row
 * filtering off entirely, so a thread that keeps it by accident reads every tenant's data for the
 * rest of its life. These tests pin both halves: the marker does not leak onto a pooled thread on
 * its own, and it does cross when the caller says so.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class DataScopeContextTest {

    private val pool = Executors.newFixedThreadPool(1)

    @AfterTest
    fun tearDown() {
        pool.shutdownNow()
        pool.awaitTermination(5, TimeUnit.SECONDS)
    }

    @Test
    fun authorityAppliesInsideTheBlockAndNotAfterIt() {
        assertFalse(DataScopeContext.isSystem())
        DataScopeContext.runAsSystem { assertTrue(DataScopeContext.isSystem()) }
        assertFalse(DataScopeContext.isSystem(), "the marker must not outlive its block")
    }

    @Test
    fun nestedBlocksDoNotClearTheMarkerEarly() {
        DataScopeContext.runAsSystem {
            DataScopeContext.runAsSystem { assertTrue(DataScopeContext.isSystem()) }
            assertTrue(DataScopeContext.isSystem(), "the inner block's exit must not release the outer one")
        }
        assertFalse(DataScopeContext.isSystem())
    }

    @Test
    fun theMarkerIsReleasedEvenWhenTheBlockThrows() {
        runCatching { DataScopeContext.runAsSystem { throw IllegalStateException("boom") } }
        assertFalse(DataScopeContext.isSystem(), "a throwing job must not leave system authority behind")
    }

    @Test
    fun authorityDoesNotCrossToAnotherThreadOnItsOwn() {
        // The whole reason this is a plain ThreadLocal. An InheritableThreadLocal would hand system
        // authority to any thread *created* inside the block — for a pool, permanently — and that
        // failure is silent, unreproducible, and reads every tenant's rows.
        val seen = DataScopeContext.runAsSystem {
            pool.submit(Callable { DataScopeContext.isSystem() }).get()
        }
        assertFalse(seen, "system authority must not reach another thread unless it was wrapped")
    }

    @Test
    fun wrapCarriesAuthorityToAnotherThread() {
        val seen = DataScopeContext.runAsSystem {
            pool.submit(DataScopeContext.wrap(Callable { DataScopeContext.isSystem() })).get()
        }
        assertTrue(seen, "wrap() is how a fanning-out job takes its authority with it")
    }

    @Test
    fun wrapLeavesNothingBehindOnThePooledThread() {
        DataScopeContext.runAsSystem {
            pool.submit(DataScopeContext.wrap(Runnable { })).get()
        }
        // Same worker thread, next task: it must be back to no authority.
        val leaked = pool.submit(Callable { DataScopeContext.isSystem() }).get()
        assertFalse(leaked, "a wrapped task must release the authority it borrowed")
    }

    @Test
    fun wrappingOutsideASystemBlockGrantsNothing() {
        val seen = pool.submit(DataScopeContext.wrap(Callable { DataScopeContext.isSystem() })).get()
        assertFalse(seen, "wrap() propagates authority, it does not create it")
    }
}
