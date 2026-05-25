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
 * 基础只读数据访问对象，封装某数据库表的通用查询操作
 *
 * @param PK 实体主键类型
 * @param E 实体类型
 * @param T 数据库表-实体关联对象的类型
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class BaseReadOnlyDao<PK : Any, E : IDbEntity<PK, E>, T : Table<E>> : IBaseReadOnlyDao<PK, E> {

    /** 数据库表-实体关联对象 */
    private var table: T? = null

    private var entityClass: KClass<E>? = null

    /**
     * 返回数据库表-实体关联对象
     *
     * @return 数据库表-实体关联对象
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    protected fun table(): T {
        if (table == null) {
            val tableClass = GenericKit.getSuperClassGenricClass(this::class, 2) as KClass<T>
            table = requireNotNull(tableClass.objectInstance) {
                "DAO泛型表类型[${tableClass.qualifiedName}]必须是 object 单例。"
            }
        }
        return requireNotNull(table) { "未能初始化DAO对应的表对象。" }
    }

    /**
     * 返回当前 DAO 对应的实体类型。
     *
     * @return 实体 KClass
     */
    @Suppress("UNCHECKED_CAST")
    protected fun entityClass(): KClass<E> {
        if (entityClass == null) {
            entityClass = GenericKit.getSuperClassGenricClass(this::class, 1) as KClass<E>
        }
        return requireNotNull(entityClass) { "未能解析DAO对应的实体类型。" }
    }

    /**
     * 根据当前 DAO 表绑定实体（PO）上的 [io.kudos.base.query.sort.Sortable] 得到可排序属性名集合（与请求中的 VO/PO 返回类型无关）。
     */
    protected fun sortWhitelistFromPo(): Set<String> {
        val ec = table().entityClass ?: return emptySet()
        return sortablePropertyNamesForEntity(ec)
    }

    /**
     * 按 PO 上 [io.kudos.base.query.sort.Sortable] 过滤客户端传入的排序。
     * 参与排序的每个属性都必须在 PO 上有对应注解；未标注的项会打 WARN（含实体类与属性名）并在查询中忽略该排序。
     * [whitelist] 为 PO 上全部 @Sortable 属性名；若 PO 未标注任何 @Sortable，则 [whitelist] 为空，所有请求的排序项均会被忽略并告警。
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
                    "查询排序请求中的属性未在表实体上标注 @Sortable，已忽略该排序项: entityClass={0}, property={1}",
                    entityName,
                    order.property
                )
            }
        }
        return list.filter { it.property in whitelist }
    }

    /**
     * 返回当前表所在的数据库对象
     *
     * @return 当前表所在的数据库对象
     * @author K
     * @since 1.0.0
     */
    protected fun database(): Database = KudosContextHolder.currentDatabase()

    /**
     * 返回T指定的表的查询源，基于该对象可以进行类似对数据库表的sql一样操作
     *
     * @return 查询源
     * @author K
     * @since 1.0.0
     */
    protected fun querySource(): QuerySource = database().from(table())

    /**
     * 返回T指定的表的实体序列，基于该序列可以进行类似对集合一样的操作
     *
     * @return 实体序列
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
     * 返回主键的列(kudos数据库表规范，一个表有且仅有一个列名为id的主键)
     *
     * @return 主键列对象
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    protected fun getPkColumn(): Column<PK> {
        return table().primaryKeys[0] as Column<PK>
    }

    /**
     * 创建查询条件表达式
     *
     * @param column 列对象
     * @param operator 操作符
     * @param value 要查询的值
     * @return 列申明对象
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
     * 单属性等值查询（字符串属性名版本）。
     *
     * @param property 属性名
     * @param value 查询值
     * @param orders 排序规则
     * @return 满足条件的实体列表
     */
    private fun oneSearch(property: String, value: Any?, vararg orders: Order): List<E> {
        return doSearchEntity(mapOf(property to value), null, null, *orders)
    }

    /**
     * 单属性等值查询，仅返回单个属性（字符串属性名版本）。
     *
     * @param property 查询属性名
     * @param value 查询值
     * @param returnProperty 返回属性名
     * @param orders 排序规则
     * @return 指定属性值列表
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
     * 单属性等值查询，仅返回多属性映射（字符串属性名版本）。
     *
     * @param property 查询属性名
     * @param value 查询值
     * @param returnProperties 返回属性名集合
     * @param orders 排序规则
     * @return 多属性映射列表
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
     * 查询全部，仅返回单个属性（字符串属性名版本）。
     *
     * @param returnProperty 返回属性名
     * @param orders 排序规则
     * @return 指定属性值列表
     */
    @Suppress("UNCHECKED_CAST")
    private fun <R> allSearchProperty(returnProperty: String, vararg orders: Order): List<R> {
        val results = doSearchProperties(null, null, listOf(returnProperty), null, *orders)
        return results.flatMap { it.values } as List<R>
    }

    /**
     * 查询全部，仅返回多属性映射（字符串属性名版本）。
     *
     * @param returnProperties 返回属性名集合
     * @param orders 排序规则
     * @return 多属性映射列表
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
     * 多属性 AND 查询（字符串属性名版本），返回实体列表。
     *
     * @param properties 属性名到值映射
     * @param orders 排序规则
     * @return 满足条件的实体列表
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
     * 多属性 OR 查询（字符串属性名版本），返回实体列表。
     *
     * @param properties 属性名到值映射
     * @param orders 排序规则
     * @return 满足条件的实体列表
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
     * IN 查询（字符串属性名版本），返回实体列表。
     *
     * @param property 参与 IN 的属性名
     * @param values IN 值集合
     * @param orders 排序规则
     * @return 满足 IN 条件的实体列表
     */
    private fun inSearch(property: String, values: Collection<*>, vararg orders: Order): List<E> {
        val column = requireNotNull(ColumnHelper.columnOf(table(), property)[property]) {
            "未找到属性[$property]对应的数据库列。"
        }
        var entitySequence = entitySequence().filter { column.inCollection(values) }
        entitySequence = entitySequence.sortedBy(*sortBy(*orders))
        return entitySequence.toList()
    }

    override fun inSearch(property: KProperty1<E, *>, values: Collection<*>, vararg orders: Order): List<E> =
        inSearch(property.name, values, *orders)

    /**
     * IN 查询，仅返回单个属性（字符串属性名版本）。
     *
     * @param property 参与 IN 的属性名
     * @param values IN 值集合
     * @param returnProperty 返回属性名
     * @param orders 排序规则
     * @return 指定属性值列表
     */
    private fun inSearchProperty(
        property: String, values: Collection<*>, returnProperty: String, vararg orders: Order
    ): List<*> {
        val results = doInSearchProperties(property, values, listOf(returnProperty), *orders)
        return results.flatMap { it.values }
    }

    /**
     * IN 查询，仅返回多属性映射（字符串属性名版本）。
     *
     * @param property 参与 IN 的属性名
     * @param values IN 值集合
     * @param returnProperties 返回属性名集合
     * @param orders 排序规则
     * @return 多属性映射列表
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
     * Criteria 查询，仅返回单个属性（字符串属性名版本）。
     *
     * @param criteria 查询条件
     * @param returnProperty 返回属性名
     * @param orders 排序规则
     * @return 指定属性值列表
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
     * Criteria 查询，仅返回多属性映射（字符串属性名版本）。
     *
     * @param criteria 查询条件
     * @param returnProperties 返回属性名集合
     * @param orders 排序规则
     * @return 多属性映射列表
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
     * Criteria 分页查询，返回实体列表。
     *
     * @param criteria 查询条件，可为 null
     * @param pageNo 页码
     * @param pageSize 页大小
     * @param orders 排序规则
     * @return 当前页实体列表
     */
    override fun pagingSearch(criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order): List<E> {
        return searchEntityCriteria(criteria, pageNo, pageSize, *orders)
    }

    /**
     * Criteria 分页查询并指定返回类型。
     *
     * @param criteria 查询条件，可为 null
     * @param returnItemClass 返回元素类型，可为 null
     * @param pageNo 页码
     * @param pageSize 页大小
     * @param orders 排序规则
     * @return 当前页指定类型结果列表
     */
    override fun <T : Any> pagingSearch(
        criteria: Criteria?,
        returnItemClass: KClass<T>?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<T> {
        return if (returnItemClass == null || returnItemClass == entityClass()) {
            // 走表实体的逻辑
            @Suppress("UNCHECKED_CAST")
            pagingSearch(criteria, pageNo, pageSize, *orders) as List<T>
        } else {
            searchByCriteria(criteria, returnItemClass, pageNo, pageSize, *orders)
        }
    }

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

    /**
     * 分页查询并仅返回单列。借用 [searchPropertiesCriteria] 复用查询逻辑，再 `flatMap` 把
     * `List<Map<String, *>>` 拍扁为列值列表。
     *
     * @param R 列值类型（由调用方在公开 API 处保证类型安全）
     * @param criteria 查询条件
     * @param returnProperty 待查的列名
     * @param pageNo 页码（1 基）
     * @param pageSize 每页条数
     * @param orders 排序
     * @return 当前页该列的值列表
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
     * Criteria 分页查询，仅返回多属性映射（字符串属性名版本）。
     *
     * @param criteria 查询条件
     * @param returnProperties 返回属性名集合
     * @param pageNo 页码
     * @param pageSize 页大小
     * @param orders 排序规则
     * @return 当前页多属性映射列表
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
            "ListSearchPayload.returnProperties 不为空时，search(payload, returnItemClass) 无法保证返回元素类型。"
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
     * 根据查询载体对象查询(包括分页), 具体规则见 @see SearchPayload
     *
     * 同一属性的查询逻辑在 listSearchPayload 和 whereConditionFactory 都有指定时，以 whereConditionFactory 为准！
     *
     * @param listSearchPayload 查询载体对象，默认为null,为null时返回的列表元素类型为PO实体类，此时，若whereConditionFactory有指定，各条件间的查询逻辑为AND
     * @param whereConditionFactory where条件表达式工厂函数，可对listSearchPayload参数定义查询逻辑，也可完全自定义查询逻辑，函数返回null时将按“等于”操作处理listSearchPayload中的属性。参数默认为null
     * @return 结果列表, 有三种类型可能, @see SearchPayload
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
            // order（仅白名单内字段参与排序）
            val allowedOrders = filterOrdersBySortWhitelist(payload.orders, sortWhitelistFromPo())
            if (allowedOrders.isNotEmpty()) {
                query = query.orderBy(sortOf(*allowedOrders.toTypedArray()))
            }
            // paging（pageNo 为 null 且不允许查全量时按第 1 页分页）
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
                            requireNotNull(table().entityClass) { "表未绑定实体类型，无法创建返回对象。" }
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
     * 按 Criteria 计算记录数。
     *
     * @param criteria 查询条件，可为 null
     * @return 记录数
     */
    protected fun countByCriteria(criteria: Criteria?): Int =
        if (criteria == null) entitySequence().count()
        else filteredSequence(criteria).aggregateColumns { count(getPkColumn()) } ?: 0

    override fun count(searchPayload: ISearchPayload?): Int {
        return count(searchPayload, null)
    }

    /**
     * 计算记录数
     *
     * @param searchPayload 查询载体对象，为null时按全量记录统计；若 whereConditionFactory 有指定，各条件间默认按 AND
     * @param whereConditionFactory where条件表达式工厂函数，可对searchPayload参数定义查询逻辑，也可完全自定义查询逻辑，函数返回null时将按“等于”操作处理searchPayload中的属性。参数默认为null
     * @return 记录数
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
     * 按属性求和（字符串属性名版本）。
     *
     * @param property 属性名
     * @param criteria 查询条件，可为 null
     * @return 求和结果
     */
    @Suppress("UNCHECKED_CAST")
    private fun sum(property: String, criteria: Criteria?): Number =
        filteredSequence(criteria).aggregateColumns {
            sum(ColumnHelper.columnOf(table(), property)[property] as Column<Number>)
        } as Number

    /**
     * 按属性求平均值（字符串属性名版本）。
     *
     * @param property 属性名
     * @param criteria 查询条件，可为 null
     * @return 平均值结果
     */
    @Suppress("UNCHECKED_CAST")
    private fun avg(property: String, criteria: Criteria?): Number =
        filteredSequence(criteria).aggregateColumns {
            avg(ColumnHelper.columnOf(table(), property)[property] as Column<Number>)
        } as Number

    /**
     * 按属性求最大值（字符串属性名版本）。
     *
     * @param property 属性名
     * @param criteria 查询条件，可为 null
     * @return 最大值；无数据返回 null
     */
    @Suppress("UNCHECKED_CAST")
    private fun <R : Comparable<R>> max(property: String, criteria: Criteria?): R? =
        filteredSequence(criteria).aggregateColumns {
            max(ColumnHelper.columnOf(table(), property)[property] as Column<Comparable<Any>>)
        } as R?

    /**
     * 按属性求最小值（字符串属性名版本）。
     *
     * @param property 属性名
     * @param criteria 查询条件，可为 null
     * @return 最小值；无数据返回 null
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
     * 把业务侧的 [Order] 数组翻译成 ktorm 的 `OrderByExpression` 列表，喂给 `query.orderBy(...)`。
     *
     * 列名 → 列对象走 [ColumnHelper.columnOf]，找不到列直接 `requireNotNull` 抛异常——
     * 这种情况通常是属性名拼错或对应字段未在 [io.kudos.ability.data.rdb.ktorm.support.ITable] 暴露，
     * 早抛错好定位。
     *
     * @param orders 排序规则数组；为空时返回空列表（query 不会追加 order by）
     * @return ktorm 排序表达式列表
     * @author K
     * @since 1.0.0
     */
    private fun sortOf(vararg orders: Order): List<OrderByExpression> {
        return if (orders.isNotEmpty()) {
            val orderExpressions = mutableListOf<OrderByExpression>()
            orders.forEach {
                val column = requireNotNull(ColumnHelper.columnOf(table(), it.property)[it.property]) {
                    "未找到属性[${it.property}]对应的数据库列。"
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
     * 把 [sortOf] 的 [OrderByExpression] 列表再包成 `EntitySequence.sortedBy(...)` 所需的
     * `Array<(T) -> OrderByExpression>` 形态——闭包忽略入参直接返回静态表达式（ktorm 实体序列签名要求）。
     *
     * 给 `EntitySequence` 路径（[searchEntityCriteria]）用，与 `Query` 路径走 [sortOf] 区分开。
     *
     * @param orders 排序规则
     * @return ktorm 实体序列的排序函数数组
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
     * 把"属性名 → (操作符, 值)"映射翻译成 ktorm 的 where 条件表达式（AND / OR 复合）。
     *
     * 分支语义：
     * - `IS_NULL` / `IS_NOT_NULL`：直接走列的 null 检查
     * - 值为 null 或 ""：默认走 `column IS NULL`；`ignoreNull=true` 时把决定权交给 `whereConditionFactory`
     * - 否则走 [SqlWhereExpressionFactory] 按操作符生成表达式
     * - 显式 `whereConditionFactory` 可覆盖默认行为（用于子类做自定义条件）
     *
     * 兜底：所有属性都没产生表达式且有自定义工厂时，遍历所有列让工厂"为每一列再尝试一次"，
     * 允许工厂基于列名给出条件（典型用途：列名后缀约定）。
     *
     * @param propertyMap 属性 → (操作符, 值)
     * @param andOr 多条件之间的拼接逻辑，null 默认按 AND
     * @param ignoreNull 是否在值为 null/"" 时跳过该列（true：让 factory 决定）
     * @param whereConditionFactory 自定义条件生成器，null 时走默认
     * @return 合并后的条件表达式；无任何条件时返回 null（调用方据此跳过 where）
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
                "属性[$property]缺少查询操作符和值。"
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
     * 走 EntitySequence 路径按"属性 = 值"map 查整实体；操作符固定为 EQ，逻辑由 [logic] 决定。
     * 与 [searchEntityCriteria] 区别：入参是属性值 map（非 Criteria DSL），由 [processWhere] 翻译。
     *
     * @param propertyMap 属性 → 值；null 表示无 where
     * @param logic AND / OR 拼接逻辑
     * @param whereConditionFactory 自定义条件生成器
     * @param orders 排序
     * @return 实体列表
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
     * 走 Query 路径按"属性 = 值"map 查多列；操作符固定为 EQ，逻辑由 [logic] 决定。
     * 与 [doSearchEntity] 区别：返回列由 [returnProperties] 指定，而非全实体字段。
     *
     * @param propertyMap 属性 → 值；null 表示无 where
     * @param logic AND / OR 拼接逻辑
     * @param returnProperties 待返回的列名集合
     * @param whereConditionFactory 自定义条件生成器
     * @param orders 排序
     * @return 列名 → 列值 的 list-of-map
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
     * 把 ktorm [Query] 结果按 `returnColumnMap` 重新组装成业务类 [T] 的实例列表。
     * 实例化委托给 [instantiateResultItem]——支持 data class / Entity / Map 多种目标类型。
     *
     * @param T 目标类型
     * @param query 已构建的 ktorm 查询
     * @param returnItemClass 目标类 KClass
     * @param returnColumnMap 属性名 → 列 映射（由 [getColumns] 算出）
     * @return 反序列化后的对象列表
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
     * 把 ktorm [Query] 结果直接拍成 `List<Map<String, *>>`（属性名 → 列值）。
     * 走"只查多列、不需要业务类型"的路径，避免反射开销。
     *
     * @param query 已构建的 ktorm 查询
     * @param returnColumnMap 属性名 → 列 映射
     * @return 每行一个 map
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
     * `IN (...)` 查询的属性多列版本：按 `property` 的列 IN values，返回 `returnProperties` 多列结果。
     *
     * @param property 用于 IN 查询的列名
     * @param values IN 候选值集合
     * @param returnProperties 待返回的列名集合
     * @param orders 排序
     * @return 列名 → 列值 的 list-of-map
     * @author K
     * @since 1.0.0
     */
    private fun doInSearchProperties(
        property: String, values: Collection<*>, returnProperties: Collection<String>, vararg orders: Order
    ): List<Map<String, *>> {
        val column = requireNotNull(ColumnHelper.columnOf(table(), property)[property]) {
            "未找到属性[$property]对应的数据库列。"
        }
        val returnColumnMap = ColumnHelper.columnOf(table(), *returnProperties.toTypedArray())
        var query = querySource().select(returnColumnMap.values)
        query = query.where { column.inCollection(values) }
        query = query.orderBy(sortOf(*orders))
        return processResult(query, returnColumnMap)
    }

    /**
     * 走 ktorm EntitySequence 路径的查询：把 criteria 转为 sequence filter 后排序 + 分页。
     * 返回完整实体 [E]，与 [searchByCriteria] 的 Query 路径互补——sequence 适合"按 entity 取出全部字段"。
     *
     * 分页阈值：`pageNo=0 || pageSize=0` 视为"不分页"，全量返回（drop/take 都跳过）。
     *
     * @param criteria 查询条件，null 表示不加 where
     * @param pageNo 页码（1 基；0 表示不分页）
     * @param pageSize 每页条数（0 表示不分页）
     * @param orders 排序
     * @return 实体列表
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
     * 走 ktorm Query 路径的多列投影查询：仅查 `returnProperties` 指定的列，返回 list-of-map。
     * 性能优于 [searchEntityCriteria]——只读所需列，避免全量行 → 实体的反序列化开销。
     *
     * @param criteria 查询条件
     * @param returnProperties 待查列名集合
     * @param pageNo 页码（1 基；0 表示不分页）
     * @param pageSize 每页条数
     * @param orders 排序
     * @return 列名 → 列值的 list-of-map
     * @author K
     * @since 1.0.0
     */
    private fun searchPropertiesCriteria(
        criteria: Criteria, returnProperties: Collection<String>,
        pageNo: Int = 0, pageSize: Int = 0, vararg orders: Order
    ): List<Map<String, *>> {
        val returnColumnMap = ColumnHelper.columnOf(table(), *returnProperties.toTypedArray())

        // where、order、paging
        val query = prepareQuery(criteria, returnColumnMap, pageNo, pageSize, *orders)

        // result
        return processResult(query, returnColumnMap)
    }

    /**
     * 按 [returnItemClass] 选列 + criteria 查 + 分页 + 反序列化为指定类型 list。
     * 与 [searchPropertiesCriteria] 相比多了反序列化步骤（→ T），与 [searchEntityCriteria] 相比
     * 列由 returnItemClass 决定（典型用法：业务 VO/DTO 与表实体不一致）。
     *
     * @param T 目标返回类型
     * @param criteria 查询条件
     * @param returnItemClass 目标类型 KClass
     * @param pageNo 页码（1 基；0 表示不分页）
     * @param pageSize 每页条数
     * @param orders 排序
     * @return 目标类型对象列表
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

        // select、where、order、paging
        val query = prepareQuery(criteria, returnColumnMap, pageNo, pageSize, *orders)

        // result
        return processResult(query, returnItemClass, returnColumnMap)
    }

    /**
     * 构建 ktorm Query 的统一模板：select 指定列 → where (criteria 非空时) → orderBy → limit。
     * 给 [searchPropertiesCriteria] / [searchByCriteria] / [searchByPayload] 复用，避免重复模板代码。
     *
     * @param criteria 查询条件，null 时跳过 where
     * @param returnColumnMap 属性名 → 列
     * @param pageNo 页码（1 基；0 表示不分页）
     * @param pageSize 每页条数
     * @param orders 排序
     * @return 构建好的 ktorm [Query]
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
     * 基于 [ISearchPayload] 的全功能查询入口：支持
     * - select 列：payload.returnProperties / payload.returnEntityClass / override 三级回退
     * - where：payload 的 property 值 + nullProperties + criterions 三段拼装（详见 [getWherePropertyMap]）
     * - order + paging：payload 自带的 orders / pageNo / pageSize
     *
     * 是 ms-* 中 service 层最常用的查询入口；其它私有 search* 系列方法是它针对 Criteria 的简化版本。
     *
     * @param searchPayload 业务查询载荷；null 时退化为"全表查 + 默认排序"
     * @param whereConditionFactory 业务自定义 where 生成器
     * @param returnItemClassOverride 显式指定返回类型，优先级高于 payload.returnEntityClass
     * @return 反序列化后的对象列表（类型由 effectiveReturnClass 决定）
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
        val returnProps = entityProperties.intersect(props.toSet()) // 取交集,保证要查询的列一定存在
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
     * 反射拿到表绑定的实体类的属性名列表；过滤掉 ktorm Entity 接口自带的 3 个 meta 属性
     * （`entityClass` / `properties` / `changedProperties`）避免把它们当业务字段查询。
     *
     * @return 实体业务属性名列表
     * @throws IllegalArgumentException 表未绑定实体类型
     * @author K
     * @since 1.0.0
     */
    protected fun getEntityProperties(): List<String> {
        val tableEntityClass = requireNotNull(table().entityClass) { "表未绑定实体类型，无法提取实体属性。" }
        return tableEntityClass.memberProperties
            .filter { it.name !in setOf("entityClass", "properties", "changedProperties") }
            .map { it.name }
    }

    /**
     * 把 [ISearchPayload] 拆成"列名 → (操作符, 值)" map，准备喂给 [processWhere]。
     *
     * 三段拼装：
     * 1. payload 的 bean 属性 + 显式 operator 映射 → 默认 EQ
     * 2. nullProperties 标记的列：仅在值未在 bean 中出现时才补 `IS_NULL`（避免与"值存在但为 null"语义冲突）
     * 3. payload 自带的 criterions（OR/复合）追加并覆盖：业务方手写的优先级最高
     *
     * @param searchPayload 业务侧传入的查询载荷
     * @param entityProperties 当前表实体的合法属性列表（用作白名单过滤）
     * @return 列名 → (操作符, 值) 的 map
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
     * 按 returnType 反射出的属性 ∩ 表实体属性，生成 ktorm Column 映射，供后续 select 用。
     *
     * 取交集是防御性设计——returnType 可能是 VO/DTO 多含/少含字段，硬塞列会让 ktorm 报错；
     * 交集保证查的列在表上一定存在，多余字段调用方在 mapTo 阶段会被忽略或留空。
     *
     * @param returnType 目标返回类型
     * @return 属性名 → 列 的 map
     * @author K
     * @since 1.0.0
     */
    protected fun getColumns(returnType: KClass<*>): Map<String, Column<Any>> {
        val entityProperties = getEntityProperties()
        val properties = returnType.memberProperties.map { it.name }
        val returnProps = entityProperties.intersect(properties.toSet()) // 取交集,保证要查询的列一定存在
        return ColumnHelper.columnOf(table(), *returnProps.toTypedArray())
    }

    /**
     * 将 QueryRowSet 按列映射自动填充到指定类型实例
     *
     * @param rowSet QueryRowSet
     * @param destClass 目标类型
     * @param defaultValues 可选，属性名 -> 默认值（如 id 的 ""）
     * @param extraColumns 可选，主表外的列信息（如关联表列），属性名 -> 列；与 getColumns 合并使用，extraColumns 优先；传 null 与不传等价于空 Map
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
     * 把 Query 行的列值反序列化成 [destClass] 实例。
     *
     * 三档策略：
     * 1. **Ktorm Entity 接口型** (`destClass.isSubclassOf(Entity::class)`)：走 `Entity.create` +
     *    [populateResultItem]——Ktorm Entity 接口 PO 无 Kotlin 构造器，必须走代理工厂
     * 2. **传统 JavaBean / 普通 Kotlin 类**：找无参构造 + 属性回填
     * 3. **data class / 仅主构造**：[callBy] 注入参数，缺失的必填非空字段会抛异常
     *
     * @param R 目标类型
     * @param destClass 目标类
     * @param propertyValues 列名 → 列值
     * @param defaultValues 列名 → 默认值（如 id 用 ""）；优先级低于 propertyValues
     * @return 反序列化好的实例
     * @throws IllegalArgumentException 缺少必填字段时
     * @author K
     * @since 1.0.0
     */
    private fun <R : Any> instantiateResultItem(
        destClass: KClass<R>,
        propertyValues: Map<String, Any?>,
        defaultValues: Map<String, Any?> = emptyMap()
    ): R {
        // Ktorm 接口型 PO（companion 继承 DbEntityFactory / Entity.Factory）无 Kotlin 构造器，须通过 Entity.create 实例化。
        if (destClass.isSubclassOf(Entity::class)) {
            @Suppress("UNCHECKED_CAST")
            val entity = Entity.create(destClass as KClass<Entity<*>>) as R
            populateResultItem(entity, defaultValues + propertyValues)
            return entity
        }

        // 兼容传统 JavaBean/普通 Kotlin 类：优先走无参构造 + 属性回填。
        destClass.constructors.firstOrNull { it.parameters.isEmpty() }?.let {
            return it.call().also { obj -> populateResultItem(obj, propertyValues) }
        }

        // 兼容 data class 等不可变对象：通过主构造器按参数名装配。
        val constructor = requireNotNull(destClass.primaryConstructor) {
            "类${destClass.qualifiedName}既没有无参构造器，也没有主构造器，无法封装查询结果。"
        }
        val args = mutableMapOf<KParameter, Any?>()
        val missingRequiredParams = mutableListOf<String>()

        constructor.parameters.forEach { param ->
            val name = param.name ?: return@forEach
            val hasPropertyValue = propertyValues.containsKey(name)
            val hasDefaultValue = defaultValues.containsKey(name)
            if (!hasPropertyValue && !hasDefaultValue) {
                // 非可空且无默认值的必填构造参数，必须能从查询结果中拿到值。
                if (!param.isOptional && !param.type.isMarkedNullable) {
                    missingRequiredParams.add(name)
                }
                return@forEach
            }
            val value = when {
                hasPropertyValue -> propertyValues[name]
                else -> defaultValues[name]
            }
            // 对带 Kotlin 默认值的参数，传 null 会覆盖默认值；这里直接跳过，让 callBy 使用默认值。
            if (value == null && param.isOptional) {
                return@forEach
            }
            args[param] = value
        }

        require(missingRequiredParams.isEmpty()) {
            "类${destClass.qualifiedName}缺少构造参数${missingRequiredParams.joinToString(prefix = "[", postfix = "]")}对应的查询结果，无法实例化。"
        }
        return constructor.callBy(args)
    }

    /**
     * 把列值反向写到已实例化的对象。
     *
     * - **Ktorm Entity**：走 `target[property] = value` 由代理维护 changed 标记，绕过代理直接反射会
     *   破坏脏字段追踪，导致后续 `update` 漏字段
     * - **普通对象**：走 [BeanKit.setProperty] 走 setter
     *
     * @param target 目标对象
     * @param propertyValues 列名 → 列值
     * @author K
     * @since 1.0.0
     */
    private fun populateResultItem(target: Any, propertyValues: Map<String, Any?>) {
        if (target is Entity<*>) {
            // Ktorm Entity 通过代理维护属性值，直接走其下标写入，避免绕过代理状态。
            propertyValues.forEach { (property, value) ->
                target[property] = value
            }
            return
        }
        // 普通对象仍沿用 BeanKit，兼容 setter / 反射字段写入。
        propertyValues.forEach { (property, value) ->
            BeanKit.setProperty(target, property, value)
        }
    }

    companion object {
        private val log = LogFactory.getLog(BaseReadOnlyDao::class)
    }

}


// 解决ktorm总是要调用到可变参数的inList方法的问题及泛型问题
@Suppress("UNCHECKED_CAST")
private fun <T : Any> ColumnDeclaring<T>.inCollection(list: Collection<*>): InListExpression {
    return InListExpression(left = asExpression(), values = list.map { wrapArgument(it as T?) })
}
