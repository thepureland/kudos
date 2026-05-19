package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.ability.cache.common.aop.keyvalue.process.IRemoteCacheProcessor
import io.kudos.context.core.KudosContextHolder
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Aspect
@Lazy(false)
@Component
@Order(0) // 单条 Cacheable 类切面，与其他 Cacheable 注解互斥，相对顺序不关键；显式标 0 避免与 Spring 默认 LOWEST_PRECEDENCE 混。
class TenantAdvancedCacheableAspect {

    @Autowired(required = false)
    private val remoteCacheProcess: IRemoteCacheProcessor? = null

    /**
     * 定义切入点
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.keyvalue.TenantAdvancedCacheable)")
    fun cut() {
    }

    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        if (remoteCacheProcess == null) {
            return joinPoint.proceed()
        }
        val signature = joinPoint.signature as MethodSignature
        val cacheable = signature.method.getAnnotation(TenantAdvancedCacheable::class.java)
        val tenantId = KudosContextHolder.get().tenantId
        val cacheKey = "${cacheable.cacheKey}::$tenantId"
        val dataKey = cacheable.dataKey
        val timeOut = cacheable.timeOut
        remoteCacheProcess.getCacheData(cacheKey, dataKey)?.let { return it }
        //加载数据，并存入到hash里
        return joinPoint.proceed()?.also {
            remoteCacheProcess.writeCacheData(cacheKey, dataKey, it, timeOut)
        }
    }
}
