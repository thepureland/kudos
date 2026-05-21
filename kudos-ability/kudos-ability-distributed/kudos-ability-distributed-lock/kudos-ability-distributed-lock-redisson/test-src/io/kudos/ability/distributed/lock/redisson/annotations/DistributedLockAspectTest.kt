package io.kudos.ability.distributed.lock.redisson.annotations

import io.kudos.ability.distributed.lock.common.annotations.DistributedLock
import io.kudos.ability.distributed.lock.common.exception.DistributedLockAcquireException
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
        RedissonLockKit.clearCachedLockers()
        RedissonLockKit.setLockKeyPrefix(RedissonLockKit.DEFAULT_LOCK_KEY_PREFIX)
        KudosContextHolder.clear()
    }

    @Test
    fun around_rethrowsBusinessExceptionWithoutRuntimeExceptionWrapping() {
        val lock = RecordingRLock()
        RedissonLockKit.bindLocker(
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

    @Test
    fun around_lockFailureThrowsByDefaultInsteadOfReturningNull() {
        val lock = RecordingRLock(tryLockResult = false)
        RedissonLockKit.bindLocker(
            RedissonLocker().apply {
                setPrivateField("redissonClient", redissonClientReturning(lock.proxy))
            }
        )

        assertFailsWith<DistributedLockAcquireException> {
            DistributedLockAspect().around(
                joinPoint(Target::class.java.getDeclaredMethod("locked")) {
                    "should-not-run"
                }
            )
        }

        assertEquals(1, lock.tryLockCalls)
        assertEquals(0, lock.unlockCalls)
    }

    @Test
    fun around_lockFailureCanReturnNullForLegacyCallers() {
        val lock = RecordingRLock(tryLockResult = false)
        RedissonLockKit.bindLocker(
            RedissonLocker().apply {
                setPrivateField("redissonClient", redissonClientReturning(lock.proxy))
            }
        )

        val result = DistributedLockAspect().around(
            joinPoint(Target::class.java.getDeclaredMethod("legacyNull")) {
                "should-not-run"
            }
        )

        kotlin.test.assertNull(result)
        assertEquals(1, lock.tryLockCalls)
        assertEquals(0, lock.unlockCalls)
    }

    private class Target {
        @DistributedLock(waitTime = 0, leaseTime = 30)
        fun fail() = Unit

        @DistributedLock(waitTime = 0, leaseTime = 30)
        fun locked(): String = "locked"

        @DistributedLock(waitTime = 0, leaseTime = 30, throwOnFailure = false)
        fun legacyNull(): String = "legacy"
    }

    private class TypedBusinessException : RuntimeException("typed")

    private class RecordingRLock(private val tryLockResult: Boolean = true) {
        var tryLockCalls = 0
        var unlockCalls = 0

        val proxy: RLock = proxy(RLock::class.java) { method, _ ->
            when (method.name) {
                "tryLock" -> {
                    tryLockCalls++
                    tryLockResult
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

        private fun joinPointThatThrows(exception: Throwable): ProceedingJoinPoint =
            joinPoint(Target::class.java.getDeclaredMethod("fail")) {
                throw exception
            }

        private fun joinPoint(targetMethod: Method, proceed: () -> Any?): ProceedingJoinPoint =
            proxy(ProceedingJoinPoint::class.java) { joinPointMethod, _ ->
                when (joinPointMethod.name) {
                    "getSignature" -> methodSignature(targetMethod)
                    "getTarget" -> target
                    "getArgs" -> emptyArray<Any>()
                    "proceed" -> proceed()
                    else -> defaultValue(joinPointMethod.returnType)
                }
            }

        private fun methodSignature(method: Method): MethodSignature =
            proxy(MethodSignature::class.java) { signatureMethod, _ ->
                when (signatureMethod.name) {
                    "getMethod" -> method
                    "getName" -> method.name
                    "getParameterNames" -> emptyArray<String>()
                    else -> defaultValue(signatureMethod.returnType)
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
