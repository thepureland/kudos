package io.kudos.ability.data.rdb.ktorm.service

import io.kudos.ability.data.rdb.ktorm.support.BaseReadOnlyDao
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.support.iservice.IBaseReadOnlyService
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.base.support.payload.SearchPayload
import io.kudos.base.support.query.ReadQuery
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

    /**
     * 查询指定主键值的实体，可以通过泛型指定返回的对象类型。
     *
     * get(id: PK, returnType: KClass<R>)的快捷方法。
     *
     * @param R 返回对象的类型
     * @param id 主键值，类型必须为以下之一：String、Int、Long
     * @return 指定类型的结果对象，找不到返回null
     * @author K
     * @since 1.0.0
     */
    inline fun <reified R : Any> getAs(id: PK): R? = get(id, R::class)


    override fun getByIds(ids: Collection<PK>, countOfEachBatch: Int): List<E> =
        dao.getByIds(ids, countOfEachBatch)

    override fun <T : Any> getByIds(
        ids: Collection<PK>,
        returnItemClass: KClass<T>?,
        countOfEachBatch: Int
    ): List<T> = dao.getByIds(ids, returnItemClass, countOfEachBatch)

    /**
     * 批量查询指定主键值的实体
     *
     * getByIds(vararg ids: PK, returnItemClass: KClass<T>?, countOfEachBatch: Int)的快捷方法
     *
     * @param T 结果列表的元素类型
     * @param ids 主键集合，元素类型必须为以下之一：String、Int、Long，为空时返回空列表
     * @param countOfEachBatch 每批大小，缺省为1000
     * @return 指定返回元素类型的对象列表，ids为空时返回空列表
     * @author K
     * @since 1.0.0
     */
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

    override fun allSearchPropertiesBy(
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, *>> = dao.allSearchPropertiesBy(returnProperties, *orders)

    override fun andSearchBy(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E> =
        dao.andSearchBy(properties, *orders)

    override fun orSearchBy(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E> =
        dao.orSearchBy(properties, *orders)

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

    override fun search(query: ReadQuery): List<E> = dao.search(query)

    override fun <T : Any> search(query: ReadQuery, returnItemClass: KClass<T>?): List<T> =
        dao.search(query, returnItemClass)

    /**
     * 复杂条件查询，可以指定返回的封装类。会忽略与表实体不匹配的属性。
     *
     * 该方法的目的主要是为了避免各应用场景下，需要将PO转为所需VO的麻烦与性能开销。
     *
     * 为search(criteria: Criteria?, returnItemClass: KClass<T>?, vararg orders: Order)的快捷方法。
     *
     * @param T 返回列表项的类型
     * @param criteria 查询条件，为null表示无条件查询，缺省为null
     * @param orders   排序规则
     * @return 指定的返回类型的对象列表
     * @author K
     * @since 1.0.0
     */
    inline fun <reified T : Any> searchAs(
        criteria: Criteria?,
        vararg orders: Order
    ): List<T> = search(ReadQuery(criteria = criteria, orders = orders.toList()), T::class)

    override fun <R> searchProperty(
        criteria: Criteria,
        returnProperty: KProperty1<E, R>,
        vararg orders: Order
    ): List<R> =
        dao.searchProperty(criteria, returnProperty, *orders)

    override fun searchPropertiesBy(
        criteria: Criteria,
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, Any?>> = dao.searchPropertiesBy(criteria, returnProperties, *orders)

    /**
     * 分页查询，可以指定返回的封装类。会忽略与表实体不匹配的属性。
     *
     * 该方法的目的主要是为了避免各应用场景下，需要将PO转为所需VO的麻烦与性能开销。
     *
     * 为pagingSearch(criteria: Criteria?,returnItemClass: KClass<T>?,pageNo: Int,pageSize: Int,vararg orders: Order)
     * 的快捷方法。
     *
     * @param T 返回列表项的类型
     * @param criteria 查询条件，为null表示无条件查询，缺省为null
     * @param pageNo   当前页码(从1开始)
     * @param pageSize 每页条数
     * @param orders   排序规则
     * @return 指定的返回类型对象列表
     * @author K
     * @since 1.0.0
     */
    inline fun <reified T : Any> pagingSearchAs(
        criteria: Criteria?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<T> = search(
        ReadQuery(criteria = criteria, orders = orders.toList(), pageNo = pageNo, pageSize = pageSize),
        T::class
    )

    override fun <R> pagingReturnProperty(
        criteria: Criteria,
        returnProperty: KProperty1<E, R>,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<R> = dao.pagingReturnProperty(criteria, returnProperty, pageNo, pageSize, *orders)

    override fun pagingReturnPropertiesBy(
        criteria: Criteria,
        returnProperties: Collection<KProperty1<E, *>>,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<Map<String, *>> = dao.pagingReturnPropertiesBy(criteria, returnProperties, pageNo, pageSize, *orders)

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

    override fun count(query: ReadQuery): Int = dao.count(query)

    override fun count(searchPayload: SearchPayload): Int = dao.count(searchPayload)

    override fun sum(property: KProperty1<E, *>, criteria: Criteria?): Number = dao.sum(property, criteria)
    override fun avg(property: KProperty1<E, *>, criteria: Criteria?): Number = dao.avg(property, criteria)
    override fun <R : Comparable<R>> max(property: KProperty1<E, R?>, criteria: Criteria?): R? =
        dao.max(property, criteria)

    override fun <R : Comparable<R>> min(property: KProperty1<E, R?>, criteria: Criteria?): R? =
        dao.min(property, criteria)

}