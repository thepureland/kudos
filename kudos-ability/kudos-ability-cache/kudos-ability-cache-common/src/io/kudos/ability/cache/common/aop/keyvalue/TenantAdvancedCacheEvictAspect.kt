package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.ability.cache.common.aop.keyvalue.process.IRemoteCacheProcessor
import io.kudos.context.core.KudosContextHolder
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Aspect for [TenantAdvancedCacheEvict]: after the method returns, assembles the remote key as `cacheKey::tenantId`
 * and calls [IRemoteCacheProcessor.clearCache] to clear the cache.
 *
 * When no `remoteCacheProcess` implementation is injected (`required=false`), it is a no-op — this lets unit tests and
 * local-mode flows still work end-to-end.
 *
 * @author K
 * @since 1.0.0
 */
@Aspect
@Lazy(false)
@Component
@Order(-100) // Same semantics as [TenantCachingAspect] (evict after write); kept at the same order so that the relative
             // order among evict paths is not unpredictable.
class TenantAdvancedCacheEvictAspect {

    @Autowired(required = false)
    private val remoteCacheProcess: IRemoteCacheProcessor? = null

    /**
     * Defines the pointcut.
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.keyvalue.TenantAdvancedCacheEvict)")
    fun cut() {
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
