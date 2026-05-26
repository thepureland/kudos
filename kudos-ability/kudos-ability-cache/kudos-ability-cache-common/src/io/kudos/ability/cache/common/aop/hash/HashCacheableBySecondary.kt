package io.kudos.ability.cache.common.aop.hash

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.reflect.KClass

/**
 * Cache annotation for equality queries by secondary properties: first queries the Hash cache's listBySetIndex using
 * "index + query value"; on a miss, executes the method and writes the result back via saveBatch.
 *
 * Each element of the **[filterExpressions] filter expressions** array = one "equality-by-secondary-property" query
 * dimension (it determines both which index to use and the query value for that dimension):
 * - Each element must be a **single-parameter SpEL** (e.g. `#type`, `#subSystemCode`).
 * - **Index name**: derived from the element by the rule `#paramName` -> index name `paramName` (must match the entity's
 *   secondary property name and be present in [filterableProperties]).
 * - **Query value**: the result of evaluating this SpEL against the method arguments at invocation time, used as the
 *   `value` of `listBySetIndex(cacheName, entityClass, property, value)`.
 * - **Single element**: single-condition equality, e.g. `["#type"]` performs an equality query against the type index.
 * - **Multiple elements**: multi-condition equality **AND**, e.g. `["#subSystemCode", "#url"]` runs listBySetIndex against
 *   subSystemCode and url separately, then intersects by id.
 *
 * Each element must be in the form `#paramName`; compound expressions (such as `"#a + '_' + #b"`) are not supported. For
 * multiple conditions, use multiple elements.
 *
 * Terminology: **primary property** is id; **secondary properties** are represented by [filterableProperties] /
 * [sortableProperties] and indexed on write-back.
 * Supported method return types: List&lt;IIdEntity&gt; (returns the list on hit and is responsible for saveBatch),
 * String? (returns the first id on hit; on miss, write-back is handled by the method body), List&lt;String&gt; (returns
 * the id list on hit; on miss, write-back is handled by the method body).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class HashCacheableBySecondary(

    /** Hash cache names (must exist in the configuration with hash=true). */
    val cacheNames: Array<String> = [],

    /**
     * Filter expressions: each element is a single-parameter SpEL (e.g. "#type", "#subSystemCode"); the aspect uses the
     * parameter name as listBySetIndex's property (the index name) and the SpEL result as the value (the query value).
     * A single element means a single condition; multiple elements mean multi-condition AND (multiple listBySetIndex calls
     * intersected by id). Examples: ["#type"], ["#subSystemCode", "#url"].
     */
    val filterExpressions: Array<String> = ["#type"],

    /** SpEL condition that determines whether to use the cache; the cache is queried/written only when this is empty or evaluates to true. */
    val condition: String = "",

    /** SpEL condition that determines whether to skip caching; when it evaluates to true, the method result is not written to the cache. */
    val unless: String = "",

    /** Cache entity type, used as the KClass for listBySetIndex / saveBatch. */
    val entityClass: KClass<out IIdEntity<*>>,

    /**
     * Filterable secondary property names (Set index for equality queries); indexes are built on write-back. Must include
     * the index names derived from each element of [filterExpressions]. Exception: numeric range query conditions go in
     * [sortableProperties].
     */
    val filterableProperties: Array<String> = [],

    /** Sortable / range secondary property names (ZSet index); optional on write-back. Numeric range query conditions go here. */
    val sortableProperties: Array<String> = [],

    /**
     * When the method return type is [List][String] or [Set][String], on cache hit the value of this property is taken
     * from each entity to form the returned collection; when empty, the entity id list is returned as usual. On miss,
     * the method body is responsible for the write-back and return.
     */
    val returnProperty: String = ""
)
