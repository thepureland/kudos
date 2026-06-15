package io.kudos.context.lock

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.error.ServiceException
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for the default methods of ILeaseLockProvider / ILockProvider
 *
 * Coverage (driven through a recording fake so the interface DefaultImpls are hit directly):
 * - lockExecute(Supplier): acquired -> supplier runs, unLock in finally (also on exception)
 * - lockExecute(Supplier): not acquired + errorCode != null -> ServiceException with that errorCode
 * - lockExecute(Supplier): not acquired + errorCode == null -> null, supplier never runs
 * - lockExecute(Runnable): the same three paths, plus the exception-release path
 * - ILockProvider.order() default value and BEAN_NAME constant
 *
 * @author K
 * @since 1.0.0
 */
internal class LockProviderDefaultsTest {

    private enum class TestErrorCode(
        override val code: String,
        override val defaultDisplayText: String,
        override val i18nKeyPrefix: String
    ) : IErrorCodeEnum {
        LOCK_BUSY("9001", "lock is busy", "")
    }

    /** Recording fake: acquisition result is scripted; unLock invocations are counted. */
    private class FakeLeaseLock(private val acquirable: Boolean) : ILeaseLockProvider {
        val unlockedKeys = mutableListOf<String>()
        var lastSec: Int? = null
        override fun tryLock(lockKey: String, sec: Int): Boolean {
            lastSec = sec
            return acquirable
        }

        override fun unLock(key: String) {
            unlockedKeys += key
        }
    }

    // ============================================================
    // Supplier overload
    // ============================================================

    @Test
    fun supplierLockExecuteRunsAndReleasesOnSuccess() {
        val fake = FakeLeaseLock(acquirable = true)
        val result = fake.lockExecute<String>("k1", { "value" }, 30, null)
        assertEquals("value", result)
        assertEquals(listOf("k1"), fake.unlockedKeys, "the lock must be released exactly once")
        assertEquals(30, fake.lastSec)
    }

    @Test
    fun supplierLockExecuteReleasesEvenWhenSupplierThrows() {
        val fake = FakeLeaseLock(acquirable = true)
        assertFailsWith<IllegalStateException> {
            fake.lockExecute<String>("k2", { throw IllegalStateException("boom") }, 30, null)
        }
        assertEquals(listOf("k2"), fake.unlockedKeys, "finally must release on the exception path")
    }

    @Test
    fun supplierLockExecuteThrowsServiceExceptionWhenBusyAndErrorCodeGiven() {
        val fake = FakeLeaseLock(acquirable = false)
        val e = assertFailsWith<ServiceException> {
            fake.lockExecute<String>("k3", { "never" }, 30, TestErrorCode.LOCK_BUSY)
        }
        assertSame(TestErrorCode.LOCK_BUSY, e.errorCode)
        assertTrue(fake.unlockedKeys.isEmpty(), "a lock that was never acquired must not be released")
    }

    @Test
    fun supplierLockExecuteReturnsNullWhenBusyAndNoErrorCode() {
        val fake = FakeLeaseLock(acquirable = false)
        var ran = false
        val result = fake.lockExecute<String>("k4", { ran = true; "never" }, 30, null)
        assertNull(result)
        assertFalse(ran, "the supplier must not run when the lock is busy")
        assertTrue(fake.unlockedKeys.isEmpty())
    }

    // ============================================================
    // Runnable overload
    // ============================================================

    @Test
    fun runnableLockExecuteRunsAndReleasesOnSuccess() {
        val fake = FakeLeaseLock(acquirable = true)
        var ran = false
        fake.lockExecute("r1", Runnable { ran = true }, 15, null)
        assertTrue(ran)
        assertEquals(listOf("r1"), fake.unlockedKeys)
        assertEquals(15, fake.lastSec)
    }

    @Test
    fun runnableLockExecuteReleasesEvenWhenRunnableThrows() {
        val fake = FakeLeaseLock(acquirable = true)
        assertFailsWith<IllegalArgumentException> {
            fake.lockExecute("r2", Runnable { throw IllegalArgumentException("bad") }, 15, null)
        }
        assertEquals(listOf("r2"), fake.unlockedKeys)
    }

    @Test
    fun runnableLockExecuteThrowsServiceExceptionWhenBusyAndErrorCodeGiven() {
        val fake = FakeLeaseLock(acquirable = false)
        val e = assertFailsWith<ServiceException> {
            fake.lockExecute("r3", Runnable { error("never") }, 15, TestErrorCode.LOCK_BUSY)
        }
        assertSame(TestErrorCode.LOCK_BUSY, e.errorCode)
    }

    @Test
    fun runnableLockExecuteSilentlySkipsWhenBusyAndNoErrorCode() {
        val fake = FakeLeaseLock(acquirable = false)
        var ran = false
        fake.lockExecute("r4", Runnable { ran = true }, 15, null)
        assertFalse(ran, "the runnable must be skipped when the lock is busy and no errorCode is given")
        assertTrue(fake.unlockedKeys.isEmpty())
    }

    // ============================================================
    // ILockProvider defaults
    // ============================================================

    private class MinimalLockProvider : ILockProvider<ReentrantLock> {
        override fun tryLock(lockKey: String, sec: Int) = true
        override fun unLock(key: String) {}
        override fun lock(key: String): ReentrantLock? = null
        override fun unLock(lock: Lock, key: String) {}
    }

    @Test
    fun orderDefaultsTo99() {
        assertEquals(99, MinimalLockProvider().order())
    }

    @Test
    fun beanNameConstant() {
        assertEquals("failedLockProvider", ILockProvider.BEAN_NAME)
    }
}
