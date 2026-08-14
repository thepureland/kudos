package io.kudos.base.support.service

import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.model.payload.ISearchPayload
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.model.payload.UpdatePayload
import io.kudos.base.query.Criteria
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.query.sort.Order
import io.kudos.base.support.dao.IBaseCrudDao
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Scriptable / recording fake DAO used by the BaseReadOnlyService / BaseCrudService tests.
 *
 * Read-only methods simply record the last call args and return a configurable canned value, so the
 * service-layer delegation contract can be verified without a real persistence layer. The write/delete
 * methods needed by the built-in-row protection logic are backed by tweakable lambdas.
 *
 * @author K
 * @since 1.0.0
 */
internal class FakeCrudDao<PK : Any, E : IIdEntity<PK>> : IBaseCrudDao<PK, E> {

    /** Records the name of the last invoked method and its primary argument(s) for delegation checks. */
    var lastCall: String? = null
    var lastArgs: List<Any?> = emptyList()

    /** Canned return values for the read-only methods (set per-test as needed). */
    var getResult: E? = null
    var getTypedResult: Any? = null
    var listResult: List<Any?> = emptyList()
    var mapListResult: List<Map<String, Any?>> = emptyList()
    var countResult: Int = 0
    var numberResult: Number = 0
    var comparableResult: Any? = null

    /** Behavior hooks for delete / search used by the built-in protection branches. */
    var batchDeleteCriteriaHandler: (Criteria) -> Int = { 0 }
    var searchPayloadHandler: (ListSearchPayload?) -> List<*> = { emptyList<Any?>() }
    var searchPropertyHandler: (Criteria) -> List<*> = { emptyList<Any?>() }
    var searchPropertiesHandler: (Criteria) -> List<Map<String, Any?>> = { emptyList() }
    var inSearchPropertiesByIdHandler: (Collection<PK>) -> List<Map<String, Any?>> = { emptyList() }

    /** Write-method recorders. */
    var insertResult: PK? = null
    var updateResult: Boolean = false
    var intResult: Int = 0
    var deleteResult: Boolean = false

    private fun record(name: String, vararg args: Any?) {
        lastCall = name
        lastArgs = args.toList()
    }

    // ---------- read-only ----------
    override fun get(id: PK): E? { record("get", id); return getResult }

    @Suppress("UNCHECKED_CAST")
    override fun <R : Any> get(id: PK, returnType: KClass<R>?): R? {
        record("getTyped", id, returnType); return getTypedResult as R?
    }

