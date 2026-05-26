package io.kudos.ability.cache.common.batch.keyvalue

import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.lang.string.toType
import io.kudos.base.logger.LogFactory
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
 * Aspect for batch cache.
 *
 * @author K
 * @since 1.0.0
 */
@Aspect
@Component
@Lazy(false)
@ConditionalOnBean(MixCacheManager::class)
@Order(10) // Batch Cacheable runs slightly after single-entry Cacheable(0); they are mutually exclusive in theory, but the explicit ordering eases debugging of combined scenarios.
class BatchCacheableAspect {

    /**
     * Defines the pointcut.
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.cache.common.batch.keyvalue.BatchCacheable)")
    fun cut() {
        // do nothing
    }

    /**
     * Around advice.
     *
     * @return Map(cache key, cache value)
     * @author K
     * @since 1.0.0
     */
    @Around("cut()")
    fun around(joinPoint: ProceedingJoinPoint): Map<String, Any?> {
        // Get the annotation defined on the method
        val function = (joinPoint.signature as MethodSignature).method.kotlinFunction!!
        val batchCacheable = function.findAnnotation<BatchCacheable>()!!

        // Validate constraints
        val cacheName = validate(joinPoint, function, batchCacheable)

        // Get all cache keys
        val cachedData = linkedMapOf<String, Any?>() // Cached data. Map<cache key, cache value>
        val keys = getAllCacheKeys(joinPoint, function, batchCacheable)
        keys.forEach { cachedData[it] = null } // preserve order

        // Read data that already exists in the cache
        readCachedData(keys, cacheName, batchCacheable, cachedData)

        // For entries missing from the cache (for collection params, strip out the parts already loaded from cache), read from the @BatchCacheable method
        val noExistKeys = cachedData.filterValues { it == null }.keys
        val uncachedData = readUncachedData(noExistKeys, joinPoint, function, batchCacheable) // Uncached data. Map<cache key, cache value>

        // Verify that the underlying method's returned key set matches the missing key set. A mismatch usually indicates a bug in the contract between keysGenerator and the method;
        // the legacy implementation would "silently return fewer / more" entries, which is hard for callers to detect. Here we only log (avoid triggering failures beyond cache consistency).
        if (uncachedData != null) {
            val returnedKeys = uncachedData.keys
            val missing = noExistKeys - returnedKeys
            val extra = returnedKeys - noExistKeys
            if (missing.isNotEmpty() || extra.isNotEmpty()) {
                log.warn(
                    "BatchCacheable method returned key set differs from missing set cacheName={0} missing={1} extra={2}",
                    cacheName, missing, extra
                )
            }
        }

        // Cache the data read (uncached) from the @BatchCacheable method (note: existing cache entries are not updated)
        uncachedData?.forEach { (k, v) -> KeyValueCacheKit.putIfAbsent(cacheName, k, v) }

        // Assemble the two parts of data: read from cache and just loaded, returning them as the @BatchCacheable method's return value
        if (uncachedData != null) {
            cachedData.forEach { (k, v) -> if (v == null) cachedData[k] = uncachedData[k] }
        }

        return cachedData.filterValues { it != null } // drop entries whose value is null
    }

    /**
     * Validates constraints.
     *
     * @param joinPoint join point
     * @param function the @BatchCacheable-annotated method
     * @param batchCacheable @BatchCacheable annotation
     * @return cache name
     * @author K
     * @since 1.0.0
     */
    private fun validate(joinPoint: ProceedingJoinPoint, function: KFunction<*>, batchCacheable: BatchCacheable): String {
        val clazz = joinPoint.target::class
        val cacheConfig = clazz.findAnnotation<CacheConfig>()
        val cacheName = batchCacheable.cacheNames.firstOrNull()
            ?: cacheConfig?.cacheNames?.firstOrNull()
            ?: error("cacheNames is not set! Please specify it via @CacheConfig on class ${clazz}, or via @BatchCacheable on method ${function}!")

        check(Map::class.isSuperclassOf(function.returnType.classifier as KClass<*>)) {
            "In class ${clazz}, the @BatchCacheable-annotated method [${function}] must have a Map return type!"
        }
        return cacheName
    }

    /**
     * Resolves the keys generator specified by [BatchCacheable.keysGenerator].
     *
     * First tries to obtain a bean from the Spring container by type; falls back to [DefaultKeysGenerator] if missing.
     * Missing scenario: unit tests disable caching so configuration classes are not loaded; avoid throwing so that tests can still run.
     *
     * @param batchCacheable annotation instance
     * @return keys generator
     * @author K
     * @since 1.0.0
     */
    private fun getKeysGenerator(batchCacheable: BatchCacheable): IKeysGenerator =
        (SpringKit.getBeanOrNull(batchCacheable.keysGenerator) as? IKeysGenerator)
            ?: DefaultKeysGenerator()

