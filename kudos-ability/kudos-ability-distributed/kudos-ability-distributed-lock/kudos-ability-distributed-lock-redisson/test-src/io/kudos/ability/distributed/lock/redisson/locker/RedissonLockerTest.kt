package io.kudos.ability.distributed.lock.redisson.locker

import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame


internal class RedissonLockerTest {

    @Test
    fun lock_usesBoundedTryLockInsteadOfBlockingLock() {
        val lock = RecordingRLock(tryLockResult = true)
        val locker = RedissonLocker().apply {
            setPrivateField("redissonClient", redissonClientReturning(lock.proxy))
        }

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
        val locker = RedissonLocker().apply {
            setPrivateField("redissonClient", redissonClientReturning(lock.proxy))
        }

        assertNull(locker.lock("order:2"))
        assertEquals(1, lock.tryLockCalls)
        assertEquals(0, lock.blockingLockCalls)
    }

    private fun Any.setPrivateField(name: String, value: Any?) {
        val field = this::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }

    private fun redissonClientReturning(lock: RLock): RedissonClient =
        proxy(RedissonClient::class.java) { method, args ->
            when (method.name) {
                "getLock" -> lock
                else -> defaultValue(method.returnType)
            }
        }

    private class RecordingRLock(private val tryLockResult: Boolean) {
        var tryLockCalls = 0
        var blockingLockCalls = 0
        var lastWaitTime: Long? = null
        var lastLeaseTime: Long? = null
        var lastUnit: TimeUnit? = null

        val proxy: RLock = proxy(RLock::class.java) { method, args ->
            when (method.name) {
                "tryLock" -> {
                    tryLockCalls++
                    lastWaitTime = args?.get(0) as Long
                    lastLeaseTime = args[1] as Long
                    lastUnit = args[2] as TimeUnit
                    tryLockResult
                }

                "lock" -> {
                    blockingLockCalls++
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
