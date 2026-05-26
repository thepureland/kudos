package io.kudos.ability.cache.common.aop.hash

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.reflect.KClass

/**
 * Hash annotation cacheable by primary property; semantics similar to Spring's [org.springframework.cache.annotation.Cacheable].
 *
 * Terminology: **primary property** is the entity's unique identifier (id); **secondary properties** are properties other than
 * id that participate in secondary indexes, list queries, and sorting.
 * This annotation only supports **querying a single record by primary property**: the method return value must be [IIdEntity]
 * or its subclass (nullable); [key] is a SpEL expression whose result is used as the primary property (id).
 * - Hit: return directly from the Hash cache via getById
 * - Miss: execute the method; if the return value is non-null, save it into the Hash cache and return (secondary properties
 *   may be specified to build filterable/sortable indexes)
 *
 * Before use, configure `hash = true` for the given [cacheNames] in the cache configuration.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class HashCacheableByPrimary(

    /** Hash cache names (must exist in the configuration with hash=true). */
    val cacheNames: Array<String> = [],

    /**
     * SpEL expression for the primary property; the result is used as the entity id for getById/save.
     * For example "#id", "#resourceId".
     */
    val key: String = "#id",

    /**
     * SpEL condition that determines whether to use the cache; the cache is queried/written only when this is empty or
     * evaluates to true.
     */
    val condition: String = "",

    /**
     * SpEL condition that determines whether to skip caching; when it evaluates to true, the method result is not written
     * to the cache.
     */
    val unless: String = "",

    /** Cache entity type, used as the KClass for getById/save. */
    val entityClass: KClass<out IIdEntity<*>>,

    /**
     * Filterable secondary property names (Set index for equality queries); no index is built when empty. Exception:
     * numeric range query conditions should be placed in [sortableProperties].
     */
    val filterableProperties: Array<String> = [],

    /**
     * Sortable / range secondary property names (ZSet index); used by listPageByZSetIndex and the like. Exception:
     * numeric range query conditions should be placed here.
     */
    val sortableProperties: Array<String> = []
)
