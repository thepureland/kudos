package io.kudos.base.support.service.impl

import io.kudos.base.bean.BeanKit
import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.error.ServiceException
import io.kudos.base.lang.GenericKit
import io.kudos.base.model.contract.common.IHasBuiltIn
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.model.payload.ISearchPayload
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.model.payload.MutableListSearchPayload
import io.kudos.base.model.payload.UpdatePayload
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.dao.IBaseCrudDao
import io.kudos.base.support.service.iservice.IBaseCrudService
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Basic writable business operations: delegates [IBaseCrudService] to [IBaseCrudDao]; depends only on the DAO contract in the base module, with no binding to Ktorm.
 * Transactions are declared by containers such as Spring on concrete business Service implementation classes using `@Transactional`; this module does not depend on Spring.
 *
 * Deletion notes: when entity type [E] implements [IHasBuiltIn], all deletion entry points only allow deletion of rows where `builtIn == false`;
 * if a caller attempts to delete a built-in row (`builtIn == true`), a [ServiceException] ([CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE]) is thrown.
 * The implementation appends the `builtIn` condition at the Service layer and delegates to the existing DAO without modifying it; during validation, it tries to query only the `builtIn` column (or `id` + `builtIn`) to avoid full-row `get` calls.
 *
 * @param PK Entity primary key type
 * @param E Entity type; must implement [IIdEntity]
 * @param DAO Writable DAO implementation type (must also provide read-only capabilities; see [IBaseCrudDao] extending [io.kudos.base.support.dao.IBaseReadOnlyDao])
 * @author K
 * @since 1.0.0
 */
