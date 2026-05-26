package io.kudos.ability.cache.common.batch.keyvalue

import java.lang.annotation.Inherited
import kotlin.reflect.KClass

/**
 * Marks a method as supporting batch caching.
 *
 * Batch caching principles: cache first, batch query, individual caching.
 *
 *
 * Constraints:
 *
 * 1. The cache key must be String.
 *
 * 2. Cache names must be specified explicitly; unlike Spring's @Cacheable, the method name is not used as default.
 *
 * 3. The method return type must be Map whose key is the cache key and whose value is defined by @BatchCacheable.valueClass. Collection-typed values must be declared as Collection.
 *
 * 4. The method must be open.
 *
 * 5. Method parameters used to compose the cache key may only contain primitive-typed elements if they are arrays.
 *
 * Note: due to the complexity of parameter combinations, batch caching cannot precisely guarantee that fetched data is not already in the cache; it can only minimize such cases.
 *       In such cases, the fetched data will not overwrite existing cache entries. The more parameters or the more elements per parameter, the more pronounced this becomes.
 *
 * @author K
 * @since 1.0.0
 */
@MustBeDocumented
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class BatchCacheable(

    /** Cache name, may also be specified at class level via @CacheConfig. **/
    val cacheNames: Array<String> = [],

    /** Name of the Spring bean implementing IKeysGenerator. */
    val keysGenerator: String = "defaultKeysGenerator",

    /** Type of the cache value for a single key; Collection-typed values must be declared as Collection. */
    val valueClass: KClass<*>,

    /** Indexes of parameters to ignore when composing the cache key. */
    val ignoreParamIndexes: IntArray = []

)