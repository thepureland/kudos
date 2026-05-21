package io.kudos.ability.distributed.lock.redisson.annotations

import io.kudos.ability.distributed.lock.common.annotations.DistributedLock
import io.kudos.ability.distributed.lock.redisson.kit.RedissonLockKit
import io.kudos.ability.distributed.lock.redisson.locker.RedissonLocker
import io.kudos.context.core.KudosContextHolder
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame


internal class DistributedLockAspectTest {

    @AfterTest
    fun tearDown() {
        RedissonLockKit.setPrivateField("lockBean", null)
        KudosContextHolder.clear()
    }

    @Test
    fun around_rethrowsBusinessExceptionWithoutRuntimeExceptionWrapping() {
        val lock = RecordingRLock()
        RedissonLockKit.setPrivateField(
            "lockBean",
            RedissonLocker().apply {
                setPrivateField("redissonClient", redissonClientReturning(lock.proxy))
            }
        )
        val businessException = TypedBusinessException()

        val thrown = assertFailsWith<TypedBusinessException> {
            DistributedLockAspect().around(joinPointThatThrows(businessException))
        }

        assertSame(businessException, thrown)
        assertEquals(1, lock.tryLockCalls)
        assertEquals(1, lock.unlockCalls)
    }

    private class Target {
        @DistributedLock(waitTime = 0, leaseTime = 30)
        fun fail() = Unit
    }

    private class TypedBusinessException : RuntimeException("typed")

    private class RecordingRLock {
        var tryLockCalls = 0
        var unlockCalls = 0

        val proxy: RLock = proxy(RLock::class.java) { method, _ ->
            when (method.name) {
                "tryLock" -> {
                    tryLockCalls++
                    true
                }

                "isLocked" -> true
                "isHeldByCurrentThread" -> true
                "unlock" -> {
                    unlockCalls++
                    null
                }

                else -> defaultValue(method.returnType)
            }
        }
    }

    companion object {
        private val target = Target()
        private val targetMethod = Target::class.java.getDeclaredMethod("fail")

        private fun joinPointThatThrows(exception: Throwable): ProceedingJoinPoint =
            proxy(ProceedingJoinPoint::class.java) { method, _ ->
                when (method.name) {
                    "getSignature" -> methodSignature()
                    "getTarget" -> target
                    "getArgs" -> emptyArray<Any>()
                    "proceed" -> throw exception
                    else -> defaultValue(method.returnType)
                }
            }

        private fun methodSignature(): MethodSignature =
            proxy(MethodSignature::class.java) { method, _ ->
                when (method.name) {
                    "getMethod" -> targetMethod
                    "getName" -> targetMethod.name
                    "getParameterNames" -> emptyArray<String>()
                    else -> defaultValue(method.returnType)
                }
            }

        private fun redissonClientReturning(lock: RLock): RedissonClient =
            proxy(RedissonClient::class.java) { method, _ ->
                when (method.name) {
                    "getLock" -> lock
                    else -> defaultValue(method.returnType)
                }
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