    override fun getByIds(ids: Collection<PK>, countOfEachBatch: Int): List<E> {
        record("getByIds", ids, countOfEachBatch)
        @Suppress("UNCHECKED_CAST")
        return listResult as List<E>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getByIds(ids: Collection<PK>, returnItemClass: KClass<T>?, countOfEachBatch: Int): List<T> {
        record("getByIdsTyped", ids, returnItemClass, countOfEachBatch); return listResult as List<T>
    }

    @Suppress("UNCHECKED_CAST")
    override fun oneSearch(property: KProperty1<E, *>, value: Any?, vararg orders: Order): List<E> {
        record("oneSearch", property, value, orders.toList()); return listResult as List<E>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R> oneSearchProperty(
        property: KProperty1<E, *>, value: Any?, returnProperty: KProperty1<E, R>, vararg orders: Order
    ): List<R> { record("oneSearchProperty", property, value, returnProperty); return listResult as List<R> }

    override fun oneSearchProperties(
        property: KProperty1<E, *>, value: Any?, returnProperties: Collection<KProperty1<E, *>>, vararg orders: Order
    ): List<Map<String, *>> { record("oneSearchProperties", property, value); return mapListResult }

    @Suppress("UNCHECKED_CAST")
    override fun allSearch(vararg orders: Order): List<E> { record("allSearch", orders.toList()); return listResult as List<E> }

    @Suppress("UNCHECKED_CAST")
    override fun <R> allSearchProperty(returnProperty: KProperty1<E, R>, vararg orders: Order): List<R> {
        record("allSearchProperty", returnProperty); return listResult as List<R>
    }

    override fun allSearchProperties(
        returnProperties: Collection<KProperty1<E, *>>, vararg orders: Order
    ): List<Map<String, *>> { record("allSearchProperties", returnProperties); return mapListResult }

    @Suppress("UNCHECKED_CAST")
    override fun andSearch(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E> {
        record("andSearch", properties); return listResult as List<E>
    }

    @Suppress("UNCHECKED_CAST")
    override fun orSearch(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E> {
        record("orSearch", properties); return listResult as List<E>
    }

    @Suppress("UNCHECKED_CAST")
    override fun inSearch(property: KProperty1<E, *>, values: Collection<*>, vararg orders: Order): List<E> {
        record("inSearch", property, values); return listResult as List<E>
    }

    @Suppress("UNCHECKED_CAST")
    override fun inSearchById(values: Collection<PK>, vararg orders: Order): List<E> {
        record("inSearchById", values); return listResult as List<E>
    }

    override fun inSearchPropertyById(values: Collection<PK>, returnProperty: String, vararg orders: Order): List<*> {
        record("inSearchPropertyByIdStr", values, returnProperty); return listResult
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R> inSearchPropertyById(values: Collection<PK>, returnProperty: KProperty1<E, R>, vararg orders: Order): List<R> {
        record("inSearchPropertyByIdTyped", values, returnProperty); return listResult as List<R>
    }

    override fun inSearchPropertiesById(
        values: Collection<PK>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>> {
        record("inSearchPropertiesById", values, returnProperties)
        return inSearchPropertiesByIdHandler(values)
    }

    @Suppress("UNCHECKED_CAST")
    override fun search(criteria: Criteria?, vararg orders: Order): List<E> {
        record("searchCriteria", criteria); return listResult as List<E>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> search(criteria: Criteria?, returnItemClass: KClass<T>?, vararg orders: Order): List<T> {
        record("searchCriteriaTyped", criteria, returnItemClass); return listResult as List<T>
    }

    @Suppress("UNCHECKED_CAST")
    override fun pagingSearch(criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order): List<E> {
        record("pagingSearchCriteria", criteria, pageNo, pageSize); return listResult as List<E>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> pagingSearch(
        criteria: Criteria?, returnItemClass: KClass<T>?, pageNo: Int, pageSize: Int, vararg orders: Order
    ): List<T> { record("pagingSearchCriteriaTyped", criteria, pageNo, pageSize); return listResult as List<T> }

    @Suppress("UNCHECKED_CAST")
    override fun <R> searchProperty(criteria: Criteria, returnProperty: KProperty1<E, R>, vararg orders: Order): List<R> {
        record("searchProperty", criteria, returnProperty)
        return searchPropertyHandler(criteria) as List<R>
    }

    override fun searchProperties(
        criteria: Criteria, returnProperties: Collection<KProperty1<E, *>>, vararg orders: Order
    ): List<Map<String, Any?>> {
        record("searchProperties", criteria, returnProperties)
        return searchPropertiesHandler(criteria)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R> pagingReturnProperty(
        criteria: Criteria, returnProperty: KProperty1<E, R>, pageNo: Int, pageSize: Int, vararg orders: Order
    ): List<R> { record("pagingReturnProperty", criteria, returnProperty, pageNo, pageSize); return listResult as List<R> }

    override fun pagingReturnProperties(
        criteria: Criteria, returnProperties: Collection<KProperty1<E, *>>, pageNo: Int, pageSize: Int, vararg orders: Order
    ): List<Map<String, *>> { record("pagingReturnProperties", criteria, pageNo, pageSize); return mapListResult }

    override fun search(listSearchPayload: ListSearchPayload?): List<*> {
        record("searchPayload", listSearchPayload)
        return searchPayloadHandler(listSearchPayload)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> search(listSearchPayload: ListSearchPayload?, returnItemClass: KClass<T>): List<T> {
        record("searchPayloadTyped", listSearchPayload, returnItemClass); return listResult as List<T>
    }

    override fun searchMaps(listSearchPayload: ListSearchPayload?): List<Map<String, Any?>> {
        record("searchMaps", listSearchPayload); return mapListResult.map { it.mapValues { (_, v) -> v } }
    }

    override fun searchValues(listSearchPayload: ListSearchPayload): List<Any?> {
        record("searchValues", listSearchPayload); return mapListResult.flatMap { it.values }
    }

    @Suppress("UNCHECKED_CAST")
    override fun pagingSearchWithTotal(
        criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order
    ): PagingSearchResult<E> {
        record("pagingSearchWithTotalCriteria", criteria, pageNo, pageSize)
        return PagingSearchResult(listResult as List<E>, countResult)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> pagingSearchWithTotal(
        listSearchPayload: ListSearchPayload, returnItemClass: KClass<T>
    ): PagingSearchResult<T> {
        record("pagingSearchWithTotalPayload", listSearchPayload, returnItemClass)
        return PagingSearchResult(listResult as List<T>, countResult)
    }

    override fun count(criteria: Criteria?): Int { record("countCriteria", criteria); return countResult }
    override fun count(searchPayload: ISearchPayload?): Int { record("countPayload", searchPayload); return countResult }
    override fun sum(property: KProperty1<E, *>, criteria: Criteria?): Number { record("sum", property, criteria); return numberResult }
    override fun avg(property: KProperty1<E, *>, criteria: Criteria?): Number { record("avg", property, criteria); return numberResult }

    @Suppress("UNCHECKED_CAST")
    override fun <R : Comparable<R>> max(property: KProperty1<E, R?>, criteria: Criteria?): R? {
        record("max", property, criteria); return comparableResult as R?
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R : Comparable<R>> min(property: KProperty1<E, R?>, criteria: Criteria?): R? {
        record("min", property, criteria); return comparableResult as R?
    }

    // ---------- write ----------
    override fun insert(any: Any): PK { record("insert", any); return insertResult!! }
    override fun insertOnly(entity: E, vararg propertyNames: String): PK { record("insertOnly", entity, propertyNames.toList()); return insertResult!! }
    override fun insertExclude(entity: E, vararg excludePropertyNames: String): PK { record("insertExclude", entity); return insertResult!! }
    override fun batchInsert(objects: Collection<Any>, countOfEachBatch: Int): Int { record("batchInsert", objects, countOfEachBatch); return intResult }
    override fun batchInsertOnly(entities: Collection<E>, countOfEachBatch: Int, vararg propertyNames: String): Int { record("batchInsertOnly", entities, countOfEachBatch); return intResult }
    override fun batchInsertExclude(entities: Collection<E>, countOfEachBatch: Int, vararg excludePropertyNames: String): Int { record("batchInsertExclude", entities, countOfEachBatch); return intResult }

    override fun update(any: Any): Boolean { record("update", any); return updateResult }
    override fun updateWhen(entity: E, criteria: Criteria): Boolean { record("updateWhen", entity, criteria); return updateResult }
    override fun updateProperties(id: PK, properties: Map<String, *>): Boolean { record("updateProperties", id, properties); return updateResult }
    override fun updatePropertiesWhen(id: PK, properties: Map<String, *>, criteria: Criteria): Boolean { record("updatePropertiesWhen", id, properties, criteria); return updateResult }
    override fun updateOnly(entity: E, vararg propertyNames: String): Boolean { record("updateOnly", entity, propertyNames.toList()); return updateResult }
    override fun updateOnlyWhen(entity: E, criteria: Criteria, vararg propertyNames: String): Boolean { record("updateOnlyWhen", entity, criteria); return updateResult }
    override fun updateExcludeProperties(entity: E, vararg excludePropertyNames: String): Boolean { record("updateExcludeProperties", entity); return updateResult }
    override fun updateExcludePropertiesWhen(entity: E, criteria: Criteria, vararg excludePropertyNames: String): Boolean { record("updateExcludePropertiesWhen", entity, criteria); return updateResult }
    override fun batchUpdate(entities: Collection<E>, countOfEachBatch: Int): Int { record("batchUpdate", entities, countOfEachBatch); return intResult }
    override fun batchUpdateWhen(entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int): Int { record("batchUpdateWhen", entities, criteria, countOfEachBatch); return intResult }
    override fun <S : ISearchPayload> batchUpdateWhen(updatePayload: UpdatePayload<S>): Int { record("batchUpdateWhenPayload", updatePayload); return intResult }
    override fun batchUpdateProperties(criteria: Criteria, properties: Map<String, *>): Int { record("batchUpdateProperties", criteria, properties); return intResult }
    override fun batchUpdateOnly(entities: Collection<E>, countOfEachBatch: Int, vararg propertyNames: String): Int { record("batchUpdateOnly", entities, countOfEachBatch); return intResult }
    override fun batchUpdateOnlyWhen(entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int, vararg propertyNames: String): Int { record("batchUpdateOnlyWhen", entities, criteria, countOfEachBatch); return intResult }
    override fun batchUpdateExcludeProperties(entities: Collection<E>, countOfEachBatch: Int, vararg excludePropertyNames: String): Int { record("batchUpdateExcludeProperties", entities, countOfEachBatch); return intResult }
    override fun batchUpdateExcludePropertiesWhen(entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int, vararg excludePropertyNames: String): Int { record("batchUpdateExcludePropertiesWhen", entities, criteria, countOfEachBatch); return intResult }

    // ---------- delete ----------
    override fun deleteById(id: PK): Boolean { record("deleteById", id); return deleteResult }
    override fun delete(entity: E): Boolean { record("delete", entity); return deleteResult }
    override fun batchDelete(ids: Collection<PK>): Int { record("batchDelete", ids); return intResult }
    override fun batchDeleteCriteria(criteria: Criteria): Int { record("batchDeleteCriteria", criteria); return batchDeleteCriteriaHandler(criteria) }
    override fun batchDeleteWhen(searchPayload: ISearchPayload): Int { record("batchDeleteWhen", searchPayload); return intResult }
}