open class BaseCrudService<PK : Any, E : IIdEntity<PK>, DAO : IBaseCrudDao<PK, E>>(dao: DAO) :
    BaseReadOnlyService<PK, E, DAO>(dao), IBaseCrudService<PK, E> {

    override fun insert(any: Any): PK = dao.insert(any)

    override fun insertOnly(entity: E, vararg propertyNames: String): PK = dao.insertOnly(entity, *propertyNames)

    override fun insertExclude(entity: E, vararg excludePropertyNames: String): PK =
        dao.insertExclude(entity, *excludePropertyNames)

    override fun batchInsert(objects: Collection<Any>, countOfEachBatch: Int): Int =
        dao.batchInsert(objects, countOfEachBatch)

    override fun batchInsertOnly(entities: Collection<E>, countOfEachBatch: Int, vararg propertyNames: String): Int =
        dao.batchInsertOnly(entities, countOfEachBatch, *propertyNames)

    override fun batchInsertExclude(
        entities: Collection<E>, countOfEachBatch: Int, vararg excludePropertyNames: String
    ): Int = dao.batchInsertExclude(entities, countOfEachBatch, *excludePropertyNames)

    override fun update(any: Any): Boolean = dao.update(any)

    override fun updateWhen(entity: E, criteria: Criteria): Boolean = dao.updateWhen(entity, criteria)

    override fun updateProperties(id: PK, properties: Map<String, *>): Boolean =
        dao.updateProperties(id, properties)

    override fun updatePropertiesWhen(id: PK, properties: Map<String, *>, criteria: Criteria): Boolean =
        dao.updatePropertiesWhen(id, properties, criteria)

    override fun updateOnly(entity: E, vararg propertyNames: String): Boolean = dao.updateOnly(entity, *propertyNames)

    override fun updateOnlyWhen(entity: E, criteria: Criteria, vararg propertyNames: String): Boolean =
        dao.updateOnlyWhen(entity, criteria, *propertyNames)

    override fun updateExcludePropertiesWhen(
        entity: E, criteria: Criteria, vararg excludePropertyNames: String
    ): Boolean = dao.updateExcludePropertiesWhen(entity, criteria, *excludePropertyNames)

    override fun batchUpdate(entities: Collection<E>, countOfEachBatch: Int): Int =
        dao.batchUpdate(entities, countOfEachBatch)

    override fun batchUpdateWhen(entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int): Int =
        dao.batchUpdateWhen(entities, criteria, countOfEachBatch)

    override fun <S : ISearchPayload> batchUpdateWhen(updatePayload: UpdatePayload<S>): Int =
        dao.batchUpdateWhen(updatePayload)

    override fun updateExcludeProperties(entity: E, vararg excludePropertyNames: String): Boolean =
        dao.updateExcludeProperties(entity, *excludePropertyNames)

    override fun batchUpdateProperties(criteria: Criteria, properties: Map<String, *>): Int =
        dao.batchUpdateProperties(criteria, properties)

    override fun batchUpdateOnly(entities: Collection<E>, countOfEachBatch: Int, vararg propertyNames: String): Int =
        dao.batchUpdateOnly(entities, countOfEachBatch, *propertyNames)

    override fun batchUpdateOnlyWhen(
        entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int, vararg propertyNames: String
    ): Int = dao.batchUpdateOnlyWhen(entities, criteria, countOfEachBatch, *propertyNames)

    override fun batchUpdateExcludeProperties(
        entities: Collection<E>, countOfEachBatch: Int, vararg excludePropertyNames: String
    ): Int = dao.batchUpdateExcludeProperties(entities, countOfEachBatch, *excludePropertyNames)

    override fun batchUpdateExcludePropertiesWhen(
        entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int, vararg excludePropertyNames: String
    ): Int = dao.batchUpdateExcludePropertiesWhen(entities, criteria, countOfEachBatch, *excludePropertyNames)

    //region Delete (with IHasBuiltIn built-in row protection; implemented only at the Service layer, without modifying the DAO)

    /**
     * The second generic parameter of [BaseCrudService] on the current concrete Service subclass, i.e. the entity type [E].
     * Used to determine whether it implements [IHasBuiltIn] and to resolve the property reflection corresponding to the [builtIn] column.
     */
    @Suppress("UNCHECKED_CAST")
    private val entityClass: KClass<E> by lazy {
        GenericKit.getSuperClassGenricClass(this::class, 1) as KClass<E>
    }

    /**
     * When true, [E] carries the built-in marker field, and deletion methods take the "only builtIn == false can be deleted" branch; otherwise, the behavior is consistent with the native [dao].
     */
    private val hasBuiltInField: Boolean by lazy {
        IHasBuiltIn::class.java.isAssignableFrom(entityClass.java)
    }

    /**
     * The property on the entity with the same name as [IHasBuiltIn.builtIn], used by [searchProperty] / [searchProperties] to pull only the boolean column for failure-reason judgment.
     */
    @Suppress("UNCHECKED_CAST")
    private val builtInProperty: KProperty1<E, Boolean?> by lazy {
        entityClass.memberProperties.first { it.name == IHasBuiltIn::builtIn.name } as KProperty1<E, Boolean?>
    }

    /** Entity primary key property name (taken from [IIdEntity.id]; avoids hard-coding strings when constructing conditions). */
    private val idPropertyName: String
        get() = IIdEntity<PK>::id.name

    /** Built-in marker property name (taken from [IHasBuiltIn.builtIn]; avoids hard-coding strings when constructing conditions). */
    private val builtInPropertyName: String
        get() = IHasBuiltIn::builtIn.name

    /** Excludes built-in rows from deletion: equivalent to the SQL condition `built_in = false` (the property name follows the entity). */
    private fun builtInFalseCriteria(): Criteria =
        Criteria.of(builtInPropertyName, OperatorEnum.EQ, false)

    /** Used to determine "whether built-in rows exist under the given business conditions" (probe only; does not delete data). */
    private fun builtInTrueCriteria(): Criteria =
        Criteria.of(builtInPropertyName, OperatorEnum.EQ, true)

    /**
     * Uniformly throws the business exception "built-in row is not deletable".
     * Declaring the return type as [Nothing] allows the type inference at the call site to retain the concrete type from the other branch of if/else.
     *
     * @throws ServiceException Always thrown; errorCode is [CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE]
     * @author K
     * @since 1.0.0
     */
    private fun throwBuiltInNotDeletable(): Nothing =
        throw ServiceException(CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE)

    /**
     * Copies the WHERE/pagination semantics of [ListSearchPayload], and restricts SELECT to only the primary key and [builtIn] via [MutableListSearchPayload.setReturnProperties].
     * Used by [batchDeleteWhenForBuiltInEntity] to avoid loading the entire PO while staying aligned with the original payload behavior.
     */
    private fun toIdBuiltInProbePayload(source: ListSearchPayload): MutableListSearchPayload {
        val probe = MutableListSearchPayload()
        BeanKit.copyProperties(source, probe)
        probe.setReturnProperties(listOf(idPropertyName, builtInPropertyName))
        return probe
    }

    /**
     * Deletes by primary key (built-in table): `WHERE id = ? AND builtIn = false`.
     * - Affects 1 row: success.
     * - Affects 0 rows: queries only the [builtIn] value for that primary key; if it is true, throws built-in-not-deletable; otherwise, treats the record as nonexistent and returns false.
     */
    private fun deleteByIdForBuiltInEntity(id: PK): Boolean {
        val deleteCriteria = Criteria(idPropertyName, OperatorEnum.EQ, id).addAnd(builtInFalseCriteria())
        if (dao.batchDeleteCriteria(deleteCriteria) == 1) {
            return true
        }
        val builtInValues = searchProperty(Criteria(idPropertyName, OperatorEnum.EQ, id), builtInProperty)
        if (builtInValues.isNotEmpty() && builtInValues.first() == true) {
            throwBuiltInNotDeletable()
        }
        return false
    }

    /**
     * Deletes by a collection of primary keys (built-in table): `WHERE id IN (...) AND builtIn = false` (the input is first [distinct]).
     * If the number of deleted rows is less than the number of distinct primary keys, [inSearchPropertiesById] is called once to fetch `id` + `builtIn`:
     * if there are still primary keys with `builtIn == true`, an exception is thrown; otherwise, this is interpreted as some primary keys simply not existing, and the actual number of deleted rows is returned.
     *
     * Note: when mixed with built-in rows, non-built-in rows may already have been deleted in the same DELETE before the error is thrown; the caller must accept this behavior under transactional semantics.
     */
    private fun batchDeleteForBuiltInEntity(ids: Collection<PK>): Int {
        require(ids.isNotEmpty()) { "When batch-deleting entity objects, the primary key collection must not be empty!" }
        val distinctIds = ids.distinct()
        val deleteCriteria =
            Criteria(idPropertyName, OperatorEnum.IN, distinctIds.toList()).addAnd(builtInFalseCriteria())
        val deleted = dao.batchDeleteCriteria(deleteCriteria)
        if (deleted == distinctIds.size) {
            return deleted
        }
        val rows = inSearchPropertiesById(distinctIds, listOf(idPropertyName, builtInPropertyName))
        if (rows.any { (it[builtInPropertyName] as? Boolean) == true }) {
            throwBuiltInNotDeletable()
        }
        return deleted
    }

    /**
     * Deletes by [Criteria] (built-in table): ANDs `builtIn = false` onto the original conditions.
     * If 0 rows are deleted, then uses "original conditions AND builtIn = true" to SELECT only the [builtIn] column as a probe:
     * any hit indicates that built-in rows exist in the range and deletion is not allowed under the current semantics, so an exception is thrown; no hit returns 0 (no match).
     */
    private fun batchDeleteCriteriaForBuiltInEntity(criteria: Criteria): Int {
        require(!criteria.isEmpty()) { "When batch-deleting entity objects, the query conditions must not be empty!" }
        val safeCriteria = Criteria().addAnd(criteria).addAnd(builtInFalseCriteria())
        val deleted = dao.batchDeleteCriteria(safeCriteria)
        if (deleted > 0) {
            return deleted
        }
        val probeCriteria = Criteria().addAnd(criteria).addAnd(builtInTrueCriteria())
        val builtInProbe = searchProperties(probeCriteria, listOf(builtInProperty))
        if (builtInProbe.isNotEmpty()) {
            throwBuiltInNotDeletable()
        }
        return deleted
    }

    /**
     * Deletes by search payload (built-in table). Without modifying the DAO, expressions cannot be appended to `batchDeleteWhen`, so [searchPayload] is required to be a [ListSearchPayload].
     *
     * Flow: copy the payload into a probe query that returns only `id` + [builtIn] -> throw an exception if the results contain a built-in row -> otherwise call [batchDeleteForBuiltInEntity] for the filtered primary keys.
     */
    private fun batchDeleteWhenForBuiltInEntity(searchPayload: ISearchPayload): Int {
        require(searchPayload is ListSearchPayload) {
            "When an entity implementing IHasBuiltIn executes batchDeleteWhen, searchPayload must be a ListSearchPayload."
        }
        val probePayload = toIdBuiltInProbePayload(searchPayload)
        @Suppress("UNCHECKED_CAST")
        val rows = dao.search(probePayload) as List<Map<String, Any?>>
        if (rows.any { (it[builtInPropertyName] as? Boolean) == true }) {
            throwBuiltInNotDeletable()
        }
        @Suppress("UNCHECKED_CAST")
        val ids = rows.mapNotNull { @Suppress("UNCHECKED_CAST") it[idPropertyName] as? PK }
        if (ids.isEmpty()) {
            return 0
        }
        return batchDeleteForBuiltInEntity(ids)
    }

    /** Deletes by primary key; if [E] implements [IHasBuiltIn], only non-built-in rows are deleted. */
    override fun deleteById(id: PK): Boolean =
        if (!hasBuiltInField) dao.deleteById(id) else deleteByIdForBuiltInEntity(id)

    /** Batch-deletes by primary key; if [E] implements [IHasBuiltIn], only non-built-in rows are deleted. */
    override fun batchDelete(ids: Collection<PK>): Int =
        if (!hasBuiltInField) dao.batchDelete(ids) else batchDeleteForBuiltInEntity(ids)

    /** Batch-deletes by condition; if [E] implements [IHasBuiltIn], `builtIn = false` is automatically appended. */
    override fun batchDeleteCriteria(criteria: Criteria): Int =
        if (!hasBuiltInField) dao.batchDeleteCriteria(criteria) else batchDeleteCriteriaForBuiltInEntity(criteria)

    /**
     * Batch-deletes by search payload; if [E] implements [IHasBuiltIn], [searchPayload] must be a [ListSearchPayload] (see [batchDeleteWhenForBuiltInEntity]).
     */
    override fun batchDeleteWhen(searchPayload: ISearchPayload): Int =
        if (!hasBuiltInField) dao.batchDeleteWhen(searchPayload) else batchDeleteWhenForBuiltInEntity(searchPayload)

    /** Deletes the row corresponding to the entity; for a built-in table, this is equivalent to [deleteById] (with builtIn in the database as the source of truth). */
    override fun delete(entity: E): Boolean =
        if (!hasBuiltInField) dao.delete(entity) else deleteByIdForBuiltInEntity(entity.id)

    //endregion Delete

}
