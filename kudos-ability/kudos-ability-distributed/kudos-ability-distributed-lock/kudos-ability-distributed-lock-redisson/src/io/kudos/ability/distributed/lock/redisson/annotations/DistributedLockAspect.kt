package io.kudos.ability.distributed.lock.redisson.annotations

import io.kudos.ability.distributed.lock.common.annotations.DistributedLock
import io.kudos.ability.distributed.lock.common.exception.DistributedLockAcquireException
import io.kudos.ability.distributed.lock.common.locker.DistributedLockContext
import io.kudos.ability.distributed.lock.redisson.kit.RedissonLockKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.support.Consts
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.context.annotation.Lazy
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.TypedValue
import org.springframework.expression.spel.standard.SpelExpressionParser
import java.util.concurrent.TimeUnit

/**
 * Distributed lock aspect.
 *
 * Adds distributed lock behavior to methods via AOP, with SpEL-based dynamic key generation.
 *
 * Core features:
 * 1. Lock key generation, two strategies:
 *    - When key is not specified: auto-generated as "serviceCode::tenantId::className::methodName::parameterTypes".
 *    - When a SpEL expression is specified: evaluated dynamically, with access to method args and context.
 * 2. Lock acquisition: uses tryLock with configurable waitTime and leaseTime.
 * 3. Callbacks: invokes success/failure handlers registered in DistributedLockContext.
 * 4. Auto release: the lock is released after the method completes (whether normally or by exception).
 *
 * Workflow:
 * - Intercept methods annotated with @DistributedLock.
 * - Parse annotation config and build the lock key.
 * - Attempt to acquire the distributed lock.
 * - On success, proceed with the method; on failure, return null without invoking the method.
 * - Release the lock after the method completes.
 *
 * Notes:
 * - If lock acquisition fails the method is not executed and null is returned.
 * - Lock release runs in a finally block so it still happens when the method throws.
 * - DistributedLockContext can be used to register success/failure callbacks.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Aspect
@Lazy(false)
class DistributedLockAspect {
    private val log = LogFactory.getLog(this::class)
    private val parameterNameDiscoverer = DefaultParameterNameDiscoverer()

    /**
     * Pointcut: matches methods annotated with [io.kudos.ability.distributed.lock.common.annotations.DistributedLock].
     * The body stays as `Unit`; it only serves as a pointcut placeholder.
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.distributed.lock.common.annotations.DistributedLock)")
    fun cut() = Unit

    companion object {
        private val spelParser = SpelExpressionParser()
    }

    /**
     * Around advice: implements the core distributed lock logic.
     *
     * Workflow:
     * 1. Parse @DistributedLock to obtain lock config (wait time, lease time, etc.).
     * 2. Build the lock key via genLockKey.
     * 3. Try to acquire the distributed lock:
     *    - Success: invoke the target method and release the lock afterwards.
     *    - Failure: do not invoke the method; return null and trigger the failure callback.
     * 4. Trigger the appropriate callback regardless of outcome.
     * 5. Release the lock in a finally block so it still happens on exception.
     *
     * Exception handling:
     * - Errors during acquisition are logged as warnings and treated as failure.
     * - On failure, [DistributedLockAcquireException] is thrown by default; with `throwOnFailure=false`, null is returned.
     * - Exceptions thrown by the target method are rethrown as-is to preserve typed catch behavior.
     * - Errors during release are logged as warnings without affecting the return value.
     *
     * Return value:
     * - null if lock acquisition fails.
     * - The target method's return value on success.
     *
     * @param joinPoint join point containing target method info and arguments
     * @return target method result, or null when lock acquisition fails
     */
    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val annotation = requireNotNull(signature.method.getAnnotation(DistributedLock::class.java)) {
            "@DistributedLock is missing on ${joinPoint.target.javaClass.name}.${signature.method.name}"
        }
        val lockKey = genLockKey(joinPoint, signature, annotation)
        log.debug("Trying to acquire distributed lock: key=$lockKey")
        val lockerBeanName = annotation.lockerBeanName.ifBlank { RedissonLockKit.REDISSON_LOCKER_BEAN_NAME }
        val acquired = runCatching {
            RedissonLockKit.tryLock(lockKey, TimeUnit.SECONDS, annotation.waitTime, annotation.leaseTime, lockerBeanName)
        }.onFailure { log.warn("Failed to acquire distributed lock, a task may still be in progress: ${it.message}") }.getOrDefault(false)
        lockCallback(acquired, lockKey)
        if (!acquired) {
            log.warn("Failed to acquire distributed lock, a task may still be in progress. key=$lockKey")
            if (annotation.throwOnFailure) {
                throw DistributedLockAcquireException(lockKey)
            }
            return null
        }
        return try {
            // Business exceptions propagate as-is (no wrapping) so typed catch on the caller side keeps working.
            joinPoint.proceed()
        } finally {
            log.debug("Releasing lock: key=$lockKey")
            runCatching { RedissonLockKit.unlock(lockKey, lockerBeanName) }
                .onFailure { log.warn("Lock release error, it may have been auto-released after timeout. message={0}", it.message) }
        }
    }

    /**
     * Post-lock callback.
     * @param success
     * @param lockKey
     */
    private fun lockCallback(success: Boolean, lockKey: String) {
        DistributedLockContext.get()?.let { cb ->
            if (success) cb.doLockSuccess(lockKey) else cb.doLockFail(lockKey)
        }
    }

    /**
     * Build the distributed lock key.
     *
     * Two strategies are supported:
     * 1. Auto-generation (no key on the annotation):
     *    - Format: serviceCode::tenantId::className::methodName::parameterTypes
     *    - Example: "service::1001::com.example.UserService::getUser-java.lang.String"
     *    - Parameter types are joined with "-" when there are multiple parameters.
     *
     * 2. SpEL expression (key set on the annotation):
     *    - Evaluates the SpEL expression with access to method args and context.
     *    - Builds a MethodBasedEvaluationContext containing method arguments and parameter names.
     *    - Evaluates the expression; result format is "tenantId::expressionResult".
     *    - Example: key="#userId" yields "1001::123".
     *
     * Tenant isolation:
     * - Every lock key includes the tenant id so locks across tenants stay isolated.
     * - The current tenant id is read from KudosContext.
     *
     * Notes:
     * - The auto-generated key includes the full class name and method signature to avoid collisions.
     * - SpEL expressions must return String, otherwise an exception is thrown.
     * - Parameter types use getTypeName() and include the full package path.
     *
     * @param joinPoint join point containing target method info and arguments
     * @return generated lock key string
     */
    private fun genLockKey(
        joinPoint: ProceedingJoinPoint,
        signature: MethodSignature,
        annotation: DistributedLock
    ): String {
        val kudosContext = KudosContextHolder.get()
        val tenantId = kudosContext.tenantId
        if (annotation.key.isBlank()) {
            val className = joinPoint.target.javaClass.name
            val methodName = signature.method.name
            val paramType = signature.method.parameterTypes.joinToString("-") { it.typeName }
            return arrayOf(
                kudosContext.atomicServiceCode,
                tenantId,
                className,
                methodName,
                paramType
            ).joinToString(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        }

        val context = MethodBasedEvaluationContext(
            TypedValue.NULL,
            signature.method,
            joinPoint.args,
            parameterNameDiscoverer
        )
        val result = spelParser.parseExpression(annotation.key).getValue(context, String::class.java)
        return "$tenantId::$result"
    }
}
