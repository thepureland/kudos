package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.TenantCacheKeyGenerator
import io.kudos.context.kit.SpringKit
import io.kudos.context.lock.LockTool
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.annotation.Order
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import java.util.concurrent.locks.Lock

/**
 * 优先于cacheable执行
 */
@Aspect
@Lazy(false)
@Order(-999)
class DistributedCacheGuardAspect {

    private val parser: ExpressionParser = SpelExpressionParser()
    private val nameDiscoverer = DefaultParameterNameDiscoverer()

    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.keyvalue.DistributedCacheGuard)")
    private fun cut() {
    }

    @Around("cut()")
    fun around(pjp: ProceedingJoinPoint): Any? {
        // 2. 解析cache属性
        val cachePair = getCachePair(pjp)
        val cacheName = cachePair.first
        val cacheKey = cachePair.second

        val lockKey = "lock:$cacheName:$cacheKey"
        // 3. 先查缓存,有检查到直接返回
        var cached = CacheKit.getValue(cacheName, cacheKey)
        if (cached != null) {
            return cached
        }
        // 4. 没检查到，则同一个key进行竞争锁
        val lock = lockProvider.lock(lockKey) as Lock
        try {
            // 5. 锁内双重检查
            cached = CacheKit.getValue(cacheName, cacheKey)
            if (cached != null) {
                return cached
            }
            // 6. 没有缓存才放行 @Cacheable 方法
            return pjp.proceed()
        } finally {
            lockProvider.unLock(lock, lockKey)
        }
    }

    private fun getCachePair(pjp: ProceedingJoinPoint): Pair<String, Any> {
        // 1. 获取目标方法和参数
        val signature = pjp.signature as MethodSignature
        val method = signature.method
        // 2. 获取方法上的 @Cacheable 注解
        val cacheable = method.getAnnotation(Cacheable::class.java)
        val tenantCacheable = method.getAnnotation(TenantCacheable::class.java)
        check(!(cacheable == null && tenantCacheable == null)) { "@DistributedCacheGuard 只能和 @Cacheable或@TenantCacheable 一起用！" }
        if (cacheable != null) {
            // 3. 解析 cacheName 和 key
            val cacheName: String =
                (if (cacheable.value.isNotEmpty()) cacheable.value[0] else (if (cacheable.cacheNames.isNotEmpty()) cacheable.cacheNames[0] else null))!!
            requireNotNull(cacheName) { "@Cacheable.value 必须指定缓存名" }

            val keySpel = cacheable.key
            require(!(keySpel.isEmpty())) { "@Cacheable.key 必须指定" }
            // 4. 解析 SpEL key
            val context = MethodBasedEvaluationContext(
                null, method, pjp.args, nameDiscoverer
            )
            val cacheKey = parser.parseExpression(keySpel).getValue<String>(context, String::class.java)
            return Pair<String, Any>(cacheName, cacheKey!!)
        } else {
            // 3. 解析 cacheName 和 key
            val cacheName =
                if (tenantCacheable.value.isNotEmpty()) tenantCacheable.value[0] else (if (tenantCacheable.cacheNames.isNotEmpty()) tenantCacheable.cacheNames[0] else null)
            val cacheKey = SpringKit.getBean(TenantCacheKeyGenerator::class)
                .generalNormalKey(pjp.target, method, tenantCacheable.suffix, *pjp.getArgs())
            return Pair(cacheName!!, cacheKey)
        }
    }

    companion object {
        private val lockProvider = LockTool.lockProvider
    }
}
