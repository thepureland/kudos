package io.kudos.ability.cache.common.aop.keyvalue

import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.support.TenantCacheKeyGenerator
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.TransactionTool
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.lang.reflect.Method

/**
 * Tenant cache aspect.
 *
 * Implements the AOP aspect for the @TenantCaching annotation, supporting tenant-isolated cache eviction.
 *
 * Core features:
 * 1. Pre-method eviction: supports clearing the cache before the method runs (beforeInvocation=true).
 * 2. Post-method eviction: supports clearing the cache after the method runs (after the transaction commits).
 * 3. Tenant isolation: uses TenantCacheKeyGenerator to generate cache keys containing tenant information.
 * 4. Pattern eviction: supports clearing the entire tenant namespace by pattern.
 *
 * Workflow:
 * 1. Intercepts methods annotated with @TenantCaching.
 * 2. Pre-eviction: iterates the evicts configuration and performs the beforeInvocation=true eviction operations.
 * 3. Executes the original method: calls pjp.proceed() to invoke the original method.
 * 4. Post-transaction eviction: performs beforeInvocation=false eviction operations after the transaction commits.
 *
 * Transaction synchronization:
 * - Uses TransactionTool.doAfterTransactionCommit to ensure execution after the transaction commits.
 * - Avoids clearing the cache on transaction rollback, preserving data consistency.
 * - If the transaction commit fails, the eviction operation is not performed.
 *
 * Eviction strategies:
 * - allEntries=true: pattern-clears the entire tenant namespace (may affect performance).
 * - allEntries=false: clears a single key.
 *
 * Notes:
 * - Pattern eviction may trigger a scan, with a relatively large performance cost.
 * - Eviction is performed after the transaction commits, ensuring data has been persisted.
 * - Uses the tenant key generator to ensure tenant isolation.
 */
@Aspect
@Lazy(false)
@Component
// Evict aspect on write methods; takes effect before cache-read annotations (@TenantCacheable / @Cacheable, etc.).
// Priority sits between DistributedCacheGuard (-999) and single-record Cacheable (0).
@Order(-100)
class TenantCachingAspect {

    @Autowired
    private val cacheManager: MixCacheManager? = null

    @Autowired
    private val tenantCacheKeyGenerator: TenantCacheKeyGenerator? = null

    /**
     * Whether to rethrow to the caller when post-commit cache eviction fails.
     * - false (default): only logs an error and counts; the transaction has already committed and the caller has already
     *   returned, so throwing inside the after-commit callback is just noise and cannot be recovered from.
     * - true: rethrows the original exception (unwrapped) so a transaction listener can intercept it for alerts/fallback
     *   — the business side must handle it.
     */
    @Value($$"${kudos.ability.cache.evict.throwOnAfterCommitFailure:false}")
    private var throwOnAfterCommitFailure: Boolean = false

    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.keyvalue.TenantCaching)")
    fun cut() {
    }

    /**
     * Around advice that implements the tenant cache eviction logic.
     *
     * Clears the cache before and after method execution according to the configuration, with transaction synchronization
     * support.
     *
     * Workflow:
     * 1. Get the method annotation: extract the @TenantCaching configuration.
     * 2. Pre-eviction: iterate over evicts and perform beforeInvocation=true evictions.
     * 3. Execute the original method: call pjp.proceed() to invoke the business method.
     * 4. Register post-commit eviction: use TransactionTool to register a post-commit eviction task.
     * 5. Return the result: return the business method's return value.
     *
     * Transaction synchronization:
     * - Eviction is performed after the transaction commits.
     * - On transaction rollback, eviction is not performed.
     * - Ensures the cache is consistent with the database.
     *
     * @param pjp pointcut information
     * @return the business method's return value
     */
    @Around("cut()")
    fun around(pjp: ProceedingJoinPoint): Any? {
        val method = (pjp.signature as MethodSignature).method
        val multicast = method.getAnnotation(TenantCaching::class.java)
        val target = pjp.target
        val args = pjp.args

        // Evict before the method runs.
        for (ev in multicast.evicts) {
            if (ev.beforeInvocation) {
                doEvict(ev, target, method, args)
            }
        }
        val result = pjp.proceed()
        TransactionTool.doAfterTransactionCommit {
            try {
                doAfterEvict(pjp, multicast, target, method, args)
            } catch (e: Throwable) {
                // This is an after-commit callback; the business thread has already returned the result. Throwing a
                // RuntimeException up from here is invisible to the caller -> the cache-eviction failure would be silently
                // swallowed, leaving stale cache invisible to downstream observers. Replaced with an explicit error log
                // + stack trace, plus a configurable rethrow.
                log.error(
                    e,
                    "Failed to clear cache after transaction commit - stale cache may result. class={0} method={1} caches={2}",
                    target::class.java.name,
                    method.name,
                    multicast.evicts.flatMap { it.cacheNames.toList() }.joinToString(",")
                )
                if (throwOnAfterCommitFailure) throw e
            }
        }
        return result
    }

    companion object {
        /** Logger; logs ERROR + stack trace on after-commit failures to aid in diagnosing stale cache. */
        private val log = LogFactory.getLog(TenantCachingAspect::class)
    }

    /**
     * Performs eviction after the transaction commits.
     *
     * Iterates the evicts configuration and performs beforeInvocation=false eviction operations.
     *
     * @param pjp pointcut information (unused)
     * @param multicast the @TenantCaching annotation
     * @param target target object
     * @param method target method
     * @param args method arguments
     */
    @Throws(Throwable::class)
    private fun doAfterEvict(
        pjp: ProceedingJoinPoint?, multicast: TenantCaching,
        target: Any, method: Method, args: Array<Any?>
    ) {
        // Evict after the method runs.
        for (ev in multicast.evicts) {
            if (!ev.beforeInvocation) {
                doEvict(ev, target, method, args)
            }
        }
    }

    /**
     * Performs the cache eviction operation.
     *
     * Clears the specified cache key or the entire tenant namespace according to the configuration.
     *
     * Workflow:
     * 1. Iterate the cache names: perform eviction for each configured cache name.
     * 2. Get the cache instance: obtain the cache instance from the cache manager.
     * 3. Generate the cache key: use TenantCacheKeyGenerator to generate a key containing tenant information.
     * 4. Perform eviction:
     *    - allEntries=true: pattern-clears the entire tenant namespace.
     *    - allEntries=false: clears a single key.
     *
     * Tenant isolation:
     * - The key generator automatically adds the tenant prefix.
     * - Pattern eviction only affects the current tenant's cache.
     * - Ensures data isolation in a multi-tenant environment.
     *
     * Performance considerations:
     * - Pattern eviction may trigger a scan, with a relatively large performance cost.
     * - Prefer single-key eviction.
     * - Pattern eviction is suitable for batch update scenarios.
     *
     * @param ev cache eviction configuration
     * @param target target object
     * @param m target method
     * @param args method arguments
     */
    private fun doEvict(
        ev: TenantCacheEvict,
        target: Any, m: Method, args: Array<Any?>
    ) {
        for (cacheName in ev.cacheNames) {
            val cache = cacheManager!!.getCache(cacheName)
            if (cache != null) {
                val key = tenantCacheKeyGenerator!!.generalNormalKey(target, m, ev.suffix, *args)
                if (ev.allEntries) {
                    // Clear the entire tenant namespace (keyGenerator already added the prefix).
                    // Triggering a scan here may cause performance issues.
                    cacheManager.evictByPattern(cacheName, key.toString())
                } else {
                    // Delete a single key.
                    cache.evict(key)
                }
            }
        }
    }
}
