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
 * 
 * 通过AOP方式为方法添加分布式锁功能，支持SpEL表达式动态生成锁键。
 * 
 * 核心功能：
 * 1. 锁键生成：支持两种方式生成锁键
 *    - 未指定key时：自动生成，格式为"服务代码::租户ID::类名::方法名::参数类型"
 *    - 指定SpEL表达式时：解析表达式动态生成锁键，支持引用方法参数和上下文信息
 * 2. 锁获取：使用tryLock方法尝试获取锁，支持配置等待时间（waitTime）和租约时间（leaseTime）
 * 3. 锁回调：获取锁成功或失败时，会触发DistributedLockContext中注册的回调处理器
 * 4. 自动释放：方法执行完成后（无论成功或异常），自动释放锁
 * 
 * 工作流程：
 * - 拦截标注@DistributedLock的方法
 * - 解析注解配置，生成锁键
 * - 尝试获取分布式锁
 * - 获取成功则执行方法，失败则直接返回null（不执行方法）
 * - 方法执行完成后释放锁
 * 
 * 注意事项：
 * - 如果获取锁失败，方法不会被执行，直接返回null
 * - 锁的释放会在finally块中执行，确保即使方法抛出异常也能正确释放
 * - 支持通过DistributedLockContext设置锁获取成功/失败的回调处理
 */
@Aspect
@Lazy(false)
class DistributedLockAspect {
    private val log = LogFactory.getLog(this)
    private val parameterNameDiscoverer = DefaultParameterNameDiscoverer()

    @Pointcut("@annotation(io.kudos.ability.distributed.lock.common.annotations.DistributedLock)")
    fun cut() {
    }

    /**
     * 环绕通知：实现分布式锁的核心逻辑
     * 
     * 工作流程：
     * 1. 解析@DistributedLock注解，获取锁配置（等待时间、租约时间等）
     * 2. 生成锁键（通过genLockKey方法）
     * 3. 尝试获取分布式锁：
     *    - 成功：执行目标方法，执行完成后释放锁
     *    - 失败：不执行目标方法，直接返回null，触发失败回调
     * 4. 无论成功或失败，都会触发相应的回调处理
     * 5. 锁的释放在finally块中执行，确保即使方法抛出异常也能正确释放
     * 
     * 异常处理：
     * - 获取锁时如果抛出异常，会记录警告日志，但不中断流程，会触发失败回调
     * - 方法执行时如果抛出异常，会包装为RuntimeException重新抛出
     * - 释放锁时如果抛出异常，会记录警告日志，但不影响方法返回值
     * 
     * 返回值：
     * - 如果获取锁失败，返回null
     * - 如果获取锁成功，返回目标方法的执行结果
     * 
     * @param joinPoint 连接点，包含目标方法的信息和参数
     * @return 目标方法的返回值，如果获取锁失败返回null
     */
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

    /**
     * 生成分布式锁的键
     * 
     * 支持两种方式生成锁键：
     * 1. 自动生成（注解中未指定key）：
     *    - 格式：服务代码::租户ID::类名::方法名::参数类型
     *    - 例如："service::1001::com.example.UserService::getUser-java.lang.String"
     *    - 参数类型使用"-"分隔，多个参数类型会拼接在一起
     * 
     * 2. SpEL表达式（注解中指定了key）：
     *    - 解析SpEL表达式，支持引用方法参数和上下文信息
     *    - 创建MethodBasedEvaluationContext，包含方法参数和参数名
     *    - 解析表达式获取结果，格式为"租户ID::表达式结果"
     *    - 例如：key="#userId"，结果为"1001::123"
     * 
     * 租户隔离：
     * - 所有锁键都会包含租户ID，确保不同租户的锁相互隔离
     * - 从KudosContext中获取当前租户ID
     * 
     * 注意事项：
     * - 自动生成的锁键包含完整的类名和方法签名，确保不同方法的锁不会冲突
     * - SpEL表达式必须返回String类型，否则会抛出异常
     * - 参数类型使用getTypeName()获取，包含完整的包路径
     * 
     * @param joinPoint 连接点，包含目标方法的信息和参数
     * @return 生成的锁键字符串
     */
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
