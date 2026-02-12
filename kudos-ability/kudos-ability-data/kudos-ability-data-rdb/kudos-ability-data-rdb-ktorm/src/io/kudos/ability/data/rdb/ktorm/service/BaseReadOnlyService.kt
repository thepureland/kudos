package io.kudos.ability.data.rdb.ktorm.service

import io.kudos.ability.data.rdb.ktorm.support.BaseReadOnlyDao
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.support.iservice.IBaseReadOnlyService
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.base.support.payload.SearchPayload
import org.springframework.beans.factory.annotation.Autowired
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * 基于关系型数据库表的基础的只读业务操作
 *
 * @param PK 实体主键类型
 * @param E 实体类型
 * @author K
 * @since 1.0.0
 */
open class BaseReadOnlyService<PK : Any, E : IDbEntity<PK, E>, DAO : BaseReadOnlyDao<PK, E, *>> : IBaseReadOnlyService<PK, E> {

    @Autowired //!!! 不能改为 @Resource
    protected lateinit var dao: DAO

    override fun get(id: PK): E? = dao.get(id)

    override fun <R : Any> get(id: PK, returnType: KClass<R>): R? = dao.get(id, returnType)

    inline fun <reified R : Any> getAs(id: PK): R? = get(id, R::class)

    override fun getByIds(ids: Collection<PK>, countOfEachBatch: Int): List<E> =
        dao.getByIds(ids, countOfEachBatch)

    override fun <T : Any> getByIds(
        ids: Collection<PK>,
        returnItemClass: KClass<T>?,
        countOfEachBatch: Int
    ): List<T> = dao.getByIds(ids, returnItemClass, countOfEachBatch)

    inline fun <reified T : Any> getByIdsAs(ids: Collection<PK>, countOfEachBatch: Int = 1000): List<T> =
        getByIds(ids, T::class, countOfEachBatch)

    override fun oneSearch(property: KProperty1<E, *>, value: Any?, vararg orders: Order): List<E> =
        dao.oneSearch(property, value, *orders)

    override fun <R> oneSearchProperty(
        property: KProperty1<E, *>,
        value: Any?,
        returnProperty: KProperty1<E, R>,
        vararg orders: Order
    ): List<R> = dao.oneSearchProperty(property, value, returnProperty, *orders)

    override fun oneSearchProperties(
        property: KProperty1<E, *>,
        value: Any?,
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, *>> = dao.oneSearchProperties(property, value, returnProperties, *orders)

    override fun <R> allSearchProperty(returnProperty: KProperty1<E, R>, vararg orders: Order): List<R> =
        dao.allSearchProperty(returnProperty, *orders)

    override fun allSearchProperties(
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, *>> = dao.allSearchProperties(returnProperties, *orders)

    override fun andSearch(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E> =
        dao.andSearch(properties, *orders)

    override fun orSearch(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E> =
        dao.orSearch(properties, *orders)

    override fun inSearch(property: KProperty1<E, *>, values: Collection<*>, vararg orders: Order): List<E> =
        dao.inSearch(property, values, *orders)

    override fun allSearch(vararg orders: Order): List<E> = dao.allSearch(*orders)

    override fun inSearchById(values: Collection<PK>, vararg orders: Order): List<E> =
        dao.inSearchById(values, *orders)

    override fun inSearchPropertyById(values: Collection<PK>, returnProperty: String, vararg orders: Order): List<*> =
        dao.inSearchPropertyById(values, returnProperty, *orders)

    override fun <R> inSearchPropertyById(
        values: Collection<PK>,
        returnProperty: KProperty1<E, R>,
        vararg orders: Order
    ): List<R> = dao.inSearchPropertyById(values, returnProperty, *orders)

    override fun inSearchPropertiesById(
        values: Collection<PK>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>> = dao.inSearchPropertiesById(values, returnProperties, *orders)

    override fun search(criteria: Criteria?, vararg orders: Order): List<E> = dao.search(criteria, *orders)

    override fun <T : Any> search(criteria: Criteria?, returnItemClass: KClass<T>?, vararg orders: Order): List<T> =
        dao.search(criteria, returnItemClass, *orders)

    inline fun <reified T : Any> searchAs(criteria: Criteria?, vararg orders: Order): List<T> =
        search(criteria, T::class, *orders)

    override fun pagingSearch(criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order): List<E> =
        dao.pagingSearch(criteria, pageNo, pageSize, *orders)

    override fun <T : Any> pagingSearch(
        criteria: Criteria?,
        returnItemClass: KClass<T>?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<T> = dao.pagingSearch(criteria, returnItemClass, pageNo, pageSize, *orders)

    inline fun <reified T : Any> pagingSearchAs(
        criteria: Criteria?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<T> = pagingSearch(criteria, T::class, pageNo, pageSize, *orders)


    override fun <R> searchProperty(
        criteria: Criteria,
        returnProperty: KProperty1<E, R>,
        vararg orders: Order
    ): List<R> =
        dao.searchProperty(criteria, returnProperty, *orders)

    override fun searchProperties(
        criteria: Criteria,
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, Any?>> = dao.searchProperties(criteria, returnProperties, *orders)

    override fun <R> pagingReturnProperty(
        criteria: Criteria,
        returnProperty: KProperty1<E, R>,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<R> = dao.pagingReturnProperty(criteria, returnProperty, pageNo, pageSize, *orders)

    override fun pagingReturnProperties(
        criteria: Criteria,
        returnProperties: Collection<KProperty1<E, *>>,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<Map<String, *>> = dao.pagingReturnProperties(criteria, returnProperties, pageNo, pageSize, *orders)

    override fun pagingSearch(listSearchPayload: ListSearchPayload): Pair<List<*>, Int> {
        val results = search(listSearchPayload)
        val count = if (listSearchPayload.pageNo != null) {
            count(listSearchPayload)
        } else {
            results.size
        }
        return Pair(results, count)
    }

    override fun search(listSearchPayload: ListSearchPayload): List<*> = dao.search(listSearchPayload)

    override fun <T : Any> search(listSearchPayload: ListSearchPayload, returnItemClass: KClass<T>): List<T> =
        dao.search(listSearchPayload, returnItemClass)

    override fun count(criteria: Criteria?): Int = dao.count(criteria)

    override fun count(searchPayload: SearchPayload): Int = dao.count(searchPayload)

    override fun sum(property: KProperty1<E, *>, criteria: Criteria?): Number = dao.sum(property, criteria)
    override fun avg(property: KProperty1<E, *>, criteria: Criteria?): Number = dao.avg(property, criteria)
    override fun <R : Comparable<R>> max(property: KProperty1<E, R?>, criteria: Criteria?): R? =
        dao.max(property, criteria)

    override fun <R : Comparable<R>> min(property: KProperty1<E, R?>, criteria: Criteria?): R? =
        dao.min(property, criteria)

}