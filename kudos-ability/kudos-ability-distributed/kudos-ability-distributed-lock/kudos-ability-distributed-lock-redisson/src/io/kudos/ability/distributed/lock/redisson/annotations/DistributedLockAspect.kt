package io.kudos.ability.distributed.lock.redisson.annotations

import io.kudos.ability.distributed.lock.common.annotations.DistributedLock
import io.kudos.ability.distributed.lock.common.locker.DistributedLockContext
import io.kudos.ability.distributed.lock.redisson.kit.RedissonLockKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.context.core.KudosContextHolder
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
 * 分布式锁切面
 * 通过AOP方式为方法添加分布式锁功能，支持SpEL表达式动态生成锁键
 */
@Aspect
@Lazy(false)
class DistributedLockAspect {
    private val log = LogFactory.getLog(this)
    private val parameterNameDiscoverer = DefaultParameterNameDiscoverer()

    @Pointcut("@annotation(io.kudos.ability.distributed.lock.common.annotations.DistributedLock)")
    fun cut() {
    }

    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(DistributedLock::class.java)
        val lockKey = genLockKey(joinPoint)
        var res = false
        var obj: Any? = null
        try {
            //尝试获取分布式锁
            log.debug("尝试获取分布式锁：key=$lockKey")
            res = RedissonLockKit.tryLock(lockKey, TimeUnit.SECONDS, annotation.waitTime, annotation.leaseTime)
        } catch (e: Throwable) {
            log.warn("无法取得分布式锁，可能有任务还未完成." + e.message)
        }
        lockCallback(res, lockKey)
        if (!res) {
            log.warn("无法取得分布式锁，可能有任务还未完成.key = $lockKey")
        } else {
            try {
                obj = joinPoint.proceed()
            } catch (e: Throwable) {
                throw RuntimeException(e)
            } finally {
                log.debug("释放锁：key=$lockKey")
                try {
                    RedissonLockKit.unlock(lockKey)
                } catch (e: Exception) {
                    log.warn("释放锁异常，可能超时被自动释放.message={0}", e.message)
                }
            }
        }
        return obj
    }

    /**
     * 上锁后回调
     * @param success
     * @param lockKey
     */
    private fun lockCallback(success: Boolean, lockKey: String) {
        val lockCallback = DistributedLockContext.get()
        if (lockCallback != null) {
            if (success) {
                lockCallback.doLockSuccess(lockKey)
            } else {
                lockCallback.doLockFail(lockKey)
            }
        }
    }

    private fun genLockKey(joinPoint: ProceedingJoinPoint): String {
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(DistributedLock::class.java)
        val kudosContext = KudosContextHolder.get()
        val tenantId = kudosContext.tenantId
        if (annotation.key.isNullOrBlank()) {
            val classNme = joinPoint.target.javaClass.getName()
            val methodName = signature.method.name
            var paramType = ""
            val parameterTypes = signature.method.parameterTypes
            if (parameterTypes.size > 0) {
                for (parameterType in parameterTypes) {
                    paramType += "-" + parameterType.getTypeName()
                }
            }
            paramType = paramType.replaceFirst("-".toRegex(), "")
            return arrayOf(kudosContext.atomicServiceCode, tenantId, classNme, methodName, paramType)
                .joinToString(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        }

        val context = MethodBasedEvaluationContext(
            TypedValue.NULL,
            signature.method,
            joinPoint.args,
            parameterNameDiscoverer
        )
        val parser = SpelExpressionParser()
        val expression = parser.parseExpression(annotation.key)
        val result = expression.getValue(context, String::class.java)
        return "$tenantId::$result"
    }
}
