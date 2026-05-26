package io.kudos.base.support.dao

import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.model.payload.ISearchPayload
import io.kudos.base.model.payload.ListSearchPayload
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Read-only data access interface.
 *
 * Defines general-purpose read-only query capabilities covering primary key lookups, property lookups, complex condition queries, paginated queries,
 * payload-based queries, and aggregate queries; supports returning entities, single-property lists, or multi-property mapping results.
 *
 * @param PK Primary key type
 * @param E Entity type; must implement [IIdEntity]
 * @author K
 * @since 1.0.0
 */
interface IBaseReadOnlyDao<PK : Any, E : IIdEntity<PK>> {

    /**
     * Queries the entity by primary key.
     *
     * @param id Primary key value
     * @return The matched entity; returns null if not found
     */
    fun get(id: PK): E?

    /**
     * Queries by primary key and specifies the return object type.
     *
     * Commonly used to map only some fields to a DTO/VO to avoid a second copy on the business side.
     *
     * @param id Primary key value
     * @param returnType Return object type; when null, the implementation may use the entity type
     * @return The matched object; returns null if not found
     */
    fun <R : Any> get(id: PK, returnType: KClass<R>? = null): R?

    /**
     * Batch-queries entities by a collection of primary keys.
     *
     * @param ids Collection of primary keys
     * @param countOfEachBatch Batch size for querying, to prevent oversized IN clauses from stressing SQL or memory
     * @return List of entities; returns an empty list when the input is empty
     */
    fun getByIds(ids: Collection<PK>, countOfEachBatch: Int = 1000): List<E>

    /**
     * Batch-queries by a collection of primary keys and specifies the return element type.
     *
     * @param ids Collection of primary keys
     * @param returnItemClass Return element type; when null, the implementation may use the entity type
     * @param countOfEachBatch Batch size for querying
     * @return Result list of the specified type
     */
    fun <T: Any> getByIds(
        ids: Collection<PK>,
        returnItemClass: KClass<T>? = null,
        countOfEachBatch: Int = 1000
    ): List<T>

    /**
     * Single-property equality query, returning a list of entities.
     *
     * @param property Query property
     * @param value Query value
     * @param orders Sort rules
     * @return List of entities matching the condition
     */
    fun oneSearch(property: KProperty1<E, *>, value: Any?, vararg orders: Order): List<E>

    /**
     * Single-property equality query, returning only the values of the specified property.
     *
     * @param property Query property
     * @param value Query value
     * @param returnProperty Return property
     * @param orders Sort rules
     * @return List of the specified property values
     */
    fun <R> oneSearchProperty(
        property: KProperty1<E, *>,
        value: Any?,
        returnProperty: KProperty1<E, R>,
        vararg orders: Order
    ): List<R>

