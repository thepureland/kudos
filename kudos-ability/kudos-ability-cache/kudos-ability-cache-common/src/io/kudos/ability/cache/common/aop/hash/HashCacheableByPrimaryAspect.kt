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
@Order(0) // 与其他单条 Cacheable 切面同序；这些注解互斥，相对顺序不影响行为，标注主要是避免默认 LOWEST_PRECEDENCE 带来的不确定。
class HashCacheableByPrimaryAspect {

    /** SpEL 表达式求值用的参数名发现器，从 [SpelExpressionCache] 拿单例 */
    private val nameDiscoverer = SpelExpressionCache.parameterNameDiscoverer

    /** 切点：匹配标注 [HashCacheableByPrimary] 的方法 */
    @Pointcut("@annotation(io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary)")
    fun cut() {}

    /**
     * 主键 hash 缓存切面：
     * 1. 解析注解 cacheName / condition / key（走 SpEL）；
     * 2. 命中条件 + cache 激活时，先查 [HashCacheKit] hash 缓存；命中直接返回；
     * 3. miss 时调用原方法；
     * 4. 通过 unless 条件后，把返回的 [IIdEntity] 写回 hash 缓存（连同 filterable/sortable 属性）。
     *
     * @param joinPoint AOP 切入点
     * @return 缓存命中或方法执行结果
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
     * 解析 cache 名：优先取 [HashCacheableByPrimary.cacheNames] 第一个；
     * 注解未设时回落到目标类上的 `@CacheConfig.cacheNames` 第一个；都没有返回 null（切面静默 proceed）。
     *
     * @param joinPoint AOP 切入点
     * @param ann 注解实例
     * @return cache 名，未配置返回 null
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
     * 把通配的 `KClass<out IIdEntity<*>>` 强转为 `KClass<out IIdEntity<Any?>>`。
     * 单独抽出来集中 `@Suppress("UNCHECKED_CAST")` 警告，主流程更整洁。
     *
     * @param entityClass 通配实体类型
     * @return 已收窄的实体类型
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    private fun resolveEntityClass(entityClass: KClass<out IIdEntity<*>>): KClass<out IIdEntity<Any?>> {
        return entityClass as KClass<out IIdEntity<Any?>>
    }
}
