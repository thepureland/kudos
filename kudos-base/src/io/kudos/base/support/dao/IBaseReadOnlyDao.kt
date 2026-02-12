package io.kudos.base.support.dao

import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.support.IIdEntity
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.base.support.payload.SearchPayload
import io.kudos.base.support.query.ReadQuery
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface IBaseReadOnlyDao<PK : Any, E : IIdEntity<PK>> {

    fun get(id: PK): E?

    fun <R : Any> get(id: PK, returnType: KClass<R>? = null): R?

    fun getByIds(ids: Collection<PK>, countOfEachBatch: Int = 1000): List<E>

    fun <T: Any> getByIds(
        ids: Collection<PK>,
        returnItemClass: KClass<T>? = null,
        countOfEachBatch: Int = 1000
    ): List<T>

    fun oneSearch(property: KProperty1<E, *>, value: Any?, vararg orders: Order): List<E>

    fun <R> oneSearchProperty(
        property: KProperty1<E, *>,
        value: Any?,
        returnProperty: KProperty1<E, R>,
        vararg orders: Order
    ): List<R>

    fun oneSearchProperties(
        property: KProperty1<E, *>,
        value: Any?,
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, *>>

    fun allSearch(vararg orders: Order): List<E>

    fun <R> allSearchProperty(returnProperty: KProperty1<E, R>, vararg orders: Order): List<R>

    fun allSearchPropertiesBy(
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, *>>

    fun andSearchBy(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E>

    fun orSearchBy(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E>

    fun inSearch(property: KProperty1<E, *>, values: Collection<*>, vararg orders: Order): List<E>

    fun inSearchById(values: Collection<PK>, vararg orders: Order): List<E>

    fun inSearchPropertyById(values: Collection<PK>, returnProperty: String, vararg orders: Order): List<*>

    fun <R> inSearchPropertyById(values: Collection<PK>, returnProperty: KProperty1<E, R>, vararg orders: Order): List<R>

    fun inSearchPropertiesById(
        values: Collection<PK>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>>

    fun search(query: ReadQuery): List<E>

    fun <T: Any> search(query: ReadQuery, returnItemClass: KClass<T>? = null): List<T>

    fun <R> searchProperty(criteria: Criteria, returnProperty: KProperty1<E, R>, vararg orders: Order): List<R>

    fun searchPropertiesBy(
        criteria: Criteria,
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, Any?>>

    fun <R> pagingReturnProperty(
        criteria: Criteria,
        returnProperty: KProperty1<E, R>,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<R>

    fun pagingReturnPropertiesBy(
        criteria: Criteria,
        returnProperties: Collection<KProperty1<E, *>>,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<Map<String, *>>

    /**
     * 根据列表查询载体查询结果列表。
     *
     * 返回类型由 `listSearchPayload.returnProperties` 与 `listSearchPayload.returnEntityClass` 决定，
     * 可能为实体列表、单属性值列表或属性映射列表。
     */
    fun search(listSearchPayload: ListSearchPayload? = null): List<*>

    /**
     * 根据列表查询载体查询，并指定返回元素类型。
     *
     * 仅当 `listSearchPayload.returnProperties` 为空时可安全使用该方法。
     */
    fun <T : Any> search(listSearchPayload: ListSearchPayload? = null, returnItemClass: KClass<T>): List<T>

    fun count(query: ReadQuery): Int

    fun count(searchPayload: SearchPayload? = null): Int

    fun sum(property: KProperty1<E, *>, criteria: Criteria? = null): Number
    fun avg(property: KProperty1<E, *>, criteria: Criteria? = null): Number
    fun <R : Comparable<R>> max(property: KProperty1<E, R?>, criteria: Criteria? = null): R?
    fun <R : Comparable<R>> min(property: KProperty1<E, R?>, criteria: Criteria? = null): R?
}