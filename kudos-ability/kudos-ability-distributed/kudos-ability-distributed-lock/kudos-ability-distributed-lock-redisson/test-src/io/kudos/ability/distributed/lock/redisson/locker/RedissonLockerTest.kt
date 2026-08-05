package io.kudos.ability.distributed.lock.redisson.locker

import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [RedissonLocker] covering: getLock, the bounded default lock, lease-based
 * lock overloads, tryLock success/failure/interruption, unlock guards (by key and by RLock)
 * and the fail-fast behavior when no RedissonClient is injected.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class RedissonLockerTest {

    @Test
    fun getLock_returnsLockFromClientWithoutAcquiring() {
        val lock = RecordingRLock(tryLockResult = true)
        val locker = lockerWith(lock)

        val result = locker.getLock("order:0")

        assertSame(lock.proxy, result)
        assertEquals(0, lock.tryLockCalls)
        assertEquals(0, lock.blockingLockCalls)
    }

    @Test
    fun lock_usesBoundedTryLockInsteadOfBlockingLock() {
        val lock = RecordingRLock(tryLockResult = true)
        val locker = lockerWith(lock)

        val result = locker.lock("order:1")

        assertSame(lock.proxy, result)
        assertEquals(1, lock.tryLockCalls)
        assertEquals(0, lock.blockingLockCalls)
        assertEquals(RedissonLocker.DEFAULT_WAIT_SECONDS, lock.lastWaitTime)
        assertEquals(RedissonLocker.DEFAULT_LEASE_SECONDS, lock.lastLeaseTime)
        assertEquals(TimeUnit.SECONDS, lock.lastUnit)
    }

    @Test
    fun lock_returnsNullWhenDefaultWaitTimesOut() {
        val lock = RecordingRLock(tryLockResult = false)
        val locker = lockerWith(lock)

        assertNull(locker.lock("order:2"))
        assertEquals(1, lock.tryLockCalls)
        assertEquals(0, lock.blockingLockCalls)
    }

    @Test
    fun lock_returnsNullAndRestoresInterruptFlagWhenInterrupted() {
        val lock = RecordingRLock(tryLockResult = true, interruptOnTryLock = true)
        val locker = lockerWith(lock)

        val result = locker.lock("order:3")

        assertNull(result)
        // The interrupt flag must be restored; Thread.interrupted() also clears it for later tests.
        assertTrue(Thread.interrupted())
    }

    @Test
    fun lock_withTimeoutSeconds_callsBlockingLockWithLease() {
        val lock = RecordingRLock(tryLockResult = true)
        val locker = lockerWith(lock)

        val result = locker.lock("order:4", 17L)

        assertSame(lock.proxy, result)
        assertEquals(1, lock.blockingLockCalls)
        assertEquals(17L, lock.lastLeaseTime)
        assertEquals(TimeUnit.SECONDS, lock.lastUnit)
    }

    @Test
    fun lock_withTimeoutAndUnit_callsBlockingLockWithLease() {
        val lock = RecordingRLock(tryLockResult = true)
        val locker = lockerWith(lock)

        val result = locker.lock("order:5", TimeUnit.MILLISECONDS, 250L)

        assertSame(lock.proxy, result)
        assertEquals(1, lock.blockingLockCalls)
        assertEquals(250L, lock.lastLeaseTime)
        assertEquals(TimeUnit.MILLISECONDS, lock.lastUnit)
    }

    @Test
    fun tryLock_passesWaitLeaseAndUnitAndReturnsResult() {
        val lock = RecordingRLock(tryLockResult = true)
        val locker = lockerWith(lock)

        assertTrue(locker.tryLock("order:6", TimeUnit.MINUTES, 1L, 2L))
        assertEquals(1, lock.tryLockCalls)
        assertEquals(1L, lock.lastWaitTime)
        assertEquals(2L, lock.lastLeaseTime)
        assertEquals(TimeUnit.MINUTES, lock.lastUnit)
    }

    @Test
    fun tryLock_returnsFalseWhenLockNotAvailable() {
        val lock = RecordingRLock(tryLockResult = false)
        val locker = lockerWith(lock)

        assertFalse(locker.tryLock("order:7", TimeUnit.SECONDS, 0L, 5L))
    }

    @Test
    fun tryLock_returnsFalseAndRestoresInterruptFlagWhenInterrupted() {
        val lock = RecordingRLock(tryLockResult = true, interruptOnTryLock = true)
        val locker = lockerWith(lock)

        assertFalse(locker.tryLock("order:8", TimeUnit.SECONDS, 0L, 5L))
        assertTrue(Thread.interrupted())
    }

    @Test
    fun unlockByKey_unlocksOnlyWhenHeldByCurrentThread() {
        val held = RecordingRLock(tryLockResult = true, isLocked = true, isHeldByCurrentThread = true)
        lockerWith(held).unlock("order:9")
        assertEquals(1, held.unlockCalls)

        val notHeld = RecordingRLock(tryLockResult = true, isLocked = true, isHeldByCurrentThread = false)
        lockerWith(notHeld).unlock("order:10")
        assertEquals(0, notHeld.unlockCalls)

        val notLocked = RecordingRLock(tryLockResult = true, isLocked = false, isHeldByCurrentThread = true)
        lockerWith(notLocked).unlock("order:11")
        assertEquals(0, notLocked.unlockCalls)
    }

    @Test
    fun unlockByLock_unlocksOnlyWhenHeldByCurrentThread() {
        val locker = lockerWith(RecordingRLock(tryLockResult = true))

        val held = RecordingRLock(tryLockResult = true, isLocked = true, isHeldByCurrentThread = true)
        locker.unlock(held.proxy)
        assertEquals(1, held.unlockCalls)

        val notHeld = RecordingRLock(tryLockResult = true, isLocked = true, isHeldByCurrentThread = false)
        locker.unlock(notHeld.proxy)
        assertEquals(0, notHeld.unlockCalls)

        val notLocked = RecordingRLock(tryLockResult = true, isLocked = false, isHeldByCurrentThread = true)
        locker.unlock(notLocked.proxy)
        assertEquals(0, notLocked.unlockCalls)
    }

    @Test
    fun missingRedissonClient_failsFastWithExplicitMessage() {
        val locker = RedissonLocker() // no client injected

        val e = assertFailsWith<IllegalArgumentException> { locker.getLock("order:12") }
        assertTrue(e.message!!.contains("RedissonClient is not initialized"))
    }

    private fun lockerWith(lock: RecordingRLock): RedissonLocker =
        RedissonLocker().apply {
            setPrivateField("redissonClient", redissonClientReturning(lock.proxy))
        }

    private fun Any.setPrivateField(name: String, value: Any?) {
        val field = this::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }

    private fun redissonClientReturning(lock: RLock): RedissonClient =
        proxy(RedissonClient::class.java) { method, _ ->
            when (method.name) {
                "getLock" -> lock
                else -> defaultValue(method.returnType)
            }
        }

    /**
     * Test proxy that records Redisson RLock interactions and can simulate interruption.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class RecordingRLock(
        private val tryLockResult: Boolean,
        private val interruptOnTryLock: Boolean = false,
        private val isLocked: Boolean = false,
        private val isHeldByCurrentThread: Boolean = false
    ) {
        var tryLockCalls = 0
        var blockingLockCalls = 0
        var unlockCalls = 0
        var lastWaitTime: Long? = null
        var lastLeaseTime: Long? = null
        var lastUnit: TimeUnit? = null

        val proxy: RLock = proxy(RLock::class.java) { method, args ->
            when (method.name) {
                "tryLock" -> {
                    tryLockCalls++
                    if (interruptOnTryLock) throw InterruptedException("simulated interruption")
                    lastWaitTime = args?.get(0) as Long
                    lastLeaseTime = args[1] as Long
                    lastUnit = args[2] as TimeUnit
                    tryLockResult
                }

                "lock" -> {
                    blockingLockCalls++
                    if (args != null && args.size == 2) {
                        lastLeaseTime = args[0] as Long
                        lastUnit = args[1] as TimeUnit
                    }
                    null
                }

                "isLocked" -> isLocked
                "isHeldByCurrentThread" -> isHeldByCurrentThread
                "unlock" -> {
                    unlockCalls++
                    null
                }

                else -> defaultValue(method.returnType)
            }
        }
    }

    companion object {
        private fun <T> proxy(type: Class<T>, handler: (java.lang.reflect.Method, Array<Any?>?) -> Any?): T =
            type.cast(
                Proxy.newProxyInstance(
                    type.classLoader,
                    arrayOf(type),
                    InvocationHandler { _, method, args -> handler(method, args) }
                )
            )

        private fun defaultValue(returnType: Class<*>): Any? =
            when (returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Byte.TYPE -> 0.toByte()
                java.lang.Short.TYPE -> 0.toShort()
                java.lang.Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Float.TYPE -> 0f
                java.lang.Double.TYPE -> 0.0
                java.lang.Character.TYPE -> 0.toChar()
                java.lang.Void.TYPE -> null
                else -> null
            }
    }

}
