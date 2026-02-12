package io.kudos.base.support.iservice

import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.support.IIdEntity
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.base.support.payload.SearchPayload
import io.kudos.base.support.query.ReadQuery
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface IBaseReadOnlyService<PK : Any, E : IIdEntity<PK>> {

    //region by id

    /**
     * 查询指定主键值的实体
     *
     * @param id 主键值，类型必须为以下之一：String、Int、Long
     * @return 实体，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun get(id: PK): E?

    /**
     * 查询指定主键值的实体，可以指定返回的对象类型
     *
     * @param id 主键值，类型必须为以下之一：String、Int、Long
     * @param returnType 返回对象的类型, 为null表示PO类型，缺省为null
     * @return 指定类型的结果对象，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun <R : Any> get(id: PK, returnType: KClass<R>): R?

    /**
     * 批量查询指定主键值的实体
     *
     * @param ids 主键集合，元素类型必须为以下之一：String、Int、Long，为空时返回空列表
     * @param countOfEachBatch 每批大小，缺省为1000
     * @return 实体列表，ids为空时返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getByIds(ids: Collection<PK>, countOfEachBatch: Int = 1000): List<E>

    /**
     * 批量查询指定主键值的实体
     *
     * @param T 结果列表的元素类型
     * @param ids 主键集合，元素类型必须为以下之一：String、Int、Long，为空时返回空列表
     * @param returnItemClass 结果列表的元素类型，为null表示PO类型，缺省为null
     * @param countOfEachBatch 每批大小，缺省为1000
     * @return 指定返回元素类型的对象列表，ids为空时返回空列表
     * @author K
     * @since 1.0.0
     */
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

    fun pagingSearch(listSearchPayload: ListSearchPayload): Pair<List<*>, Int>

    /**
     * 根据列表查询载体查询结果列表。
     *
     * 返回类型由 `listSearchPayload.returnProperties` 与 `listSearchPayload.returnEntityClass` 决定，
     * 可能为实体列表、单属性值列表或属性映射列表。
     */
    fun search(listSearchPayload: ListSearchPayload): List<*>

    /**
     * 根据列表查询载体查询，并指定返回元素类型。
     *
     * 仅当 `listSearchPayload.returnProperties` 为空时可安全使用该方法。
     */
    fun <T : Any> search(listSearchPayload: ListSearchPayload, returnItemClass: KClass<T>): List<T>

    fun count(query: ReadQuery): Int
    fun count(searchPayload: SearchPayload): Int

    fun sum(property: KProperty1<E, *>, criteria: Criteria? = null): Number
    fun avg(property: KProperty1<E, *>, criteria: Criteria? = null): Number
    fun <R : Comparable<R>> max(property: KProperty1<E, R?>, criteria: Criteria? = null): R?
    fun <R : Comparable<R>> min(property: KProperty1<E, R?>, criteria: Criteria? = null): R?
}