    /**
     * Gets all cache keys.
     *
     * @param joinPoint join point
     * @param function the @BatchCacheable-annotated method
     * @param batchCacheable @BatchCacheable annotation
     * @return List(cache key)
     * @author K
     * @since 1.0.0
     */
    private fun getAllCacheKeys(
        joinPoint: ProceedingJoinPoint, function: KFunction<*>, batchCacheable: BatchCacheable
    ): List<String> {
        val keysGenerator = getKeysGenerator(batchCacheable)
        return keysGenerator.generate(joinPoint.target, function, *joinPoint.args)
    }

    /**
     * Reads data that exists in the cache.
     *
     * @param keys List(cache key)
     * @param batchCacheable @BatchCacheable annotation
     * @param result Map(cache key, cache value)
     * @author K
     * @since 1.0.0
     */
    private fun readCachedData(
        keys: List<String>, cacheName: String, batchCacheable: BatchCacheable, result: MutableMap<String, Any?>
    ) {
        keys.forEach { key ->
            //TODO How to prevent cache breakdown when the cache entry does not exist
            KeyValueCacheKit.getValue(cacheName, key, batchCacheable.valueClass)
                ?.let { result[key] = it }
        }
    }

    /**
     * Reads data not in the cache.
     *
     * For entries missing from the cache (for collection params, strip out the parts already loaded from cache), read from the @BatchCacheable method.
     *
     * @param result already cached data, Map(cache key, cache value)
     * @param joinPoint join point
     * @param function the @BatchCacheable-annotated method
     * @param batchCacheable @BatchCacheable annotation
     * @return data not in the cache, Map(cache key, cache value)
     * @author K
     * @since 1.0.0
     */
    private fun readUncachedData(
        noExistKeys: Set<String>, joinPoint: ProceedingJoinPoint, function: KFunction<*>,
        batchCacheable: BatchCacheable
    ): Map<String, Any?>? {
        if (noExistKeys.isEmpty()) return null
        val keysGenerator = getKeysGenerator(batchCacheable)
        val delimiter = keysGenerator.getDelimiter()
        val paramIndexes = keysGenerator.getParamIndexes(function, *joinPoint.args)
        val parameterTypes = (joinPoint.signature as MethodSignature).parameterTypes
        // The legacy implementation directly mutated the original array via `joinPoint.args[index] = params`: subsequent AOP reads of joinPoint.args would see the modified values,
        // which is a hard-to-debug "implicit side effect". We replace on a copy here to avoid polluting the upstream args array.
        val newArgs: Array<Any?> = joinPoint.args.copyOf()
        parameterTypes.forEachIndexed { index, clazz ->
            val paramValue = newArgs[index]
            if (index in paramIndexes && (paramValue is Collection<*> || paramValue is Array<*>)) {
                val segIndex = paramIndexes.indexOf(index) // segment index within the key
                val firstElemValue: Any? = when (paramValue) {
                    is Collection<*> -> paramValue.firstOrNull()
                    is Array<*> -> paramValue.firstOrNull()
                    else -> null
                }
                // The legacy `firstElemValue!!::class` would NPE on empty collection/array. Give a clear error here.
                requireNotNull(firstElemValue) {
                    "Cannot infer element type from empty collection/array: param index=$index. Callers should filter empty inputs before entering @BatchCacheable."
                }
                val elemType = firstElemValue::class
                // The legacy implementation split by a fixed delimiter: if a key segment contains the same character, the split is incorrect. We still take by segment, but as a known constraint
                // keysGenerator.getDelimiter is expected to provide a "collision-free enough" delimiter. A complete fix requires escaping at the keysGenerator
                // layer, which is out of scope for this pass.
                val elemValues = noExistKeys.map { key -> key.split(delimiter)[segIndex].toType(elemType) }
                // The `as List<X>` casts below are definitely safe: each item in elemValues is a `toType(elemType)` result,
                // and elemType is the actual KClass of the original input collection's element. We only convert List<Any> to a strongly typed List and then call toTypedArray().
                @Suppress("UNCHECKED_CAST")
                val params: Any? = when (clazz.kotlin) {
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
                    else -> null
                }
                newArgs[index] = params
            }
        }
        val proceeded = joinPoint.proceed(newArgs)
        // The entry [validate] already checked that the method return type must be Map, but the generic parameters are erased:
        // - If the method signature is Map<Int, X>, proceed returns Map<Int, X>; casting to Map<String, X> does not blow up immediately at the JVM level,
        //   but later when `cachedData[key]` is read with a String key it silently misses -> users observe "caching not working".
        // Explicitly check that result is non-null and sample-validate that keys are Strings to give a meaningful error.
        require(proceeded is Map<*, *>) {
            "@BatchCacheable method return value must be Map, actual: ${proceeded?.let { it::class.qualifiedName } ?: "null"}"
        }
        proceeded.keys.firstOrNull { it != null && it !is String }?.let { wrongKey ->
            error(
                "@BatchCacheable method returned a Map containing non-String keys (e.g. $wrongKey: ${wrongKey::class.qualifiedName}). " +
                    "The aspect matches by String cacheKey; please change the return Map's key type to String."
            )
        }
        @Suppress("UNCHECKED_CAST")
        return proceeded as Map<String, Any?>
    }

    companion object {
        private val log = LogFactory.getLog(BatchCacheableAspect::class)
    }

}