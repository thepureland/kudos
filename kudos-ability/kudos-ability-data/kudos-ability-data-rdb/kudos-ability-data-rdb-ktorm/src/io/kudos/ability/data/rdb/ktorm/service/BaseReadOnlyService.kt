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

    override fun oneSearch(property: String, value: Any?, vararg orders: Order): List<E> =
        dao.oneSearch(property, value, *orders)

    override fun oneSearchProperty(
        property: String, value: Any?, returnProperty: String, vararg orders: Order
    ): List<*> = dao.oneSearchProperty(property, value, returnProperty, *orders)

    override fun oneSearchProperties(
        property: String, value: Any?, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>> = dao.oneSearchProperties(property, value, returnProperties, *orders)

    override fun allSearch(vararg orders: Order): List<E> = dao.allSearch(*orders)

    override fun allSearchProperty(returnProperty: String, vararg orders: Order): List<*> =
        dao.allSearchProperty(returnProperty, *orders)

    override fun allSearchProperties(returnProperties: Collection<String>, vararg orders: Order): List<Map<String, *>> =
        dao.allSearchProperties(returnProperties, *orders)

    override fun andSearch(properties: Map<String, *>, vararg orders: Order): List<E> =
        dao.andSearch(properties, *orders)

    override fun andSearchProperty(properties: Map<String, *>, returnProperty: String, vararg orders: Order): List<*> =
        dao.andSearchProperty(properties, returnProperty, *orders)

    override fun andSearchProperties(
        properties: Map<String, *>, returnProperties: Collection<String>, vararg orders: Order,
    ): List<Map<String, *>> = dao.andSearchProperties(properties, returnProperties, *orders)

    override fun orSearch(properties: Map<String, *>, vararg orders: Order): List<E> = dao.orSearch(properties, *orders)

    override fun orSearchProperty(
        properties: Map<String, *>, returnProperty: String, vararg orders: Order
    ): List<*> = dao.orSearchProperty(properties, returnProperty, *orders)

    override fun orSearchProperties(
        properties: Map<String, *>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>> = dao.orSearchProperties(properties, returnProperties, *orders)

    override fun inSearch(property: String, values: Collection<*>, vararg orders: Order): List<E> =
        dao.inSearch(property, values, *orders)

    override fun inSearchProperty(
        property: String, values: Collection<*>, returnProperty: String, vararg orders: Order
    ): List<*> = dao.inSearchProperty(property, values, returnProperty, *orders)

    override fun inSearchProperties(
        property: String, values: Collection<*>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>> = dao.inSearchProperties(property, values, returnProperties, *orders)

    override fun inSearchById(values: Collection<PK>, vararg orders: Order): List<E> =
        dao.inSearchById(values, *orders)

    override fun inSearchPropertyById(values: Collection<PK>, returnProperty: String, vararg orders: Order): List<*> =
        dao.inSearchPropertyById(values, returnProperty, *orders)

    override fun inSearchPropertiesById(
        values: Collection<PK>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>> = dao.inSearchPropertiesById(values, returnProperties, *orders)

    override fun search(criteria: Criteria, vararg orders: Order): List<E> =
        dao.search(criteria, *orders)

    override fun <T : Any> search(
        criteria: Criteria?,
        returnItemClass: KClass<T>?,
        vararg orders: Order
    ): List<T> = dao.search(criteria, returnItemClass, *orders)

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
    ): List<T> = search(criteria, T::class, *orders)

    override fun searchProperty(criteria: Criteria, returnProperty: String, vararg orders: Order): List<*> =
        dao.searchProperty(criteria, returnProperty, *orders)

    override fun searchProperties(
        criteria: Criteria, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, Any?>> = dao.searchProperties(criteria, returnProperties, *orders)

    override fun pagingSearch(criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order): List<E> =
        dao.pagingSearch(criteria, pageNo, pageSize, *orders)

    override fun <T : Any> pagingSearch(
        criteria: Criteria?,
        returnItemClass: KClass<T>?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<T> = dao.pagingSearch(criteria, returnItemClass, pageNo, pageSize, *orders)

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
    ): List<T> = pagingSearch(criteria, T::class, pageNo, pageSize, *orders)

    override fun pagingReturnProperty(
        criteria: Criteria, returnProperty: String, pageNo: Int, pageSize: Int, vararg orders: Order
    ): List<*> = dao.pagingReturnProperty(criteria, returnProperty, pageNo, pageSize, *orders)

    override fun pagingReturnProperties(
        criteria: Criteria, returnProperties: Collection<String>, pageNo: Int, pageSize: Int, vararg orders: Order
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

    override fun count(criteria: Criteria?): Int = dao.count(criteria)

    override fun count(searchPayload: SearchPayload): Int = dao.count(searchPayload)

    override fun sum(property: String, criteria: Criteria?): Number = dao.sum(property, criteria)

    override fun avg(property: String, criteria: Criteria?): Number = dao.avg(property, criteria)

    override fun max(property: String, criteria: Criteria?): Any = dao.avg(property, criteria)

    override fun min(property: String, criteria: Criteria?): Any = dao.avg(property, criteria)

}