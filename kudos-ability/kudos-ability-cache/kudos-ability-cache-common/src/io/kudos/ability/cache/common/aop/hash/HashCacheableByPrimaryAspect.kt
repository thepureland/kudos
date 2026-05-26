package io.kudos.ability.cache.common.aop.hash

import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.SpelExpressionCache
import io.kudos.base.model.contract.entity.IIdEntity
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.cache.annotation.CacheConfig
import org.springframework.context.annotation.Lazy
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction

/**
 * [HashCacheableByPrimary] aspect: first queries the Hash cache by the primary property (id); on a miss, executes the
 * method and writes the result back, optionally with secondary properties to build Set/ZSet indexes.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Aspect
@Component
@Lazy(false)
@ConditionalOnBean(MixHashCacheManager::class)
@Order(0) // Same order as other single-record Cacheable aspects; these annotations are mutually exclusive, so the relative
          // order does not affect behavior. The annotation mainly avoids the nondeterminism of the default LOWEST_PRECEDENCE.
class HashCacheableByPrimaryAspect {

    /** Parameter name discoverer used for SpEL expression evaluation; obtained as a singleton from [SpelExpressionCache]. */
    private val nameDiscoverer = SpelExpressionCache.parameterNameDiscoverer

    /** Pointcut: matches methods annotated with [HashCacheableByPrimary]. */
    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary)")
    fun cut() {}

    /**
     * Primary-key hash cache aspect:
     * 1. Resolve the annotation's cacheName / condition / key (via SpEL).
     * 2. When the condition passes and the cache is active, first query the [HashCacheKit] hash cache; on hit return directly.
     * 3. On miss, invoke the original method.
     * 4. After the unless condition passes, write the returned [IIdEntity] back to the hash cache (together with the
     *    filterable/sortable properties).
     *
     * @param joinPoint AOP join point
     * @return cache hit value or the method execution result
     * @author K
     * @since 1.0.0
     */
    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val function = method.kotlinFunction ?: return joinPoint.proceed()
        val ann = function.findAnnotation<HashCacheableByPrimary>() ?: return joinPoint.proceed()

        val cacheName = resolveCacheName(joinPoint, ann) ?: return joinPoint.proceed()
        val context = MethodBasedEvaluationContext(null, method, joinPoint.args, nameDiscoverer)

        if (ann.condition.isNotBlank()) {
            val conditionResult = SpelExpressionCache.get(ann.condition).getValue(context, Boolean::class.java)
            if (conditionResult != true) return joinPoint.proceed()
        }

        val keyValue = SpelExpressionCache.get(ann.key).getValue(context) ?: return joinPoint.proceed()
        val entityClass = resolveEntityClass(ann.entityClass)

        val hashCache = HashCacheKit.getHashCache(cacheName)
        if (KeyValueCacheKit.isCacheActive(cacheName)) {
            hashCache.getById(cacheName, keyValue, entityClass)?.let { return it }
        }

        val result = joinPoint.proceed()

        if (result == null) return result
        if (ann.unless.isNotBlank()) {
            val unlessContext = MethodBasedEvaluationContext(result, method, joinPoint.args, nameDiscoverer)
            unlessContext.setVariable("result", result)
            val unlessResult = SpelExpressionCache.get(ann.unless).getValue(unlessContext, Boolean::class.java)
            if (unlessResult == true) return result
        }

        if (result is IIdEntity<*> && KeyValueCacheKit.isCacheActive(cacheName) && KeyValueCacheKit.isWriteInTime(cacheName)) {
            val filterable = ann.filterableProperties.toSet()
            val sortable = ann.sortableProperties.toSet()
            @Suppress("UNCHECKED_CAST")
            hashCache.save(cacheName, result as IIdEntity<Any?>, filterable, sortable)
        }

        return result
    }

    /**
     * Resolve the cache name: prefer the first of [HashCacheableByPrimary.cacheNames]; when not set on the annotation,
     * fall back to the first of `@CacheConfig.cacheNames` on the target class; returns null if neither is configured
     * (the aspect silently proceeds).
     *
     * @param joinPoint AOP join point
     * @param ann annotation instance
     * @return cache name, or null if not configured
     * @author K
     * @since 1.0.0
     */
    private fun resolveCacheName(joinPoint: ProceedingJoinPoint, ann: HashCacheableByPrimary): String? {
        if (ann.cacheNames.isNotEmpty()) return ann.cacheNames.first()
        val cacheConfig = joinPoint.target::class.findAnnotation<CacheConfig>()
        if (cacheConfig != null && cacheConfig.cacheNames.isNotEmpty()) return cacheConfig.cacheNames.first()
        return null
    }

    /**
     * Cast the wildcard `KClass<out IIdEntity<*>>` to `KClass<out IIdEntity<Any?>>`.
     * Extracted separately to localize the `@Suppress("UNCHECKED_CAST")` warning and keep the main flow tidy.
     *
     * @param entityClass wildcard entity type
     * @return narrowed entity type
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    private fun resolveEntityClass(entityClass: KClass<out IIdEntity<*>>): KClass<out IIdEntity<Any?>> {
        return entityClass as KClass<out IIdEntity<Any?>>
    }
}
