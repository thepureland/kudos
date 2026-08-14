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
        val keys = getAllCacheKeys(joinPoint, function, batchCacheable)

        // Read everything already cached in a single call. Hit/miss is decided by key *presence*: a key mapped
        // to null is a cached null (a hit), not a miss. The previous implementation seeded every key with null
        // and treated "still null" as missing, which made cached nulls indistinguishable from misses and sent
        // those keys back to the source on every single call.
        val cachedData = readCachedData(keys, cacheName)

        // For entries missing from the cache (for collection params, strip out the parts already loaded from cache), read from the @BatchCacheable method
        val noExistKeys = keys.filterNot { cachedData.containsKey(it) }.toSet()
        val uncachedData = readUncachedData(keys, noExistKeys, joinPoint, function, batchCacheable) // Uncached data. Map<cache key, cache value>

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

        // Optionally remember "the source has nothing for this key" so the next batch does not ask again. Off by
        // default: it is real negative caching, so a row created later stays invisible until the entry expires
        // or the handler's insert hook evicts it.
        if (batchCacheable.cacheNullForMissingKeys && uncachedData != null) {
            noExistKeys.asSequence()
                .filterNot { uncachedData.containsKey(it) }
                .forEach { KeyValueCacheKit.putIfAbsent(cacheName, it, null) }
        }

        // Assemble both halves in the caller's key order. Null values are dropped from the *result* — the
        // annotated method declares a Map with a non-null value type, so a null would break its own signature —
        // even though they are kept in the cache.
        val result = linkedMapOf<String, Any?>()
        keys.forEach { key ->
            val value = if (cachedData.containsKey(key)) cachedData[key] else uncachedData?.get(key)
            if (value != null) result[key] = value
        }
        return result
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
     * Reads whatever is already cached for [keys], in one bulk call.
     *
     * This used to loop `getValue` per key, so a batch of N keys cost N remote round trips — most of the point
     * of batching, given away. [KeyValueCacheKit.multiGet] pushes the whole key set down to the cache manager,
     * which serves the local tier in a single pass and Redis in a single `MGET`.
     *
     * Note the return contract: **a key present in the map was a hit even if its value is null.** Callers must
     * test `containsKey`, otherwise cached nulls are re-fetched from the source forever.
     *
     * Values are not type-checked against `@BatchCacheable.valueClass` here; a bulk read cannot filter per key
     * without giving up the single round trip, and a type mismatch means the cache was populated with the wrong
     * type — a bug to fix at the write site rather than to paper over on every read.
     *
     * @param keys List(cache key)
     * @param cacheName cache name
     * @return found keys mapped to their (possibly null) cached values
     * @author K
     * @since 1.0.0
     */
    private fun readCachedData(keys: List<String>, cacheName: String): Map<String, Any?> {
        if (keys.isEmpty()) return emptyMap()
        val found = KeyValueCacheKit.multiGet(cacheName, keys)
        if (found.isEmpty()) return emptyMap()
        val result = LinkedHashMap<String, Any?>(found.size)
        keys.forEach { key -> if (found.containsKey(key)) result[key] = found[key] }
        return result
    }

    /**
     * Reads data not in the cache.
     *
     * For entries missing from the cache (for collection params, strip out the parts already loaded from cache), read from the @BatchCacheable method.
     *
     * ## Missing elements are selected by position, not by parsing keys back
     *
     * Keys are built by positional zip — key `i` comes from element `i` of every collection/array parameter
     * (see [DefaultKeysGenerator]). So the elements to re-request are simply the ones at the positions of the
     * missing keys, taken straight from the original arguments.
     *
     * This used to work the other way round: split each missing key on the generator's delimiter and convert
     * segment `n` back to the parameter's element type. That was fragile in two ways — a parameter value
     * containing the delimiter (`::` by default, which is also what tenant-aware keys are built with) split
     * into the wrong number of segments and silently produced wrong arguments, and the round trip through
     * `toString`/`toType` could not restore values whose textual form is lossy. Indexing needs neither the
     * delimiter nor the conversion, and keeps the original element instances.
     *
     * @param allKeys all cache keys, in the positional order they were generated
     * @param noExistKeys the subset of [allKeys] that missed the cache
     * @param joinPoint join point
     * @param function the @BatchCacheable-annotated method
     * @param batchCacheable @BatchCacheable annotation
     * @return data not in the cache, Map(cache key, cache value)
     * @author K
     * @since 1.0.0
     */
    private fun readUncachedData(
        allKeys: List<String>, noExistKeys: Set<String>, joinPoint: ProceedingJoinPoint, function: KFunction<*>,
        batchCacheable: BatchCacheable
    ): Map<String, Any?>? {
        if (noExistKeys.isEmpty()) return null
        val keysGenerator = getKeysGenerator(batchCacheable)
        val paramIndexes = keysGenerator.getParamIndexes(function, *joinPoint.args)
        val parameterTypes = (joinPoint.signature as MethodSignature).parameterTypes
        val missingPositions = allKeys.indices.filter { allKeys[it] in noExistKeys }
        // The legacy implementation directly mutated the original array via `joinPoint.args[index] = params`: subsequent AOP reads of joinPoint.args would see the modified values,
        // which is a hard-to-debug "implicit side effect". We replace on a copy here to avoid polluting the upstream args array.
        val newArgs: Array<Any?> = joinPoint.args.copyOf()
        parameterTypes.forEachIndexed { index, clazz ->
            val paramValue = newArgs[index]
            if (index in paramIndexes && (paramValue is Collection<*> || paramValue is Array<*>)) {
                val elements: List<Any?> = when (paramValue) {
                    is Collection<*> -> paramValue.toList()
                    is Array<*> -> paramValue.toList()
                    else -> emptyList()
                }
                val elemValues = missingPositions.mapNotNull { position -> elements.getOrNull(position) }
                // Re-wrap into the originally declared parameter type. Note: every `Array<X>::class` erases to the
                // same `kotlin.Array` KClass, so dispatching on `Array<Int>::class`, `Array<Long>::class`, ... would
                // collapse to the first Array branch and silently mis-handle non-String arrays. Instead we detect the
                // array case via the Java reflection `clazz.isArray` and build an array whose runtime component type is
                // the actual element type, so reflective invoke receives e.g. a real `Integer[]`/`Long[]`.
                val params: Any? = when {
                    clazz.isArray -> {
                        val elemType = elemValues.firstOrNull()?.let { it::class }
                            ?: elements.firstOrNull()?.let { it::class }
                        requireNotNull(elemType) {
                            "Cannot infer element type from empty collection/array: param index=$index. Callers should filter empty inputs before entering @BatchCacheable."
                        }
                        toTypedArray(elemValues, elemType)
                    }
                    Set::class.java.isAssignableFrom(clazz) -> elemValues.toSet()
                    else -> elemValues // List / Collection and any other Collection subtype
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

        /**
         * Builds an array whose runtime component type matches [elemType], populated with [elemValues].
         *
         * Using [java.lang.reflect.Array.newInstance] (rather than Kotlin's `List.toTypedArray()`, which always
         * yields an `Object[]`/`Array<Any>`) guarantees the produced array's component type is the real element type
         * (e.g. `Integer[]`, `Long[]`, `BigDecimal[]`). This is required because the target method is invoked
         * reflectively via `joinPoint.proceed(newArgs)`: passing a `String[]` where the signature declares `Integer[]`
         * throws `IllegalArgumentException: argument type mismatch`.
         *
         * @param elemValues the converted element values
         * @param elemType the element KClass (derived from the original collection/array's first element)
         * @return a typed array of component type [elemType]
         * @author K
         * @since 1.0.0
         */
        internal fun toTypedArray(elemValues: List<Any?>, elemType: KClass<*>): Any {
            // Use javaObjectType (boxed, e.g. java.lang.Long) not java (which is the primitive `long` for Long::class):
            // a Kotlin `Array<Long>` parameter compiles to a boxed `Long[]`, so the array's component type must be boxed.
            val array = java.lang.reflect.Array.newInstance(elemType.javaObjectType, elemValues.size)
            elemValues.forEachIndexed { i, v -> java.lang.reflect.Array.set(array, i, v) }
            return array
        }
    }

}