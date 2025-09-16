package io.kudos.ability.cache.common.aop

import io.kudos.ability.cache.common.aop.process.IRemoteCacheProcessor
import io.kudos.context.core.KudosContextHolder
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy

@Aspect
@Lazy(false)
class TenantAdvancedCacheEvictAspect {

    @Autowired(required = false)
    private val remoteCacheProcess: IRemoteCacheProcessor? = null

    /**
     * 定义切入点
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.TenantAdvancedCacheEvict)")
    private fun cut() {
    }

    @After("cut()")
    @Throws(Throwable::class)
    fun afterAdvice(joinPoint: JoinPoint) {
        if (remoteCacheProcess == null) {
            return
        }
        val signature = joinPoint.signature as MethodSignature
        val cacheable = signature.method.getAnnotation(TenantAdvancedCacheEvict::class.java)
        val tenantId = KudosContextHolder.get().tenantId
        val cacheKey = "${cacheable.cacheKey}::$tenantId"
        remoteCacheProcess.clearCache(cacheKey, cacheable.dataKey, cacheable.allEntries)
    }
}
