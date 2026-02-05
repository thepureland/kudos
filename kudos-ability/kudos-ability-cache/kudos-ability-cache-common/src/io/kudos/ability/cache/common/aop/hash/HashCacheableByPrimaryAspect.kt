package io.kudos.ability.cache.common.aop.hash

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.base.support.IIdEntity
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
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
 * [HashCacheableByPrimary] 切面：先按主属性（id）查 Hash 缓存，未命中则执行方法并回写；回写时可带副属性以建 Set/ZSet 索引。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Aspect
@Component
@Lazy(false)
@ConditionalOnBean(MixHashCacheManager::class)
class HashCacheableByPrimaryAspect {

    private val parser: ExpressionParser = SpelExpressionParser()
    private val nameDiscoverer = DefaultParameterNameDiscoverer()

    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary)")
    fun cut() {}

    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val function = method.kotlinFunction ?: return joinPoint.proceed()
        val ann = function.findAnnotation<HashCacheableByPrimary>() ?: return joinPoint.proceed()

        val cacheName = resolveCacheName(joinPoint, ann) ?: return joinPoint.proceed()
        val context = MethodBasedEvaluationContext(null, method, joinPoint.args, nameDiscoverer)

        if (ann.condition.isNotBlank()) {
            val conditionResult = parser.parseExpression(ann.condition).getValue(context, Boolean::class.java)
            if (conditionResult != true) return joinPoint.proceed()
        }

        val keyValue = parser.parseExpression(ann.key).getValue(context) ?: return joinPoint.proceed()
        val entityClass = ann.entityClass as KClass<out IIdEntity<Any?>>

        val hashCache = HashCacheKit.getHashCache(cacheName)
        if (CacheKit.isCacheActive(cacheName)) {
            @Suppress("UNCHECKED_CAST")
            val cached = hashCache.getById(cacheName, keyValue, entityClass)
            if (cached != null) return cached
        }

        val result = joinPoint.proceed()

        if (result == null) return result
        if (ann.unless.isNotBlank()) {
            val unlessContext = MethodBasedEvaluationContext(result, method, joinPoint.args, nameDiscoverer)
            unlessContext.setVariable("result", result)
            val unlessResult = parser.parseExpression(ann.unless).getValue(unlessContext, Boolean::class.java)
            if (unlessResult == true) return result
        }

        if (result is IIdEntity<*> && CacheKit.isCacheActive(cacheName) && CacheKit.isWriteInTime(cacheName)) {
            val filterable = ann.filterableProperties.toSet()
            val sortable = ann.sortableProperties.toSet()
            @Suppress("UNCHECKED_CAST")
            hashCache.save(cacheName, result as IIdEntity<Any?>, filterable, sortable)
        }

        return result
    }

    private fun resolveCacheName(joinPoint: ProceedingJoinPoint, ann: HashCacheableByPrimary): String? {
        if (ann.cacheNames.isNotEmpty()) return ann.cacheNames.first()
        val cacheConfig = joinPoint.target::class.findAnnotation<CacheConfig>()
        if (cacheConfig != null && cacheConfig.cacheNames.isNotEmpty()) return cacheConfig.cacheNames.first()
        return null
    }
}
