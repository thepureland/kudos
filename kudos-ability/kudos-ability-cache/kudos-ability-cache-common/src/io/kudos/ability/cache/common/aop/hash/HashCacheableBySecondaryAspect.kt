package io.kudos.ability.cache.common.aop.hash

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.cache.common.support.IIdEntitiesHashCache
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
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction

/**
 * [HashCacheableBySecondary] 切面：先按副属性等值（listBySetIndex）查缓存，未命中则执行方法并将结果 saveBatch 回写。
 * 支持三种方法返回类型：
 * - [List]&lt;[IIdEntity]&gt;：命中返回列表，未命中将方法返回的列表 saveBatch 回写。
 * - [String]（可空）：命中返回首个实体的 id，未命中仅执行方法（回写由方法体内完成）。
 * - [List]&lt;[String]&gt;：命中返回 id 列表，未命中仅执行方法（回写由方法体内完成）。
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

        val keyExpressions = ann.filterExpressions
        if (keyExpressions.isEmpty()) return joinPoint.proceed()
        val propertyValues = keyExpressions.map { expr ->
            val prop = derivePropertyFromKey(expr.trim())
                ?: throw IllegalArgumentException("HashCacheableBySecondary: filterExpressions 每项须为单参数 SpEL（如 #type），当前: $expr")
            val value = parser.parseExpression(expr.trim()).getValue(context) ?: return joinPoint.proceed()
            prop to value
        }
        val entityClass = ann.entityClass as KClass<out IIdEntity<Any?>>
        val returnMode = resolveReturnMode(method)

        val hashCache = HashCacheKit.getHashCache(cacheName)
        if (CacheKit.isCacheActive(cacheName)) {
            @Suppress("UNCHECKED_CAST")
            val cached = queryByKeys(hashCache, cacheName, entityClass, propertyValues)
            if (cached.isNotEmpty()) {
                return when (returnMode) {
                    ReturnMode.SINGLE_ID -> cached.firstOrNull()?.id
                    ReturnMode.LIST_IDS -> cached.mapNotNull { it.id }
                    ReturnMode.LIST_ENTITIES -> cached
                }
            }
        }

        val result = joinPoint.proceed()

        if (result == null) return result
        if (ann.unless.isNotBlank()) {
            val unlessContext = MethodBasedEvaluationContext(result, method, joinPoint.args, nameDiscoverer)
            unlessContext.setVariable("result", result)
            val unlessResult = parser.parseExpression(ann.unless).getValue(unlessContext, Boolean::class.java)
            if (unlessResult == true) return result
        }

        when (returnMode) {
            ReturnMode.SINGLE_ID, ReturnMode.LIST_IDS -> return result
            ReturnMode.LIST_ENTITIES -> {
                val list = (result as? List<*>)?.filterIsInstance<IIdEntity<*>>() ?: return result
                if (list.isNotEmpty() && CacheKit.isCacheActive(cacheName) && CacheKit.isWriteInTime(cacheName)) {
                    val filterable = ann.filterableProperties.toSet()
                    val sortable = ann.sortableProperties.toSet()
                    @Suppress("UNCHECKED_CAST")
                    hashCache.saveBatch(cacheName, list as List<IIdEntity<Any?>>, filterable, sortable)
                }
                return result
            }
        }
    }

    private enum class ReturnMode { SINGLE_ID, LIST_IDS, LIST_ENTITIES }

    private fun resolveReturnMode(method: java.lang.reflect.Method): ReturnMode {
        val returnType = method.returnType
        if (returnType == String::class.java) return ReturnMode.SINGLE_ID
        if (List::class.java.isAssignableFrom(returnType)) {
            val generic = method.genericReturnType
            if (generic is ParameterizedType) {
                val actual = generic.actualTypeArguments.getOrNull(0)
                if (actual == String::class.java) return ReturnMode.LIST_IDS
            }
        }
        return ReturnMode.LIST_ENTITIES
    }

    private fun resolveCacheName(joinPoint: ProceedingJoinPoint, ann: HashCacheableBySecondary): String? {
        if (ann.cacheNames.isNotEmpty()) return ann.cacheNames.first()
        val cacheConfig = joinPoint.target::class.findAnnotation<CacheConfig>()
        if (cacheConfig != null && cacheConfig.cacheNames.isNotEmpty()) return cacheConfig.cacheNames.first()
        return null
    }

    /** "#paramName" → paramName，否则 null */
    private fun derivePropertyFromKey(keyExpr: String): String? =
        Regex("^#(\\w+)$").find(keyExpr)?.groupValues?.get(1)

    /**
     * 单 key：一次 listBySetIndex；多 key：多次 listBySetIndex 后按 id 取交集，再按 id 从缓存 getById 取实体。
     * 多 key 时用 getById 保证 LOCAL_REMOTE 下返回本地同一引用（避免第二次 listBySetIndex 回写远程实例覆盖本地后与 firstList 引用不一致）。
     */
    @Suppress("UNCHECKED_CAST")
    private fun queryByKeys(
        hashCache: IIdEntitiesHashCache,
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
            hashCache.getById(cacheName, id, entityClass) as? IIdEntity<Any?>
        }
    }
}
