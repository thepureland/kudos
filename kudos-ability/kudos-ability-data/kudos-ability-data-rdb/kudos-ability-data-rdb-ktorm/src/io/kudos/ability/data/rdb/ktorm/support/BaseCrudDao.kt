package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.bean.BeanKit
import io.kudos.base.lang.string.underscoreToHump
import io.kudos.base.model.contract.common.IAuditable
import io.kudos.base.model.payload.ISearchPayload
import io.kudos.base.model.payload.UpdatePayload
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.GroupExecutor
import io.kudos.base.support.dao.IBaseCrudDao
import io.kudos.base.support.logic.AndOrEnum
import org.ktorm.dsl.*
import org.ktorm.entity.Entity
import org.ktorm.entity.removeIf
import org.ktorm.entity.update
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.Table
import java.time.LocalDateTime
import kotlin.reflect.full.superclasses

/**
 * 基础数据访问对象，封装某数据库表的通用操作
 *
 * @param PK 实体主键类型
 * @param E 实体类型
 * @param T 数据库表-实体关联对象的类型
 * @author K
 * @since 1.0.0
 */
open class BaseCrudDao<PK : Any, E : IDbEntity<PK, E>, T : Table<E>>
    : BaseReadOnlyDao<PK, E, T>(),
    IBaseCrudDao<PK, E> {


    //region Insert

    @Suppress("UNCHECKED_CAST")
    override fun insert(any: Any): PK {
        val entity = if (any is IDbEntity<*, *>) {
            any
        } else {
            val entityClass = requireNotNull(table().entityClass) { "表未绑定实体类型，无法创建实体实例。" }
            val entity = Entity.create(entityClass)
            BeanKit.copyProperties(any, entity)
            entity
        } as E
        val idPropName = getPkColumn().name.underscoreToHump()
        // 用户在实体里显式设过 id 时按用户值插入（兼容非自增主键，如 string/uuid 或预先生成的 int）；
        // 未设时则排除 id 让 DB 自增。Ktorm Entity.properties 只在 setter 被调用过时收录该 key。
        val id = if (entity.properties.containsKey(idPropName)) {
            insertOnly(entity, *entity.properties.keys.toTypedArray())
        } else {
            insertExclude(entity, idPropName)
        }
        // 按主键列对应的实体属性名回写，避免 Ktorm 代理下通过 entity.id 触发 interface 的 default setId 导致 IllegalStateException（如 SysSystem.id 委托给 code）
        BeanKit.setProperty(entity, idPropName, id)
        return id
    }

    @Suppress("UNCHECKED_CAST")
    override fun insertOnly(entity: E, vararg propertyNames: String): PK {
        val properties = entity.properties
        val columns = ColumnHelper.columnOf(table(), *propertyNames)
        return database().insertAndGenerateKey(table()) {
            columns.forEach { (propertyName, column) ->
                set(column, properties[propertyName])
            }
        } as PK
    }

    override fun insertExclude(entity: E, vararg excludePropertyNames: String): PK {
        val onlyProperties = entity.properties.keys.filter { !excludePropertyNames.contains(it) }
        return insertOnly(entity, *onlyProperties.toTypedArray())
    }

    @Suppress("UNCHECKED_CAST")
    override fun batchInsert(objects: Collection<Any>, countOfEachBatch: Int): Int {
        if (objects.isEmpty()) return 0
        return if (objects.first() is IDbEntity<*, *>) {
            batchInsertOnly(objects as Collection<E>, countOfEachBatch, *objects.first().properties.keys.toTypedArray())
        } else {
            val propertyNames = getEntityProperties()
            val columnMap = ColumnHelper.columnOf(table(), *propertyNames.toTypedArray())
            var totalCount = 0
            GroupExecutor(objects, countOfEachBatch) {
                val counts = database().batchInsert(table()) {
                    it.forEach { insertPayload ->
                        item {
                            val propMap = BeanKit.extract(insertPayload)
                            for ((name, value) in propMap) {
                                if (name in propertyNames) {
                                    val column = requireNotNull(columnMap[name]) { "未找到属性[$name]对应的数据库列。" }
                                    set(column, value)
                                }
                            }
                        }
                    }
                }
                totalCount += counts.sum()
            }.execute()
            totalCount
        }
    }

    override fun batchInsertOnly(entities: Collection<E>, countOfEachBatch: Int, vararg propertyNames: String): Int {
        val columnMap = ColumnHelper.columnOf(table(), *propertyNames)
        var totalCount = 0
        GroupExecutor(entities, countOfEachBatch) {
            val counts = database().batchInsert(table()) {
                it.forEach { entity ->
                    item {
                        for ((name, value) in entity.properties) {
                            if (name in propertyNames) {
                                val column = requireNotNull(columnMap[name]) { "未找到属性[$name]对应的数据库列。" }
                                set(column, value)
                            }
                        }
                    }
                }
            }
            totalCount += counts.sum()
        }.execute()
        return totalCount
    }

    override fun batchInsertExclude(
        entities: Collection<E>, countOfEachBatch: Int, vararg excludePropertyNames: String
    ): Int {
        val onlyPropertyNames = entities.first().properties.keys.filter { it !in excludePropertyNames }
        return batchInsertOnly(entities, countOfEachBatch, *onlyPropertyNames.toTypedArray())
    }

    //endregion Insert


    //region Update

    @Suppress("UNCHECKED_CAST")
    override fun update(any: Any): Boolean {
        return if (any is IDbEntity<*, *>) {
            setDefault(any as E)
            entitySequence().update(any) == 1
        } else {
            val entityClass = requireNotNull(table().entityClass) { "表未绑定实体类型，无法创建实体实例。" }
            val entity = Entity.create(entityClass)
            BeanKit.copyProperties(any, entity)
            setDefault(entity as E)
            this.update(entity)
        }
    }

    override fun updateWhen(entity: E, criteria: Criteria): Boolean {
        require(!criteria.isEmpty()) { "有条件的更新实体对象时，查询条件不能为空！" }
        setDefault(entity)
        return updateByCriteria(entity.id, entity.properties, criteria)
    }

    override fun updateProperties(id: PK, properties: Map<String, *>): Boolean {
        val props = properties.toMutableMap()
        setDefault(props)
        val propertyNames = props.keys.filter { it != IDbEntity<PK, E>::id.name }.toTypedArray()
        val columnMap = ColumnHelper.columnOf(table(), *propertyNames)
        return database().update(table()) {
            props.forEach { (name, value) ->
                val column = requireNotNull(columnMap[name]) { "未找到属性[$name]对应的数据库列。" }
                set(column, value)
            }
            where { getPkColumn() eq id }
        } == 1
    }

    override fun updatePropertiesWhen(id: PK, properties: Map<String, *>, criteria: Criteria): Boolean {
        require(!criteria.isEmpty()) { "有条件的更新实体对象时，查询条件不能为空！" }
        return updateByCriteria(id, properties, criteria)
    }

    override fun updateOnly(entity: E, vararg propertyNames: String): Boolean {
        val properties = entity.properties.filter { it.key in propertyNames }
        return updateByCriteria(entity.id, properties, null)
    }

    override fun updateOnlyWhen(entity: E, criteria: Criteria, vararg propertyNames: String): Boolean {
        require(!criteria.isEmpty()) { "有条件的更新实体对象时，查询条件不能为空！" }
        val properties = entity.properties.filter { it.key in propertyNames }
        return updateByCriteria(entity.id, properties, criteria)
    }

    override fun updateExcludeProperties(entity: E, vararg excludePropertyNames: String): Boolean {
        val properties = entity.properties.filter { it.key !in excludePropertyNames }
        return updateByCriteria(entity.id, properties, null)
    }

    override fun updateExcludePropertiesWhen(
        entity: E,
        criteria: Criteria,
        vararg excludePropertyNames: String
    ): Boolean {
        require(!criteria.isEmpty()) { "有条件的更新实体对象时，查询条件不能为空！" }
        val properties = entity.properties.filter { it.key !in excludePropertyNames }
        return updateByCriteria(entity.id, properties, criteria)
    }

    override fun batchUpdate(entities: Collection<E>, countOfEachBatch: Int): Int {
        return batchUpdateByCriteria(entities, countOfEachBatch, null)
    }

    override fun batchUpdateWhen(entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int): Int {
        require(!criteria.isEmpty()) { "批量更新实体对象时，查询条件不能为空！" }
        return batchUpdateByCriteria(entities, countOfEachBatch, criteria)
    }

    override fun <S : ISearchPayload> batchUpdateWhen(updatePayload: UpdatePayload<S>): Int {
        return batchUpdateWhen(updatePayload, null)
    }

    /**
     * 有条件的批量更新指定属性
     * 更新规则见 @see UpdatePayload 类，查询规则见 @see SearchPayload
     *
     * 同一属性的查询逻辑在 updatePayload.searchPayload 和 whereConditionFactory 都有指定时，以 whereConditionFactory 为准！
     *
     * @param S 查询项载体类型
     * @param updatePayload 更新项载体，当 whereConditionFactory 为null时，updatePayload.searchPayload不能为null。updatePayload.searchPayload为null时，条件间的查询逻辑为AND
     * @param whereConditionFactory where条件表达式工厂函数，可对updatePayload.searchPayload的项定义操作符，也可完全自定义查询逻辑，函数返回null时将按“等于”操作处理updatePayload.searchPayload的项。该参数为null时，updatePayload.searchPayload 必须指定，默认为null
     * @return 更新的记录数
     * @throws IllegalArgumentException 无查询条件时
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    open fun <S : ISearchPayload> batchUpdateWhen(
        updatePayload: UpdatePayload<S>,
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)? = null
    ): Int {
        val searchPayload = updatePayload.searchPayload
        val wherePropertyMap = if (searchPayload == null) {
            emptyMap()
        } else {
            val entityProperties = getEntityProperties()
            getWherePropertyMap(searchPayload, entityProperties)
        }

        val updatePropertyMap = mutableMapOf<String, Any?>()
        val updatePropMap = BeanKit.extract(updatePayload).toMutableMap()
        setDefault(updatePropMap)
        updatePropMap.filter {
            it.value != null && it.key != IDbEntity<PK, E>::id.name && it.key != UpdatePayload<S>::searchPayload.name
        }.forEach { (prop, value) ->
            if (prop == UpdatePayload<S>::nullProperties.name) {
                (value as Collection<String>).forEach {
                    updatePropertyMap[it] = null
                }
            } else {
                updatePropertyMap[prop] = value
            }
        }

        val updateColumnMap = ColumnHelper.columnOf(table(), *updatePropertyMap.keys.toTypedArray())
        val andOr = searchPayload?.getAndOr() ?: AndOrEnum.AND
        val whereExpression = processWhere(wherePropertyMap, andOr, true, whereConditionFactory)
        whereExpression ?: throw IllegalArgumentException("不能做无条件的数据库表的更新操作！")

        return database().batchUpdate(table()) {
            item {
                updatePropertyMap.forEach { (name, value) ->
                    val column = requireNotNull(updateColumnMap[name]) { "未找到属性[$name]对应的数据库列。" }
                    set(column, value)
                }
                where {
                    whereExpression
                }
            }
        }.sum()
    }

    override fun batchUpdateProperties(criteria: Criteria, properties: Map<String, *>): Int {
        require(properties.isNotEmpty()) { "未指定要更新的属性Map！" }
        require(!criteria.isEmpty()) { "批量更新实体对象时，查询条件不能为空！" }

        val props = properties.toMutableMap()
        setDefault(props)
        val whereExpression = CriteriaConverter.convert(criteria, table())
        val columnMap = ColumnHelper.columnOf(table(), *props.keys.toTypedArray())
        return database().batchUpdate(table()) {
            item {
                props.forEach { (name, value) ->
                    val column = requireNotNull(columnMap[name]) { "未找到属性[$name]对应的数据库列。" }
                    set(column, value)
                }
                where { whereExpression }
            }
        }.sum()
    }

    override fun batchUpdateOnly(entities: Collection<E>, countOfEachBatch: Int, vararg propertyNames: String): Int {
        return batchUpdateByCriteria(entities, countOfEachBatch, null, false, *propertyNames)
    }

    override fun batchUpdateOnlyWhen(
        entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int, vararg propertyNames: String
    ): Int {
        require(!criteria.isEmpty()) { "批量更新实体对象时，查询条件不能为空！" }
        return batchUpdateByCriteria(entities, countOfEachBatch, criteria, false, *propertyNames)
    }

    override fun batchUpdateExcludeProperties(
        entities: Collection<E>, countOfEachBatch: Int, vararg excludePropertyNames: String
    ): Int {
        return batchUpdateByCriteria(entities, countOfEachBatch, null, true, *excludePropertyNames)
    }

    override fun batchUpdateExcludePropertiesWhen(
        entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int, vararg excludePropertyNames: String
    ): Int {
        require(!criteria.isEmpty()) { "有条件的批量更新实体对象时，查询条件不能为空！" }
        return batchUpdateByCriteria(entities, countOfEachBatch, criteria, true, *excludePropertyNames)
    }

    //endregion Update


    //region Delete

    override fun deleteById(id: PK): Boolean {
        // Ktorm 实体未给 id 赋值时，其代理在非空主键属性上返回 `java.lang.Object` 占位符
        // 而非抛错；这里先按支持的主键类型守住，避免下游绑参时降级为 ClassCastException。
        check(id is String || id is Int || id is Long) { "不支持的主键类型【${id::class}】" }
        val count = entitySequence().removeIf { getPkColumn() eq id }
        return count == 1
    }

    override fun delete(entity: E): Boolean {
        return deleteById(entity.id)
    }

    override fun batchDelete(ids: Collection<PK>): Int {
        require(!ids.isEmpty()) { "批量删除实体对象时，主键集合不能为空！" }
        val criteria = Criteria.of(IDbEntity<PK, E>::id.name, OperatorEnum.IN, ids.toList())
        return entitySequence().removeIf { CriteriaConverter.convert(criteria, table()) }
    }

    override fun batchDeleteCriteria(criteria: Criteria): Int {
        require(!criteria.isEmpty()) { "批量删除实体对象时，查询条件不能为空！" }
        return entitySequence().removeIf { CriteriaConverter.convert(criteria, table()) }
    }

    override fun batchDeleteWhen(searchPayload: ISearchPayload): Int {
        return batchDeleteWhen(searchPayload, null)
    }

    /**
     * 批量删除指定条件的实体对象
     *
     * 同一属性的查询逻辑在 listSearchPayload 和 whereConditionFactory 都有指定时，以 whereConditionFactory 为准！
     *
     * @param searchPayload 查询项载体，为null时 whereConditionFactory 必须指定，此时条件间的查询逻辑为AND，默认为null
     * @param whereConditionFactory where条件表达式工厂函数，可对searchPayload的项定义操作符，也可完全自定义查询逻辑，函数返回null时将按“等于”操作处理searchPayload的项。该参数为null时，searchPayload 必须指定，默认为null
     * @return 删除的记录数
     * @throws IllegalArgumentException 无查询条件时
     * @author K
     * @since 1.0.0
     */
    open fun batchDeleteWhen(
        searchPayload: ISearchPayload? = null,
        whereConditionFactory: ((Column<Any>, Any?) -> ColumnDeclaring<Boolean>?)? = null
    ): Int {
        val wherePropertyMap = if (searchPayload == null) {
            emptyMap()
        } else {
            val entityProperties = getEntityProperties()
            getWherePropertyMap(searchPayload, entityProperties)
        }

        val andOr = searchPayload?.getAndOr() ?: AndOrEnum.AND
        val whereExpression = processWhere(wherePropertyMap, andOr, true, whereConditionFactory)
        whereExpression ?: throw IllegalArgumentException("不能做无条件的数据库表的删除操作！")
        return entitySequence().removeIf { whereExpression }
    }

    //endregion Delete

    /**
     * 按主键 + 可选 [Criteria] 更新指定属性。
     *
     * 设计细节：
     * - id 必须非空（更新一定要锁定行），propertyMap 中的 id 字段会被显式跳过避免误改主键
     * - criteria 用于在 id 上加额外约束（乐观锁场景：where id = ? AND version = ?）
     * - 返回是否恰好更新 1 行；> 1 不会发生（id 是主键），< 1 通常是 criteria 不匹配
     *
     * @param id 主键值
     * @param propertyMap 待更新的属性 map
     * @param criteria 附加 where 条件
     * @return 是否更新成功
     * @throws IllegalArgumentException id 为 null
     * @author K
     * @since 1.0.0
     */
    private fun updateByCriteria(id: PK?, propertyMap: Map<String, *>, criteria: Criteria?): Boolean {
        require(id != null) { "更新操作时，数据库实体主键不能为空！" }
        val props = propertyMap.toMutableMap()
        setDefault(props)
        val propertyNames = props.keys.toTypedArray()
        val columnMap = ColumnHelper.columnOf(table(), *propertyNames)
        return database().update(table()) {
            props.filter { it.key != IDbEntity<PK, E>::id.name }.forEach { (name, value) ->
                val column = requireNotNull(columnMap[name]) { "未找到属性[$name]对应的数据库列。" }
                set(column, value)
            }
            where {
                var whereExpression = getPkColumn() eq id
                criteria?.let { whereExpression = whereExpression.and(CriteriaConverter.convert(it, table())) }
                whereExpression
            }
        } == 1
    }

    /**
     * 批量更新实体集合，按 [countOfEachBatch] 切片走 ktorm `batchUpdate`，减少单事务过长的风险。
     *
     * - `propertyNames` + `exclude` 一起决定"包含"/"排除"哪些列：
     *   - `exclude=false`：只更新 propertyNames 列出的字段（白名单）
     *   - `exclude=true`：propertyNames 列出的字段不更新（黑名单）
     * - id 永远不会被更新（即便误传入 propertyNames）
     *
     * @param entities 待更新实体集合（不能为空）
     * @param countOfEachBatch 每批大小，控制单 SQL 的 IN list 长度
     * @param criteria 附加 where 条件（每个实体共享同一条）
     * @param exclude 是否把 propertyNames 当排除列表
     * @param propertyNames 列名数组；空数组配合 exclude=false 表示更新所有非 id 列
     * @return 实际更新的总行数
     * @throws IllegalArgumentException entities 为空集合
     * @author K
     * @since 1.0.0
     */
    private fun batchUpdateByCriteria(
        entities: Collection<E>,
        countOfEachBatch: Int,
        criteria: Criteria?,
        exclude: Boolean = false,
        vararg propertyNames: String = emptyArray()
    ): Int {
        require(entities.isNotEmpty()) { "实体集合参数不能为空集合！" }
        var totalCount = 0
        entities.forEach { setDefault(it) }
        var columnMap = ColumnHelper.columnOf(table(), *entities.first().properties.keys.toTypedArray())
        if (propertyNames.isNotEmpty()) {
            columnMap = if (exclude) {
                columnMap.filter { it.key !in propertyNames }
            } else {
                columnMap.filter { it.key in propertyNames }
            }
        }
        val criteriaExpression = criteria?.let { CriteriaConverter.convert(it, table()) }
        GroupExecutor(entities, countOfEachBatch) { it ->
            val counts = database().batchUpdate(table()) {
                for (entity in it) {
                    item {
                        entity.properties.filter { it.key != IDbEntity<PK, E>::id.name }.forEach { (name, value) ->
                            if (columnMap.containsKey(name)) {
                                val column = requireNotNull(columnMap[name]) { "未找到属性[$name]对应的数据库列。" }
                                set(column, value)
                            }
                        }
                        where {
                            var whereExpression = getPkColumn() eq entity.id
                            criteriaExpression?.let { whereExpression = whereExpression.and(it) }
                            whereExpression
                        }
                    }
                }
            }
            totalCount += counts.sum()
        }.execute()
        return totalCount
    }

    /**
     * 设置要自动更新的字段
     *
     * @param e 数据库表实体
     */
    private fun setDefault(e: E) {
        if (e is IAuditable && e.updateTime == null) {
            e.updateTime = LocalDateTime.now()
        }
    }

    /**
     * 设置要自动更新的字段
     *
     * @param properties Map(属性名，属性值)
     */
    private fun setDefault(properties: MutableMap<String, Any?>) {
        val key = IAuditable::updateTime.name
        if (!properties.containsKey(key) && entityClass().superclasses.contains(IAuditable::class)) {
            properties[key] = LocalDateTime.now()
        }
    }

}