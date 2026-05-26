package io.kudos.ability.cache.common.batch.hash

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.reflect.KClass

/**
 * Hash-based batch cacheable annotation keyed by primary attribute, semantically aligned with [BatchCacheable]:
 * loads from Hash cache by a batch of primary attributes (id) first, then invokes the method for misses and writes back.
 *
 * Terminology: **primary attribute** is the entity's unique identifier (id); **secondary attributes** are the fields, other than id, that participate in secondary indexes, list queries and sorting.
 * Constraints:
 * - The cache key is the primary attribute id (String or convertible to String); [keysGenerator] derives the id list from method parameters.
 * - The method return type must be [Map]; key is id, value is [IIdEntity] (nullable).
 * - The method must be open.
 * - Before use, the [cacheNames] entry must be configured with `hash = true` and `writeInTime = true` to enable writeback.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class HashBatchCacheableByPrimary(

    /** Hash cache name (must exist in configuration with hash=true). */
    val cacheNames: Array<String> = [],

    /** Spring bean name implementing [io.kudos.ability.cache.common.batch.keyvalue.IKeysGenerator], used to derive the primary attribute (id) list from parameters. */
    val keysGenerator: String = "defaultHashBatchKeysGenerator",

    /** Cache entity type, the KClass used by findByIds / saveBatch. */
    val entityClass: KClass<out IIdEntity<*>>,

    /** Parameter indexes to ignore when generating keys. */
    val ignoreParamIndexes: IntArray = [],

    /**
     * Filterable secondary attribute names (Set index for equality queries); none are created if empty. Exception: numeric range query conditions must go into [sortableProperties].
     */
    val filterableProperties: Array<String> = [],

    /**
     * Sortable/range secondary attribute names (ZSet index); used by listPageByZSetIndex etc. Exception: numeric range query conditions belong here.
     */
    val sortableProperties: Array<String> = []
)
