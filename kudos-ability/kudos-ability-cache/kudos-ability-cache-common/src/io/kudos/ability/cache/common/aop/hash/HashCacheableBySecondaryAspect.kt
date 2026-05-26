package io.kudos.ability.cache.common.aop.hash

import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.cache.common.support.SpelExpressionCache
import io.kudos.base.bean.BeanKit
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
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction

/**
 * [HashCacheableBySecondary] aspect: first queries the cache by secondary-property equality (listBySetIndex); on a miss,
 * executes the method and writes the result back via saveBatch.
 * Supports four method return types:
 * - [List]&lt;[IIdEntity]&gt;: on hit returns the list; on miss writes the returned list back via saveBatch.
 * - [String] (nullable): on hit returns the first entity's id; on miss only executes the method (write-back is handled
 *   inside the method body).
 * - [List]&lt;[String]&gt;: on hit returns the id list; on miss only executes the method (write-back is handled inside
 *   the method body).
 * - Single object (nullable): on hit returns the first element of the cached list; on miss writes back the single
 *   returned entity (if it is an [IIdEntity]) via saveBatch and returns it.
 * Works with any Hash cache implementation such as Caffeine or Redis.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Aspect
@Component
@Lazy(false)
@ConditionalOnBean(MixHashCacheManager::class)
@Order(0) // Same as [HashCacheableByPrimaryAspect]. All Cacheable-class aspects are marked 0.
class HashCacheableBySecondaryAspect {

    private val nameDiscoverer = SpelExpressionCache.parameterNameDiscoverer

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
            val conditionResult = SpelExpressionCache.get(ann.condition).getValue(context, Boolean::class.java)
            if (conditionResult != true) return joinPoint.proceed()
        }

        val keyExpressions = ann.filterExpressions
        if (keyExpressions.isEmpty()) return joinPoint.proceed()
        val propertyValues = keyExpressions.map { expr ->
            val prop = derivePropertyFromKey(expr.trim())
                ?: throw IllegalArgumentException("HashCacheableBySecondary: each filterExpressions element must be a single-parameter SpEL (e.g. #type); current: $expr")
            val value = SpelExpressionCache.get(expr.trim()).getValue(context) ?: return joinPoint.proceed()
            prop to value
        }
        val entityClass = resolveEntityClass(ann.entityClass)
        val returnMode = resolveReturnMode(method)

        val hashCache = HashCacheKit.getHashCache(cacheName)
        if (KeyValueCacheKit.isCacheActive(cacheName)) {
            val cached = queryByKeys(hashCache, cacheName, entityClass, propertyValues)
            if (cached.isNotEmpty()) {
                val returnProperty = ann.returnProperty
                return when (returnMode) {
                    ReturnMode.SINGLE_ID -> cached.firstOrNull()?.id
                    ReturnMode.LIST_IDS -> {
                        val values = if (returnProperty.isNotBlank()) {
                            cached.mapNotNull { BeanKit.getProperty(it, returnProperty)?.toString() }.distinct()
                        } else {
                            cached.mapNotNull { it.id?.toString() }
                        }
                        if (method.returnType == java.util.Set::class.java) values.toSet() else values
                    }
                    ReturnMode.LIST_ENTITIES -> cached
                    ReturnMode.SINGLE_ENTITY -> cached.firstOrNull()
                }
            }
        }

        val result = joinPoint.proceed()

        if (result == null) return result
        if (ann.unless.isNotBlank()) {
            val unlessContext = MethodBasedEvaluationContext(result, method, joinPoint.args, nameDiscoverer)
            unlessContext.setVariable("result", result)
            val unlessResult = SpelExpressionCache.get(ann.unless).getValue(unlessContext, Boolean::class.java)
            if (unlessResult == true) return result
        }

        when (returnMode) {
            ReturnMode.SINGLE_ID, ReturnMode.LIST_IDS -> return result
            ReturnMode.LIST_ENTITIES -> {
                val list = (result as? List<*>)?.filterIsInstance<IIdEntity<*>>() ?: return result
                if (list.isNotEmpty() && KeyValueCacheKit.isCacheActive(cacheName) && KeyValueCacheKit.isWriteInTime(cacheName)) {
                    val filterable = ann.filterableProperties.toSet()
                    val sortable = ann.sortableProperties.toSet()
                    @Suppress("UNCHECKED_CAST")
                    hashCache.saveBatch(cacheName, list as List<IIdEntity<Any?>>, filterable, sortable)
                }
                return result
            }
            ReturnMode.SINGLE_ENTITY -> {
                val entity = (result as? IIdEntity<*>)?.let { listOf(it) }
                if (!entity.isNullOrEmpty() && KeyValueCacheKit.isCacheActive(cacheName) && KeyValueCacheKit.isWriteInTime(cacheName)) {
                    val filterable = ann.filterableProperties.toSet()
                    val sortable = ann.sortableProperties.toSet()
                    @Suppress("UNCHECKED_CAST")
                    hashCache.saveBatch(cacheName, entity as List<IIdEntity<Any?>>, filterable, sortable)
                }
                return result
            }
        }
    }

    /**
     * Annotates the shape of the method's return value; determines how the aspect interacts with the hash cache (read/write).
     */
    private enum class ReturnMode { SINGLE_ID, LIST_IDS, LIST_ENTITIES, SINGLE_ENTITY }

    /**
     * Determines the [ReturnMode] based on the reflected Java return type.
     *
     * - `String` -> SINGLE_ID
     * - `Set<String>` / `List<String>` -> LIST_IDS
     * - `List<...other>` -> LIST_ENTITIES
     * - other -> SINGLE_ENTITY
     *
     * After generic type erasure, `ParameterizedType` is still available, so the type is accessed via reflection on
     * `genericReturnType`.
     *
     * @param method target method
     * @return inferred return shape
     * @author K
     * @since 1.0.0
     */
    private fun resolveReturnMode(method: java.lang.reflect.Method): ReturnMode {
        val returnType = method.returnType
        if (returnType == String::class.java) return ReturnMode.SINGLE_ID
        if (Set::class.java.isAssignableFrom(returnType)) {
            val generic = method.genericReturnType
            if (generic is ParameterizedType && generic.actualTypeArguments.getOrNull(0) == String::class.java) {
                return ReturnMode.LIST_IDS
            }
        }
        if (List::class.java.isAssignableFrom(returnType)) {
            val generic = method.genericReturnType
            if (generic is ParameterizedType) {
                val actual = generic.actualTypeArguments.getOrNull(0)
                if (actual == String::class.java) return ReturnMode.LIST_IDS
            }
            return ReturnMode.LIST_ENTITIES
        }
        return ReturnMode.SINGLE_ENTITY
    }

    /**
     * Resolve the cache name: annotation cacheNames > class-level `@CacheConfig.cacheNames` > null.
     *
     * @param joinPoint AOP join point
     * @param ann annotation instance
     * @return cache name, or null if neither is configured
     * @author K
     * @since 1.0.0
     */
    private fun resolveCacheName(joinPoint: ProceedingJoinPoint, ann: HashCacheableBySecondary): String? {
        if (ann.cacheNames.isNotEmpty()) return ann.cacheNames.first()
        val cacheConfig = joinPoint.target::class.findAnnotation<CacheConfig>()
        if (cacheConfig != null && cacheConfig.cacheNames.isNotEmpty()) return cacheConfig.cacheNames.first()
        return null
    }

    /** "#paramName" -> paramName, otherwise null. */
    private fun derivePropertyFromKey(keyExpr: String): String? =
        Regex("^#(\\w+)$").find(keyExpr)?.groupValues?.get(1)

    /**
     * Single key: one listBySetIndex call; multiple keys: multiple listBySetIndex calls intersected by id, then getById
     * from the cache to fetch each entity.
     * For multiple keys, getById is used to ensure the same local reference is returned under LOCAL_REMOTE (otherwise a
     * second listBySetIndex would write the remote instance back over the local copy, leaving its references inconsistent
     * with firstList).
     */
    private fun queryByKeys(
        hashCache: IHashCache,
        cacheName: String,
        entityClass: KClass<out IIdEntity<Any?>>,
        propertyValues: List<Pair<String, Any>>
    ): List<IIdEntity<Any?>> {
        if (propertyValues.isEmpty()) return emptyList()
        if (propertyValues.size == 1) {
            val (prop, value) = propertyValues.single()
            return hashCache.listBySetIndex(cacheName, entityClass, prop, value)
        }
        val lists = propertyValues.map { (prop, value) ->
            hashCache.listBySetIndex(cacheName, entityClass, prop, value)
        }
        val intersectIds = lists.map { list -> list.mapNotNull { it.id }.toSet() }.reduce { a, b -> a.intersect(b) }
        if (intersectIds.isEmpty()) return emptyList()
        return intersectIds.mapNotNull { id ->
            hashCache.getById(cacheName, id, entityClass)
        }
    }

    /**
     * Cast the wildcard `KClass<out IIdEntity<*>>` to `KClass<out IIdEntity<Any?>>`, localizing the unchecked cast warning.
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
