package io.kudos.ability.distributed.lock.redisson.annotations

import io.kudos.ability.distributed.lock.common.annotations.DistributedLock
import io.kudos.ability.distributed.lock.common.exception.DistributedLockAcquireException
import io.kudos.ability.distributed.lock.common.locker.DistributedLockContext
import io.kudos.ability.distributed.lock.common.locker.IDistributedLockCallback
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
import kotlin.test.assertTrue

/**
 * Unit tests for [DistributedLockAspect] covering lock/unlock and exception propagation.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class DistributedLockAspectTest {

    @AfterTest
    fun tearDown() {
        RedissonLockKit.clearCachedLockers()
        RedissonLockKit.setLockKeyPrefix(RedissonLockKit.DEFAULT_LOCK_KEY_PREFIX)
        KudosContextHolder.clear()
        DistributedLockContext.clear()
    }

    @Test
    fun cut_isPlainPointcutPlaceholder() {
        // The pointcut method body must do nothing; invoking it directly only marks coverage.
        DistributedLockAspect().cut()
    }

    @Test
    fun around_successReturnsTargetMethodResultAndReleasesLock() {
        val lock = RecordingRLock()
        RedissonLockKit.bindLocker(
            RedissonLocker().apply {
                setPrivateField("redissonClient", redissonClientReturning(lock.proxy))
            }
        )

        val result = DistributedLockAspect().around(
            joinPoint(Target::class.java.getDeclaredMethod("locked")) { "locked" }
        )

        assertEquals("locked", result)
        assertEquals(1, lock.tryLockCalls)
        assertEquals(1, lock.unlockCalls)
    }

    @Test
    fun around_missingAnnotationFailsFastWithExplicitMessage() {
        val e = assertFailsWith<IllegalArgumentException> {
            DistributedLockAspect().around(
                joinPoint(Target::class.java.getDeclaredMethod("plain")) { "plain" }
            )
        }
        assertTrue(e.message!!.contains("@DistributedLock is missing"))
    }

    @Test
    fun around_tryLockErrorIsTreatedAsAcquireFailure() {
        val lock = RecordingRLock(tryLockThrows = true)
        RedissonLockKit.bindLocker(
            RedissonLocker().apply {
                setPrivateField("redissonClient", redissonClientReturning(lock.proxy))
            }
        )

        assertFailsWith<DistributedLockAcquireException> {
            DistributedLockAspect().around(
                joinPoint(Target::class.java.getDeclaredMethod("locked")) { "should-not-run" }
            )
        }
        assertEquals(0, lock.unlockCalls)
    }

    @Test
    fun around_unlockErrorDoesNotMaskMethodResult() {
        val lock = RecordingRLock(unlockThrows = true)
        RedissonLockKit.bindLocker(
            RedissonLocker().apply {
                setPrivateField("redissonClient", redissonClientReturning(lock.proxy))
            }
        )

        val result = DistributedLockAspect().around(
            joinPoint(Target::class.java.getDeclaredMethod("locked")) { "locked" }
        )

        assertEquals("locked", result)
        assertEquals(1, lock.unlockCalls)
    }

    @Test
    fun around_invokesSuccessCallbackWithLockKey() {
        val lock = RecordingRLock()
        RedissonLockKit.bindLocker(
            RedissonLocker().apply {
                setPrivateField("redissonClient", redissonClientReturning(lock.proxy))
            }
        )
        val callback = RecordingCallback()
        DistributedLockContext.set(callback)

        DistributedLockAspect().around(
            joinPoint(Target::class.java.getDeclaredMethod("locked")) { "locked" }
        )

        assertEquals(1, callback.successKeys.size)
        assertTrue(callback.successKeys.single().endsWith("locked::"))
        assertEquals(0, callback.failKeys.size)
    }

    @Test
    fun around_invokesFailCallbackWithLockKey() {
        val lock = RecordingRLock(tryLockResult = false)
        RedissonLockKit.bindLocker(
            RedissonLocker().apply {
                setPrivateField("redissonClient", redissonClientReturning(lock.proxy))
            }
        )
        val callback = RecordingCallback()
        DistributedLockContext.set(callback)

        val result = DistributedLockAspect().around(
            joinPoint(Target::class.java.getDeclaredMethod("legacyNull")) { "should-not-run" }
        )

        kotlin.test.assertNull(result)
        assertEquals(0, callback.successKeys.size)
        assertEquals(1, callback.failKeys.size)
    }

    @Test
    fun around_spelKeyIsEvaluatedWithArgsAndPrefixedByTenantId() {
        KudosContextHolder.get().tenantId = "1001"
        val client = NameRecordingRedissonClient()
        RedissonLockKit.bindLocker(
            RedissonLocker().apply {
                setPrivateField("redissonClient", client.proxy)
            }
        )

        val result = DistributedLockAspect().around(
            joinPoint(
                Target::class.java.getDeclaredMethod("spel", String::class.java),
                arrayOf("A-001")
            ) { "spel-ok" }
        )

        assertEquals("spel-ok", result)
        assertEquals("REDISSON::1001::order:A-001", client.lockNames.first())
    }

    @Test
    fun around_spelKeyWithUnicodeArgument() {
        KudosContextHolder.get().tenantId = "租户7"
        val client = NameRecordingRedissonClient()
        RedissonLockKit.bindLocker(
            RedissonLocker().apply {
                setPrivateField("redissonClient", client.proxy)
            }
        )

        DistributedLockAspect().around(
            joinPoint(
                Target::class.java.getDeclaredMethod("spel", String::class.java),
                arrayOf("订单：001")
            ) { "ok" }
        )

        assertEquals("REDISSON::租户7::order:订单：001", client.lockNames.first())
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

    /**
     * Test target class hosting methods annotated with [DistributedLock].
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class Target {
        @DistributedLock(waitTime = 0, leaseTime = 30)
        fun fail() = Unit

        @DistributedLock(waitTime = 0, leaseTime = 30)
        fun locked(): String = "locked"

        @DistributedLock(waitTime = 0, leaseTime = 30, throwOnFailure = false)
        fun legacyNull(): String = "legacy"

        @Suppress("unused")
        fun plain(): String = "plain"

        @Suppress("unused", "UNUSED_PARAMETER")
        @DistributedLock(key = "'order:' + #p0", waitTime = 0, leaseTime = 30)
        fun spel(orderId: String): String = "spel"
    }

    /**
     * Recording implementation of [IDistributedLockCallback] capturing callback keys.
     *
     * @author K
     * @author AI: Claude
     * @since 1.0.0
     */
    private class RecordingCallback : IDistributedLockCallback {
        val successKeys = mutableListOf<String>()
        val failKeys = mutableListOf<String>()

        override fun doLockSuccess(lockKey: String) {
            successKeys.add(lockKey)
        }

        override fun doLockFail(lockKey: String) {
            failKeys.add(lockKey)
        }
    }

    /**
     * RedissonClient stub recording getLock key names; the returned lock always succeeds.
     *
     * @author K
     * @author AI: Claude
     * @since 1.0.0
     */
    private class NameRecordingRedissonClient {
        val lockNames = mutableListOf<String>()

        private val lock: RLock = proxy(RLock::class.java) { method, _ ->
            when (method.name) {
                "tryLock" -> true
                "isLocked" -> true
                "isHeldByCurrentThread" -> true
                else -> defaultValue(method.returnType)
            }
        }

        val proxy: RedissonClient = proxy(RedissonClient::class.java) { method, args ->
            when (method.name) {
                "getLock" -> {
                    lockNames.add(args?.get(0) as String)
                    lock
                }

                else -> defaultValue(method.returnType)
            }
        }
    }

    /**
     * Test exception used to verify the aspect does not wrap typed business exceptions.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class TypedBusinessException : RuntimeException("typed")

    /**
     * Test proxy that records Redisson lock call counts.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class RecordingRLock(
        private val tryLockResult: Boolean = true,
        private val tryLockThrows: Boolean = false,
        private val unlockThrows: Boolean = false
    ) {
        var tryLockCalls = 0
        var unlockCalls = 0

        val proxy: RLock = proxy(RLock::class.java) { method, _ ->
            when (method.name) {
                "tryLock" -> {
                    tryLockCalls++
                    if (tryLockThrows) throw IllegalStateException("simulated tryLock error")
                    tryLockResult
                }

                "isLocked" -> true
                "isHeldByCurrentThread" -> true
                "unlock" -> {
                    unlockCalls++
                    if (unlockThrows) throw IllegalStateException("simulated unlock error")
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
            joinPoint(targetMethod, emptyArray<Any?>(), proceed)

        private fun joinPoint(
            targetMethod: Method,
            args: Array<out Any?>,
            proceed: () -> Any?
        ): ProceedingJoinPoint =
            proxy(ProceedingJoinPoint::class.java) { joinPointMethod, _ ->
                when (joinPointMethod.name) {
                    "getSignature" -> methodSignature(targetMethod)
                    "getTarget" -> target
                    "getArgs" -> args
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
