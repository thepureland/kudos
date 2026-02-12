package io.kudos.base.support.dao

import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.support.IIdEntity
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.base.support.payload.SearchPayload
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * 只读数据访问接口。
 *
 * 该接口定义通用只读查询能力，覆盖主键查询、属性查询、复杂条件查询、分页查询、
 * 载体查询与聚合查询，并支持返回实体、单属性列表或多属性映射结果。
 *
 * @param PK 主键类型
 * @param E 实体类型，需实现 [IIdEntity]
 * @author K
 * @since 1.0.0
 */
interface IBaseReadOnlyDao<PK : Any, E : IIdEntity<PK>> {

    /**
     * 按主键查询实体。
     *
     * @param id 主键值
     * @return 命中实体；未命中返回 null
     */
    fun get(id: PK): E?

    /**
     * 按主键查询并指定返回对象类型。
     *
     * 常用于只映射部分字段到 DTO/VO，避免业务侧再做二次拷贝。
     *
     * @param id 主键值
     * @param returnType 返回对象类型；为 null 时实现可按实体类型处理
     * @return 命中对象；未命中返回 null
     */
    fun <R : Any> get(id: PK, returnType: KClass<R>? = null): R?

    /**
     * 按主键集合批量查询实体。
     *
     * @param ids 主键集合
     * @param countOfEachBatch 分批查询大小，防止超大 IN 导致 SQL 或内存压力
     * @return 实体列表；入参为空时返回空列表
     */
    fun getByIds(ids: Collection<PK>, countOfEachBatch: Int = 1000): List<E>

    /**
     * 按主键集合批量查询并指定返回元素类型。
     *
     * @param ids 主键集合
     * @param returnItemClass 返回元素类型；为 null 时实现可按实体类型处理
     * @param countOfEachBatch 分批查询大小
     * @return 指定类型的结果列表
     */
    fun <T: Any> getByIds(
        ids: Collection<PK>,
        returnItemClass: KClass<T>? = null,
        countOfEachBatch: Int = 1000
    ): List<T>

    /**
     * 单属性等值查询，返回实体列表。
     *
     * @param property 查询属性
     * @param value 查询值
     * @param orders 排序规则
     * @return 满足条件的实体列表
     */
    fun oneSearch(property: KProperty1<E, *>, value: Any?, vararg orders: Order): List<E>

    /**
     * 单属性等值查询，仅返回指定属性列表。
     *
     * @param property 查询属性
     * @param value 查询值
     * @param returnProperty 返回属性
     * @param orders 排序规则
     * @return 指定属性值列表
     */
    fun <R> oneSearchProperty(
        property: KProperty1<E, *>,
        value: Any?,
        returnProperty: KProperty1<E, R>,
        vararg orders: Order
    ): List<R>

