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

/**
 * Advanced tenant cache aspect (hash structure + TTL).
 *
 * Difference from a plain `@Cacheable`: uses [IRemoteCacheProcessor] to go through a Redis hash structure where each
 * (cacheKey, dataKey) tuple maps to one cache entry, with per-entry TTL support.
 * `cacheKey` automatically appends a tenant id suffix to enforce multi-tenant isolation.
 *
 * When `remoteCacheProcess` is missing, the aspect silently proceeds (the injection is `required=false`), so upper-layer
 * applications can still start without a remote cache implementation.
 *
 * @author K
 * @since 1.0.0
 */
@Aspect
@Lazy(false)
@Component
@Order(0) // Single-record Cacheable-class aspect; mutually exclusive with other Cacheable annotations, so the relative
          // order is not critical. Explicitly marked 0 to avoid clashing with Spring's default LOWEST_PRECEDENCE.
class TenantAdvancedCacheableAspect {

    /** Remote cache processor; the aspect degrades to a no-op when null. */
    @Autowired(required = false)
    private val remoteCacheProcess: IRemoteCacheProcessor? = null

    /**
     * Defines the pointcut.
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.keyvalue.TenantAdvancedCacheable)")
    fun cut() {
    }

    /**
     * Main flow: fetch (cacheKey, dataKey) from the hash structure and return on hit; on miss, proceed and write back
     * (with TTL).
     *
     * `cacheKey` is appended with the current tenant id to enforce tenant isolation; empty results are not written back,
     * to avoid caching "meaningless null" placeholders.
     *
     * @param joinPoint join point
     * @return cache hit value or method execution result
     * @author K
     * @since 1.0.0
     */
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
        // Load the data and store it into the hash.
        return joinPoint.proceed()?.also {
            remoteCacheProcess.writeCacheData(cacheKey, dataKey, it, timeOut)
        }
    }
}
