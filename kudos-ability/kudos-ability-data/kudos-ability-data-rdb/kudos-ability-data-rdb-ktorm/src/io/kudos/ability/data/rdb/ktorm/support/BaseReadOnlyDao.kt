package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.ability.data.rdb.ktorm.datasource.currentDatabase
import io.kudos.base.bean.BeanKit
import io.kudos.base.lang.GenericKit
import io.kudos.base.lang.reflect.newInstance
import io.kudos.base.lang.string.underscoreToHump
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.base.support.GroupExecutor
import io.kudos.base.support.dao.IBaseReadOnlyDao
import io.kudos.base.support.logic.AndOrEnum
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.base.support.payload.SearchPayload
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
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * 基础只读数据访问对象，封装某数据库表的通用查询操作
 *
 * @param PK 实体主键类型
 * @param E 实体类型
 * @param T 数据库表-实体关联对象的类型
 * @author K
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
    fun <A : Any> whereExpr(column: Column<A>, operator: OperatorEnum, value: A?): ColumnDeclaring<Boolean>? {
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
                val bean = returnType.newInstance()
                columnMap.forEach { (property, column) ->
                    BeanKit.setProperty(bean, property, row[column])
                }
                return bean
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
    private fun allSearchPropertiesByNames(returnProperties: Collection<String>, vararg orders: Order): List<Map<String, *>> {
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
    ): List<Map<String, *>> = pagingReturnPropertiesByNames(criteria, returnProperties.map { it.name }, pageNo, pageSize, *orders)

    //endregion pagingSearch


    //region payload search

    override fun search(listSearchPayload: ListSearchPayload?): List<*> {
        return search(listSearchPayload, null)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> search(listSearchPayload: ListSearchPayload?, returnItemClass: KClass<T>): List<T> {
        require(listSearchPayload?.returnProperties.isNullOrEmpty()) {
            "ListSearchPayload.returnProperties 不为空时，search(payload, returnItemClass) 无法保证返回元素类型。"
        }
        return if (listSearchPayload == null) {
            search(criteria = null, returnItemClass = returnItemClass)
        } else {
            val originalReturnEntityClass = listSearchPayload.returnEntityClass
            listSearchPayload.returnEntityClass = returnItemClass
            try {
                search(listSearchPayload) as List<T>
            } finally {
                listSearchPayload.returnEntityClass = originalReturnEntityClass
            }
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
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)? = null
    ): List<*> {
        // select, where
        val objects = searchByPayload(listSearchPayload, whereConditionFactory)
        var query = objects[0] as Query
        val returnProps = objects[1] as Set<String>
        val returnColumnMap = objects[2] as Map<String, Column<Any>>

        // order
        if (listSearchPayload != null) {
            val orders = listSearchPayload.orders
            if (!orders.isNullOrEmpty()) {
                val orderExps = sortOf(*orders.toTypedArray())
                query = query.orderBy(orderExps)
            }
        }

        // paging
        if (listSearchPayload != null) {
            val pageNo = listSearchPayload.pageNo
            if (pageNo != null) {
                val pageSize = listSearchPayload.pageSize ?: 10
                query = query.limit((pageNo - 1) * pageSize, pageSize)
            }
        }

        // result
        val returnProperties = listSearchPayload?.returnProperties ?: emptyList()
        val mapList = processResult(query, returnColumnMap)
        return when {
            returnProperties.isEmpty() -> {
                val beanList = mutableListOf<Any>()
                mapList.forEach { map ->
                    val bean = if (listSearchPayload?.returnEntityClass != null) {
                        requireNotNull(listSearchPayload.returnEntityClass).newInstance()
                    } else {
                        val tableEntityClass = requireNotNull(table().entityClass) { "表未绑定实体类型，无法创建返回对象。" }
                        Entity.create(tableEntityClass)
                    }
                    returnProps.forEach { prop ->
                        BeanKit.setProperty(bean, prop, map[prop])
                    }
                    beanList.add(bean)
                }
                beanList
            }

            returnProperties.size == 1 -> {
                mapList.flatMap { it.values }
            }

            else -> {
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
    protected fun countByCriteria(criteria: Criteria?): Int {
        return if (criteria == null) {
            entitySequence().count()
        } else {
            entitySequence()
                .filter { CriteriaConverter.convert(criteria, table()) }
                .aggregateColumns { count(getPkColumn()) } ?: 0
        }
    }

    override fun count(searchPayload: SearchPayload?): Int {
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
        searchPayload: SearchPayload?,
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
    private fun sum(property: String, criteria: Criteria?): Number {
        var entitySequence = entitySequence()
        if (criteria != null) {
            entitySequence = entitySequence.filter { CriteriaConverter.convert(criteria, table()) }
        }
        return entitySequence.aggregateColumns {
            sum(ColumnHelper.columnOf(table(), property)[property] as Column<Number>)
        } as Number
    }

    /**
     * 按属性求平均值（字符串属性名版本）。
     *
     * @param property 属性名
     * @param criteria 查询条件，可为 null
     * @return 平均值结果
     */
    @Suppress("UNCHECKED_CAST")
    private fun avg(property: String, criteria: Criteria?): Number {
        var entitySequence = entitySequence()
        if (criteria != null) {
            entitySequence = entitySequence.filter { CriteriaConverter.convert(criteria, table()) }
        }
        return entitySequence.aggregateColumns {
            avg(ColumnHelper.columnOf(table(), property)[property] as Column<Number>)
        } as Number
    }

    /**
     * 按属性求最大值（字符串属性名版本）。
     *
     * @param property 属性名
     * @param criteria 查询条件，可为 null
     * @return 最大值；无数据返回 null
     */
    @Suppress("UNCHECKED_CAST")
    private fun <R : Comparable<R>> max(property: String, criteria: Criteria?): R? {
        var entitySequence = entitySequence()
        if (criteria != null) {
            entitySequence = entitySequence.filter { CriteriaConverter.convert(criteria, table()) }
        }
        return entitySequence.aggregateColumns {
            max(ColumnHelper.columnOf(table(), property)[property] as Column<Comparable<Any>>)
        } as R?
    }

    /**
     * 按属性求最小值（字符串属性名版本）。
     *
     * @param property 属性名
     * @param criteria 查询条件，可为 null
     * @return 最小值；无数据返回 null
     */
    @Suppress("UNCHECKED_CAST")
    private fun <R : Comparable<R>> min(property: String, criteria: Criteria?): R? {
        var entitySequence = entitySequence()
        if (criteria != null) {
            entitySequence = entitySequence.filter { CriteriaConverter.convert(criteria, table()) }
        }
        return entitySequence.aggregateColumns {
            min(ColumnHelper.columnOf(table(), property)[property] as Column<Comparable<Any>>)
        } as R?
    }

    override fun sum(property: KProperty1<E, *>, criteria: Criteria?): Number = sum(property.name, criteria)
    override fun avg(property: KProperty1<E, *>, criteria: Criteria?): Number = avg(property.name, criteria)
    override fun <R : Comparable<R>> max(property: KProperty1<E, R?>, criteria: Criteria?): R? = max(property.name, criteria)
    override fun <R : Comparable<R>> min(property: KProperty1<E, R?>, criteria: Criteria?): R? = min(property.name, criteria)

    //endregion aggregate

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

    private fun sortBy(vararg orders: Order): Array<(T) -> OrderByExpression> {
        val orderByExps = sortOf(*orders)
        val orderExpressions = mutableListOf<(T) -> OrderByExpression>()
        orderByExps.forEach { orderByExp -> orderExpressions.add { orderByExp } }
        return orderExpressions.toTypedArray()
    }

    @Suppress("UNCHECKED_CAST")
    protected fun processWhere(
        propertyMap: Map<String, Pair<OperatorEnum, *>>,
        andOr: AndOrEnum?,
        ignoreNull: Boolean = false,
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)? = null
    ): ColumnDeclaring<Boolean>? {
        val properties = propertyMap.keys.toTypedArray()
        val columns = ColumnHelper.columnOf(table(), *properties)
        val expressions = mutableListOf<ColumnDeclaring<Boolean>>()
        columns.forEach { (property, column) ->
            val operatorAndValue = requireNotNull(propertyMap[property]) { "属性[$property]缺少查询操作符和值。" }
            val operator = operatorAndValue.first
            val value = operatorAndValue.second
            val expression = if (operator == OperatorEnum.IS_NULL) {
                column.isNull()
            } else if (operator == OperatorEnum.IS_NOT_NULL) {
                column.isNotNull()
            } else if (value == null || value == "") {
                if (ignoreNull) {
                    whereConditionFactory?.let { it(column, value) }
                } else {
                    column.isNull()
                }
            } else {
                if (whereConditionFactory == null) {
                    SqlWhereExpressionFactory.create(column, operator, value)
                } else {
                    whereConditionFactory(column, value) ?: (column eq value)
                }
            }
            if (expression != null) {
                expressions.add(expression)
            }
        }

        if (expressions.isEmpty()) {
            if (whereConditionFactory != null) {
                table().columns.forEach { column ->
                    val expression = whereConditionFactory(column as Column<Any>, null)
                    if (expression != null) {
                        expressions.add(expression)
                    }
                }
            }
        }

        return if (expressions.isEmpty()) {
            null
        } else {
            var fullExpression = expressions[0]
            expressions.forEachIndexed { index, expression ->
                if (index != 0) {
                    fullExpression = if (andOr == AndOrEnum.AND) {
                        fullExpression.and(expression)
                    } else {
                        fullExpression.or(expression)
                    }
                }
            }
            fullExpression
        }
    }

    private fun doSearchEntity(
        propertyMap: Map<String, *>?,
        logic: AndOrEnum?,
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)? = null,
        vararg orders: Order
    ): List<E> {
        var entitySequence = entitySequence()
        if (propertyMap != null) {
            val propMap = propertyMap.map { (prop, value) -> prop to Pair(OperatorEnum.EQ, value) }.toMap()
            val fullExpression = processWhere(propMap, logic, false, whereConditionFactory)
            if (fullExpression != null) {
                entitySequence = entitySequence.filter { fullExpression }
            }
        }
        entitySequence = entitySequence.sortedBy(*sortBy(*orders))
        return entitySequence.toList()
    }

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
        if (propertyMap != null) {
            val propMap = propertyMap.map { (prop, value) -> prop to Pair(OperatorEnum.EQ, value) }.toMap()
            val fullExpression = processWhere(propMap, logic, false, whereConditionFactory)
            if (fullExpression != null) {
                query = query.where { fullExpression }
            }
        }

        // order
        query = query.orderBy(sortOf(*orders))

        // result
        return processResult(query, returnColumnMap)
    }

    private fun <T : Any> processResult(
        query: Query,
        returnItemClass: KClass<T>,
        returnColumnMap: Map<String, Column<Any>>
    ): List<T> {
        val resultList = mutableListOf<T>()
        val propNames = returnColumnMap.keys
        query.forEach { row ->
            val item = returnItemClass.newInstance()
            propNames.forEach { propName ->
                val column = requireNotNull(returnColumnMap[propName]) { "未找到返回属性[$propName]对应的数据库列。" }
                BeanKit.setProperty(item, propName, row[column])
            }
            resultList.add(item)
        }
        return resultList
    }

    private fun processResult(query: Query, returnColumnMap: Map<String, Column<Any>>): List<Map<String, *>> {
        val returnValues = mutableListOf<Map<String, Any?>>()
        query.forEach { row ->
            val map = returnColumnMap.map { (propName, column) -> propName to row[column] }.toMap()
            returnValues.add(map)
        }
        return returnValues
    }

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

    private fun searchEntityCriteria(
        criteria: Criteria?, pageNo: Int = 0, pageSize: Int = 0, vararg orders: Order
    ): List<E> {
        var entitySequence = entitySequence()

        // where
        if (criteria != null) {
            entitySequence = entitySequence.filter { CriteriaConverter.convert(criteria, table()) }
        }

        // sort
        entitySequence = entitySequence.sortedBy(*sortBy(*orders))

        // paging
        if (pageNo != 0 && pageSize != 0) {
            entitySequence = entitySequence.drop((pageNo - 1) * pageSize).take(pageSize)
        }

        return entitySequence.toList()
    }

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

    private fun searchByPayload(
        searchPayload: SearchPayload? = null,
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)? = null
    ): List<Any> {
        val entityProperties = getEntityProperties()

        // select
        val returnProperties = searchPayload?.returnProperties ?: emptyList()
        val props = returnProperties.ifEmpty {
            val returnEntityClass = searchPayload?.returnEntityClass
            returnEntityClass?.memberProperties?.map { it.name } ?: entityProperties
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
        val andOr = searchPayload?.andOr ?: AndOrEnum.AND
        val fullExpression = processWhere(propMap, andOr, true, whereConditionFactory)
        if (fullExpression != null) {
            query = query.where { fullExpression }
        }

        return listOf(query, returnProps, returnColumnMap)
    }

    protected fun getEntityProperties(): List<String> {
        val tableEntityClass = requireNotNull(table().entityClass) { "表未绑定实体类型，无法提取实体属性。" }
        return tableEntityClass.memberProperties
            .filter { it.name !in setOf("entityClass", "properties", "changedProperties") }
            .map { it.name }
    }

    protected fun getWherePropertyMap(
        searchPayload: SearchPayload, entityProperties: List<String>
    ): Map<String, Pair<OperatorEnum, *>> {
        val propAndValueMap = BeanKit.extract(searchPayload).filter { entityProperties.contains(it.key) }
        val operatorMap = searchPayload.operators ?: emptyMap()
        val resultMap = mutableMapOf<String, Pair<OperatorEnum, *>>()
        propAndValueMap.forEach { (prop, value) ->
            var operator = operatorMap[prop]
            if (operator == null) {
                operator = OperatorEnum.EQ
            }
            resultMap[prop] = Pair(operator, value)
        }
        val nullProperties = searchPayload.nullProperties
        if (!nullProperties.isNullOrEmpty()) {
            nullProperties.forEach { propName ->
                if (propAndValueMap[propName] == null) {
                    resultMap[propName] = Pair(OperatorEnum.IS_NULL, null)
                }
            }
        }
        val criterions = searchPayload.criterions
        if (!criterions.isNullOrEmpty()) {
            criterions.forEach {
                resultMap[it.property] = Pair(it.operator, it.value)
            }
        }
        return resultMap
    }

    protected fun getColumns(returnType: KClass<*>): Map<String, Column<Any>> {
        val entityProperties = getEntityProperties()
        val properties = returnType.memberProperties.map { it.name }
        val returnProps = entityProperties.intersect(properties.toSet()) // 取交集,保证要查询的列一定存在
        return ColumnHelper.columnOf(table(), *returnProps.toTypedArray())
    }

}


// 解决ktorm总是要调用到可变参数的inList方法的问题及泛型问题
@Suppress("UNCHECKED_CAST")
private fun <T : Any> ColumnDeclaring<T>.inCollection(list: Collection<*>): InListExpression {
    return InListExpression(left = asExpression(), values = list.map { wrapArgument(it as T?) })
}