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
 * [HashCacheableBySecondary] 切面：先按副属性等值（listBySetIndex）查缓存，未命中则执行方法并将结果 saveBatch 回写。
 * 适用于 Caffeine、Redis 等任意 Hash 缓存实现。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Aspect
@Component
@Lazy(false)
@ConditionalOnBean(MixHashCacheManager::class)
class HashCacheableBySecondaryAspect {

    private val parser: ExpressionParser = SpelExpressionParser()
    private val nameDiscoverer = DefaultParameterNameDiscoverer()

    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary)")
    fun cut() {}

    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val function = method.kotlinFunction ?: return joinPoint.proceed()
        val ann = function.findAnnotation<HashCacheableBySecondary>() ?: return joinPoint.proceed()

        val cacheName = resolveCacheName(joinPoint, ann) ?: return joinPoint.proceed()
        val context = MethodBasedEvaluationContext(null, method, joinPoint.args, nameDiscoverer)

        if (ann.condition.isNotBlank()) {
            val conditionResult = parser.parseExpression(ann.condition).getValue(context, Boolean::class.java)
            if (conditionResult != true) return joinPoint.proceed()
        }

        val indexValue = parser.parseExpression(ann.key).getValue(context) ?: return joinPoint.proceed()
        val entityClass = ann.entityClass as KClass<out IIdEntity<Any?>>
        val property = ann.property

        val hashCache = HashCacheKit.getHashCache(cacheName)
        if (hashCache != null && CacheKit.isCacheActive(cacheName)) {
            @Suppress("UNCHECKED_CAST")
            val cached = hashCache.listBySetIndex(cacheName, entityClass, property, indexValue)
            if (cached.isNotEmpty()) return cached
        }

        val result = joinPoint.proceed()

        if (result == null) return result
        if (ann.unless.isNotBlank()) {
            val unlessContext = MethodBasedEvaluationContext(result, method, joinPoint.args, nameDiscoverer)
            unlessContext.setVariable("result", result)
            val unlessResult = parser.parseExpression(ann.unless).getValue(unlessContext, Boolean::class.java)
            if (unlessResult == true) return result
        }

        val list = (result as? List<*>)?.filterIsInstance<IIdEntity<*>>() ?: return result
        if (list.isNotEmpty() && hashCache != null && CacheKit.isCacheActive(cacheName) && CacheKit.isWriteInTime(cacheName)) {
            val filterable = ann.filterableProperties.toSet()
            val sortable = ann.sortableProperties.toSet()
            @Suppress("UNCHECKED_CAST")
            hashCache.saveBatch(cacheName, list as List<IIdEntity<Any?>>, filterable, sortable)
        }

        return result
    }

    private fun resolveCacheName(joinPoint: ProceedingJoinPoint, ann: HashCacheableBySecondary): String? {
        if (ann.cacheNames.isNotEmpty()) return ann.cacheNames.first()
        val cacheConfig = joinPoint.target::class.findAnnotation<CacheConfig>()
        if (cacheConfig != null && cacheConfig.cacheNames.isNotEmpty()) return cacheConfig.cacheNames.first()
        return null
    }
}
