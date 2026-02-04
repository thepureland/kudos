package io.kudos.ability.cache.common.aop.hash

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.base.support.IIdEntity
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import io.kudos.ability.cache.common.core.MixHashCacheManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.cache.annotation.CacheConfig
import org.springframework.context.annotation.Lazy
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.stereotype.Component
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction

/**
 * [HashCacheable] 切面：先按 key(id) 查 Hash 缓存，未命中则执行方法并回写缓存。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Aspect
@Component
@Lazy(false)
@ConditionalOnBean(MixHashCacheManager::class)
class HashCacheableAspect {

    private val parser: ExpressionParser = SpelExpressionParser()
    private val nameDiscoverer = DefaultParameterNameDiscoverer()

    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.hash.HashCacheable)")
    fun cut() {}

    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val function = method.kotlinFunction ?: return joinPoint.proceed()
        val hashCacheable = function.findAnnotation<HashCacheable>() ?: return joinPoint.proceed()

        val cacheName = resolveCacheName(joinPoint, hashCacheable)
            ?: return joinPoint.proceed()
        val context = MethodBasedEvaluationContext(null, method, joinPoint.args, nameDiscoverer)

        if (hashCacheable.condition.isNotBlank()) {
            val conditionResult = parser.parseExpression(hashCacheable.condition).getValue(context, Boolean::class.java)
            if (conditionResult != true) return joinPoint.proceed()
        }

        val keyValue = parser.parseExpression(hashCacheable.key).getValue(context) ?: return joinPoint.proceed()
        val entityClass = hashCacheable.entityClass as KClass<out IIdEntity<Any?>>

        val hashCache = HashCacheKit.getHashCache(cacheName)
        if (hashCache != null && CacheKit.isCacheActive(cacheName)) {
            @Suppress("UNCHECKED_CAST")
            val cached = hashCache.getById(cacheName, keyValue, entityClass)
            if (cached != null) return cached
        }

        val result = joinPoint.proceed()

        if (result == null) return result
        if (hashCacheable.unless.isNotBlank()) {
            val unlessContext = MethodBasedEvaluationContext(result, method, joinPoint.args, nameDiscoverer)
            unlessContext.setVariable("result", result)
            val unlessResult = parser.parseExpression(hashCacheable.unless).getValue(unlessContext, Boolean::class.java)
            if (unlessResult == true) return result
        }

        if (result is IIdEntity<*> && hashCache != null && CacheKit.isCacheActive(cacheName) && CacheKit.isWriteInTime(cacheName)) {
            @Suppress("UNCHECKED_CAST")
            hashCache.save(cacheName, result as IIdEntity<Any?>, emptySet(), emptySet())
        }

        return result
    }

    private fun resolveCacheName(joinPoint: ProceedingJoinPoint, hashCacheable: HashCacheable): String? {
        if (hashCacheable.cacheNames.isNotEmpty()) return hashCacheable.cacheNames.first()
        val cacheConfig = joinPoint.target::class.findAnnotation<CacheConfig>()
        if (cacheConfig != null && cacheConfig.cacheNames.isNotEmpty()) return cacheConfig.cacheNames.first()
        return null
    }
}
