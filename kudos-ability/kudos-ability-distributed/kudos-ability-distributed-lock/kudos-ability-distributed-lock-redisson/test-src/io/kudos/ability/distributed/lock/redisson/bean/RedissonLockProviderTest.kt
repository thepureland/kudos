package io.kudos.ability.distributed.lock.redisson.bean

import org.redisson.api.RLock
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals


internal class RedissonLockProviderTest {

    @Test
    fun unLock_rLockNotHeldByCurrentThread_doesNotCallUnlock() {
        val lock = RecordingRLock(isLocked = true, isHeldByCurrentThread = false)

        RedissonLockProvider().unLock(lock.proxy, "order:1")

        assertEquals(1, lock.isLockedCalls)
        assertEquals(1, lock.isHeldByCurrentThreadCalls)
        assertEquals(0, lock.unlockCalls)
    }

    @Test
    fun unLock_rLockHeldByCurrentThread_callsUnlock() {
        val lock = RecordingRLock(isLocked = true, isHeldByCurrentThread = true)

        RedissonLockProvider().unLock(lock.proxy, "order:2")

        assertEquals(1, lock.unlockCalls)
    }

    private class RecordingRLock(
        private val isLocked: Boolean,
        private val isHeldByCurrentThread: Boolean
    ) {
        var isLockedCalls = 0
        var isHeldByCurrentThreadCalls = 0
        var unlockCalls = 0

        val proxy: RLock = proxy(RLock::class.java) { method, _ ->
            when (method.name) {
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
                TimeUnit::class.java -> TimeUnit.SECONDS
                else -> null
            }
    }

}
