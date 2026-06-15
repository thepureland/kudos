package io.kudos.context.lock

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.error.ServiceException
import io.kudos.context.kit.SpringKit
import org.springframework.context.support.StaticApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for LockTool
 *
 * Coverage:
 * - The lazy provider resolution path (no ILockProvider bean in the context -> NormalLockService fallback)
 * - [LockTool.lockProvider] returns the same instance on every access (lazy caching)
 * - [LockTool.hasKeyLock] negated tryLock semantics (deprecated but still shipped)
 * - All four lockExecute overloads: Supplier/Runnable x default-lease/explicit-lease
 * - The failure path throwing [ServiceException] carrying the given errorCode
 *
 * Strategy: LockTool resolves its provider from [SpringKit] on first access. The test installs a
 * StaticApplicationContext (containing no ILockProvider bean) before touching LockTool so the resolution
 * always succeeds with the in-process fallback regardless of test ordering, then restores the previous
 * context to avoid polluting Spring Boot tests in the same JVM.
 *
 * @author K
 * @since 1.0.0
 */
internal class LockToolTest {

    private enum class TestErrorCode(
        override val code: String,
        override val defaultDisplayText: String,
        override val i18nKeyPrefix: String
    ) : IErrorCodeEnum {
        BUSY("9002", "resource busy", "")
    }

    private var previousContext: org.springframework.context.ApplicationContext? = null
    private var ownContext: StaticApplicationContext? = null

    @BeforeTest
    fun installStaticContext() {
        previousContext = runCatching { SpringKit.applicationContext }.getOrNull()
        ownContext = StaticApplicationContext().also {
            it.refresh()
            SpringKit.applicationContext = it
        }
    }

    @AfterTest
    fun restoreContext() {
        SpringKit.applicationContext = previousContext
        ownContext?.close()
    }

    /** Unique key per call so cases never interfere, regardless of execution order. */
    private fun uniqueKey(prefix: String) = "$prefix-${System.nanoTime()}"

    @Test
    fun lockProviderFallsBackToNormalLockServiceAndIsCached() {
        val provider = LockTool.lockProvider
        assertNotNull(provider)
        assertTrue(provider is NormalLockService, "without an ILockProvider bean the fallback should be NormalLockService")
        assertSame(provider, LockTool.lockProvider, "the lazy provider must be cached")
    }

    @Suppress("DEPRECATION")
    @Test
    fun hasKeyLockIsNegationOfTryLock() {
        val key = uniqueKey("hk")
        // First call acquires the lock -> tryLock true -> hasKeyLock false
        assertFalse(LockTool.hasKeyLock(key, 60))
        // Second call: lock already held -> tryLock false -> hasKeyLock true
        assertTrue(LockTool.hasKeyLock(key, 60))
        LockTool.lockProvider.unLock(key)
    }

    @Test
    fun supplierLockExecuteWithExplicitLeaseReturnsValueAndReleases() {
        val key = uniqueKey("sup")
        val result = LockTool.lockExecute<String>(key, { "computed" }, 30, TestErrorCode.BUSY)
        assertEquals("computed", result)
        assertTrue(LockTool.lockProvider.tryLock(key, 1), "the lock should have been released after execution")
        LockTool.lockProvider.unLock(key)
    }

    @Test
    fun supplierLockExecuteWithDefaultLeaseDelegates() {
        val key = uniqueKey("sup90")
        val result = LockTool.lockExecute<Int>(key, { 7 }, TestErrorCode.BUSY)
        assertEquals(7, result)
    }

    @Test
    fun runnableLockExecuteWithExplicitLeaseRunsAndReleases() {
        val key = uniqueKey("run")
        val ran = AtomicBoolean(false)
        LockTool.lockExecute(key, Runnable { ran.set(true) }, 30, TestErrorCode.BUSY)
        assertTrue(ran.get())
        assertTrue(LockTool.lockProvider.tryLock(key, 1), "the lock should have been released after execution")
        LockTool.lockProvider.unLock(key)
    }

    @Test
    fun runnableLockExecuteWithDefaultLeaseDelegates() {
        val key = uniqueKey("run90")
        val ran = AtomicBoolean(false)
        LockTool.lockExecute(key, Runnable { ran.set(true) }, TestErrorCode.BUSY)
        assertTrue(ran.get())
    }

    @Test
    fun lockExecuteThrowsServiceExceptionWhenKeyAlreadyHeld() {
        val key = uniqueKey("busy")
        assertTrue(LockTool.lockProvider.tryLock(key, 60), "pre-acquire to simulate contention")
        try {
            val e = assertFailsWith<ServiceException> {
                LockTool.lockExecute<String>(key, { "never" }, 30, TestErrorCode.BUSY)
            }
            assertSame(TestErrorCode.BUSY, e.errorCode)

            val e2 = assertFailsWith<ServiceException> {
                LockTool.lockExecute(key, Runnable { error("never") }, 30, TestErrorCode.BUSY)
            }
            assertSame(TestErrorCode.BUSY, e2.errorCode)
        } finally {
            LockTool.lockProvider.unLock(key)
        }
    }
}
