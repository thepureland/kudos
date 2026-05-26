package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.ability.data.rdb.ktorm.datasource.currentDatabase
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.lang.GenericKit
import io.kudos.base.lang.string.underscoreToHump
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.base.query.sort.sortablePropertyNamesForEntity
import io.kudos.base.support.GroupExecutor
import io.kudos.base.support.dao.IBaseReadOnlyDao
import io.kudos.base.support.logic.AndOrEnum
import io.kudos.base.model.payload.ISearchPayload
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.model.payload.MutableListSearchPayload
import io.kudos.context.core.KudosContextHolder
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.expression.InListExpression
import org.ktorm.expression.OrderByExpression
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Base read-only data access object that encapsulates common query operations for a database table.
 *
 * @param PK Entity primary key type
 * @param E Entity type
 * @param T Type of the database table-entity binding object
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class BaseReadOnlyDao<PK : Any, E : IDbEntity<PK, E>, T : Table<E>> : IBaseReadOnlyDao<PK, E> {

    /** Database table-entity binding object */
    private var table: T? = null

    private var entityClass: KClass<E>? = null

    /**
     * Returns the database table-entity binding object.
     *
     * @return Database table-entity binding object
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    protected fun table(): T {
        if (table == null) {
            val tableClass = GenericKit.getSuperClassGenricClass(this::class, 2) as KClass<T>
            table = requireNotNull(tableClass.objectInstance) {
                "DAO generic table type [${tableClass.qualifiedName}] must be an object singleton."
            }
        }
        return requireNotNull(table) { "Failed to initialize the table object corresponding to the DAO." }
    }

    /**
     * Returns the entity type corresponding to the current DAO.
     *
     * @return Entity KClass
     */
    @Suppress("UNCHECKED_CAST")
    protected fun entityClass(): KClass<E> {
        if (entityClass == null) {
            entityClass = GenericKit.getSuperClassGenricClass(this::class, 1) as KClass<E>
        }
        return requireNotNull(entityClass) { "Failed to resolve the entity type corresponding to the DAO." }
    }

    /**
     * Returns the set of sortable property names based on [io.kudos.base.query.sort.Sortable] annotations
     * on the entity (PO) bound to the current DAO's table (independent of the VO/PO return type in the request).
     */
    protected fun sortWhitelistFromPo(): Set<String> {
        val ec = table().entityClass ?: return emptySet()
        return sortablePropertyNamesForEntity(ec)
    }

    /**
     * Filters client-supplied sort orders by the [io.kudos.base.query.sort.Sortable] annotations on the PO.
     * Every property used for sorting must have the corresponding annotation on the PO; unannotated entries
     * are logged at WARN (with entity class and property name) and ignored in the query.
     * [whitelist] contains all @Sortable property names on the PO; if the PO has no @Sortable annotations,
     * [whitelist] is empty and all requested sort entries are ignored with a warning.
     */
    protected fun filterOrdersBySortWhitelist(rawOrders: List<Order>?, whitelist: Set<String>): List<Order> {
        val list = rawOrders ?: emptyList()
        val rejected = list.filter { it.property !in whitelist }
        if (rejected.isNotEmpty()) {
            val entityName = table().entityClass?.qualifiedName
                ?: table().entityClass?.simpleName
                ?: "unknown"
            for (order in rejected) {
                log.warn(
                    "Sort property in query request is not annotated with @Sortable on the table entity; ignored: entityClass={0}, property={1}",
                    entityName,
                    order.property
                )
            }
        }
        return list.filter { it.property in whitelist }
    }

    /**
     * Returns the database object that the current table belongs to.
     *
     * @return Database object containing the current table
     * @author K
     * @since 1.0.0
     */
    protected fun database(): Database = KudosContextHolder.currentDatabase()

    /**
     * Returns the query source for the table specified by T, on which SQL-like operations can be performed.
     *
     * @return Query source
     * @author K
     * @since 1.0.0
     */
    protected fun querySource(): QuerySource = database().from(table())

    /**
     * Returns the entity sequence for the table specified by T, on which collection-like operations can be performed.
     *
     * @return Entity sequence
     * @author K
     * @since 1.0.0
     */
    protected fun entitySequence(): EntitySequence<E, T> = database().sequenceOf(table())

    private fun filteredSequence(criteria: Criteria?): EntitySequence<E, T> {
        val seq = entitySequence()
        return if (criteria == null) seq
        else seq.filter { CriteriaConverter.convert(criteria, table()) }
    }

    /**
     * Returns the primary key column (kudos table convention: every table has exactly one primary key column named id).
     *
     * @return Primary key column object
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    protected fun getPkColumn(): Column<PK> {
        return table().primaryKeys[0] as Column<PK>
    }

    /**
     * Creates a query condition expression.
     *
     * @param column Column object
     * @param operator Operator
     * @param value Value to query
     * @return Column declaration object
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    open fun <A : Any> whereExpr(column: Column<A>, operator: OperatorEnum, value: A?): ColumnDeclaring<Boolean>? {
        return SqlWhereExpressionFactory.create(column as Column<Any>, operator, value)
    }

    //region Search

    override fun get(id: PK): E? {
        return entitySequence().firstOrNull { getPkColumn() eq id }
    }

    override fun <R : Any> get(id: PK, returnType: KClass<R>?): R? {
        return if (returnType == null || returnType == entityClass()) {
            @Suppress("UNCHECKED_CAST")
            get(id) as R?
        } else {
            val query = querySource()
                .select()
                .where { getPkColumn().eq(id) }
            val columnMap = getColumns(returnType)
            query.forEach { row ->
                val values = columnMap.mapValues { (_, column) -> row[column] }
                return instantiateResultItem(returnType, values)
            }
            null
        }
    }

    /**
     * Queries an entity by its primary key value; the result type may be specified via generics.
     *
     * Shortcut for get(id: PK, returnType: KClass<R>).
     *
     * @param R Type of the returned object
     * @param id Primary key value; must be one of: String, Int, Long
     * @return Result object of the specified type; null if not found
     * @author K
     * @since 1.0.0
     */
    inline fun <reified R : Any> getAs(id: PK): R? = get(id, R::class)

    override fun getByIds(ids: Collection<PK>, countOfEachBatch: Int): List<E> {
        if (ids.isEmpty()) return listOf()
        val results = mutableListOf<E>()
        GroupExecutor(ids, countOfEachBatch) { subList ->
            val result = entitySequence().filter { getPkColumn().inList(subList) }.toList()
            results.addAll(result)
        }.execute()
        return results
    }

    override fun <T : Any> getByIds(
        ids: Collection<PK>,
        returnItemClass: KClass<T>?,
        countOfEachBatch: Int
    ): List<T> {
        if (ids.isEmpty()) return listOf()
        return if (returnItemClass == null || returnItemClass == entityClass()) {
            @Suppress("UNCHECKED_CAST")
            getByIds(ids, countOfEachBatch = countOfEachBatch) as List<T>
        } else {
            val results = mutableListOf<T>()
            val idProperty = getPkColumn().name.underscoreToHump()
            GroupExecutor(ids, countOfEachBatch) { subList ->
                val criteria = Criteria(idProperty, OperatorEnum.IN, subList)
                val result = search(criteria, returnItemClass)
                results.addAll(result)
            }.execute()
            results
        }
    }

    /**
     * Batch-queries entities by their primary key values.
     *
     * Shortcut for getByIds(vararg ids: PK, returnItemClass: KClass<T>?, countOfEachBatch: Int).
     *
     * @param T Element type of the result list
     * @param ids Primary key collection; element type must be one of: String, Int, Long; returns empty list when empty
     * @param countOfEachBatch Batch size; defaults to 1000
     * @return List of objects of the specified element type; empty list when ids is empty
     * @author K
     * @since 1.0.0
     */
    inline fun <reified T : Any> getByIdsAs(ids: Collection<PK>, countOfEachBatch: Int = 1000): List<T> =
        getByIds(ids, T::class, countOfEachBatch)

    //endregion Search

    override fun oneSearch(property: KProperty1<E, *>, value: Any?, vararg orders: Order): List<E> =
        oneSearch(property.name, value, *orders)

    override fun <R> oneSearchProperty(
        property: KProperty1<E, *>,
        value: Any?,
        returnProperty: KProperty1<E, R>,
        vararg orders: Order
    ): List<R> = oneSearchProperty(property.name, value, returnProperty.name, *orders)

    override fun oneSearchProperties(
        property: KProperty1<E, *>,
        value: Any?,
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, *>> = oneSearchProperties(property.name, value, returnProperties.map { it.name }, *orders)

    //region oneSearch

    /**
     * Single-property equality query (string property-name version).
     *
     * @param property Property name
     * @param value Query value
     * @param orders Sort orders
     * @return List of entities matching the condition
     */
    private fun oneSearch(property: String, value: Any?, vararg orders: Order): List<E> {
        return doSearchEntity(mapOf(property to value), null, null, *orders)
    }

    /**
     * Single-property equality query returning only a single property (string property-name version).
     *
     * @param property Query property name
     * @param value Query value
     * @param returnProperty Return property name
     * @param orders Sort orders
     * @return List of values for the specified property
     */
    @Suppress("UNCHECKED_CAST")
    private fun <R> oneSearchProperty(
        property: String,
        value: Any?,
        returnProperty: String,
        vararg orders: Order
    ): List<R> {
        val results = doSearchProperties(mapOf(property to value), null, listOf(returnProperty), null, *orders)
        return results.flatMap { it.values } as List<R>
    }

    /**
     * Single-property equality query returning multiple property maps (string property-name version).
     *
     * @param property Query property name
     * @param value Query value
     * @param returnProperties Return property name collection
     * @param orders Sort orders
     * @return List of multi-property maps
     */
    private fun oneSearchProperties(
        property: String, value: Any?, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>> {
        return doSearchProperties(mapOf(property to value), null, returnProperties, null, *orders)
    }

    //endregion oneSearch


    //region allSearch

    override fun allSearch(vararg orders: Order): List<E> {
        return entitySequence().toList()
    }

    /**
     * Query all rows, returning only a single property (string property-name version).
     *
     * @param returnProperty Return property name
     * @param orders Sort orders
     * @return List of values for the specified property
     */
    @Suppress("UNCHECKED_CAST")
    private fun <R> allSearchProperty(returnProperty: String, vararg orders: Order): List<R> {
        val results = doSearchProperties(null, null, listOf(returnProperty), null, *orders)
        return results.flatMap { it.values } as List<R>
    }

    /**
     * Query all rows, returning multiple property maps (string property-name version).
     *
     * @param returnProperties Return property name collection
     * @param orders Sort orders
     * @return List of multi-property maps
     */
    private fun allSearchPropertiesByNames(
        returnProperties: Collection<String>,
        vararg orders: Order
    ): List<Map<String, *>> {
        return doSearchProperties(null, null, returnProperties, null, *orders)
    }

    override fun <R> allSearchProperty(returnProperty: KProperty1<E, R>, vararg orders: Order): List<R> =
        allSearchProperty(returnProperty.name, *orders)

    override fun allSearchProperties(
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, *>> = allSearchPropertiesByNames(returnProperties.map { it.name }, *orders)

    //endregion allSearch


    //region andSearch

    /**
     * Multi-property AND query (string property-name version) returning an entity list.
     *
     * @param properties Property-name-to-value map
     * @param orders Sort orders
     * @return List of entities matching the condition
     */
    private fun andSearchByNames(properties: Map<String, *>, vararg orders: Order): List<E> {
        return doSearchEntity(properties, AndOrEnum.AND, null, *orders)
    }

    override fun andSearch(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E> =
        andSearchByNames(properties.entries.associate { it.key.name to it.value }, *orders)

    open fun andSearch(
        properties: Map<KProperty1<E, *>, *>,
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)?,
        vararg orders: Order
    ): List<E> {
        val mapped = properties.entries.associate { it.key.name to it.value }
        return doSearchEntity(mapped, AndOrEnum.AND, whereConditionFactory, *orders)
    }

    //endregion andSearch


    //region orSearch

    /**
     * Multi-property OR query (string property-name version) returning an entity list.
     *
     * @param properties Property-name-to-value map
     * @param orders Sort orders
     * @return List of entities matching the condition
     */
    private fun orSearchByNames(properties: Map<String, *>, vararg orders: Order): List<E> {
        return doSearchEntity(properties, AndOrEnum.OR, null, *orders)
    }

    override fun orSearch(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E> =
        orSearchByNames(properties.entries.associate { it.key.name to it.value }, *orders)

    open fun orSearch(
        properties: Map<KProperty1<E, *>, *>,
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)?,
        vararg orders: Order
    ): List<E> {
        val mapped = properties.entries.associate { it.key.name to it.value }
        return doSearchEntity(mapped, AndOrEnum.OR, whereConditionFactory, *orders)
    }

    //endregion orSearch


    //region inSearch

    /**
     * IN query (string property-name version) returning an entity list.
     *
     * @param property Property name used in the IN clause
     * @param values IN value collection
     * @param orders Sort orders
     * @return List of entities matching the IN condition
     */
    private fun inSearch(property: String, values: Collection<*>, vararg orders: Order): List<E> {
        val column = requireNotNull(ColumnHelper.columnOf(table(), property)[property]) {
            "Database column for property [$property] not found."
        }
        var entitySequence = entitySequence().filter { column.inCollection(values) }
        entitySequence = entitySequence.sortedBy(*sortBy(*orders))
        return entitySequence.toList()
    }

    override fun inSearch(property: KProperty1<E, *>, values: Collection<*>, vararg orders: Order): List<E> =
        inSearch(property.name, values, *orders)

    /**
     * IN query returning only a single property (string property-name version).
     *
     * @param property Property name used in the IN clause
     * @param values IN value collection
     * @param returnProperty Return property name
     * @param orders Sort orders
     * @return List of values for the specified property
     */
    private fun inSearchProperty(
        property: String, values: Collection<*>, returnProperty: String, vararg orders: Order
    ): List<*> {
        val results = doInSearchProperties(property, values, listOf(returnProperty), *orders)
        return results.flatMap { it.values }
    }

    /**
     * IN query returning multiple property maps (string property-name version).
     *
     * @param property Property name used in the IN clause
     * @param values IN value collection
     * @param returnProperties Return property name collection
     * @param orders Sort orders
     * @return List of multi-property maps
     */
    private fun inSearchProperties(
        property: String, values: Collection<*>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>> {
        return doInSearchProperties(property, values, returnProperties, *orders)
    }

    override fun inSearchById(values: Collection<PK>, vararg orders: Order): List<E> {
        return inSearch(IDbEntity<PK, E>::id.name, values, *orders)
    }

    override fun inSearchPropertyById(values: Collection<PK>, returnProperty: String, vararg orders: Order): List<*> {
        val results = doInSearchProperties(IDbEntity<PK, E>::id.name, values, listOf(returnProperty), *orders)
        return results.flatMap { it.values }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R> inSearchPropertyById(
        values: Collection<PK>,
        returnProperty: KProperty1<E, R>,
        vararg orders: Order
    ): List<R> = inSearchPropertyById(values, returnProperty.name, *orders) as List<R>

    override fun inSearchPropertiesById(
        values: Collection<PK>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>> {
        return doInSearchProperties(IDbEntity<PK, E>::id.name, values, returnProperties, *orders)
    }

    //endregion inSearch


    //region search Criteria

    /**
     * Complex-condition query allowing the caller to specify the result wrapper class. Properties that do not
     * match the table entity are ignored.
     *
     * The purpose is to avoid the overhead and boilerplate of converting POs to the required VOs across scenarios.
     *
     * Shortcut for search(criteria: Criteria?, returnItemClass: KClass<T>?, vararg orders: Order).
     *
     * @param T Element type of the result list
     * @param criteria Query condition; null means unconditional query; defaults to null
     * @param orders Sort orders
     * @return List of objects of the specified result type
     * @author K
     * @since 1.0.0
     */
    inline fun <reified T : Any> searchAs(
        criteria: Criteria? = null,
        vararg orders: Order
    ): List<T> = search(criteria, T::class, *orders)

    override fun search(criteria: Criteria?, vararg orders: Order): List<E> {
        return searchEntityCriteria(criteria, 0, 0, *orders)
    }

    override fun <T : Any> search(criteria: Criteria?, returnItemClass: KClass<T>?, vararg orders: Order): List<T> {
        return if (returnItemClass == null || returnItemClass == entityClass()) {
            @Suppress("UNCHECKED_CAST")
            if (criteria == null) {
                allSearch(*orders) as List<T>
            } else {
                search(criteria, *orders) as List<T>
            }
        } else {
            searchByCriteria(criteria, returnItemClass, orders = orders)
        }
    }

    /**
     * Criteria query returning only a single property (string property-name version).
     *
     * @param criteria Query condition
     * @param returnProperty Return property name
     * @param orders Sort orders
     * @return List of values for the specified property
     */
    @Suppress("UNCHECKED_CAST")
    private fun <R> searchProperty(criteria: Criteria, returnProperty: String, vararg orders: Order): List<R> {
        val results = searchPropertiesCriteria(criteria, listOf(returnProperty), 0, 0, *orders)
        return results.flatMap { it.values } as List<R>
    }

    override fun <R> searchProperty(
        criteria: Criteria,
        returnProperty: KProperty1<E, R>,
        vararg orders: Order
    ): List<R> =
        searchProperty(criteria, returnProperty.name, *orders)

    /**
     * Criteria query returning multiple property maps (string property-name version).
     *
     * @param criteria Query condition
     * @param returnProperties Return property name collection
     * @param orders Sort orders
     * @return List of multi-property maps
     */
    private fun searchPropertiesByNames(
        criteria: Criteria, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, Any?>> {
        return searchPropertiesCriteria(criteria, returnProperties, 0, 0, *orders)
    }

    override fun searchProperties(
        criteria: Criteria,
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, Any?>> = searchPropertiesByNames(criteria, returnProperties.map { it.name }, *orders)

    //endregion search Criteria


    //region pagingSearch

    /**
     * Criteria paging query returning an entity list.
     *
     * @param criteria Query condition; may be null
     * @param pageNo Page number
     * @param pageSize Page size
     * @param orders Sort orders
     * @return Entity list for the current page
     */
    override fun pagingSearch(criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order): List<E> {
        return searchEntityCriteria(criteria, pageNo, pageSize, *orders)
    }

    /**
     * Criteria paging query with explicit return type.
     *
     * @param criteria Query condition; may be null
     * @param returnItemClass Result element type; may be null
     * @param pageNo Page number
     * @param pageSize Page size
     * @param orders Sort orders
     * @return List of results of the specified type for the current page
     */
    override fun <T : Any> pagingSearch(
        criteria: Criteria?,
        returnItemClass: KClass<T>?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<T> {
        return if (returnItemClass == null || returnItemClass == entityClass()) {
            // Use the table-entity logic
            @Suppress("UNCHECKED_CAST")
            pagingSearch(criteria, pageNo, pageSize, *orders) as List<T>
        } else {
            searchByCriteria(criteria, returnItemClass, pageNo, pageSize, *orders)
        }
    }

    /**
     * Paging query allowing the caller to specify the result wrapper class. Properties that do not match
     * the table entity are ignored.
     *
     * The purpose is to avoid the overhead and boilerplate of converting POs to the required VOs across scenarios.
     *
     * Shortcut for pagingSearch(criteria: Criteria?, returnItemClass: KClass<T>?, pageNo: Int, pageSize: Int, vararg orders: Order).
     *
     * @param T Element type of the result list
     * @param criteria Query condition; null means unconditional query; defaults to null
     * @param pageNo Current page number (1-based)
     * @param pageSize Rows per page
     * @param orders Sort orders
     * @return List of objects of the specified result type
     * @author K
     * @since 1.0.0
     */
    inline fun <reified T : Any> pagingSearchAs(
        criteria: Criteria?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<T> = pagingSearch(criteria, T::class, pageNo, pageSize, *orders)

    /**
     * Paging query that returns only a single column. Reuses [searchPropertiesCriteria] for the query
     * logic, then uses `flatMap` to flatten `List<Map<String, *>>` into a list of column values.
     *
     * @param R Column value type (caller guarantees type safety at the public API boundary)
     * @param criteria Query condition
     * @param returnProperty Column name to query
     * @param pageNo Page number (1-based)
     * @param pageSize Rows per page
     * @param orders Sort orders
     * @return List of values for that column in the current page
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    private fun <R> pagingReturnProperty(
        criteria: Criteria, returnProperty: String, pageNo: Int, pageSize: Int, vararg orders: Order
    ): List<R> {
        val results = searchPropertiesCriteria(criteria, listOf(returnProperty), pageNo, pageSize, *orders)
        return results.flatMap { it.values } as List<R>
    }

    override fun <R> pagingReturnProperty(
        criteria: Criteria,
        returnProperty: KProperty1<E, R>,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<R> = pagingReturnProperty(criteria, returnProperty.name, pageNo, pageSize, *orders)

    /**
     * Criteria paging query returning multiple property maps (string property-name version).
     *
     * @param criteria Query condition
     * @param returnProperties Return property name collection
     * @param pageNo Page number
     * @param pageSize Page size
     * @param orders Sort orders
     * @return List of multi-property maps for the current page
     */
    private fun pagingReturnPropertiesByNames(
        criteria: Criteria, returnProperties: Collection<String>, pageNo: Int, pageSize: Int, vararg orders: Order
    ): List<Map<String, *>> {
        return searchPropertiesCriteria(criteria, returnProperties, pageNo, pageSize, *orders)
    }

    override fun pagingReturnProperties(
        criteria: Criteria,
        returnProperties: Collection<KProperty1<E, *>>,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<Map<String, *>> =
        pagingReturnPropertiesByNames(criteria, returnProperties.map { it.name }, pageNo, pageSize, *orders)

    //endregion pagingSearch


    //region payload search

    override fun search(listSearchPayload: ListSearchPayload?): List<*> {
        return search(listSearchPayload, null)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> search(listSearchPayload: ListSearchPayload?, returnItemClass: KClass<T>): List<T> {
        require(listSearchPayload?.getReturnProperties().isNullOrEmpty()) {
            "When ListSearchPayload.returnProperties is non-empty, search(payload, returnItemClass) cannot guarantee the result element type."
        }
        return if (listSearchPayload == null) {
            search(criteria = null, returnItemClass = returnItemClass)
        } else if (listSearchPayload is MutableListSearchPayload) {
            val originalReturnEntityClass = listSearchPayload.getReturnEntityClass()
            try {
                listSearchPayload.setReturnEntityClass(returnItemClass)
                search(listSearchPayload) as List<T>
            } finally {
                listSearchPayload.setReturnEntityClass(originalReturnEntityClass)
            }
        } else {
            search(listSearchPayload, null, returnItemClass) as List<T>
        }
    }

    /**
     * Queries (including paging) based on the search payload object. See @see SearchPayload for the detailed rules.
     *
     * When the query logic for the same property is specified in both listSearchPayload and whereConditionFactory,
     * whereConditionFactory takes precedence!
     *
     * @param listSearchPayload Search payload; defaults to null. When null, the result list element type is the PO entity class;
     *   in this case, if whereConditionFactory is supplied, the conditions are combined with AND.
     * @param whereConditionFactory Factory function for where-clause expressions. It can define the query logic for the
     *   listSearchPayload parameters or be entirely custom. When the function returns null, the property in
     *   listSearchPayload is treated with an "equals" operation. Defaults to null.
     * @return Result list — three possible types; see @see SearchPayload
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    open fun search(
        listSearchPayload: ListSearchPayload? = null,
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)? = null,
        returnItemClassOverride: KClass<*>? = null
    ): List<*> {
        // select, where
        val objects = searchByPayload(listSearchPayload, whereConditionFactory, returnItemClassOverride)
        var query = objects[0] as Query
        val returnProps = objects[1] as Set<String>
        val returnColumnMap = objects[2] as Map<String, Column<Any>>

        listSearchPayload?.let { payload ->
            // order (only whitelisted fields participate in sorting)
            val allowedOrders = filterOrdersBySortWhitelist(payload.orders, sortWhitelistFromPo())
            if (allowedOrders.isNotEmpty()) {
                query = query.orderBy(sortOf(*allowedOrders.toTypedArray()))
            }
            // paging (when pageNo is null and unpaged search is disallowed, fall back to page 1)
            val pageNo = payload.pageNo?.let { maxOf(1, it) }
                ?: if (payload.isUnpagedSearchAllowed()) null else 1
            pageNo?.let {
                val rawSize = payload.pageSize ?: 10
                val pageSize = minOf(rawSize, payload.getMaxPageSize())
                query = query.limit((it - 1) * pageSize, pageSize)
            }
        }

        // result
        val returnProperties = listSearchPayload?.getReturnProperties() ?: emptyList()
        val effectiveReturnClass = returnItemClassOverride ?: listSearchPayload?.getReturnEntityClass()
        return when {
            returnProperties.isEmpty() -> {
                val beanList = mutableListOf<Any>()
                processResult(query, returnColumnMap).forEach { map ->
                    val bean = if (effectiveReturnClass == null) {
                        val tableEntityClass =
                            requireNotNull(table().entityClass) { "Table is not bound to an entity type; cannot create a return object." }
                        Entity.create(tableEntityClass).also { populateResultItem(it, map) }
                    } else {
                        instantiateResultItem(effectiveReturnClass, map)
                    }
                    beanList.add(bean)
                }
                beanList
            }

            returnProperties.size == 1 -> {
                val mapList = processResult(query, returnColumnMap)
                mapList.flatMap { it.values }
            }

            else -> {
                val mapList = processResult(query, returnColumnMap)
                mapList
            }
        }
    }

    //endregion payload search


    //region aggregate

    /**
     * Counts records based on a Criteria.
     *
     * @param criteria Query condition; may be null
     * @return Record count
     */
    protected fun countByCriteria(criteria: Criteria?): Int =
        if (criteria == null) entitySequence().count()
        else filteredSequence(criteria).aggregateColumns { count(getPkColumn()) } ?: 0

    override fun count(searchPayload: ISearchPayload?): Int {
        return count(searchPayload, null)
    }

    /**
     * Counts records.
     *
     * @param searchPayload Search payload; null counts all records. When whereConditionFactory is supplied,
     *   conditions are combined with AND by default.
     * @param whereConditionFactory Factory function for where-clause expressions. It can define the query logic for
     *   the searchPayload parameters or be entirely custom. When the function returns null, properties in searchPayload
     *   are treated with an "equals" operation. Defaults to null.
     * @return Record count
     * @author K
     * @since 1.0.0
     */
    open fun count(
        searchPayload: ISearchPayload?,
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)? = null
    ): Int {
        val query = searchByPayload(searchPayload, whereConditionFactory)[0] as Query
        return query.totalRecordsInAllPages
    }

    override fun count(criteria: Criteria?): Int = countByCriteria(criteria)

    /**
     * Sum of a property (string property-name version).
     *
     * @param property Property name
     * @param criteria Query condition; may be null
     * @return Sum result
     */
    @Suppress("UNCHECKED_CAST")
    private fun sum(property: String, criteria: Criteria?): Number =
        filteredSequence(criteria).aggregateColumns {
            sum(ColumnHelper.columnOf(table(), property)[property] as Column<Number>)
        } as Number

    /**
     * Average of a property (string property-name version).
     *
     * @param property Property name
     * @param criteria Query condition; may be null
     * @return Average result
     */
    @Suppress("UNCHECKED_CAST")
    private fun avg(property: String, criteria: Criteria?): Number =
        filteredSequence(criteria).aggregateColumns {
            avg(ColumnHelper.columnOf(table(), property)[property] as Column<Number>)
        } as Number

    /**
     * Maximum of a property (string property-name version).
     *
     * @param property Property name
     * @param criteria Query condition; may be null
     * @return Maximum value; null if no data
     */
    @Suppress("UNCHECKED_CAST")
    private fun <R : Comparable<R>> max(property: String, criteria: Criteria?): R? =
        filteredSequence(criteria).aggregateColumns {
            max(ColumnHelper.columnOf(table(), property)[property] as Column<Comparable<Any>>)
        } as R?

    /**
     * Minimum of a property (string property-name version).
     *
     * @param property Property name
     * @param criteria Query condition; may be null
     * @return Minimum value; null if no data
     */
    @Suppress("UNCHECKED_CAST")
    private fun <R : Comparable<R>> min(property: String, criteria: Criteria?): R? =
        filteredSequence(criteria).aggregateColumns {
            min(ColumnHelper.columnOf(table(), property)[property] as Column<Comparable<Any>>)
        } as R?

    override fun sum(property: KProperty1<E, *>, criteria: Criteria?): Number = sum(property.name, criteria)
    override fun avg(property: KProperty1<E, *>, criteria: Criteria?): Number = avg(property.name, criteria)
    override fun <R : Comparable<R>> max(property: KProperty1<E, R?>, criteria: Criteria?): R? =
        max(property.name, criteria)

    override fun <R : Comparable<R>> min(property: KProperty1<E, R?>, criteria: Criteria?): R? =
        min(property.name, criteria)

    //endregion aggregate

    /**
     * Translates the business-side [Order] array into a list of ktorm `OrderByExpression`, ready to feed
     * `query.orderBy(...)`.
     *
     * Column name → column object resolves via [ColumnHelper.columnOf]; a missing column throws via
     * `requireNotNull` — typically caused by a misspelled property name or a field not exposed on
     * [io.kudos.ability.data.rdb.ktorm.support.ITable]. Failing fast aids diagnosis.
     *
     * @param orders Sort orders array; empty returns an empty list (query gets no order by)
     * @return List of ktorm sort expressions
     * @author K
     * @since 1.0.0
     */
    private fun sortOf(vararg orders: Order): List<OrderByExpression> {
        return if (orders.isNotEmpty()) {
            val orderExpressions = mutableListOf<OrderByExpression>()
            orders.forEach {
                val column = requireNotNull(ColumnHelper.columnOf(table(), it.property)[it.property]) {
                    "Database column for property [${it.property}] not found."
                }
                val orderByExpression = if (it.isAscending()) {
                    column.asc()
                } else column.desc()
                orderExpressions.add(orderByExpression)
            }
            orderExpressions
        } else {
            emptyList()
        }
    }

    /**
     * Wraps the [OrderByExpression] list from [sortOf] into the
     * `Array<(T) -> OrderByExpression>` shape required by `EntitySequence.sortedBy(...)` — the closures
     * ignore their input and return the static expressions (required by the ktorm entity-sequence signature).
     *
     * Used by the `EntitySequence` path ([searchEntityCriteria]); the `Query` path uses [sortOf] directly.
     *
     * @param orders Sort orders
     * @return Array of sort functions for ktorm entity sequences
     * @author K
     * @since 1.0.0
     */
    private fun sortBy(vararg orders: Order): Array<(T) -> OrderByExpression> {
        val orderByExps = sortOf(*orders)
        val orderExpressions = mutableListOf<(T) -> OrderByExpression>()
        orderByExps.forEach { orderByExp -> orderExpressions.add { orderByExp } }
        return orderExpressions.toTypedArray()
    }

    /**
     * Translates a "property name → (operator, value)" map into a ktorm where-clause expression (AND / OR composite).
     *
     * Branch semantics:
     * - `IS_NULL` / `IS_NOT_NULL`: directly use the column null check
     * - Value is null or "": defaults to `column IS NULL`; when `ignoreNull=true`, the decision is delegated to `whereConditionFactory`
     * - Otherwise, [SqlWhereExpressionFactory] generates the expression based on the operator
     * - An explicit `whereConditionFactory` can override the default behavior (used by subclasses for custom conditions)
     *
     * Fallback: if no property produces an expression and a custom factory is provided, iterate over all columns
     * and let the factory "try again for each column", allowing it to derive a condition based on the column name
     * (typical use: column-name suffix conventions).
     *
     * @param propertyMap Property → (operator, value)
     * @param andOr Logical combinator between conditions; null defaults to AND
     * @param ignoreNull Whether to skip a column when its value is null/"" (true: let the factory decide)
     * @param whereConditionFactory Custom condition generator; null uses the default
     * @return Combined condition expression; null when no condition is produced (the caller should skip the where clause)
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    protected fun processWhere(
        propertyMap: Map<String, Pair<OperatorEnum, *>>,
        andOr: AndOrEnum?,
        ignoreNull: Boolean = false,
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)? = null
    ): ColumnDeclaring<Boolean>? {
        val columns = ColumnHelper.columnOf(table(), *propertyMap.keys.toTypedArray())
        val expressions = columns.mapNotNull { (property, column) ->
            val (operator, value) = requireNotNull(propertyMap[property]) {
                "Property [$property] is missing a query operator and value."
            }
            when {
                operator == OperatorEnum.IS_NULL -> column.isNull()
                operator == OperatorEnum.IS_NOT_NULL -> column.isNotNull()
                value == null || value == "" ->
                    if (ignoreNull) whereConditionFactory?.invoke(column, value) else column.isNull()
                whereConditionFactory == null -> SqlWhereExpressionFactory.create(column, operator, value)
                else -> whereConditionFactory(column, value) ?: (column eq value)
            }
        }.toMutableList()

        if (expressions.isEmpty() && whereConditionFactory != null) {
            table().columns.mapNotNullTo(expressions) { whereConditionFactory(it as Column<Any>, null) }
        }

        if (expressions.isEmpty()) return null
        val combine: (ColumnDeclaring<Boolean>, ColumnDeclaring<Boolean>) -> ColumnDeclaring<Boolean> =
            if (andOr == AndOrEnum.AND) ColumnDeclaring<Boolean>::and else ColumnDeclaring<Boolean>::or
        return expressions.reduce(combine)
    }

    /**
     * Uses the EntitySequence path to query full entities by a "property = value" map; the operator is fixed
     * to EQ and the combinator is decided by [logic]. Differs from [searchEntityCriteria] in that the input is
     * a property-value map (not a Criteria DSL), translated by [processWhere].
     *
     * @param propertyMap Property → value; null means no where clause
     * @param logic AND / OR combinator
     * @param whereConditionFactory Custom condition generator
     * @param orders Sort orders
     * @return Entity list
     * @author K
     * @since 1.0.0
     */
    private fun doSearchEntity(
        propertyMap: Map<String, *>?,
        logic: AndOrEnum?,
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)? = null,
        vararg orders: Order
    ): List<E> {
        var entitySequence = entitySequence()
        propertyMap?.let { map ->
            val propMap = map.mapValues { (_, value) -> OperatorEnum.EQ to value }
            processWhere(propMap, logic, false, whereConditionFactory)?.let { expr ->
                entitySequence = entitySequence.filter { expr }
            }
        }
        return entitySequence.sortedBy(*sortBy(*orders)).toList()
    }

    /**
     * Uses the Query path to query multiple columns by a "property = value" map; the operator is fixed to EQ
     * and the combinator is decided by [logic]. Differs from [doSearchEntity] in that the returned columns
     * are specified by [returnProperties], not the full entity fields.
     *
     * @param propertyMap Property → value; null means no where clause
     * @param logic AND / OR combinator
     * @param returnProperties Column names to return
     * @param whereConditionFactory Custom condition generator
     * @param orders Sort orders
     * @return List of column-name → column-value maps
     * @author K
     * @since 1.0.0
     */
    private fun doSearchProperties(
        propertyMap: Map<String, *>?,
        logic: AndOrEnum?,
        returnProperties: Collection<String>,
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)? = null,
        vararg orders: Order
    ): List<Map<String, *>> {
        // select
        val returnColumnMap = ColumnHelper.columnOf(table(), *returnProperties.toTypedArray())
        var query = querySource().select(returnColumnMap.values)

        // where
        propertyMap?.let { map ->
            val propMap = map.mapValues { (_, value) -> OperatorEnum.EQ to value }
            processWhere(propMap, logic, false, whereConditionFactory)?.let { expr ->
                query = query.where { expr }
            }
        }

        // order
        query = query.orderBy(sortOf(*orders))

        // result
        return processResult(query, returnColumnMap)
    }

    /**
     * Reassembles ktorm [Query] results into instances of business class [T] according to `returnColumnMap`.
     * Instantiation is delegated to [instantiateResultItem], which supports data classes, Entities, and Maps.
     *
     * @param T Target type
     * @param query A pre-built ktorm query
     * @param returnItemClass Target class KClass
     * @param returnColumnMap Property-name → column map (computed by [getColumns])
     * @return List of deserialized objects
     * @author K
     * @since 1.0.0
     */
    private fun <T : Any> processResult(
        query: Query,
        returnItemClass: KClass<T>,
        returnColumnMap: Map<String, Column<Any>>
    ): List<T> {
        val resultList = mutableListOf<T>()
        query.forEach { row ->
            val values = returnColumnMap.mapValues { (_, column) -> row[column] }
            val item = instantiateResultItem(returnItemClass, values)
            resultList.add(item)
        }
        return resultList
    }

    /**
     * Flattens ktorm [Query] results directly into `List<Map<String, *>>` (property name → column value).
     * Uses the "multi-column query without a business type" path to avoid reflection overhead.
     *
     * @param query A pre-built ktorm query
     * @param returnColumnMap Property-name → column map
     * @return One map per row
     * @author K
     * @since 1.0.0
     */
    private fun processResult(query: Query, returnColumnMap: Map<String, Column<Any>>): List<Map<String, *>> {
        val returnValues = mutableListOf<Map<String, Any?>>()
        query.forEach { row ->
            val map = returnColumnMap.map { (propName, column) -> propName to row[column] }.toMap()
            returnValues.add(map)
        }
        return returnValues
    }

    /**
     * Multi-column property version of an `IN (...)` query: filters by `property IN values` and returns the
     * columns listed in `returnProperties`.
     *
     * @param property Column name used in the IN clause
     * @param values IN candidate value collection
     * @param returnProperties Column names to return
     * @param orders Sort orders
     * @return List of column-name → column-value maps
     * @author K
     * @since 1.0.0
     */
    private fun doInSearchProperties(
        property: String, values: Collection<*>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>> {
        val column = requireNotNull(ColumnHelper.columnOf(table(), property)[property]) {
            "Database column for property [$property] not found."
        }
        val returnColumnMap = ColumnHelper.columnOf(table(), *returnProperties.toTypedArray())
        var query = querySource().select(returnColumnMap.values)
        query = query.where { column.inCollection(values) }
        query = query.orderBy(sortOf(*orders))
        return processResult(query, returnColumnMap)
    }

    /**
     * Query via the ktorm EntitySequence path: converts criteria into a sequence filter, then sorts and pages.
     * Returns full entities [E]. Complements [searchByCriteria]'s Query path — the sequence path suits "fetch
     * all fields of the entity".
     *
     * Paging threshold: `pageNo=0 || pageSize=0` means "no paging" — returns everything (drop/take are skipped).
     *
     * @param criteria Query condition; null skips the where clause
     * @param pageNo Page number (1-based; 0 means no paging)
     * @param pageSize Rows per page (0 means no paging)
     * @param orders Sort orders
     * @return Entity list
     * @author K
     * @since 1.0.0
     */
    private fun searchEntityCriteria(
        criteria: Criteria?, pageNo: Int = 0, pageSize: Int = 0, vararg orders: Order
    ): List<E> {
        var entitySequence = filteredSequence(criteria).sortedBy(*sortBy(*orders))
        if (pageNo != 0 && pageSize != 0) {
            entitySequence = entitySequence.drop((pageNo - 1) * pageSize).take(pageSize)
        }
        return entitySequence.toList()
    }

    /**
     * Multi-column projection query via the ktorm Query path: queries only the columns listed in
     * `returnProperties` and returns a list-of-map. Outperforms [searchEntityCriteria] by reading only the
     * required columns and avoiding the full-row → entity deserialization overhead.
     *
     * @param criteria Query condition
     * @param returnProperties Column names to query
     * @param pageNo Page number (1-based; 0 means no paging)
     * @param pageSize Rows per page
     * @param orders Sort orders
     * @return List of column-name → column-value maps
     * @author K
     * @since 1.0.0
     */
    private fun searchPropertiesCriteria(
        criteria: Criteria, returnProperties: Collection<String>,
        pageNo: Int = 0, pageSize: Int = 0, vararg orders: Order
    ): List<Map<String, *>> {
        val returnColumnMap = ColumnHelper.columnOf(table(), *returnProperties.toTypedArray())

        // where, order, paging
        val query = prepareQuery(criteria, returnColumnMap, pageNo, pageSize, *orders)

        // result
        return processResult(query, returnColumnMap)
    }

    /**
     * Selects columns based on [returnItemClass] + queries by criteria + applies paging + deserializes into a
     * list of the target type. Compared with [searchPropertiesCriteria], it adds the deserialization step (→ T);
     * compared with [searchEntityCriteria], the columns are determined by returnItemClass (typical use:
     * a business VO/DTO that differs from the table entity).
     *
     * @param T Target return type
     * @param criteria Query condition
     * @param returnItemClass Target type KClass
     * @param pageNo Page number (1-based; 0 means no paging)
     * @param pageSize Rows per page
     * @param orders Sort orders
     * @return List of objects of the target type
     * @author K
     * @since 1.0.0
     */
    private fun <T : Any> searchByCriteria(
        criteria: Criteria?,
        returnItemClass: KClass<T>,
        pageNo: Int = 0,
        pageSize: Int = 0,
        vararg orders: Order
    ): List<T> {
        val returnColumnMap = getColumns(returnItemClass)

        // select, where, order, paging
        val query = prepareQuery(criteria, returnColumnMap, pageNo, pageSize, *orders)

        // result
        return processResult(query, returnItemClass, returnColumnMap)
    }

    /**
     * Unified template for building a ktorm Query: select the specified columns → where (when criteria is non-null)
     * → orderBy → limit. Reused by [searchPropertiesCriteria], [searchByCriteria] and [searchByPayload] to avoid
     * duplicating boilerplate.
     *
     * @param criteria Query condition; null skips the where clause
     * @param returnColumnMap Property-name → column
     * @param pageNo Page number (1-based; 0 means no paging)
     * @param pageSize Rows per page
     * @param orders Sort orders
     * @return The constructed ktorm [Query]
     * @author K
     * @since 1.0.0
     */
    private fun prepareQuery(
        criteria: Criteria?,
        returnColumnMap: Map<String, Column<Any>>,
        pageNo: Int = 0,
        pageSize: Int = 0,
        vararg orders: Order
    ): Query {
        var query = querySource().select(returnColumnMap.values)

        // where
        if (criteria != null) {
            query = query.where { CriteriaConverter.convert(criteria, table()) }
        }

        // order
        query = query.orderBy(sortOf(*orders))

        // paging
        if (pageNo != 0 && pageSize != 0) {
            query = query.limit((pageNo - 1) * pageSize, pageSize)
        }

        return query
    }

    /**
     * Full-featured query entry point built on [ISearchPayload], supporting:
     * - select columns: three-level fallback of payload.returnProperties / payload.returnEntityClass / override
     * - where: three-stage assembly of payload property values + nullProperties + criterions (see [getWherePropertyMap])
     * - order + paging: orders / pageNo / pageSize carried by the payload
     *
     * The most common query entry in the ms-* service layer; the other private search* methods are simplified
     * versions of it for Criteria.
     *
     * @param searchPayload Business query payload; null degrades to "scan-all + default sort"
     * @param whereConditionFactory Business-supplied custom where generator
     * @param returnItemClassOverride Explicit return type; takes precedence over payload.returnEntityClass
     * @return List of deserialized objects (type determined by effectiveReturnClass)
     * @author K
     * @since 1.0.0
     */
    private fun searchByPayload(
        searchPayload: ISearchPayload? = null,
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)? = null,
        returnItemClassOverride: KClass<*>? = null
    ): List<Any> {
        val entityProperties = getEntityProperties()

        // select
        val returnProperties = searchPayload?.getReturnProperties() ?: emptyList()
        val effectiveReturnClass = returnItemClassOverride ?: searchPayload?.getReturnEntityClass()
        val props = returnProperties.ifEmpty {
            effectiveReturnClass?.memberProperties?.map { it.name } ?: entityProperties
        }
        val returnProps = entityProperties.intersect(props.toSet()) // intersection ensures the queried columns exist
        val returnColumnMap = ColumnHelper.columnOf(table(), *returnProps.toTypedArray())
        var query = querySource().select(returnColumnMap.values)

        // where
        val propMap = if (searchPayload == null) {
            emptyMap()
        } else {
            getWherePropertyMap(searchPayload, entityProperties)
        }
        val andOr = searchPayload?.getAndOr() ?: AndOrEnum.AND
        processWhere(propMap, andOr, true, whereConditionFactory)?.let { expr ->
            query = query.where { expr }
        }

        return listOf(query, returnProps, returnColumnMap)
    }

    /**
     * Uses reflection to obtain the property-name list of the entity class bound to the table. The three meta
     * properties built into the ktorm Entity interface (`entityClass` / `properties` / `changedProperties`)
     * are filtered out so they are not treated as business fields.
     *
     * @return List of business property names on the entity
     * @throws IllegalArgumentException If the table is not bound to an entity type
     * @author K
     * @since 1.0.0
     */
    protected fun getEntityProperties(): List<String> {
        val tableEntityClass = requireNotNull(table().entityClass) { "Table is not bound to an entity type; cannot extract entity properties." }
        return tableEntityClass.memberProperties
            .filter { it.name !in setOf("entityClass", "properties", "changedProperties") }
            .map { it.name }
    }

    /**
     * Decomposes [ISearchPayload] into a "column name → (operator, value)" map ready to feed [processWhere].
     *
     * Three-stage assembly:
     * 1. Bean properties of the payload + explicit operator map → default EQ
     * 2. Columns marked by nullProperties: only add `IS_NULL` when the value is absent from the bean (avoids
     *    conflicting with the "value present but null" semantics)
     * 3. Criterions carried by the payload (OR / composite): appended and overriding — business-supplied entries
     *    have the highest priority
     *
     * @param searchPayload Query payload passed from the business side
     * @param entityProperties Legal property list of the current table entity (used as a whitelist filter)
     * @return Map of column name → (operator, value)
     * @author K
     * @since 1.0.0
     */
    protected fun getWherePropertyMap(
        searchPayload: ISearchPayload, entityProperties: List<String>
    ): Map<String, Pair<OperatorEnum, *>> {
        val operatorByNameMap = searchPayload.getOperators()
            ?.entries
            ?.associate { it.key.name to it.value }
            ?: emptyMap()
        val propNameAndValueMap = BeanKit.extract(searchPayload)
            .filterKeys { it != "class" }
            .filter { entityProperties.contains(it.key) }
        val resultMap = mutableMapOf<String, Pair<OperatorEnum, *>>()
        propNameAndValueMap.forEach { (propName, value) ->
            var operator = operatorByNameMap[propName]
            if (operator == null) {
                operator = OperatorEnum.EQ
            }
            resultMap[propName] = Pair(operator, value)
        }
        val nullProperties = searchPayload.getNullProperties()
        if (!nullProperties.isNullOrEmpty()) {
            nullProperties.forEach { propName ->
                if (propNameAndValueMap[propName] == null) {
                    resultMap[propName] = Pair(OperatorEnum.IS_NULL, null)
                }
            }
        }
        val criterions = searchPayload.getCriterions()
        if (!criterions.isNullOrEmpty()) {
            criterions.forEach {
                resultMap[it.property] = Pair(it.operator, it.value)
            }
        }
        return resultMap
    }

    /**
     * Intersects the properties reflected from returnType with the table entity properties, then produces a
     * ktorm Column map for subsequent select operations.
     *
     * The intersection is a defensive design: returnType may be a VO/DTO with extra or missing fields. Forcing
     * such columns into ktorm would error out. The intersection guarantees the queried columns exist on the
     * table; extra fields will be ignored or left empty by the caller during the mapTo stage.
     *
     * @param returnType Target return type
     * @return Map of property name → column
     * @author K
     * @since 1.0.0
     */
    protected fun getColumns(returnType: KClass<*>): Map<String, Column<Any>> {
        val entityProperties = getEntityProperties()
        val properties = returnType.memberProperties.map { it.name }
        val returnProps = entityProperties.intersect(properties.toSet()) // intersection ensures the queried columns exist
        return ColumnHelper.columnOf(table(), *returnProps.toTypedArray())
    }

    /**
     * Populates a target instance from a QueryRowSet using the column map.
     *
     * @param rowSet QueryRowSet
     * @param destClass Target type
     * @param defaultValues Optional: property name -> default value (e.g. "" for id)
     * @param extraColumns Optional: column info outside the primary table (e.g. joined-table columns), property name -> column;
     *   merged with getColumns, extraColumns takes precedence; passing null is equivalent to an empty Map
     */
    protected fun <R : Any> mapTo(
        rowSet: QueryRowSet,
        destClass: KClass<R>,
        defaultValues: Map<String, Any>? = null,
        extraColumns: Map<String, Column<Any>>? = null
    ): R {
        val columnMap = getColumns(destClass)
        val extras = extraColumns ?: emptyMap()
        val values = (columnMap + extras).mapValues { (_, column) -> rowSet[column] }
        return instantiateResultItem(destClass, values, defaultValues ?: emptyMap())
    }

    /**
     * Deserializes Query-row column values into a [destClass] instance.
     *
     * Three strategies:
     * 1. **Ktorm Entity interface type** (`destClass.isSubclassOf(Entity::class)`): use `Entity.create` +
     *    [populateResultItem] — a Ktorm Entity interface PO has no Kotlin constructor and must go through the proxy factory
     * 2. **Traditional JavaBean / plain Kotlin class**: find the no-arg constructor + property backfill
     * 3. **data class / primary-constructor-only**: inject parameters via [callBy]; missing required non-null fields throw
     *
     * @param R Target type
     * @param destClass Target class
     * @param propertyValues Column name → column value
     * @param defaultValues Column name → default value (e.g. "" for id); lower priority than propertyValues
     * @return The deserialized instance
     * @throws IllegalArgumentException When required fields are missing
     * @author K
     * @since 1.0.0
     */
    private fun <R : Any> instantiateResultItem(
        destClass: KClass<R>,
        propertyValues: Map<String, Any?>,
        defaultValues: Map<String, Any?> = emptyMap()
    ): R {
        // Ktorm interface-type POs (companion inherits DbEntityFactory / Entity.Factory) have no Kotlin constructor; must be instantiated via Entity.create.
        if (destClass.isSubclassOf(Entity::class)) {
            @Suppress("UNCHECKED_CAST")
            val entity = Entity.create(destClass as KClass<Entity<*>>) as R
            populateResultItem(entity, defaultValues + propertyValues)
            return entity
        }

        // For traditional JavaBeans / plain Kotlin classes: prefer the no-arg constructor + property backfill.
        destClass.constructors.firstOrNull { it.parameters.isEmpty() }?.let {
            return it.call().also { obj -> populateResultItem(obj, propertyValues) }
        }

        // For immutable types such as data classes: assemble through the primary constructor by parameter name.
        val constructor = requireNotNull(destClass.primaryConstructor) {
            "Class ${destClass.qualifiedName} has neither a no-arg constructor nor a primary constructor; cannot wrap the query result."
        }
        val args = mutableMapOf<KParameter, Any?>()
        val missingRequiredParams = mutableListOf<String>()

        constructor.parameters.forEach { param ->
            val name = param.name ?: return@forEach
            val hasPropertyValue = propertyValues.containsKey(name)
            val hasDefaultValue = defaultValues.containsKey(name)
            if (!hasPropertyValue && !hasDefaultValue) {
                // Non-nullable required constructor parameters without a default must receive a value from the query result.
                if (!param.isOptional && !param.type.isMarkedNullable) {
                    missingRequiredParams.add(name)
                }
                return@forEach
            }
            val value = when {
                hasPropertyValue -> propertyValues[name]
                else -> defaultValues[name]
            }
            // For parameters with a Kotlin default value, passing null overrides the default; skip here so callBy uses the default.
            if (value == null && param.isOptional) {
                return@forEach
            }
            args[param] = value
        }

        require(missingRequiredParams.isEmpty()) {
            "Class ${destClass.qualifiedName} is missing query results for constructor parameters ${missingRequiredParams.joinToString(prefix = "[", postfix = "]")}; cannot instantiate."
        }
        return constructor.callBy(args)
    }

    /**
     * Writes column values back into an already-instantiated object.
     *
     * - **Ktorm Entity**: uses `target[property] = value` so the proxy maintains the changed flag; bypassing the
     *   proxy with direct reflection breaks dirty-field tracking and causes subsequent `update` calls to miss fields
     * - **Plain objects**: uses [BeanKit.setProperty] via the setter
     *
     * @param target Target object
     * @param propertyValues Column name → column value
     * @author K
     * @since 1.0.0
     */
    private fun populateResultItem(target: Any, propertyValues: Map<String, Any?>) {
        if (target is Entity<*>) {
            // Ktorm Entity maintains property values through its proxy; use index assignment to keep proxy state.
            propertyValues.forEach { (property, value) ->
                target[property] = value
            }
            return
        }
        // Plain objects still use BeanKit, supporting setter / reflective field writes.
        propertyValues.forEach { (property, value) ->
            BeanKit.setProperty(target, property, value)
        }
    }

    companion object {
        private val log = LogFactory.getLog(BaseReadOnlyDao::class)
    }

}


// Works around ktorm always dispatching to the vararg inList method and its generic-type limitations.
@Suppress("UNCHECKED_CAST")
private fun <T : Any> ColumnDeclaring<T>.inCollection(list: Collection<*>): InListExpression {
    return InListExpression(left = asExpression(), values = list.map { wrapArgument(it as T?) })
}
