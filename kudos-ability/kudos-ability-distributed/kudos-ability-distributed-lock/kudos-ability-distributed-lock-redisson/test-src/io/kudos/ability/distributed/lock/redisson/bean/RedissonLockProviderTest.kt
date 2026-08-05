package io.kudos.ability.distributed.lock.redisson.bean

import io.kudos.ability.distributed.lock.redisson.kit.RedissonLockKit
import io.kudos.ability.distributed.lock.redisson.locker.RedissonLocker
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [RedissonLockProvider] covering unlock-guard behavior (RLock held/not-held/
 * name-mismatch and plain JDK Lock fallback), key-based lock/unlock/tryLock delegation and
 * the provider order.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class RedissonLockProviderTest {

    @AfterTest
    fun tearDown() {
        RedissonLockKit.clearCachedLockers()
        RedissonLockKit.setLockKeyPrefix(RedissonLockKit.DEFAULT_LOCK_KEY_PREFIX)
    }

    @Test
    fun unLock_rLockNotHeldByCurrentThread_doesNotCallUnlock() {
        val lock = RecordingRLock(
            name = "REDISSON::order:1",
            isLocked = true,
            isHeldByCurrentThread = false
        )

        RedissonLockProvider().unLock(lock.proxy, "order:1")

        assertEquals(1, lock.isLockedCalls)
        assertEquals(1, lock.isHeldByCurrentThreadCalls)
        assertEquals(0, lock.unlockCalls)
    }

    @Test
    fun unLock_rLockHeldByCurrentThread_callsUnlock() {
        val lock = RecordingRLock(
            name = "REDISSON::order:2",
            isLocked = true,
            isHeldByCurrentThread = true
        )

        RedissonLockProvider().unLock(lock.proxy, "order:2")

        assertEquals(1, lock.unlockCalls)
    }

    @Test
    fun unLock_rLockKeyMismatch_doesNotCallUnlock() {
        val lock = RecordingRLock(
            name = "REDISSON::order:other",
            isLocked = true,
            isHeldByCurrentThread = true
        )

        RedissonLockProvider().unLock(lock.proxy, "order:3")

        assertEquals(0, lock.isLockedCalls)
        assertEquals(0, lock.isHeldByCurrentThreadCalls)
        assertEquals(0, lock.unlockCalls)
    }

    @Test
    fun unLock_plainJdkLock_unlocksDirectly() {
        val lock = RecordingPlainLock()

        RedissonLockProvider().unLock(lock, "order:4")

        assertEquals(1, lock.unlockCalls)
    }

    @Test
    fun lock_delegatesToKitWithPrefixedKey() {
        val client = RecordingRedissonClient()
        RedissonLockKit.bindLocker(locker(client))

        val result = RedissonLockProvider().lock("order:5")

        assertSame(client.lock, result)
        assertEquals("REDISSON::order:5", client.lastLockName)
        assertEquals(1, client.tryLockCalls)
        assertEquals(RedissonLocker.DEFAULT_WAIT_SECONDS, client.lastWaitTime)
        assertEquals(RedissonLocker.DEFAULT_LEASE_SECONDS, client.lastLeaseTime)
    }

    @Test
    fun unLockByKey_delegatesToKitWithPrefixedKey() {
        val client = RecordingRedissonClient(isLocked = true, isHeldByCurrentThread = true)
        RedissonLockKit.bindLocker(locker(client))

        RedissonLockProvider().unLock("order:6")

        assertEquals("REDISSON::order:6", client.lastLockName)
        assertEquals(1, client.unlockCalls)
    }

    @Test
    fun tryLock_usesZeroWaitAndGivenLeaseSeconds() {
        val client = RecordingRedissonClient()
        RedissonLockKit.bindLocker(locker(client))

        val acquired = RedissonLockProvider().tryLock("order:7", 25)

        assertTrue(acquired)
        assertEquals("REDISSON::order:7", client.lastLockName)
        assertEquals(0L, client.lastWaitTime)
        assertEquals(25L, client.lastLeaseTime)
        assertEquals(TimeUnit.SECONDS, client.lastUnit)
    }

    @Test
    fun order_isNinety() {
        assertEquals(90, RedissonLockProvider().order())
    }

    /**
     * Plain JDK Lock fake that records unlock calls (non-RLock fallback branch).
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class RecordingPlainLock : Lock {
        var unlockCalls = 0

        override fun lock() = Unit
        override fun lockInterruptibly() = Unit
        override fun tryLock(): Boolean = true
        override fun tryLock(time: Long, unit: TimeUnit): Boolean = true
        override fun unlock() {
            unlockCalls++
        }

        override fun newCondition(): Condition = throw UnsupportedOperationException()
    }

    /**
     * Test proxy that records Redisson RLock unlock-related calls.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class RecordingRLock(
        private val name: String,
        private val isLocked: Boolean,
        private val isHeldByCurrentThread: Boolean
    ) {
        var isLockedCalls = 0
        var isHeldByCurrentThreadCalls = 0
        var unlockCalls = 0

        val proxy: RLock = proxy(RLock::class.java) { method, _ ->
            when (method.name) {
                "getName" -> name
                "isLocked" -> {
                    isLockedCalls++
                    isLocked
                }

                "isHeldByCurrentThread" -> {
                    isHeldByCurrentThreadCalls++
                    isHeldByCurrentThread
                }

                "unlock" -> {
                    unlockCalls++
                    null
                }

                else -> defaultValue(method.returnType)
            }
        }
    }

    /**
     * RedissonClient stub that records getLock arguments and lock interactions.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class RecordingRedissonClient(
        private val isLocked: Boolean = false,
        private val isHeldByCurrentThread: Boolean = false
    ) {
        var lastLockName: String? = null
        var tryLockCalls = 0
        var unlockCalls = 0
        var lastWaitTime: Long? = null
        var lastLeaseTime: Long? = null
        var lastUnit: TimeUnit? = null

        val lock: RLock = proxy(RLock::class.java) { method, args ->
            when (method.name) {
                "tryLock" -> {
                    tryLockCalls++
                    if (args != null && args.size == 3) {
                        lastWaitTime = args[0] as Long
                        lastLeaseTime = args[1] as Long
                        lastUnit = args[2] as TimeUnit
                    }
                    true
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

        val proxy: RedissonClient = proxy(RedissonClient::class.java) { method, args ->
            when (method.name) {
                "getLock" -> {
                    lastLockName = args?.get(0) as String
                    lock
                }

                else -> defaultValue(method.returnType)
            }
        }
    }

    companion object {
        private fun locker(client: RecordingRedissonClient): RedissonLocker =
            RedissonLocker().apply {
                val field = RedissonLocker::class.java.getDeclaredField("redissonClient")
                field.isAccessible = true
                field.set(this, client.proxy)
            }

        private fun <T> proxy(type: Class<T>, handler: (Method, Array<Any?>?) -> Any?): T =
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
                TimeUnit::class.java -> TimeUnit.SECONDS
                else -> null
            }
    }

}
