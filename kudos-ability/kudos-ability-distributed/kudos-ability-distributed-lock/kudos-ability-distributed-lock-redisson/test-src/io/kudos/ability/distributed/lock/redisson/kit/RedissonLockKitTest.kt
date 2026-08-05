package io.kudos.ability.distributed.lock.redisson.kit

import io.kudos.ability.distributed.lock.redisson.locker.RedissonLocker
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for [RedissonLockKit] covering global configuration, named-locker routing,
 * all lock/tryLock/unlock overloads, the RLock unlock guard, locker binding/unbinding
 * and key-prefix concatenation.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class RedissonLockKitTest {

    @AfterTest
    fun tearDown() {
        RedissonLockKit.clearCachedLockers()
        RedissonLockKit.setLockKeyPrefix(RedissonLockKit.DEFAULT_LOCK_KEY_PREFIX)
    }

    @Test
    fun lock_usesConfiguredKeyPrefix() {
        val client = RecordingRedissonClient()
        RedissonLockKit.setLockKeyPrefix("APP::")
        RedissonLockKit.bindLocker(locker(client))

        val result = RedissonLockKit.lock("order:1")

        assertSame(client.lock, result)
        assertEquals("APP::order:1", client.lastLockName)
    }

    @Test
    fun lock_canUseNamedLocker() {
        val defaultClient = RecordingRedissonClient()
        val reportClient = RecordingRedissonClient()
        RedissonLockKit.bindLocker(locker(defaultClient))
        RedissonLockKit.bindLocker(locker(reportClient), "reportLocker")

        RedissonLockKit.lock("order:2", "reportLocker")

        assertEquals(null, defaultClient.lastLockName)
        assertEquals("REDISSON::order:2", reportClient.lastLockName)
    }

    @Test
    fun getLock_returnsLockWithoutAcquiringIt() {
        val client = RecordingRedissonClient()
        RedissonLockKit.bindLocker(locker(client))

        val result = RedissonLockKit.getLock("order:3")

        assertSame(client.lock, result)
        assertEquals("REDISSON::order:3", client.lastLockName)
        assertEquals(0, client.tryLockCalls)
        assertEquals(0, client.blockingLockCalls)
    }

    @Test
    fun lock_withLeaseSeconds_usesBlockingLock() {
        val client = RecordingRedissonClient()
        RedissonLockKit.bindLocker(locker(client))

        val result = RedissonLockKit.lock("order:4", 12L)

        assertSame(client.lock, result)
        assertEquals("REDISSON::order:4", client.lastLockName)
        assertEquals(1, client.blockingLockCalls)
        assertEquals(12L, client.lastLeaseTime)
        assertEquals(TimeUnit.SECONDS, client.lastUnit)
    }

    @Test
    fun lock_withLeaseAndUnit_usesBlockingLock() {
        val client = RecordingRedissonClient()
        RedissonLockKit.bindLocker(locker(client))

        val result = RedissonLockKit.lock("order:5", TimeUnit.MILLISECONDS, 300L)

        assertSame(client.lock, result)
        assertEquals("REDISSON::order:5", client.lastLockName)
        assertEquals(1, client.blockingLockCalls)
        assertEquals(300L, client.lastLeaseTime)
        assertEquals(TimeUnit.MILLISECONDS, client.lastUnit)
    }

    @Test
    fun tryLock_delegatesWaitAndLeaseToLocker() {
        val client = RecordingRedissonClient()
        RedissonLockKit.bindLocker(locker(client))

        val acquired = RedissonLockKit.tryLock("order:6", TimeUnit.SECONDS, 1L, 9L)

        assertTrue(acquired)
        assertEquals("REDISSON::order:6", client.lastLockName)
        assertEquals(1, client.tryLockCalls)
        assertEquals(1L, client.lastWaitTime)
        assertEquals(9L, client.lastLeaseTime)
        assertEquals(TimeUnit.SECONDS, client.lastUnit)
    }

    @Test
    fun unlockByKey_appliesPrefixAndGuards() {
        val client = RecordingRedissonClient(isLocked = true, isHeldByCurrentThread = true)
        RedissonLockKit.bindLocker(locker(client))

        RedissonLockKit.unlock("order:7")

        assertEquals("REDISSON::order:7", client.lastLockName)
        assertEquals(1, client.unlockCalls)
    }

    @Test
    fun unlockByRLock_unlocksOnlyWhenHeldByCurrentThread() {
        val held = RecordingRedissonClient(isLocked = true, isHeldByCurrentThread = true)
        RedissonLockKit.unlock(held.lock)
        assertEquals(1, held.unlockCalls)

        val notHeld = RecordingRedissonClient(isLocked = true, isHeldByCurrentThread = false)
        RedissonLockKit.unlock(notHeld.lock)
        assertEquals(0, notHeld.unlockCalls)

        val notLocked = RecordingRedissonClient(isLocked = false, isHeldByCurrentThread = true)
        RedissonLockKit.unlock(notLocked.lock)
        assertEquals(0, notLocked.unlockCalls)
    }

    @Test
    fun bindLocker_nullRemovesBindingSoLookupFallsBackToSpring() {
        val client = RecordingRedissonClient()
        RedissonLockKit.bindLocker(locker(client), "tempLocker")
        RedissonLockKit.lock("order:8", "tempLocker")
        assertEquals("REDISSON::order:8", client.lastLockName)

        RedissonLockKit.bindLocker(null, "tempLocker")

        // Without a Spring container the fallback bean lookup must fail, proving removal.
        assertFails { RedissonLockKit.lock("order:9", "tempLocker") }
    }

    @Test
    fun getLockKey_concatenatesConfiguredPrefix() {
        assertEquals("REDISSON::a:b", RedissonLockKit.getLockKey("a:b"))

        RedissonLockKit.setLockKeyPrefix("")
        assertEquals("a:b", RedissonLockKit.getLockKey("a:b"))

        RedissonLockKit.setLockKeyPrefix("中文::")
        assertEquals("中文::a:b", RedissonLockKit.getLockKey("a:b"))
    }

    /**
     * RedissonClient stub that records getLock call arguments and lock interactions.
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
        var blockingLockCalls = 0
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
                setPrivateField("redissonClient", client.proxy)
            }

        private fun Any.setPrivateField(name: String, value: Any?) {
            val field = this::class.java.getDeclaredField(name)
            field.isAccessible = true
            field.set(this, value)
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