    /**
     * 单属性等值查询，仅返回指定多属性映射。
     *
     * @param property 查询属性
     * @param value 查询值
     * @param returnProperties 返回属性集合
     * @param orders 排序规则
     * @return 多属性映射列表（每条记录一个 Map）
     */
    fun oneSearchProperties(
        property: KProperty1<E, *>,
        value: Any?,
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, *>>

    /**
     * 查询全部数据。
     *
     * @param orders 排序规则
     * @return 全量实体列表
     */
    fun allSearch(vararg orders: Order): List<E>

    /**
     * 查询全部数据，仅返回单个属性。
     *
     * @param returnProperty 返回属性
     * @param orders 排序规则
     * @return 指定属性值列表
     */
    fun <R> allSearchProperty(returnProperty: KProperty1<E, R>, vararg orders: Order): List<R>

    /**
     * 查询全部数据，仅返回多属性映射。
     *
     * @param returnProperties 返回属性集合
     * @param orders 排序规则
     * @return 多属性映射列表（每条记录一个 Map）
     */
    fun allSearchProperties(
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, *>>

    /**
     * 多属性 AND 查询，返回实体列表。
     *
     * @param properties 属性到值的映射
     * @param orders 排序规则
     * @return 满足 AND 条件的实体列表
     */
    fun andSearch(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E>

    /**
     * 多属性 OR 查询，返回实体列表。
     *
     * @param properties 属性到值的映射
     * @param orders 排序规则
     * @return 满足 OR 条件的实体列表
     */
    fun orSearch(properties: Map<KProperty1<E, *>, *>, vararg orders: Order): List<E>

    /**
     * IN 查询，返回实体列表。
     *
     * @param property 参与 IN 的属性
     * @param values IN 值集合
     * @param orders 排序规则
     * @return 满足 IN 条件的实体列表
     */
    fun inSearch(property: KProperty1<E, *>, values: Collection<*>, vararg orders: Order): List<E>

    /**
     * 主键 IN 查询，返回实体列表。
     *
     * @param values 主键集合
     * @param orders 排序规则
     * @return 主键命中的实体列表
     */
    fun inSearchById(values: Collection<PK>, vararg orders: Order): List<E>

    /**
     * 主键 IN 查询，仅返回单个属性（字符串属性名版本）。
     *
     * 该重载通常用于与旧调用方兼容。
     *
     * @param values 主键集合
     * @param returnProperty 返回属性名
     * @param orders 排序规则
     * @return 指定属性值列表
     */
    fun inSearchPropertyById(values: Collection<PK>, returnProperty: String, vararg orders: Order): List<*>

    /**
     * 主键 IN 查询，仅返回单个属性（类型安全版本）。
     *
     * @param values 主键集合
     * @param returnProperty 返回属性
     * @param orders 排序规则
     * @return 指定属性值列表
     */
    fun <R> inSearchPropertyById(values: Collection<PK>, returnProperty: KProperty1<E, R>, vararg orders: Order): List<R>

    /**
     * 主键 IN 查询，仅返回多属性映射。
     *
     * @param values 主键集合
     * @param returnProperties 返回属性名集合
     * @param orders 排序规则
     * @return 多属性映射列表（每条记录一个 Map）
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
     * 按 [Criteria] 查询，仅返回单个属性列表。
     *
     * @param criteria 查询条件
     * @param returnProperty 返回属性
     * @param orders 排序规则
     * @return 指定属性值列表
     */
    fun <R> searchProperty(criteria: Criteria, returnProperty: KProperty1<E, R>, vararg orders: Order): List<R>

    /**
     * 按 [Criteria] 查询，仅返回多属性映射。
     *
     * @param criteria 查询条件
     * @param returnProperties 返回属性集合
     * @param orders 排序规则
     * @return 多属性映射列表（每条记录一个 Map）
     */
    fun searchProperties(
        criteria: Criteria,
        returnProperties: Collection<KProperty1<E, *>>,
        vararg orders: Order
    ): List<Map<String, Any?>>

    /**
     * 按 [Criteria] 分页查询，仅返回单个属性列表。
     *
     * @param criteria 查询条件
     * @param returnProperty 返回属性
     * @param pageNo 页码（从 1 开始）
     * @param pageSize 每页条数
     * @param orders 排序规则
     * @return 当前页的指定属性值列表
     */
    fun <R> pagingReturnProperty(
        criteria: Criteria,
        returnProperty: KProperty1<E, R>,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<R>

    /**
     * 按 [Criteria] 分页查询，仅返回多属性映射。
     *
     * @param criteria 查询条件
     * @param returnProperties 返回属性集合
     * @param pageNo 页码（从 1 开始）
     * @param pageSize 每页条数
     * @param orders 排序规则
     * @return 当前页的多属性映射列表（每条记录一个 Map）
     */
    fun pagingReturnProperties(
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
     *
     * @return 查询结果列表（元素类型由载体配置决定）
     */
    fun search(listSearchPayload: ListSearchPayload? = null): List<*>

    /**
     * 根据列表查询载体查询，并指定返回元素类型。
     *
     * 仅当 `listSearchPayload.returnProperties` 为空时可安全使用该方法。
     *
     * @return 指定类型的结果列表
     */
    fun <T : Any> search(listSearchPayload: ListSearchPayload? = null, returnItemClass: KClass<T>): List<T>

    fun count(criteria: Criteria? = null): Int

    /**
     * 按查询载体计算记录数。
     *
     * @param searchPayload 查询载体
     * @return 记录数
     */
    fun count(searchPayload: SearchPayload?): Int

    /**
     * 求和聚合。
     *
     * @param property 求和属性
     * @param criteria 过滤条件，为 null 表示全量
     * @return 求和结果
     */
    fun sum(property: KProperty1<E, *>, criteria: Criteria? = null): Number

    /**
     * 平均值聚合。
     *
     * @param property 平均值属性
     * @param criteria 过滤条件，为 null 表示全量
     * @return 平均值结果
     */
    fun avg(property: KProperty1<E, *>, criteria: Criteria? = null): Number

    /**
     * 最大值聚合。
     *
     * @param property 目标属性
     * @param criteria 过滤条件，为 null 表示全量
     * @return 最大值；无数据时返回 null
     */
    fun <R : Comparable<R>> max(property: KProperty1<E, R?>, criteria: Criteria? = null): R?

    /**
     * 最小值聚合。
     *
     * @param property 目标属性
     * @param criteria 过滤条件，为 null 表示全量
     * @return 最小值；无数据时返回 null
     */
    fun <R : Comparable<R>> min(property: KProperty1<E, R?>, criteria: Criteria? = null): R?
}