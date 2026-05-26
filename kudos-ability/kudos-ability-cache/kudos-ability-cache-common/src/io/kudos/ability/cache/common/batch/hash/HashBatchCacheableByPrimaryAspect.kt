package io.kudos.ability.cache.common.batch.hash

import io.kudos.ability.cache.common.batch.keyvalue.IKeysGenerator
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.lang.string.toType
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.context.kit.SpringKit
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.cache.annotation.CacheConfig
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.kotlinFunction

/**
 * Aspect for [HashBatchCacheableByPrimary]: first batch-loads from Hash cache by primary attribute (id) via findByIds, then invokes the method for misses, writes the result back via saveBatch (optionally with secondary attribute indexes) and returns the merged result.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Aspect
@Component
@Lazy(false)
@ConditionalOnBean(MixHashCacheManager::class)
@Order(10) // Same order as [BatchCacheableAspect]: batch Cacheable aspects are uniformly tagged 10.
class HashBatchCacheableByPrimaryAspect {

    @Pointcut("@annotation(io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary)")
    fun cut() {}

    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Map<String, Any?> {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val function = method.kotlinFunction ?: return proceedAsStringAnyMap(joinPoint)
        val ann = function.findAnnotation<HashBatchCacheableByPrimary>() ?: return proceedAsStringAnyMap(joinPoint)

        val cacheName = resolveCacheName(joinPoint, ann) ?: return proceedAsStringAnyMap(joinPoint)
        validateReturnType(function, ann)

        val keysGenerator = getKeysGenerator(ann)
        val keys = keysGenerator.generate(joinPoint.target, function, *joinPoint.args)
        if (keys.isEmpty()) return emptyMap()

        val cachedData = linkedMapOf<String, Any?>()
        keys.forEach { cachedData[it] = null }

        val hashCache = HashCacheKit.getHashCache(cacheName)
        if (KeyValueCacheKit.isCacheActive(cacheName)) {
            @Suppress("UNCHECKED_CAST")
            val entityClass = ann.entityClass as KClass<out IIdEntity<Any?>>
            val list = hashCache.findByIds(cacheName, keys, entityClass)
            list.forEach { e ->
                val tid = e.id?.toString()?.trim() ?: return@forEach
                val slot = cachedData.keys.find { it.trim() == tid } ?: tid
                cachedData[slot] = e
            }
        }

        val uncachedKeys = cachedData.filterValues { it == null }.keys.toList()
        if (uncachedKeys.isEmpty()) return cachedData.filterValues { it != null }

        val uncachedMap = readUncachedData(uncachedKeys, joinPoint, function, keysGenerator)
            ?: return cachedData.filterValues { it != null }

        if (KeyValueCacheKit.isCacheActive(cacheName) && KeyValueCacheKit.isWriteInTime(cacheName)) {
            val toSave = uncachedMap.values.filterIsInstance<IIdEntity<*>>().filter { it.id != null }
            if (toSave.isNotEmpty()) {
                val filterable = ann.filterableProperties.toSet()
                val sortable = ann.sortableProperties.toSet()
                @Suppress("UNCHECKED_CAST")
                hashCache.saveBatch(cacheName, toSave as List<IIdEntity<Any?>>, filterable, sortable)
            }
        }

        uncachedMap.forEach { (k, v) -> cachedData[k] = v }
        return cachedData.filterValues { it != null }
    }

    /**
     * Resolves the cache name: annotation cacheNames > class-level `@CacheConfig.cacheNames` > null.
     * @author K
     * @since 1.0.0
     */
    private fun resolveCacheName(joinPoint: ProceedingJoinPoint, ann: HashBatchCacheableByPrimary): String? {
        if (ann.cacheNames.isNotEmpty()) return ann.cacheNames.first()
        val cacheConfig = joinPoint.target::class.findAnnotation<CacheConfig>()
        if (cacheConfig != null && cacheConfig.cacheNames.isNotEmpty()) return cacheConfig.cacheNames.first()
        return null
    }

    /**
     * Verifies the target method's return type must be [Map]; otherwise the aspect's "batch result" assembly will be wrong.
     * Throws immediately on non-compliance so the issue is caught at development time.
     *
     * @throws IllegalStateException when the return type is not Map
     * @author K
     * @since 1.0.0
     */
    private fun validateReturnType(function: KFunction<*>, ann: HashBatchCacheableByPrimary) {
        if (!Map::class.isSuperclassOf(function.returnType.classifier as KClass<*>)) {
            error("The @HashBatchCacheableByPrimary-annotated method [${function}] must have a Map return type!")
        }
    }

    /**
     * Resolves the keys generator specified by [HashBatchCacheableByPrimary.keysGenerator]; falls back to [DefaultHashBatchKeysGenerator] if missing.
     * @author K
     * @since 1.0.0
     */
    private fun getKeysGenerator(ann: HashBatchCacheableByPrimary): IKeysGenerator =
        (SpringKit.getBeanOrNull(ann.keysGenerator) as? IKeysGenerator)
            ?: DefaultHashBatchKeysGenerator()

    /**
     * After `joinPoint.proceed()`, delegates to [validatedMap] to strictly verify the return value is `Map<String, Any?>`.
     * Extracted to keep the around main flow compact.
     * @author K
     * @since 1.0.0
     */
    private fun proceedAsStringAnyMap(joinPoint: ProceedingJoinPoint): Map<String, Any?> =
        validatedMap(joinPoint.proceed())

    /**
     * Validates that the proceed return value conforms to `Map<String, Any?>`: due to type erasure any Map passes the cast,
     * but if actual key types are wrong, the downstream "lookup by String key" silently misses. Centralize validation here.
     */
    private fun validatedMap(proceeded: Any?): Map<String, Any?> {
        if (proceeded !is Map<*, *>) {
            error("HashBatchCacheableByPrimary expects the method to return Map<String, Any?>, actual: ${proceeded?.let { it::class.qualifiedName } ?: "null"}")
        }
        require(proceeded.keys.all { it == null || it is String }) {
            "HashBatchCacheableByPrimary expects the method to return Map<String, Any?>, but detected a non-String key."
        }
        @Suppress("UNCHECKED_CAST")
        return proceeded as Map<String, Any?>
    }

    /**
     * Reconstructs the "args that should hit the DB" from the cache-missed key list, then proceeds to the target method to backfill.
     *
     * Core challenge: the business method's input may be `Collection<T>` / `Array<T>`, and each cache key is a multi-segment composition
     * (e.g. `tenantId:userId`), so we must:
     * 1. Split each noExistKey by [IKeysGenerator.getDelimiter]
     * 2. Take the segment at `paramIndex`, use the first element of the original collection as a sample to call [toType] and deserialize back to the original type
     * 3. Re-wrap into the originally declared parameter type (List/Set/Array<X>) and replace args[paramIndex]
     *
     * We cannot just pass a String list: the target method's signature may require concrete types like `List<Long>` or `Array<Int>`,
     * and skipping the type restoration would throw ClassCastException at reflective invoke.
     *
     * @param noExistKeys list of cache-missed keys (already formatted as `seg1:seg2:...`)
     * @param joinPoint AOP join point (provides original args)
     * @param function target KFunction (provides parameter index metadata)
     * @param keysGenerator key generator (provides delimiter + paramIndexes lookup)
     * @return key->value map after re-running the target method; null means no backfill is needed
     * @author K
     * @since 1.0.0
     */
    private fun readUncachedData(
        noExistKeys: List<String>,
        joinPoint: ProceedingJoinPoint,
        function: KFunction<*>,
        keysGenerator: IKeysGenerator
    ): Map<String, Any?>? {
        val delimiter = keysGenerator.getDelimiter()
        val paramIndexes = keysGenerator.getParamIndexes(function, *joinPoint.args)
        val parameterTypes = (joinPoint.signature as MethodSignature).parameterTypes
        val args = joinPoint.args.copyOf()

        paramIndexes.forEachIndexed { segIdx, paramIndex ->
            val paramValue = args[paramIndex]
            if (paramValue is Collection<*> || paramValue is Array<*>) {
                val elemValues = noExistKeys.map { key ->
                    val seg = key.split(delimiter)
                    val segStr = if (segIdx < seg.size) seg[segIdx] else key
                    val first = when (paramValue) {
                        is Collection<*> -> paramValue.first()
                        else -> (paramValue as Array<*>).first()
                    }
                    val sample = requireNotNull(first) { "Batch cache parameter collection contains null elements; cannot infer type." }
                    segStr.toType(sample::class)
                }
                val clazz = parameterTypes[paramIndex].kotlin
                // The `as List<X>` casts below are definitely safe: elemValues comes from `toType(sample::class)`, with aligned element types.
                @Suppress("UNCHECKED_CAST")
                args[paramIndex] = when (clazz) {
                    List::class, Collection::class -> elemValues
                    Set::class -> elemValues.toSet()
                    Array<String>::class -> (elemValues as List<String>).toTypedArray()
                    Array<Char>::class -> (elemValues as List<Char>).toTypedArray()
                    Array<Boolean>::class -> (elemValues as List<Boolean>).toTypedArray()
                    Array<Byte>::class -> (elemValues as List<Byte>).toTypedArray()
                    Array<Short>::class -> (elemValues as List<Short>).toTypedArray()
                    Array<Int>::class -> (elemValues as List<Int>).toTypedArray()
                    Array<Long>::class -> (elemValues as List<Long>).toTypedArray()
                    Array<Float>::class -> (elemValues as List<Float>).toTypedArray()
                    Array<Double>::class -> (elemValues as List<Double>).toTypedArray()
                    Array<BigDecimal>::class -> (elemValues as List<BigDecimal>).toTypedArray()
                    Array<BigInteger>::class -> (elemValues as List<BigInteger>).toTypedArray()
                    else -> elemValues
                }
            }
        }

        // Go through the same strict validation path as [proceedAsStringAnyMap] to avoid `as? Map<String, Any?>` silently returning an incorrect structure when key types are wrong.
        return validatedMap(joinPoint.proceed(args))
    }
}
