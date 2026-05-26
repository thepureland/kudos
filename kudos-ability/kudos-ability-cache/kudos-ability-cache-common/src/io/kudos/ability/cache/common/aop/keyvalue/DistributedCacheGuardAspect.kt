package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.SpelExpressionCache
import io.kudos.ability.cache.common.support.TenantCacheKeyGenerator
import io.kudos.base.logger.LogFactory
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
import org.springframework.core.annotation.Order

/**
 * Distributed cache breakdown-guard aspect.
 *
 * Works with `@Cacheable` / `@TenantCacheable`: on cache miss, serializes the source-of-truth load through a distributed
 * lease lock to avoid cache breakdown.
 * Ordered ahead of `@Cacheable` (`@Order(-999)`, higher priority than Spring's default `0`).
 *
 * @author K
 * @since 1.0.0
 */
@Aspect
@Lazy(false)
@Order(-999)
class DistributedCacheGuardAspect {

    /** Parameter name discoverer used for SpEL expression evaluation. */
    private val nameDiscoverer = SpelExpressionCache.parameterNameDiscoverer

    /** Pointcut: matches methods annotated with [DistributedCacheGuard]. */
    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.keyvalue.DistributedCacheGuard)")
    fun cut() {
    }

    /**
     * Main breakdown-guard flow:
     *
     * 1. Query the cache without a lock; return on hit.
     * 2. On miss, acquire a distributed lock with a lease via [tryLock] — lease-based rather than a blocking lock, to
     *    avoid a deadlock if the process crashes.
     * 3. Lock acquisition fails: back off briefly, then query the cache again (reuse another thread's loaded result on
     *    hit); on persistent miss, fall through to proceed.
     * 4. Lock acquisition succeeds: double-check inside the lock, then proceed; release the lock in finally. Lock release
     *    failures only emit a WARN and rely on lease expiration as a fallback.
     *
     * @param pjp AOP join point
     * @return cache value or method execution result
     * @author K
     * @since 1.0.0
     */
    @Around("cut()")
    fun around(pjp: ProceedingJoinPoint): Any? {
        val cachePair = getCachePair(pjp)
        val cacheName = cachePair.first
        val cacheKey = cachePair.second
        val lockKey = "lock:$cacheName:$cacheKey"

        // 1. Query the cache once without a lock; return on hit.
        KeyValueCacheKit.getValue(cacheName, cacheKey)?.let { return it }

        // 2. Miss: compete for the distributed lock. Use a lease-based tryLock:
        //    The previous implementation called lockProvider.lock(key), which under the Redisson path is `RLock.lock()`,
        //    a blocking call with no TTL -> if the process crashed inside the loader, the same key would be stuck and
        //    recovery would require manually deleting the key from Redis.
        //    Now using tryLock(key, leaseSec): after a crash the lease expires automatically; the worst case is one
        //    extra source-of-truth read, with no more deadlocks.
        val gotLock = lockProvider.tryLock(lockKey, lockLeaseSeconds)
        if (!gotLock) {
            // Failing to acquire the lock means another thread/node is already loading this key; rather than blocking
            // forever, back off briefly and then query the cache again:
            //   - Hit: reuse the result loaded by the other thread, saving one proceed.
            //   - Still miss: fall through to proceed; the worst case degrades to one extra source-of-truth read (the
            //     worst-case tolerance at the "breakdown-guard" level).
            // The old "block forever waiting for the lock" semantics are intentionally not preserved: they amplified
            // the downstream failure blast radius and depended on the lock having a TTL.
            try {
                Thread.sleep(lockBackoffMillis)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.debug("Interrupted while waiting for distributed cache lock lockKey={0}", lockKey)
            }
            return KeyValueCacheKit.getValue(cacheName, cacheKey) ?: pjp.proceed()
        }
        return try {
            // Double-check inside the lock.
            KeyValueCacheKit.getValue(cacheName, cacheKey) ?: pjp.proceed()
        } finally {
            try {
                lockProvider.unLock(lockKey)
            } catch (e: Exception) {
                // The release itself should not propagate errors into the business path; in the worst case, the lease
                // expiration acts as a fallback.
                log.warn("Failed to release distributed cache lock (will rely on lease expiration) lockKey={0}, err={1}", lockKey, e.message)
            }
        }
    }

    /**
     * Resolve (cacheName, cacheKey) from the method's `@Cacheable` / `@TenantCacheable` annotation.
     *
     * The two annotations are mutually exclusive but semantically similar — `@Cacheable` uses SpEL;
     * `@TenantCacheable` uses [TenantCacheKeyGenerator] to automatically add a tenant prefix. This method normalizes
     * both into `Pair<cacheName, cacheKey>` so that [around] only needs to focus on breakdown-guard logic.
     *
     * @param pjp join point
     * @return (cacheName, cacheKey)
     * @throws IllegalStateException when neither annotation is present
     * @author K
     * @since 1.0.0
     */
    private fun getCachePair(pjp: ProceedingJoinPoint): Pair<String, Any> {
        // 1. Get the target method and arguments.
        val signature = pjp.signature as MethodSignature
        val method = signature.method
        // 2. Get the @Cacheable annotation on the method.
        val cacheable = method.getAnnotation(Cacheable::class.java)
        val tenantCacheable = method.getAnnotation(TenantCacheable::class.java)
        if (cacheable != null) {
            val cacheName = cacheable.value.firstOrNull()
                ?: cacheable.cacheNames.firstOrNull()
                ?: error("@Cacheable must specify value or cacheNames")
            val keySpel = cacheable.key
            require(keySpel.isNotEmpty()) { "@Cacheable.key must be specified" }
            val context = MethodBasedEvaluationContext(null, method, pjp.args, nameDiscoverer)
            val cacheKey = SpelExpressionCache.get(keySpel).getValue(context, String::class.java)
                ?: error("@Cacheable.key resolved to empty: $keySpel")
            return cacheName to cacheKey
        }
        checkNotNull(tenantCacheable) { "@DistributedCacheGuard can only be used together with @Cacheable or @TenantCacheable!" }
        val cacheName = tenantCacheable.value.firstOrNull()
            ?: tenantCacheable.cacheNames.firstOrNull()
            ?: error("@TenantCacheable must specify value or cacheNames")
        val cacheKey = SpringKit.getBean<TenantCacheKeyGenerator>()
            .generalNormalKey(pjp.target, method, tenantCacheable.suffix, *pjp.args)
        return cacheName to cacheKey
    }

    companion object {
        /** Reuse the global [LockTool.lockProvider] to avoid fetching the bean from the container on every advice. */
        private val lockProvider = LockTool.lockProvider
        /** Logger. */
        private val log = LogFactory.getLog(DistributedCacheGuardAspect::class)

        /**
         * Lease duration (seconds): long enough to accommodate one "load from source and write to cache" round trip;
         * the upper bound defines the worst-case dead-lock window after a crash.
         * 30s is an empirical value; adjust or consider sharding if the business has significant slow loads.
         */
        private const val lockLeaseSeconds = 30

        /**
         * Backoff duration (milliseconds) after failing to acquire the lock; used for a single "short wait + re-read
         * the cache".
         * Do not retry in a loop: that is equivalent to restoring the old block-on-lock semantics and amplifies the
         * downstream failure blast radius.
         */
        private const val lockBackoffMillis = 200L
    }
}