    /**
     * Single-property equality query, returning only the mapping of the specified multiple properties.
     *
     * @param property Query property
     * @param value Query value
     * @param returnProperties Collection of return properties
     * @param orders Sort rules
     * @return List of multi-property mappings (one Map per record)
     */
    fun oneSearchProperties(
        property: KProperty1<E, *>,
        value: Any?,
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, *>>

    /**
     * Queries all data.
     *
     * @param orders Sort rules
     * @return Full list of entities
     */
    fun allSearch(vararg orders: Order): List<E>

    /**
     * Queries all data, returning only a single property.
     *
     * @param returnProperty Return property
     * @param orders Sort rules
     * @return List of the specified property values
     */
    fun <R> allSearchProperty(returnProperty: KProperty1<E, R>, vararg orders: Order): List<R>

    /**
     * Queries all data, returning only a mapping of multiple properties.
     *
     * @param returnProperties Collection of return properties
     * @param orders Sort rules
     * @return List of multi-property mappings (one Map per record)
     */
    fun allSearchProperties(
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, *>>

    /**
     * Multi-property AND query, returning a list of entities.
     *
     * @param properties Property-to-value mapping
     * @param orders Sort rules
     * @return List of entities satisfying the AND conditions
     */
    fun andSearch(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E>

    /**
     * Multi-property OR query, returning a list of entities.
     *
     * @param properties Property-to-value mapping
     * @param orders Sort rules
     * @return List of entities satisfying the OR conditions
     */
    fun orSearch(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E>

    /**
     * IN query, returning a list of entities.
     *
     * @param property Property participating in IN
     * @param values Collection of IN values
     * @param orders Sort rules
     * @return List of entities satisfying the IN condition
     */
    fun inSearch(property: KProperty1<E, *>, values: Collection<*>, vararg orders: Order): List<E>

    /**
     * Primary key IN query, returning a list of entities.
     *
     * @param values Collection of primary keys
     * @param orders Sort rules
     * @return List of entities matched by primary keys
     */
    fun inSearchById(values: Collection<PK>, vararg orders: Order): List<E>

    /**
     * Primary key IN query, returning only a single property (string property name version).
     *
     * This overload is generally used for compatibility with legacy callers.
     *
     * @param values Collection of primary keys
     * @param returnProperty Return property name
     * @param orders Sort rules
     * @return List of the specified property values
     */
    fun inSearchPropertyById(values: Collection<PK>, returnProperty: String, vararg orders: Order): List<*>

    /**
     * Primary key IN query, returning only a single property (type-safe version).
     *
     * @param values Collection of primary keys
     * @param returnProperty Return property
     * @param orders Sort rules
     * @return List of the specified property values
     */
    fun <R> inSearchPropertyById(values: Collection<PK>, returnProperty: KProperty1<E, R>, vararg orders: Order): List<R>

    /**
     * Primary key IN query, returning only a mapping of multiple properties.
     *
     * @param values Collection of primary keys
     * @param returnProperties Collection of return property names
     * @param orders Sort rules
     * @return List of multi-property mappings (one Map per record)
     */
    fun inSearchPropertiesById(
        values: Collection<PK>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>>

    fun search(criteria: Criteria? = null, vararg orders: Order): List<E>

    fun <T: Any> search(
        criteria: Criteria? = null,
        returnItemClass: KClass<T>? = null,
        vararg orders: Order
    ): List<T>

    fun pagingSearch(
        criteria: Criteria? = null,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<E>

    fun <T: Any> pagingSearch(
        criteria: Criteria? = null,
        returnItemClass: KClass<T>? = null,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<T>

    /**
     * Queries by [Criteria], returning only a list of a single property.
     *
     * @param criteria Query conditions
     * @param returnProperty Return property
     * @param orders Sort rules
     * @return List of the specified property values
     */
    fun <R> searchProperty(criteria: Criteria, returnProperty: KProperty1<E, R>, vararg orders: Order): List<R>

    /**
     * Queries by [Criteria], returning only a mapping of multiple properties.
     *
     * @param criteria Query conditions
     * @param returnProperties Collection of return properties
     * @param orders Sort rules
     * @return List of multi-property mappings (one Map per record)
     */
    fun searchProperties(
        criteria: Criteria,
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, Any?>>

    /**
     * Paginated query by [Criteria], returning only a list of a single property.
     *
     * @param criteria Query conditions
     * @param returnProperty Return property
     * @param pageNo Page number (starting at 1)
     * @param pageSize Page size
     * @param orders Sort rules
     * @return List of the specified property values for the current page
     */
    fun <R> pagingReturnProperty(
        criteria: Criteria,
        returnProperty: KProperty1<E, R>,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<R>

    /**
     * Paginated query by [Criteria], returning only a mapping of multiple properties.
     *
     * @param criteria Query conditions
     * @param returnProperties Collection of return properties
     * @param pageNo Page number (starting at 1)
     * @param pageSize Page size
     * @param orders Sort rules
     * @return List of multi-property mappings for the current page (one Map per record)
     */
    fun pagingReturnProperties(
        criteria: Criteria,
        returnProperties: Collection<KProperty1<E, *>>,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<Map<String, *>>

    /**
     * Queries the result list according to the list search payload; supports pagination (when listSearchPayload.pageNo is not null).
     *
     * The return type is determined by `listSearchPayload.returnProperties` and `listSearchPayload.returnEntityClass`,
     * and may be a list of entities, a list of single-property values, or a list of property mappings.
     *
     * @return Query result list (the element type is determined by the payload configuration)
     */
    fun search(listSearchPayload: ListSearchPayload? = null): List<*>

    /**
     * Queries according to the list search payload and specifies the return element type. Supports pagination (when listSearchPayload.pageNo is not null).
     *
     * This method can only be used safely when `listSearchPayload.returnProperties` is empty.
     *
     * @return Result list of the specified type
     */
    fun <T : Any> search(listSearchPayload: ListSearchPayload? = null, returnItemClass: KClass<T>): List<T>

    fun count(criteria: Criteria? = null): Int

    /**
     * Counts records according to the search payload.
     *
     * @param searchPayload Search payload
     * @return Number of records
     */
    fun count(searchPayload: ISearchPayload?): Int

    /**
     * Sum aggregation.
     *
     * @param property Property to sum
     * @param criteria Filter condition; null means all data
     * @return Sum result
     */
    fun sum(property: KProperty1<E, *>, criteria: Criteria? = null): Number

    /**
     * Average aggregation.
     *
     * @param property Property to average
     * @param criteria Filter condition; null means all data
     * @return Average result
     */
    fun avg(property: KProperty1<E, *>, criteria: Criteria? = null): Number

    /**
     * Maximum aggregation.
     *
     * @param property Target property
     * @param criteria Filter condition; null means all data
     * @return Maximum value; returns null when there is no data
     */
    fun <R : Comparable<R>> max(property: KProperty1<E, R?>, criteria: Criteria? = null): R?

    /**
     * Minimum aggregation.
     *
     * @param property Target property
     * @param criteria Filter condition; null means all data
     * @return Minimum value; returns null when there is no data
     */
    fun <R : Comparable<R>> min(property: KProperty1<E, R?>, criteria: Criteria? = null): R?
}