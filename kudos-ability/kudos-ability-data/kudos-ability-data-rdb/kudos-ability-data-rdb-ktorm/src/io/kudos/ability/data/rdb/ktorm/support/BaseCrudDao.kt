package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.ability.data.rdb.ktorm.rowscope.RowScopeEnforcer
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
import kotlin.reflect.full.isSubclassOf

/**
 * Base data access object that encapsulates common operations for a database table.
 *
 * @param PK entity primary key type
 * @param E entity type
 * @param T database table-entity association type
 * @author K
 * @author AI: Codex
 * @author AI: Claude
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
            val entityClass = requireNotNull(table().entityClass) { "Table has no bound entity type; cannot create entity instance." }
            val entity = Entity.create(entityClass)
            BeanKit.copyProperties(any, entity)
            entity
        } as E
        // Auto-fill audit columns (createTime / createUserId / updateTime / updateUserId) when the
        // entity is auditable — runs before the property-key snapshot below so the audit fields
        // become part of the INSERT statement.
        setInsertDefault(entity)
        val idPropName = getPkColumn().name.underscoreToHump()
        // When the user has explicitly set id on the entity, insert with that value (supports non-auto-increment
        // primary keys such as string/uuid or pre-generated ints); otherwise exclude id so the DB auto-increments.
        // Ktorm Entity.properties only records a key after its setter has been invoked.
        val id = if (entity.properties.containsKey(idPropName)) {
            insertOnly(entity, *entity.properties.keys.toTypedArray())
        } else {
            insertExclude(entity, idPropName)
        }
        // Write back via the entity property name corresponding to the PK column to avoid triggering the interface's
        // default setId through entity.id under Ktorm's proxy, which can throw IllegalStateException (e.g. SysSystem.id delegates to code).
        BeanKit.setProperty(entity, idPropName, id)
        return id
    }

    @Suppress("UNCHECKED_CAST")
    override fun insertOnly(entity: E, vararg propertyNames: String): PK {
        val properties = entity.properties
        val columns = ColumnHelper.columnOf(table(), *propertyNames)
        val assignments = linkedMapOf<Column<Any>, Any?>()
        columns.forEach { (propertyName, column) -> assignments[column] = properties[propertyName] }
        // A row must not be created outside the scope of whoever creates it; unset scope columns are
        // filled from the subject when that is unambiguous. See RowScopeWriteValidator.
        applyInsertScope(assignments)
        return database().insertAndGenerateKey(table()) {
            assignments.forEach { (column, value) -> set(column, value) }
        } as PK
    }

    /**
     * Applies the row-scope decision to one row's assignments: refuses values outside the subject's
     * scope, and fills scope columns the caller left unset.
     *
     * Takes the assignments already keyed by [Column] so the check sees exactly what will be
     * written — a column the caller excluded from the statement is genuinely unset, whatever the
     * in-memory entity happens to hold for it.
     */
    private fun applyInsertScope(assignments: MutableMap<Column<Any>, Any?>) {
        val enforcer = RowScopeEnforcer.current ?: return
        val byName = assignments.entries.associate { (column, value) -> column.name to value }
        val defaults = enforcer.defaultsForInsert(table(), entityClass(), byName)
        defaults.forEach { (columnName, value) ->
            val column = columnByName(columnName)
            // Only where the caller supplied nothing: a value they did set has already been checked.
            if (assignments[column] == null) assignments[column] = value
        }
    }

    /**
     * Refuses an update whose assignments would move the row out of the subject's own scope.
     *
     * @param properties property name → value being assigned
     * @param columnMap property name → column, as already resolved by the caller
     */
    private fun checkUpdateScope(properties: Map<String, Any?>, columnMap: Map<String, Column<Any>>) {
        val enforcer = RowScopeEnforcer.current ?: return
        val byName = properties.mapNotNull { (name, value) ->
            columnMap[name]?.let { it.name to value }
        }.toMap()
        enforcer.checkUpdate(table(), entityClass(), byName)
    }

    /** Resolves a scope-declared column name against the table, failing loudly if it does not exist. */
    @Suppress("UNCHECKED_CAST")
    private fun columnByName(columnName: String): Column<Any> =
        table().columns.firstOrNull { it.name.equals(columnName, ignoreCase = true) } as Column<Any>?
            ?: error(
                "Row scope declares column [$columnName] on entity [${entityClass().simpleName}], " +
                    "which table [${table().tableName}] does not have."
            )

    override fun insertExclude(entity: E, vararg excludePropertyNames: String): PK {
        val onlyProperties = entity.properties.keys.filter { !excludePropertyNames.contains(it) }
        return insertOnly(entity, *onlyProperties.toTypedArray())
    }

    @Suppress("UNCHECKED_CAST")
    override fun batchInsert(objects: Collection<Any>, countOfEachBatch: Int): Int {
        if (objects.isEmpty()) return 0
        return if (objects.first() is IDbEntity<*, *>) {
            // Fill audit defaults before snapshotting the property keys; otherwise the auto-filled
            // columns would be missing from the INSERT column list.
            (objects as Collection<E>).forEach { setInsertDefault(it) }
            val columns = uniformPropertyKeys(objects, "batchInsert")
            batchInsertOnly(objects, countOfEachBatch, *columns.toTypedArray())
        } else {
            val propertyNames = getEntityProperties()
            val columnMap = ColumnHelper.columnOf(table(), *propertyNames.toTypedArray())
            var totalCount = 0
            GroupExecutor(objects, countOfEachBatch) {
                val counts = database().batchInsert(table()) {
                    it.forEach { insertPayload ->
                        item {
                            val propMap = BeanKit.extract(insertPayload).toMutableMap()
                            // Plain-bean path: inject audit defaults into the property map so even
                            // DTO-style payloads end up with consistent createTime / createUserId.
                            setInsertDefault(propMap)
                            val assignments = linkedMapOf<Column<Any>, Any?>()
                            for ((name, value) in propMap) {
                                if (name in propertyNames) {
                                    val column = requireNotNull(columnMap[name]) { "No database column found for property [$name]." }
                                    assignments[column] = value
                                }
                            }
                            // A DTO can name a tenant just as freely as an entity can.
                            applyInsertScope(assignments)
                            assignments.forEach { (column, value) -> set(column, value) }
                        }
                    }
                }
                totalCount += counts.sum()
            }.execute()
            totalCount
        }
    }

    override fun batchInsertOnly(entities: Collection<E>, countOfEachBatch: Int, vararg propertyNames: String): Int {
        // Auto-fill audit defaults on each entity. propertyNames passed in by the caller stays
        // authoritative — if the caller intentionally excluded audit columns from the SQL, we still
        // fill the in-memory entity but the columns just aren't written. That mirrors the
        // pre-existing semantics of "caller picks the column list".
        entities.forEach { setInsertDefault(it) }
        val columnMap = ColumnHelper.columnOf(table(), *propertyNames)
        var totalCount = 0
        GroupExecutor(entities, countOfEachBatch) {
            val counts = database().batchInsert(table()) {
                it.forEach { entity ->
                    item {
                        val assignments = linkedMapOf<Column<Any>, Any?>()
                        for ((name, value) in entity.properties) {
                            if (name in propertyNames) {
                                val column = requireNotNull(columnMap[name]) { "No database column found for property [$name]." }
                                assignments[column] = value
                            }
                        }
                        // Per row, because each carries its own values — but the resulting column
                        // set stays identical across the batch: a scope column is either in
                        // `propertyNames` for every row or absent for every row, and a fill only
                        // supplies a value for a column already in that set.
                        applyInsertScope(assignments)
                        assignments.forEach { (column, value) -> set(column, value) }
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
        if (entities.isEmpty()) return 0
        entities.forEach { setInsertDefault(it) }
        val onlyPropertyNames = uniformPropertyKeys(entities, "batchInsertExclude")
            .filter { it !in excludePropertyNames }
        return batchInsertOnly(entities, countOfEachBatch, *onlyPropertyNames.toTypedArray())
    }

    //endregion Insert


    //region Update

    @Suppress("UNCHECKED_CAST")
    override fun update(any: Any): Boolean {
        return if (any is IDbEntity<*, *>) {
            val entity = any as E
            setDefault(entity)
            // A row the subject cannot see must not be a row they can change. Ktorm's
            // EntitySequence.update() writes `WHERE <pk> = ?` from the entity's identity and cannot
            // be told about the scope — it rejects a filtered sequence outright — so a scoped update
            // goes down the criteria path, which ANDs the scope in. With no scope active the ktorm
            // call is kept verbatim: it also attaches the entity and discards its change flags,
            // behaviour callers may depend on.
            if (rowScopeExpr() == null) {
                // No row filter applies, but the entity may still carry scope columns whose values
                // need checking — an unrestricted-on-reads subject is not the same as one allowed to
                // write anywhere, and `scope.self` grants read access without granting either.
                checkUpdateScope(entity.properties, ColumnHelper.columnOf(table(), *entity.properties.keys.toTypedArray()))
                entitySequence().update(entity) == 1
            } else {
                updateExcludeProperties(entity)
            }
        } else {
            val entityClass = requireNotNull(table().entityClass) { "Table has no bound entity type; cannot create entity instance." }
            val entity = Entity.create(entityClass)
            BeanKit.copyProperties(any, entity)
            setDefault(entity as E)
            this.update(entity)
        }
    }

    override fun updateWhen(entity: E, criteria: Criteria): Boolean {
        require(!criteria.isEmpty()) { "Conditional entity update requires a non-empty query criteria!" }
        setDefault(entity)
        return updateByCriteria(entity.id, entity.properties, criteria)
    }

    /**
     * Delegates to [updateByCriteria] with no extra condition rather than building its own
     * `WHERE <pk> = ?`. The hand-rolled version silently skipped the row scope — an id the subject
     * cannot read was still an id they could write — and resolved a column for every key in the map
     * including `id`, so passing the id along with the values it accompanies threw.
     */
    override fun updateProperties(id: PK, properties: Map<String, *>): Boolean =
        updateByCriteria(id, properties, null)

    override fun updatePropertiesWhen(id: PK, properties: Map<String, *>, criteria: Criteria): Boolean {
        require(!criteria.isEmpty()) { "Conditional entity update requires a non-empty query criteria!" }
        return updateByCriteria(id, properties, criteria)
    }

    override fun updateOnly(entity: E, vararg propertyNames: String): Boolean {
        val properties = entity.properties.filter { it.key in propertyNames }
        return updateByCriteria(entity.id, properties, null)
    }

    override fun updateOnlyWhen(entity: E, criteria: Criteria, vararg propertyNames: String): Boolean {
        require(!criteria.isEmpty()) { "Conditional entity update requires a non-empty query criteria!" }
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
        require(!criteria.isEmpty()) { "Conditional entity update requires a non-empty query criteria!" }
        val properties = entity.properties.filter { it.key !in excludePropertyNames }
        return updateByCriteria(entity.id, properties, criteria)
    }

    override fun batchUpdate(entities: Collection<E>, countOfEachBatch: Int): Int {
        return batchUpdateByCriteria(entities, countOfEachBatch, null)
    }

    override fun batchUpdateWhen(entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int): Int {
        require(!criteria.isEmpty()) { "Batch entity update requires a non-empty query criteria!" }
        return batchUpdateByCriteria(entities, countOfEachBatch, criteria)
    }

    override fun <S : ISearchPayload> batchUpdateWhen(updatePayload: UpdatePayload<S>): Int {
        return batchUpdateWhen(updatePayload, null)
    }

    /**
     * Conditional batch update of the specified properties.
     * For update rules see @see UpdatePayload, for query rules see @see SearchPayload.
     *
     * When the query logic for the same property is specified in both updatePayload.searchPayload and whereConditionFactory, whereConditionFactory takes precedence!
     *
     * @param S search payload type
     * @param updatePayload update payload; when whereConditionFactory is null, updatePayload.searchPayload must not be null. When updatePayload.searchPayload is null, the inter-condition logic is AND.
     * @param whereConditionFactory factory function for where expressions; can define operators on items of updatePayload.searchPayload or fully customize the query logic. When the function returns null, items of updatePayload.searchPayload are treated as "equals". When this parameter is null, updatePayload.searchPayload must be specified; defaults to null.
     * @return number of updated records
     * @throws IllegalArgumentException when no query criteria is supplied
     * @author K
     * @author AI: Claude
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
        checkUpdateScope(updatePropertyMap, updateColumnMap)
        val andOr = searchPayload?.getAndOr() ?: AndOrEnum.AND
        val whereExpression = processWhere(wherePropertyMap, andOr, true, whereConditionFactory)
        whereExpression ?: throw IllegalArgumentException("Unconditional database table update is not allowed!")
        // Payload-driven updates are scoped like every other write: the payload decides which rows
        // the caller asked for, the scope decides which of those they are allowed to touch.
        val scopedExpression = withRowScope(whereExpression)

        return database().batchUpdate(table()) {
            item {
                updatePropertyMap.forEach { (name, value) ->
                    val column = requireNotNull(updateColumnMap[name]) { "No database column found for property [$name]." }
                    set(column, value)
                }
                where {
                    scopedExpression
                }
            }
        }.sum()
    }

    override fun batchUpdateProperties(criteria: Criteria, properties: Map<String, *>): Int {
        require(properties.isNotEmpty()) { "The property map to update is not specified!" }
        require(!criteria.isEmpty()) { "Batch entity update requires a non-empty query criteria!" }

        val props = properties.toMutableMap()
        setDefault(props)
        val whereExpression = withRowScope(CriteriaConverter.convert(criteria, table()))
        val columnMap = ColumnHelper.columnOf(table(), *props.keys.toTypedArray())
        checkUpdateScope(props, columnMap)
        return database().batchUpdate(table()) {
            item {
                props.forEach { (name, value) ->
                    val column = requireNotNull(columnMap[name]) { "No database column found for property [$name]." }
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
        require(!criteria.isEmpty()) { "Batch entity update requires a non-empty query criteria!" }
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
        require(!criteria.isEmpty()) { "Conditional batch entity update requires a non-empty query criteria!" }
        return batchUpdateByCriteria(entities, countOfEachBatch, criteria, true, *excludePropertyNames)
    }

    //endregion Update


    //region Delete

    override fun deleteById(id: PK): Boolean {
        // When a Ktorm entity has no id assigned, its proxy returns a `java.lang.Object` placeholder
        // on the non-null PK property rather than throwing; guard by supported PK types here to avoid
        // a downstream ClassCastException when binding parameters.
        check(id is String || id is Int || id is Long) { "Unsupported primary key type [${id::class}]" }
        val count = entitySequence().removeIf { withRowScope(getPkColumn() eq id) }
        return count == 1
    }

    override fun delete(entity: E): Boolean {
        return deleteById(entity.id)
    }

    override fun batchDelete(ids: Collection<PK>): Int {
        require(!ids.isEmpty()) { "Batch entity delete requires a non-empty primary key collection!" }
        val criteria = Criteria.of(IDbEntity<PK, E>::id.name, OperatorEnum.IN, ids.toList())
        return entitySequence().removeIf { withRowScope(CriteriaConverter.convert(criteria, table())) }
    }

    override fun batchDeleteCriteria(criteria: Criteria): Int {
        require(!criteria.isEmpty()) { "Batch entity delete requires a non-empty query criteria!" }
        return entitySequence().removeIf { withRowScope(CriteriaConverter.convert(criteria, table())) }
    }

    override fun batchDeleteWhen(searchPayload: ISearchPayload): Int {
        return batchDeleteWhen(searchPayload, null)
    }

    /**
     * Batch delete entities that match the given criteria.
     *
     * When the query logic for the same property is specified in both listSearchPayload and whereConditionFactory, whereConditionFactory takes precedence!
     *
     * @param searchPayload search payload; when null, whereConditionFactory must be specified and the inter-condition logic is AND; defaults to null
     * @param whereConditionFactory factory function for where expressions; can define operators on items of searchPayload or fully customize the query logic. When the function returns null, items of searchPayload are treated as "equals". When this parameter is null, searchPayload must be specified; defaults to null
     * @return number of deleted records
     * @throws IllegalArgumentException when no query criteria is supplied
     * @author K
     * @author AI: Claude
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
        whereExpression ?: throw IllegalArgumentException("Unconditional database table delete is not allowed!")
        return entitySequence().removeIf { withRowScope(whereExpression) }
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
     * @author AI: Claude
     * @since 1.0.0
     */
    private fun updateByCriteria(id: PK?, propertyMap: Map<String, *>, criteria: Criteria?): Boolean {
        require(id != null) { "Database entity primary key must not be null on update operations!" }
        val props = propertyMap.toMutableMap()
        setDefault(props)
        val propertyNames = props.keys.toTypedArray()
        val columnMap = ColumnHelper.columnOf(table(), *propertyNames)
        // The scoped WHERE below proves the caller may touch this row; it says nothing about what
        // they may turn it into. Without this, a visible row can be assigned to another tenant.
        checkUpdateScope(props, columnMap)
        return database().update(table()) {
            props.filter { it.key != IDbEntity<PK, E>::id.name }.forEach { (name, value) ->
                val column = requireNotNull(columnMap[name]) { "No database column found for property [$name]." }
                set(column, value)
            }
            where {
                var whereExpression = getPkColumn() eq id
                criteria?.let { whereExpression = whereExpression.and(CriteriaConverter.convert(it, table())) }
                // Update-by-id is scoped too: without it the row a subject cannot see is still one
                // they can change, which is not a boundary.
                withRowScope(whereExpression)
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
     * @author AI: Claude
     * @since 1.0.0
     */
    private fun batchUpdateByCriteria(
        entities: Collection<E>,
        countOfEachBatch: Int,
        criteria: Criteria?,
        exclude: Boolean = false,
        vararg propertyNames: String = emptyArray()
    ): Int {
        require(entities.isNotEmpty()) { "Entity collection argument must not be empty!" }
        var totalCount = 0
        entities.forEach { setDefault(it) }
        val sharedKeys = uniformPropertyKeys(entities, "batchUpdate")
        var columnMap = ColumnHelper.columnOf(table(), *sharedKeys.toTypedArray())
        if (propertyNames.isNotEmpty()) {
            columnMap = if (exclude) {
                columnMap.filter { it.key !in propertyNames }
            } else {
                columnMap.filter { it.key in propertyNames }
            }
        }
        val criteriaExpression = criteria?.let { withRowScope(CriteriaConverter.convert(it, table())) } ?: rowScopeExpr()
        entities.forEach { entity -> checkUpdateScope(entity.properties, columnMap) }
        GroupExecutor(entities, countOfEachBatch) { it ->
            val counts = database().batchUpdate(table()) {
                for (entity in it) {
                    item {
                        entity.properties.filter { it.key != IDbEntity<PK, E>::id.name }.forEach { (name, value) ->
                            if (columnMap.containsKey(name)) {
                                val column = requireNotNull(columnMap[name]) { "No database column found for property [$name]." }
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
     * The property names every entity in [entities] sets, rejecting a batch whose entities disagree.
     *
     * A batch becomes **one** prepared statement, so its column list has to be the same for every
     * row. Deriving that list from the first entity and filtering the rest against it — what this
     * used to do — turns a disagreement into one of two bad outcomes: a property only later entities
     * set is silently dropped on the floor, and a property only earlier entities set makes ktorm
     * reject the batch with a message about SQL text that names neither the entity nor the column.
     *
     * Refusing up front says which properties differ. A caller who genuinely wants a fixed column
     * list already has [batchInsertOnly] / [batchUpdateOnly] and the `…Exclude` variants; a caller
     * whose entities merely happen to differ wants to know.
     *
     * @param entities the batch, after audit defaults have been filled
     * @param operation the calling operation, named in the error
     * @return the property names shared by all of them
     * @throws IllegalStateException when two entities set different properties
     * @author K
     * @author AI: Claude
     * @since 1.0.0
     */
    private fun uniformPropertyKeys(entities: Collection<E>, operation: String): Set<String> {
        val expected = entities.first().properties.keys
        val divergent = entities.asSequence().drop(1).firstOrNull { it.properties.keys != expected }
            ?: return expected
        val actual = divergent.properties.keys
        error(
            "$operation was given entities that do not all set the same properties, and a batch has " +
                "to be one statement with one column list. Compared with the first entity, another " +
                "one is missing ${(expected - actual).ifEmpty { "nothing" }} and additionally sets " +
                "${(actual - expected).ifEmpty { "nothing" }}. Give every entity the same properties, " +
                "or state the columns explicitly with the ...Only / ...Exclude variants."
        )
    }

    /**
     * Set the fields that are auto-updated on update. Delegates to [AuditDefaults.fillForUpdate]
     * for testable, centralized rules; only auto-fills `updateTime` and `updateUserId`, never
     * touching creation fields.
     *
     * @param e database table entity
     */
    private fun setDefault(e: E) {
        if (e is IAuditable) {
            AuditDefaults.fillForUpdate(e)
        }
    }

    /**
     * Map variant for callers that carry update values as a map (e.g. updateProperties paths).
     * Only auto-fills when the table entity actually implements [IAuditable] — extra columns on a
     * non-auditable table would error.
     *
     * Uses [isSubclassOf] (not `superclasses.contains`) so indirect implementers — e.g. entities
     * extending `IManagedDbEntity`, which inherits [IAuditable] transitively — are detected too,
     * keeping the map path consistent with the entity path's `e is IAuditable` check.
     *
     * @param properties Map(property name, property value)
     */
    private fun setDefault(properties: MutableMap<String, Any?>) {
        if (entityClass().isSubclassOf(IAuditable::class)) {
            AuditDefaults.fillForUpdate(properties)
        }
    }

    /**
     * Set the audit fields that should be auto-populated on insert. Fills `createTime`,
     * `createUserId`, `updateTime`, `updateUserId` when they are null — matches MyBatis-Plus's
     * `INSERT` field-filling default in soul. Existing values are never overridden so data-import
     * scripts can still inject explicit timestamps.
     */
    private fun setInsertDefault(e: E) {
        if (e is IAuditable) {
            AuditDefaults.fillForInsert(e)
        }
    }

    /** Map variant of [setInsertDefault] — for the plain-bean insert path that carries values as a map. */
    private fun setInsertDefault(properties: MutableMap<String, Any?>) {
        if (entityClass().isSubclassOf(IAuditable::class)) {
            AuditDefaults.fillForInsert(properties)
        }
    }

}