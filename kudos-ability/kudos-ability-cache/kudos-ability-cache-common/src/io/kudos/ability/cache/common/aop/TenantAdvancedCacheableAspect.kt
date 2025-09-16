package io.kudos.ability.cache.common.aop

import io.kudos.ability.cache.common.aop.process.IRemoteCacheProcessor
import io.kudos.context.core.KudosContextHolder
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy

@Aspect
@Lazy(false)
class TenantAdvancedCacheableAspect {
    @Autowired(required = false)
    private val remoteCacheProcess: IRemoteCacheProcessor? = null

    /**
     * 定义切入点
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.TenantAdvancedCacheable)")
    private fun cut() {
    }

    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        if (remoteCacheProcess == null) {
            return joinPoint.proceed()
        }
        val signature = joinPoint.signature as MethodSignature
        val cacheable = signature.method.getAnnotation(TenantAdvancedCacheable::class.java)
        var tenantId = KudosContextHolder.get().tenantId
        val cacheKey = "${cacheable.cacheKey}::$tenantId"
        val dataKey: String? = cacheable.dataKey
        val timeOut = cacheable.timeOut
        var o = remoteCacheProcess.getCacheData(cacheKey, dataKey)
        if (o == null) {
            //加载数据，并存入到hash里
            o = joinPoint.proceed()
            if (o != null) {
                remoteCacheProcess.writeCacheData(cacheKey, dataKey, o, timeOut)
            }
        }
        return o
    }
}
